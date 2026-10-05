// ScamShield Whisper WebAssembly Engine
// Implements client-side local speech-to-text using whisper.cpp WebAssembly

export const WHISPER_STATES = {
  NOT_LOADED: 'NOT LOADED',
  LOADING: 'LOADING',
  READY: 'READY',
  TRANSCRIBING: 'TRANSCRIBING',
  ERROR: 'ERROR'
};

export class WhisperEngine {
  constructor(options = {}) {
    this.state = WHISPER_STATES.NOT_LOADED;
    this.modelName = options.modelName || 'tiny (multilingual, 31 MB)';
    this.modelUrl = options.modelUrl || '/whisper/ggml-tiny-q5_1.bin';
    this.instance = null;
    this.module = null;
    this.nthreads = options.nthreads || 4;

    this.onStateChange = options.onStateChange || (() => {});
    this.onProgress = options.onProgress || (() => {});
    this.onLog = options.onLog || (() => {});

    // Metrics for diagnostics
    this.latestAudioDuration = 0;
    this.latestInferenceTime = 0;
    this.latestRtf = 0;
    this.currentTranscript = '';
  }

  setState(newState, detail = '') {
    this.state = newState;
    this.onStateChange(this.state, detail);
  }

  isReady() {
    return this.state === WHISPER_STATES.READY && this.instance !== null;
  }

  /**
   * Initializes the Whisper WASM runtime and loads the local model binary.
   */
  async loadModel(url = this.modelUrl) {
    if (this.state === WHISPER_STATES.READY && this.instance) {
      return true;
    }

    this.setState(WHISPER_STATES.LOADING, 'Initializing local WebAssembly runtime...');

    try {
      // Step 1: Ensure WASM glue code is loaded
      await this.ensureWasmLoaded();

      // Step 2: Fetch local model with streaming progress
      this.setState(WHISPER_STATES.LOADING, 'Loading local model weights into memory...');
      const modelBuffer = await this.fetchModelWithProgress(url);

      // Step 3: Write model to Emscripten virtual filesystem
      const filename = 'whisper.bin';
      try {
        window.Module.FS_unlink(filename);
      } catch (e) {
        // file didn't exist yet, ignore
      }
      window.Module.FS_createDataFile('/', filename, new Uint8Array(modelBuffer), true, true);
      this.onLog(`[Whisper] Stored ${modelBuffer.byteLength} bytes into virtual FS: /${filename}`);

      // Step 4: Call C++ whisper_init
      this.setState(WHISPER_STATES.LOADING, 'Instantiating neural network graph in WASM...');
      const inst = window.Module.init(filename);
      if (!inst || inst <= 0) {
        throw new Error('Module.init returned invalid instance code: ' + inst);
      }

      this.instance = inst;
      this.setState(WHISPER_STATES.READY, 'Model ready for on-device inference');
      this.onLog(`[Whisper] Instance initialized successfully: #${this.instance}`);
      return true;
    } catch (err) {
      console.error('[WhisperEngine] Load failed:', err);
      this.setState(WHISPER_STATES.ERROR, err.message || 'Model initialization failed');
      return false;
    }
  }

  /**
   * Ensures whisper.cpp.wasm.js is loaded into the browser page.
   */
  async ensureWasmLoaded() {
    if (window.Module && window.Module.init && window.Module.full_default) {
      this.module = window.Module;
      return true;
    }

    return new Promise((resolve, reject) => {
      // Set up Module callbacks before script load
      window.Module = window.Module || {};
      window.Module.print = (text) => {
        this.onLog(`[STDOUT] ${text}`);
        this.handleStdout(text);
      };
      window.Module.printErr = (text) => {
        // Filter noisy debug logs
        if (text.includes('whisper_init') || text.includes('loading model') || text.includes('total time')) {
          this.onLog(`[STDERR] ${text}`);
        }
      };

      const existingScript = document.querySelector('script[src*="whisper.cpp.wasm.js"]');
      if (existingScript) {
        if (window.Module.calledRun || (window.Module.init && window.Module.full_default)) {
          this.module = window.Module;
          resolve(true);
        } else {
          window.Module.onRuntimeInitialized = () => {
            this.module = window.Module;
            resolve(true);
          };
        }
        return;
      }

      const script = document.createElement('script');
      script.src = '/whisper/whisper.cpp.wasm.js';
      script.async = true;

      window.Module.onRuntimeInitialized = () => {
        this.module = window.Module;
        resolve(true);
      };

      script.onerror = (e) => {
        reject(new Error('Failed to load /whisper/whisper.cpp.wasm.js'));
      };

      document.head.appendChild(script);
    });
  }

  /**
   * Fetches model arrayBuffer with real progress reporting.
   */
  async fetchModelWithProgress(url) {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch model from ${url} (HTTP ${response.status})`);
    }

    const contentLength = response.headers.get('content-length');
    const total = contentLength ? parseInt(contentLength, 10) : 0;
    const reader = response.body.getReader();

    const chunks = [];
    let received = 0;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      received += value.length;

      if (total > 0) {
        const percent = Math.min(100, Math.round((received / total) * 100));
        this.onProgress(percent);
      }
    }

    const fullBuffer = new Uint8Array(received);
    let offset = 0;
    for (const chunk of chunks) {
      fullBuffer.set(chunk, offset);
      offset += chunk.length;
    }

    return fullBuffer.buffer;
  }

  /**
   * Transcribes a Float32Array PCM buffer (16 kHz, mono).
   */
  async transcribe(pcmFloat32Array, options = {}) {
    if (!this.isReady()) {
      throw new Error('WhisperEngine is not in READY state (Current: ' + this.state + ')');
    }

    const lang = options.language || 'auto';
    const audioDuration = pcmFloat32Array.length / 16000;
    this.latestAudioDuration = audioDuration;
    this.setState(WHISPER_STATES.TRANSCRIBING, `Transcribing ${audioDuration.toFixed(1)}s segment locally...`);

    const t0 = performance.now();
    this.currentSegments = [];
    let isComplete = false;

    return new Promise((resolve, reject) => {
      // Timeout guard: 30s max
      const timeoutId = setTimeout(() => {
        if (!isComplete) {
          isComplete = true;
          this.finalizeTranscription(t0, audioDuration, resolve);
        }
      }, 30000);

      this.activeResolve = () => {
        if (!isComplete) {
          isComplete = true;
          clearTimeout(timeoutId);
          this.finalizeTranscription(t0, audioDuration, resolve);
        }
      };

      try {
        const ret = window.Module.full_default(
          this.instance,
          pcmFloat32Array,
          lang,
          this.nthreads,
          false // do not translate; preserve Hindi / Hinglish / English verbatim
        );

        if (ret !== 0) {
          clearTimeout(timeoutId);
          this.setState(WHISPER_STATES.READY);
          reject(new Error('Module.full_default failed with error code: ' + ret));
          return;
        }

        // Poll check if thread finished
        let pollCount = 0;
        const checkInterval = setInterval(() => {
          pollCount++;
          if (isComplete) {
            clearInterval(checkInterval);
          } else if (pollCount > 25) {
            // After 2.5s without timings message, resolve with collected segments
            clearInterval(checkInterval);
            this.activeResolve();
          }
        }, 100);
      } catch (err) {
        clearTimeout(timeoutId);
        this.setState(WHISPER_STATES.READY);
        reject(err);
      }
    });
  }

  handleStdout(text) {
    // 1. Check for segment text: [00:00:00.000 --> 00:00:04.200]  <recognized text>
    const match = text.match(/\[\d\d:\d\d:\d\d\.\d\d\d\s*-->\s*\d\d:\d\d:\d\d\.\d\d\d\]\s*(.*)/);
    if (match && match[1]) {
      const segment = match[1].trim();
      if (segment.length > 0 && !this.currentSegments.includes(segment)) {
        this.currentSegments.push(segment);
      }
    }

    // 2. Check for completion timing: whisper_print_timings: total time = ...
    if (text.includes('whisper_print_timings:       total time =') || text.includes('whisper_print_timings: fallbacks')) {
      if (this.activeResolve) {
        // Brief settle delay to allow all print output
        setTimeout(() => {
          if (this.activeResolve) this.activeResolve();
        }, 150);
      }
    }
  }

  finalizeTranscription(t0, audioDuration, resolve) {
    const inferenceTime = Math.max(0.05, (performance.now() - t0) / 1000);
    const rtf = audioDuration > 0 ? (inferenceTime / audioDuration) : 1.0;

    this.latestInferenceTime = inferenceTime;
    this.latestRtf = rtf;

    const transcript = this.currentSegments.join(' ').trim();
    this.currentTranscript = transcript;

    this.setState(WHISPER_STATES.READY, 'Inference finished in ' + inferenceTime.toFixed(2) + 's');

    resolve({
      transcript: transcript || '(No speech detected)',
      audioDuration,
      inferenceTime,
      realTimeFactor: rtf,
      confidence: 'Not reported' // Strictly comply with Section 10
    });
  }

  /**
   * Releases WASM instance and virtual files.
   */
  free() {
    if (this.instance && window.Module && window.Module.free) {
      try {
        window.Module.free(this.instance);
      } catch (e) {
        console.warn('Whisper free error:', e);
      }
      this.instance = null;
    }

    try {
      if (window.Module && window.Module.FS_unlink) {
        window.Module.FS_unlink('whisper.bin');
      }
    } catch (e) {}

    this.currentSegments = [];
    this.setState(WHISPER_STATES.NOT_LOADED, 'Model unloaded');
  }
}

/**
 * High-speed pure JS WAV decoder converting 16-bit PCM WAV to Float32Array [-1.0, 1.0].
 */
export function parseWavToFloat32(arrayBuffer) {
  const view = new DataView(arrayBuffer);
  let offset = 12;

  while (offset + 8 <= view.byteLength) {
    const chunkId = String.fromCharCode(
      view.getUint8(offset),
      view.getUint8(offset + 1),
      view.getUint8(offset + 2),
      view.getUint8(offset + 3)
    );
    const chunkSize = view.getUint32(offset + 4, true);

    if (chunkId === 'data') {
      const dataOffset = offset + 8;
      const numSamples = Math.floor(chunkSize / 2);
      const float32 = new Float32Array(numSamples);

      for (let i = 0; i < numSamples; i++) {
        if (dataOffset + i * 2 + 1 < view.byteLength) {
          float32[i] = view.getInt16(dataOffset + i * 2, true) / 32768.0;
        }
      }
      return float32;
    }
    offset += 8 + chunkSize;
  }

  // Fallback: treat buffer directly as 16-bit PCM if header is non-standard
  const numSamples = Math.floor(arrayBuffer.byteLength / 2);
  const float32 = new Float32Array(numSamples);
  for (let i = 0; i < numSamples; i++) {
    float32[i] = view.getInt16(i * 2, true) / 32768.0;
  }
  return float32;
}
