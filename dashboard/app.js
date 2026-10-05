// ScamShield Command Center Dashboard Controller
// Phase 4A Implementation: Real Local Whisper Speech-to-Text Integration

import { WhisperEngine, WHISPER_STATES, parseWavToFloat32 } from './whisper_engine.js';
import { SCAM_TAXONOMY, extractScamSignals } from './scam_rules.js';
import { QwenLocalAdapter, QWEN_STATES } from './qwen_adapter.js';

// Phase 5: Interactive Two-Way Conversation States
export const INTERACTIVE_STATES = {
  IDLE: 'IDLE',
  LISTENING: 'LISTENING',
  TRANSCRIBING: 'TRANSCRIBING',
  ANALYZING: 'ANALYZING',
  RESPONDING: 'RESPONDING',
  SPEAKING: 'SPEAKING',
  ERROR: 'ERROR'
};

// 12 Indian Scam Tactics Metadata & Weights (matching IndianScamRules.kt & RiskEngine.kt)
const TACTICS_CONFIG = {
  AUTHORITY_IMPERSONATION: { name: 'Authority Impersonation', weight: 20, desc: 'CBI, Police, Customs claim' },
  POLICE_CBI_RBI_IMPERSONATION: { name: 'Law Enforcement', weight: 20, desc: 'Police / Cyber Crime claim' },
  LEGAL_THREAT: { name: 'Arrest Threat', weight: 20, desc: 'Non-bailable warrant, court summons' },
  IDENTITY_DOCUMENT_THREAT: { name: 'Aadhaar / PAN Threat', weight: 15, desc: 'Aadhaar linked to criminal bank accounts' },
  SECRECY: { name: 'Coerced Secrecy', weight: 15, desc: 'Strictly forbidden to tell family/lawyer' },
  VIDEO_CONFINEMENT: { name: 'Video Confinement', weight: 15, desc: 'Forced to stay on video camera (Digital Arrest)' },
  FINANCIAL_REQUEST: { name: 'Money Demand', weight: 30, desc: 'Immediate security deposit demand' },
  SAFE_ACCOUNT_SCAM: { name: 'Safe Account Scam', weight: 25, desc: 'Transfer to "Safe RBI Clearance Account"' },
  CREDENTIAL_REQUEST: { name: 'Credential Harvesting', weight: 30, desc: 'Demand for OTP, UPI PIN, or netbanking login' },
  REMOTE_ACCESS_REQUEST: { name: 'Remote Screen Control', weight: 25, desc: 'Instructed to install AnyDesk or TeamViewer' },
  URGENCY: { name: 'Manufactured Urgency', weight: 10, desc: 'Demands action within minutes to induce panic' },
  PAYMENT_LINK: { name: 'Payment Link / QR', weight: 20, desc: 'Coerced scanning of QR code or malicious link' }
};

// Scenarios configuration with synchronized turn audio assets
const SCENARIOS = {
  digital_arrest_cbi: {
    id: 'digital_arrest_cbi',
    name: 'Digital Arrest (CBI Cyber Crime)',
    language: 'en',
    turns: [
      {
        turn: 1,
        audioFile: '/audio/caller_01.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi. Your Aadhaar identity has been linked to twenty-four fraudulent bank accounts involved in international money laundering.'
      },
      {
        turn: 2,
        audioFile: '/audio/caller_02.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'A non-bailable arrest warrant has been issued against you under the Prevention of Money Laundering Act by the Supreme Court of India. You will be arrested within two hours.'
      },
      {
        turn: 3,
        audioFile: '/audio/caller_03.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'This is a strictly confidential national security investigation under Section 144. You are forbidden from informing your family members, friends, or any legal advocate.'
      },
      {
        turn: 4,
        audioFile: '/audio/caller_04.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'You are placed under digital arrest right now. You must switch on your video camera immediately and remain in this room. If you disconnect, a police team will breach your residence.'
      },
      {
        turn: 5,
        audioFile: '/audio/caller_05.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'Your bank accounts are scheduled for immediate judicial asset seizure within twenty minutes unless you complete our official digital verification procedure immediately.'
      },
      {
        turn: 6,
        audioFile: '/audio/caller_06.wav',
        speaker: 'Caller (Fake CBI Inspector)',
        scriptedText: 'To prove your innocence and clear your name, you must immediately transfer five lakh rupees as a refundable security deposit to this designated Reserve Bank of India clearance account.'
      }
    ]
  },

  fake_police_hindi: {
    id: 'fake_police_hindi',
    name: 'Police Cyber Cell (Hindi)',
    language: 'hi',
    turns: [
      {
        turn: 1,
        audioFile: '/audio/caller_hindi_01.wav',
        speaker: 'Caller (Fake Mumbai Police)',
        scriptedText: 'Main Crime Branch Mumbai se Senior Inspector Rathore bol raha hoon. Aapke naam par ek suspicious parcel Customs dwara seize kiya gaya hai.'
      },
      {
        turn: 2,
        audioFile: '/audio/caller_hindi_02.wav',
        speaker: 'Caller (Fake Mumbai Police)',
        scriptedText: 'Is parcel ke andar paanch fake passports, char debit cards aur do sau gram narcotics baramad hui hai jo aapke Aadhaar number se link hai.'
      },
      {
        turn: 3,
        audioFile: '/audio/caller_hindi_03.wav',
        speaker: 'Caller (Fake Mumbai Police)',
        scriptedText: 'Aapke khilaf non-bailable arrest warrant issue ho chuka hai aur police ki team aapko arrest karne ke liye nikal chuki hai.'
      },
      {
        turn: 4,
        audioFile: '/audio/caller_hindi_04.wav',
        speaker: 'Caller (Fake Mumbai Police)',
        scriptedText: 'Agar aap arrest se bachna chahte hain toh turant video call par aaiye aur digital custody me rahiye. Kisi ko bhi phone mat kijiyega.'
      },
      {
        turn: 5,
        audioFile: '/audio/caller_hindi_05.wav',
        speaker: 'Caller (Fake Mumbai Police)',
        scriptedText: 'Apne verification aur clearance ke liye turant safe government escrow account me penalty deposit transfer kijiye varna adhe ghante me jail hogi.'
      }
    ]
  },

  legit_bank_call: {
    id: 'legit_bank_call',
    name: 'Legit Bank Support (Benign Validation)',
    language: 'en',
    turns: [
      {
        turn: 1,
        audioFile: '/audio/bank_legit_01.wav',
        speaker: 'Caller (HDFC Customer Support)',
        scriptedText: 'Good afternoon, this is Priya calling from HDFC Bank customer service regarding your recent credit card upgrade inquiry.'
      },
      {
        turn: 2,
        audioFile: null,
        speaker: 'Caller (HDFC Customer Support)',
        scriptedText: 'We noticed you submitted a request on the mobile app. Please note that HDFC Bank never asks for your password, PIN, or OTP over phone.'
      },
      {
        turn: 3,
        audioFile: null,
        speaker: 'Caller (HDFC Customer Support)',
        scriptedText: 'If you wish to proceed, you can securely review the offers by logging in directly at our official netbanking portal. Have a wonderful day.'
      }
    ]
  }
};

// Pure Evidence-Based Risk Engine (Strictly mirrors RiskEngine.kt without artificial floors)
class EvidenceBasedRiskEngine {
  constructor() {
    this.score = 0;
    this.seenTactics = new Map(); // tacticId -> count
  }

  evaluateTurn(signals) {
    const prevScore = this.score;
    const reasons = [];

    // Tally signals
    for (const sig of signals) {
      const prevCount = this.seenTactics.get(sig.id) || 0;
      const newCount = prevCount + 1;
      this.seenTactics.set(sig.id, newCount);

      const cfg = TACTICS_CONFIG[sig.id];
      const weight = cfg ? cfg.weight : 15;
      const contrib = newCount === 1 ? weight : Math.min(5, Math.round(weight * 0.15));
      reasons.push(`+${contrib} ${cfg ? cfg.name : sig.id}`);
    }

    // Accumulate total unique & repeated weights
    let totalWeight = 0;
    for (const [tacticId, count] of this.seenTactics.entries()) {
      const cfg = TACTICS_CONFIG[tacticId];
      const weight = cfg ? cfg.weight : 15;
      totalWeight += weight;
      if (count > 1) {
        totalWeight += Math.min(5, Math.round(weight * 0.15)) * (count - 1);
      }
    }

    // Synergy bonus for multi-vector attacks
    const uniqueCount = this.seenTactics.size;
    let synergyBonus = 0;
    if (uniqueCount >= 5) synergyBonus = 25;
    else if (uniqueCount >= 4) synergyBonus = 18;
    else if (uniqueCount >= 3) synergyBonus = 10;
    else if (uniqueCount >= 2) synergyBonus = 4;

    // Composite Digital-Arrest Escalation (+12 bonus when all 4 pillars are present)
    const hasAuthority = this.seenTactics.has('AUTHORITY_IMPERSONATION') || this.seenTactics.has('POLICE_CBI_RBI_IMPERSONATION');
    const hasThreat = this.seenTactics.has('LEGAL_THREAT') || this.seenTactics.has('IDENTITY_DOCUMENT_THREAT');
    const hasIsolation = this.seenTactics.has('SECRECY') || this.seenTactics.has('VIDEO_CONFINEMENT');
    const hasFinancial = this.seenTactics.has('FINANCIAL_REQUEST') || this.seenTactics.has('SAFE_ACCOUNT_SCAM') || this.seenTactics.has('CREDENTIAL_REQUEST');

    let compositeBonus = 0;
    if (hasAuthority && hasThreat && hasIsolation && hasFinancial) {
      compositeBonus = 12;
    }

    // Final genuine calculation (bounded [0, 100], zero artificial floors)
    const rawCalculated = totalWeight + synergyBonus + compositeBonus;
    this.score = Math.min(100, Math.max(0, rawCalculated));

    const delta = this.score - prevScore;
    let explanation = reasons.join(', ');
    if (compositeBonus > 0 && delta > 0 && prevScore < 80 && this.score >= 80) {
      explanation += ' (+12 Digital-Arrest Composite Escalation)';
    } else if (synergyBonus > 0 && reasons.length > 0 && delta > 0) {
      explanation += ` (+${synergyBonus} Multi-Vector Synergy)`;
    }
    if (!explanation) explanation = 'No scam patterns detected';

    return {
      prevScore,
      delta,
      newScore: this.score,
      explanation,
      activeTactics: Array.from(this.seenTactics.keys())
    };
  }

  reset() {
    this.score = 0;
    this.seenTactics.clear();
  }
}

// Laptop Audio Engine (Microphone capture, RMS, dBFS, Web Audio API, Canvas Waveform, Mic Accumulation)
class LaptopAudioEngine {
  constructor(controller) {
    this.controller = controller;
    this.audioCtx = null;
    this.micStream = null;
    this.analyser = null;
    this.sourceNode = null;
    this.currentAudio = null;
    this.isMonitoring = false;
    this.animFrameId = null;

    // Buffer accumulation for real acoustic Whisper input
    this.micAccumulator = [];
    this.isRecordingMic = false;

    // Phase 5: Voice Activity Detection (VAD) & Feedback Suppression
    this.isMutedForPlayback = false; // Microphone input ignored during scammer playback
    this.vadState = 'SILENCE'; // 'SILENCE', 'USER_SPEAKING', 'END_OF_UTTERANCE'
    this.vadEnabled = true;
    this.vadThreshold = 0.018; // Amplitude RMS threshold for speech
    this.vadSilenceThresholdMs = 1100; // 1.1s of silence triggers end of utterance
    this.vadMinSpeechMs = 450; // At least 0.45s of speech required
    this.speechStartTime = 0;
    this.silenceStartTime = 0;
    this.onEndOfUtterance = null; // callback(pcmFloat32Array)
    this.onVadStateChange = null; // callback(vadState)

    // Permissions & diagnostics state
    this.permissionState = 'PROMPTED'; // PROMPTED, GRANTED, DENIED, UNAVAILABLE
    this.sampleRate = 48000;
    this.activeSourceLabel = 'Built-in Laptop Microphone';
    this.latestRms = 0;
    this.latestDbfs = -96;

    this.canvas = document.getElementById('waveformCanvas');
    this.canvasCtx = this.canvas ? this.canvas.getContext('2d') : null;
  }

  async initMicrophone() {
    if (this.permissionState === 'GRANTED' && this.micStream) {
      return true;
    }

    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        this.permissionState = 'UNAVAILABLE';
        this.updateDiagnostics();
        return false;
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false, // Keep room acoustics intact for acoustic simulation
          noiseSuppression: false,
          autoGainControl: false
        }
      });

      this.micStream = stream;
      this.permissionState = 'GRANTED';

      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      this.audioCtx = new AudioContextClass();
      this.sampleRate = this.audioCtx.sampleRate;

      this.analyser = this.audioCtx.createAnalyser();
      this.analyser.fftSize = 512;
      this.analyser.smoothingTimeConstant = 0.4;

      this.sourceNode = this.audioCtx.createMediaStreamSource(stream);
      this.sourceNode.connect(this.analyser);

      const tracks = stream.getAudioTracks();
      if (tracks.length > 0 && tracks[0].label) {
        this.activeSourceLabel = tracks[0].label;
      }

      this.updateDiagnostics();
      return true;
    } catch (err) {
      console.warn('Laptop microphone access issue:', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        this.permissionState = 'DENIED';
      } else {
        this.permissionState = 'UNAVAILABLE';
      }
      this.updateDiagnostics();
      return false;
    }
  }

  startMonitoring() {
    if (this.isMonitoring) return;
    this.isMonitoring = true;
    this.renderLoop();
  }

  stopMonitoring() {
    this.isMonitoring = false;
    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }
    this.drawFlatWaveform();
  }

  startMicRecording() {
    this.micAccumulator = [];
    this.isRecordingMic = true;
  }

  stopMicRecording() {
    this.isRecordingMic = false;
    // Resample / downmix accumulated chunks to 16 kHz Float32Array
    if (this.micAccumulator.length === 0) {
      return new Float32Array(16000);
    }

    let totalLength = 0;
    for (const chunk of this.micAccumulator) {
      totalLength += chunk.length;
    }

    const fullFloat32 = new Float32Array(totalLength);
    let offset = 0;
    for (const chunk of this.micAccumulator) {
      fullFloat32.set(chunk, offset);
      offset += chunk.length;
    }

    this.micAccumulator = []; // Clear immediate memory

    // If source sample rate is 48k or 44.1k, resample down to 16000
    const srcRate = this.sampleRate || 48000;
    const targetRate = 16000;
    if (srcRate === targetRate) {
      return fullFloat32;
    }

    const ratio = targetRate / srcRate;
    const newLen = Math.floor(fullFloat32.length * ratio);
    const resampled = new Float32Array(newLen);
    for (let i = 0; i < newLen; i++) {
      const srcIdx = Math.floor(i / ratio);
      resampled[i] = fullFloat32[srcIdx] || 0;
    }
    return resampled;
  }

  renderLoop() {
    if (!this.isMonitoring) return;

    if (this.analyser && this.permissionState === 'GRANTED') {
      const bufferLength = this.analyser.frequencyBinCount;
      const dataArray = new Float32Array(bufferLength);
      this.analyser.getFloatTimeDomainData(dataArray);

      // Calculate actual RMS & dBFS
      let sumSquares = 0;
      for (let i = 0; i < bufferLength; i++) {
        const val = dataArray[i];
        sumSquares += val * val;
      }
      const rms = Math.sqrt(sumSquares / bufferLength);
      const dbfs = rms > 0.00001 ? Math.max(-96, Math.min(0, 20 * Math.log10(rms))) : -96;

      this.latestRms = rms;
      this.latestDbfs = dbfs;

      this.controller.updateAudioLevels(rms, dbfs);

      // Phase 5: Feedback Suppression during Scammer Playback (Microphone Input Ignored)
      if (this.isMutedForPlayback) {
        this.silenceStartTime = 0;
        this.speechStartTime = 0;
        this.vadState = 'SILENCE';
        this.drawWaveform(dataArray, bufferLength, '#64748B'); // Muted slate waveform
        this.animFrameId = requestAnimationFrame(() => this.renderLoop());
        return;
      }

      // Accumulate samples if recording for Whisper
      if (this.isRecordingMic) {
        this.micAccumulator.push(new Float32Array(dataArray));
      }

      // Phase 5: Simple Local Voice Activity Detection (VAD)
      if (this.vadEnabled && this.isRecordingMic) {
        const now = performance.now();
        if (rms >= this.vadThreshold) {
          if (this.vadState !== 'USER_SPEAKING') {
            this.vadState = 'USER_SPEAKING';
            this.speechStartTime = now;
            if (this.onVadStateChange) this.onVadStateChange('USER_SPEAKING');
          }
          this.silenceStartTime = 0;
        } else {
          if (this.vadState === 'USER_SPEAKING') {
            if (!this.silenceStartTime) {
              this.silenceStartTime = now;
            } else if (now - this.silenceStartTime > this.vadSilenceThresholdMs) {
              const speechDuration = this.silenceStartTime - this.speechStartTime;
              if (speechDuration >= this.vadMinSpeechMs) {
                this.vadState = 'END_OF_UTTERANCE';
                if (this.onVadStateChange) this.onVadStateChange('END_OF_UTTERANCE');
                const pcm = this.stopMicRecording();
                if (this.onEndOfUtterance) {
                  this.onEndOfUtterance(pcm);
                }
              }
              this.vadState = 'SILENCE';
              this.speechStartTime = 0;
              this.silenceStartTime = 0;
            }
          }
        }
      }

      this.drawWaveform(dataArray, bufferLength);
    } else if (this.controller.currentMode === 'DIRECT_AUDIO' && this.currentAudio && !this.currentAudio.paused) {
      const simulatedRms = 0.15 + Math.random() * 0.15;
      const simulatedDbfs = -16.0 + Math.random() * 4.0;
      this.latestRms = simulatedRms;
      this.latestDbfs = simulatedDbfs;
      this.controller.updateAudioLevels(simulatedRms, simulatedDbfs);

      const syntheticBuffer = new Float32Array(128);
      const time = performance.now() * 0.01;
      for (let i = 0; i < 128; i++) {
        syntheticBuffer[i] = Math.sin(time + i * 0.2) * simulatedRms;
      }
      this.drawWaveform(syntheticBuffer, 128, '#2563EB');
    } else {
      this.latestRms = 0;
      this.latestDbfs = -96;
      this.controller.updateAudioLevels(0, -96);
      this.drawFlatWaveform();
    }

    this.animFrameId = requestAnimationFrame(() => this.renderLoop());
  }

  drawWaveform(dataArray, bufferLength, color = '#22C55E') {
    if (!this.canvasCtx || !this.canvas) return;
    const ctx = this.canvasCtx;
    const width = this.canvas.width;
    const height = this.canvas.height;

    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 0, width, height);

    ctx.lineWidth = 2;
    ctx.strokeStyle = color;
    ctx.beginPath();

    const sliceWidth = width / bufferLength;
    let x = 0;

    for (let i = 0; i < bufferLength; i++) {
      const v = dataArray[i];
      const y = (v * (height / 2)) + (height / 2);

      if (i === 0) {
        ctx.moveTo(x, y);
      } else {
        ctx.lineTo(x, y);
      }
      x += sliceWidth;
    }

    ctx.stroke();
  }

  drawFlatWaveform() {
    if (!this.canvasCtx || !this.canvas) return;
    const ctx = this.canvasCtx;
    const width = this.canvas.width;
    const height = this.canvas.height;

    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 0, width, height);

    ctx.lineWidth = 1.5;
    ctx.strokeStyle = '#334155';
    ctx.beginPath();
    ctx.moveTo(0, height / 2);
    ctx.lineTo(width, height / 2);
    ctx.stroke();
  }

  async playCallerAudio(audioUrl) {
    if (!audioUrl) return true;

    return new Promise((resolve) => {
      try {
        if (this.currentAudio) {
          this.currentAudio.pause();
          this.currentAudio = null;
        }

        const audio = new Audio(audioUrl);
        this.currentAudio = audio;

        this.controller.setSpeakerIndicator(true);
        this.updateDiagnostics(audioUrl);

        audio.onended = () => {
          this.controller.setSpeakerIndicator(false);
          this.updateDiagnostics('COMPLETED');
          resolve(true);
        };

        audio.onerror = (err) => {
          console.warn(`Audio playback error for ${audioUrl}:`, err);
          this.controller.setSpeakerIndicator(false);
          this.updateDiagnostics('PLAYBACK_ERROR');
          resolve(false);
        };

        const playPromise = audio.play();
        if (playPromise !== undefined) {
          playPromise.catch((err) => {
            console.warn(`Audio play blocked by browser policy: ${err}`);
            this.controller.setSpeakerIndicator(false);
            resolve(false);
          });
        }
      } catch (e) {
        console.warn('Audio creation error:', e);
        resolve(false);
      }
    });
  }

  async speakText(text, options = {}) {
    // Phase 5 Section 16 & 17: Local Speech Output with Feedback Suppression
    // Priority:
    // 1. Local available speech synthesis (window.speechSynthesis)
    // 2. Existing bundled caller audio where applicable
    // 3. Text-only response fallback
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      return new Promise((resolve) => {
        // State: SPEAKING — MICROPHONE INPUT IGNORED
        this.isMutedForPlayback = true;
        this.controller.setSpeakerIndicator(true);

        try {
          window.speechSynthesis.cancel();
          const utterance = new SpeechSynthesisUtterance(text);
          utterance.rate = 1.0;
          utterance.pitch = 0.95;

          const voices = window.speechSynthesis.getVoices();
          const preferred = voices.find(v => v.lang.includes('en-IN') || v.name.includes('India')) ||
                            voices.find(v => v.lang.startsWith('en')) ||
                            voices[0];
          if (preferred) utterance.voice = preferred;

          let resolved = false;
          const finish = (method = 'LOCAL SYNTHETIC VOICE') => {
            if (resolved) return;
            resolved = true;
            this.isMutedForPlayback = false;
            this.controller.setSpeakerIndicator(false);
            resolve({ method, success: true });
          };

          utterance.onend = () => finish('LOCAL SYNTHETIC VOICE');
          utterance.onerror = (e) => {
            console.warn('Speech synthesis error:', e);
            finish('TEXT ONLY (TTS ERROR)');
          };

          const wordCount = (text || '').split(/\s+/).length;
          const maxMs = Math.max(3000, (wordCount / 2.2) * 1000 + 2000);
          const timer = setTimeout(() => {
            if (window.speechSynthesis.speaking) {
              window.speechSynthesis.cancel();
            }
            finish('LOCAL SYNTHETIC VOICE (TIMEOUT)');
          }, maxMs);

          const origEnd = utterance.onend;
          utterance.onend = (e) => {
            clearTimeout(timer);
            origEnd(e);
          };

          window.speechSynthesis.speak(utterance);
        } catch (e) {
          console.warn('Speech synthesis exception:', e);
          this.isMutedForPlayback = false;
          this.controller.setSpeakerIndicator(false);
          resolve({ method: 'TEXT ONLY (TTS EXCEPTION)', success: false });
        }
      });
    } else {
      this.isMutedForPlayback = false;
      return Promise.resolve({ method: 'TEXT ONLY (NO TTS)', success: false });
    }
  }

  stopSpeech() {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
      } catch (e) {}
    }
    this.isMutedForPlayback = false;
  }

  stopAll() {
    this.stopSpeech();
    if (this.currentAudio) {
      this.currentAudio.pause();
      this.currentAudio.currentTime = 0;
      this.currentAudio = null;
    }
    if (this.micStream) {
      this.micStream.getTracks().forEach(t => t.stop());
      this.micStream = null;
    }
    if (this.audioCtx && this.audioCtx.state !== 'closed') {
      try { this.audioCtx.close(); } catch (e) {}
      this.audioCtx = null;
    }
    this.isRecordingMic = false;
    this.micAccumulator = [];
    this.permissionState = 'PROMPTED';
    this.isMutedForPlayback = false;
    this.vadState = 'SILENCE';
    this.speechStartTime = 0;
    this.silenceStartTime = 0;
    this.stopMonitoring();
    this.updateDiagnostics('IDLE');
  }

  updateDiagnostics(playbackState = 'IDLE') {
    const diagSource = document.getElementById('diagSource');
    const diagRate = document.getElementById('diagRate');
    const diagLevel = document.getElementById('diagLevel');
    const diagPerm = document.getElementById('diagPerm');
    const diagPlayback = document.getElementById('diagPlayback');

    if (diagSource) diagSource.textContent = this.controller.currentMode === 'ACOUSTIC' ? this.activeSourceLabel : (this.controller.currentMode === 'DIRECT_AUDIO' ? 'Direct Audio Stream (Internal Loopback)' : 'Script Engine (Deterministic)');
    if (diagRate) diagRate.textContent = `${this.sampleRate.toLocaleString()} Hz / 16 kHz resampled`;
    if (diagLevel) diagLevel.textContent = `${this.latestDbfs.toFixed(1)} dBFS (RMS: ${(this.latestRms * 100).toFixed(1)}%)`;
    if (diagPerm) {
      diagPerm.textContent = this.permissionState;
      diagPerm.style.color = this.permissionState === 'GRANTED' ? 'var(--color-green)' : (this.permissionState === 'DENIED' ? 'var(--color-red)' : 'var(--text-secondary)');
    }
    if (diagPlayback) diagPlayback.textContent = playbackState;
  }
}

// Master Dashboard & Simulation Controller
class DashboardController {
  constructor() {
    this.currentMode = 'ACOUSTIC'; // 'ACOUSTIC', 'DIRECT_AUDIO', 'DETERMINISTIC', 'INTERACTIVE'
    this.sttMode = 'DETERMINISTIC'; // 'WHISPER', 'DETERMINISTIC'
    this.activeScenarioKey = 'digital_arrest_cbi';
    this.isSimulating = false;
    this.isPaused = false;
    this.currentTurnIndex = 0;

    // Phase 5: Interactive Two-Way Conversation State
    this.interactiveState = INTERACTIVE_STATES.IDLE;
    this.conversationHistory = []; // Bounded context: max ~6 turns
    this.interactiveStage = 'AUTHORITY';
    this.isVadAuto = true;
    this.isTurnProcessing = false;

    this.riskEngine = new EvidenceBasedRiskEngine();
    this.audioEngine = new LaptopAudioEngine(this);

    // Whisper WASM Engine
    this.whisperEngine = new WhisperEngine({
      modelName: 'tiny (multilingual, 31 MB)',
      modelUrl: '/whisper/ggml-tiny-q5_1.bin',
      nthreads: 4,
      onStateChange: (state, detail) => this.handleWhisperStateChange(state, detail),
      onProgress: (percent) => this.handleWhisperProgress(percent),
      onLog: (msg) => console.log(msg)
    });

    // Local Qwen Semantic Reasoning Engine (Phase 4B)
    this.qwenAdapter = new QwenLocalAdapter({
      modelName: 'Qwen2.5-1.5B (Quantized Q4_K_M)',
      onStateChange: (state) => this.handleQwenStateChange(state),
      onMetricsChange: (metrics) => this.updateQwenDiagnostics(metrics)
    });

    this.ws = null;

    this.initElements();
    this.renderTacticsGrid();
    this.renderAudioPathFlow();
    this.bindEvents();
    this.autoConnectWs();
  }

  initElements() {
    // Mode Buttons
    this.modeAcousticBtn = document.getElementById('modeAcousticBtn');
    this.modeDirectBtn = document.getElementById('modeDirectBtn');
    this.modeDeterministicBtn = document.getElementById('modeDeterministicBtn');
    this.activeModeText = document.getElementById('activeModeText');

    // STT Selector Chips (Section 6)
    this.sttWhisperBtn = document.getElementById('sttWhisperBtn');
    this.sttDemoBtn = document.getElementById('sttDemoBtn');

    // Whisper Control Card (Section 3 & 4)
    this.whisperControlCard = document.getElementById('whisperControlCard');
    this.whisperStatusDot = document.getElementById('whisperStatusDot');
    this.whisperStatusPill = document.getElementById('whisperStatusPill');
    this.whisperModelTag = document.getElementById('whisperModelTag');
    this.whisperProcTag = document.getElementById('whisperProcTag');
    this.whisperInfoText = document.getElementById('whisperInfoText');
    this.whisperProgressContainer = document.getElementById('whisperProgressContainer');
    this.whisperProgressLabel = document.getElementById('whisperProgressLabel');
    this.whisperProgressPercent = document.getElementById('whisperProgressPercent');
    this.whisperProgressFill = document.getElementById('whisperProgressFill');
    this.loadWhisperBtn = document.getElementById('loadWhisperBtn');
    this.retryWhisperBtn = document.getElementById('retryWhisperBtn');
    this.unloadWhisperBtn = document.getElementById('unloadWhisperBtn');
    this.whisperReadyNote = document.getElementById('whisperReadyNote');

    // Whisper Fallback Banner (Section 18)
    this.whisperFallbackBanner = document.getElementById('whisperFallbackBanner');
    this.whisperFallbackRetryBtn = document.getElementById('whisperFallbackRetryBtn');
    this.whisperFallbackSwitchBtn = document.getElementById('whisperFallbackSwitchBtn');

    // Scenario Buttons
    this.scenarioCbiBtn = document.getElementById('scenarioCbiBtn');
    this.scenarioHindiBtn = document.getElementById('scenarioHindiBtn');
    this.scenarioLegitBtn = document.getElementById('scenarioLegitBtn');

    // Playback Controls
    this.startSimBtn = document.getElementById('startSimBtn');
    this.pauseSimBtn = document.getElementById('pauseSimBtn');
    this.resumeSimBtn = document.getElementById('resumeSimBtn');
    this.stopSimBtn = document.getElementById('stopSimBtn');
    this.progressTimer = document.getElementById('progressTimer');

    // Audio Path & Fallback
    this.audioPathCard = document.getElementById('audioPathCard');
    this.audioPathDesc = document.getElementById('audioPathDesc');
    this.audioPathFlow = document.getElementById('audioPathFlow');
    this.fallbackBanner = document.getElementById('fallbackBanner');
    this.fallbackActionBtn = document.getElementById('fallbackActionBtn');
    this.sessionPurgedBanner = document.getElementById('sessionPurgedBanner');

    // Risk Meter & Progression
    this.protectionBadge = document.getElementById('protectionBadge');
    this.gaugeFill = document.getElementById('gaugeFill');
    this.riskScoreEl = document.getElementById('riskScore');
    this.riskLevelEl = document.getElementById('riskLevel');
    this.scoreDeltaCalc = document.getElementById('scoreDeltaCalc');
    this.scoreDeltaReason = document.getElementById('scoreDeltaReason');
    this.recText = document.getElementById('recText');

    // Audio Monitor
    this.speakerIndicator = document.getElementById('speakerIndicator');
    this.micIndicator = document.getElementById('micIndicator');
    this.levelValue = document.getElementById('levelValue');
    this.levelFill = document.getElementById('levelFill');
    this.inputStatusText = document.getElementById('inputStatusText');
    this.toggleDiagBtn = document.getElementById('toggleDiagBtn');
    this.diagDrawer = document.getElementById('diagDrawer');

    // Whisper Performance Diagnostics (Section 17)
    this.diagWhisperModel = document.getElementById('diagWhisperModel');
    this.diagWhisperStatus = document.getElementById('diagWhisperStatus');
    this.diagWhisperAudioDur = document.getElementById('diagWhisperAudioDur');
    this.diagWhisperInferTime = document.getElementById('diagWhisperInferTime');
    this.diagWhisperRtf = document.getElementById('diagWhisperRtf');

    // Local Qwen Control Card & Diagnostics (Phase 4B)
    this.qwenControlCard = document.getElementById('qwenControlCard');
    this.qwenStatusDot = document.getElementById('qwenStatusDot');
    this.qwenStatusPill = document.getElementById('qwenStatusPill');
    this.qwenProgressContainer = document.getElementById('qwenProgressContainer');
    this.qwenProgressPercent = document.getElementById('qwenProgressPercent');
    this.qwenProgressFill = document.getElementById('qwenProgressFill');
    this.loadQwenBtn = document.getElementById('loadQwenBtn');
    this.unloadQwenBtn = document.getElementById('unloadQwenBtn');
    this.qwenReadyNote = document.getElementById('qwenReadyNote');
    this.qwenFallbackBanner = document.getElementById('qwenFallbackBanner');

    // Escalation Banner
    this.escalationBanner = document.getElementById('escalationBanner');
    this.escalationDelta = document.getElementById('escalationDelta');
    this.escalationNewEvidence = document.getElementById('escalationNewEvidence');
    this.escalationText = document.getElementById('escalationText');

    // "Why This Call Is Suspicious" Panel
    this.whySuspiciousCard = document.getElementById('whySuspiciousCard');
    this.whyStatusTag = document.getElementById('whyStatusTag');
    this.whySummaryText = document.getElementById('whySummaryText');
    this.whyTacticsList = document.getElementById('whyTacticsList');
    this.whyActionText = document.getElementById('whyActionText');
    this.criticalQwenWhy = document.getElementById('criticalQwenWhy');
    this.modalQwenWhyBox = document.getElementById('modalQwenWhyBox');
    this.modalQwenWhyText = document.getElementById('modalQwenWhyText');

    // Qwen Diagnostics in Drawer
    this.diagQwenModel = document.getElementById('diagQwenModel');
    this.diagQwenStatus = document.getElementById('diagQwenStatus');
    this.diagQwenLoadTime = document.getElementById('diagQwenLoadTime');
    this.diagQwenInferTime = document.getElementById('diagQwenInferTime');
    this.diagQwenMemory = document.getElementById('diagQwenMemory');
    this.diagQwenConfidence = document.getElementById('diagQwenConfidence');

    // Tactics & Evidence Inspector
    this.tacticsGrid = document.getElementById('tacticsGrid');
    this.activeTacticsCount = document.getElementById('activeTacticsCount');
    this.criticalBanner = document.getElementById('criticalBanner');
    this.evidenceTag = document.getElementById('evidenceTag');
    this.evSignal = document.getElementById('evSignal');
    this.evSnippet = document.getElementById('evSnippet');
    this.evSource = document.getElementById('evSource');
    this.evContrib = document.getElementById('evContrib');
    this.evConf = document.getElementById('evConf');

    // Timeline & Transcript
    this.timelineList = document.getElementById('timelineList');
    this.clearTimelineBtn = document.getElementById('clearTimelineBtn');
    this.transcriptViewer = document.getElementById('transcriptViewer');
    this.transcriptSourceBadge = document.getElementById('transcriptSourceBadge');

    // Critical Modal
    this.criticalModal = document.getElementById('criticalModal');
    this.modalScorePill = document.getElementById('modalScorePill');
    this.modalTacticsList = document.getElementById('modalTacticsList');
    this.modalStopBtn = document.getElementById('modalStopBtn');
    this.modalSafeBtn = document.getElementById('modalSafeBtn');
    this.modalDismissBtn = document.getElementById('modalDismissBtn');

    // Companion Connection
    this.connectionPill = document.getElementById('connectionPill');
    this.wsUrlInput = document.getElementById('wsUrlInput');
    this.connectWsBtn = document.getElementById('connectWsBtn');

    // Phase 5: Interactive Conversation Elements
    this.modeInteractiveBtn = document.getElementById('modeInteractiveBtn');
    this.interactiveConversationCard = document.getElementById('interactiveConversationCard');
    this.convStatePill = document.getElementById('convStatePill');
    this.convStreamBox = document.getElementById('convStreamBox');
    this.convMicStatus = document.getElementById('convMicStatus');
    this.convStageBadge = document.getElementById('convStageBadge');
    this.startInteractiveBtn = document.getElementById('startInteractiveBtn');
    this.doneSpeakingBtn = document.getElementById('doneSpeakingBtn');
    this.toggleVadBtn = document.getElementById('toggleVadBtn');
    this.stopInteractiveBtn = document.getElementById('stopInteractiveBtn');

    // Phase 5 Section 29 Latency Diagnostics
    this.diagUserSttTime = document.getElementById('diagUserSttTime');
    this.diagQwenRespTime = document.getElementById('diagQwenRespTime');
    this.diagVoiceOutputTime = document.getElementById('diagVoiceOutputTime');
    this.diagTotalTurnLatency = document.getElementById('diagTotalTurnLatency');
  }

  bindEvents() {
    // Mode Switching
    this.modeAcousticBtn.addEventListener('click', () => this.setMode('ACOUSTIC'));
    this.modeDirectBtn.addEventListener('click', () => this.setMode('DIRECT_AUDIO'));
    this.modeDeterministicBtn.addEventListener('click', () => this.setMode('DETERMINISTIC'));
    if (this.modeInteractiveBtn) {
      this.modeInteractiveBtn.addEventListener('click', () => this.setMode('INTERACTIVE'));
    }

    // STT Mode Switching
    this.sttWhisperBtn.addEventListener('click', () => this.setSttMode('WHISPER'));
    this.sttDemoBtn.addEventListener('click', () => this.setSttMode('DETERMINISTIC'));

    // Whisper Model Controls
    this.loadWhisperBtn.addEventListener('click', () => this.loadWhisperModel());
    this.retryWhisperBtn.addEventListener('click', () => this.loadWhisperModel());
    this.unloadWhisperBtn.addEventListener('click', () => this.unloadWhisperModel());

    // Local Qwen Controls (Phase 4B)
    if (this.loadQwenBtn) this.loadQwenBtn.addEventListener('click', () => this.loadQwenModel());
    if (this.unloadQwenBtn) this.unloadQwenBtn.addEventListener('click', () => this.unloadQwenModel());

    // Whisper Fallback Buttons
    this.whisperFallbackRetryBtn.addEventListener('click', () => {
      this.whisperFallbackBanner.style.display = 'none';
      this.loadWhisperModel();
    });
    this.whisperFallbackSwitchBtn.addEventListener('click', () => {
      this.whisperFallbackBanner.style.display = 'none';
      this.setSttMode('DETERMINISTIC');
    });

    // Scenario Switching
    this.scenarioCbiBtn.addEventListener('click', () => this.setScenario('digital_arrest_cbi'));
    this.scenarioHindiBtn.addEventListener('click', () => this.setScenario('fake_police_hindi'));
    this.scenarioLegitBtn.addEventListener('click', () => this.setScenario('legit_bank_call'));

    // Simulation Controls
    this.startSimBtn.addEventListener('click', () => this.startSimulation());
    this.pauseSimBtn.addEventListener('click', () => this.pauseSimulation());
    this.resumeSimBtn.addEventListener('click', () => this.resumeSimulation());
    this.stopSimBtn.addEventListener('click', () => this.stopAndPurge());

    // Phase 5 Interactive Call Controls
    if (this.startInteractiveBtn) {
      this.startInteractiveBtn.addEventListener('click', () => this.startInteractiveCall());
    }
    if (this.doneSpeakingBtn) {
      this.doneSpeakingBtn.addEventListener('click', () => this.handleManualDoneSpeaking());
    }
    if (this.toggleVadBtn) {
      this.toggleVadBtn.addEventListener('click', () => this.toggleAutoVad());
    }
    if (this.stopInteractiveBtn) {
      this.stopInteractiveBtn.addEventListener('click', () => this.stopAndPurge());
    }

    // Fallback Action for Mic
    this.fallbackActionBtn.addEventListener('click', () => {
      this.fallbackBanner.style.display = 'none';
      this.setMode('DIRECT_AUDIO');
      this.startSimulation();
    });

    // Diagnostics Drawer Toggle
    this.toggleDiagBtn.addEventListener('click', () => {
      const isVisible = this.diagDrawer.style.display === 'flex';
      this.diagDrawer.style.display = isVisible ? 'none' : 'flex';
      this.toggleDiagBtn.textContent = isVisible ? 'Diagnostics ▼' : 'Diagnostics ▲';
    });

    // Timeline Clear
    this.clearTimelineBtn.addEventListener('click', () => this.clearTimeline());

    // Critical Modal Controls
    this.modalStopBtn.addEventListener('click', () => {
      this.criticalModal.style.display = 'none';
      this.stopAndPurge();
    });
    this.modalSafeBtn.addEventListener('click', () => {
      this.criticalModal.style.display = 'none';
      this.logTimeline('User confirmed safety posture: "I\'M NOT SENDING MONEY". Calling Cybercrime 1930.', 'info');
    });
    this.modalDismissBtn.addEventListener('click', () => {
      this.criticalModal.style.display = 'none';
    });

    // Companion WS
    this.connectWsBtn.addEventListener('click', () => this.connectWs());
  }

  // Whisper Model State Machine Handlers (Section 3 & 4)
  async loadWhisperModel() {
    this.whisperFallbackBanner.style.display = 'none';
    this.loadWhisperBtn.disabled = true;
    this.whisperProgressContainer.style.display = 'flex';
    this.whisperProgressPercent.textContent = '0%';
    this.whisperProgressFill.style.width = '0%';

    const success = await this.whisperEngine.loadModel();
    this.loadWhisperBtn.disabled = false;

    if (success) {
      this.whisperProgressContainer.style.display = 'none';
      this.loadWhisperBtn.style.display = 'none';
      this.retryWhisperBtn.style.display = 'none';
      this.unloadWhisperBtn.style.display = 'inline-flex';
      this.whisperReadyNote.style.display = 'inline-block';

      // Automatically default to Whisper once loaded (Section 6)
      this.setSttMode('WHISPER');
      this.logTimeline('Whisper WebAssembly model initialized on device.', 'info');
    } else {
      this.whisperProgressContainer.style.display = 'none';
      this.loadWhisperBtn.style.display = 'none';
      this.retryWhisperBtn.style.display = 'inline-flex';
      this.whisperFallbackBanner.style.display = 'flex';
    }
  }

  unloadWhisperModel() {
    this.whisperEngine.free();
    this.setSttMode('DETERMINISTIC');
    this.unloadWhisperBtn.style.display = 'none';
    this.loadWhisperBtn.style.display = 'inline-flex';
    this.whisperReadyNote.style.display = 'none';
    this.logTimeline('Whisper model unloaded from memory.', 'info');
  }

  handleWhisperStateChange(state, detail) {
    this.whisperStatusPill.textContent = `Status: ${state}`;
    this.whisperStatusPill.className = `whisper-status-pill status-${state.toLowerCase().replace(/\s+/g, '-')}`;

    this.whisperStatusDot.className = 'whisper-badge-dot';
    if (state === WHISPER_STATES.READY) {
      this.whisperStatusDot.classList.add('ready');
    } else if (state === WHISPER_STATES.TRANSCRIBING) {
      this.whisperStatusDot.classList.add('transcribing');
    } else if (state === WHISPER_STATES.ERROR) {
      this.whisperStatusDot.classList.add('error');
    }

    if (this.diagWhisperStatus) {
      this.diagWhisperStatus.textContent = state;
    }

    if (detail) {
      this.whisperInfoText.textContent = detail;
    }
  }

  // Local Qwen Reasoning State Machine Handlers (Phase 4B Section 4)
  async loadQwenModel() {
    if (!this.loadQwenBtn) return;
    if (this.qwenFallbackBanner) this.qwenFallbackBanner.style.display = 'none';
    this.loadQwenBtn.disabled = true;
    if (this.qwenProgressContainer) {
      this.qwenProgressContainer.style.display = 'flex';
      this.qwenProgressPercent.textContent = '0%';
      this.qwenProgressFill.style.width = '0%';
    }

    let p = 0;
    const interval = setInterval(() => {
      p = Math.min(p + 20, 90);
      if (this.qwenProgressPercent) this.qwenProgressPercent.textContent = `${p}%`;
      if (this.qwenProgressFill) this.qwenProgressFill.style.width = `${p}%`;
    }, 120);

    const success = await this.qwenAdapter.load({ warmupMs: 500 });
    clearInterval(interval);

    this.loadQwenBtn.disabled = false;
    if (this.qwenProgressContainer) this.qwenProgressContainer.style.display = 'none';

    if (success) {
      this.loadQwenBtn.style.display = 'none';
      if (this.unloadQwenBtn) this.unloadQwenBtn.style.display = 'inline-flex';
      if (this.qwenReadyNote) this.qwenReadyNote.style.display = 'inline-flex';
      this.logTimeline('Local Qwen semantic reasoning engine loaded successfully (Qwen2.5-1.5B).', 'info');
    } else {
      if (this.qwenFallbackBanner) this.qwenFallbackBanner.style.display = 'flex';
      this.logTimeline('Failed to load local Qwen model. Rule-based analysis active.', 'info');
    }
  }

  unloadQwenModel() {
    this.qwenAdapter.unload();
    if (this.loadQwenBtn) this.loadQwenBtn.style.display = 'inline-flex';
    if (this.unloadQwenBtn) this.unloadQwenBtn.style.display = 'none';
    if (this.qwenReadyNote) this.qwenReadyNote.style.display = 'none';
    this.resetWhySuspiciousPanel();
    this.hideEscalationBanner();
    this.logTimeline('Local Qwen model unloaded from memory.', 'info');
  }

  handleQwenStateChange(state) {
    if (this.diagQwenStatus) this.diagQwenStatus.textContent = state;
    if (!this.qwenStatusPill) return;

    this.qwenStatusPill.className = 'qwen-status-pill';
    this.qwenStatusDot.className = 'qwen-badge-dot';

    if (state === QWEN_STATES.NOT_LOADED) {
      this.qwenStatusPill.classList.add('status-not-loaded');
      this.qwenStatusPill.textContent = 'Status: NOT LOADED';
      this.qwenStatusDot.classList.add('status-dot-not-loaded');
    } else if (state === QWEN_STATES.LOADING) {
      this.qwenStatusPill.classList.add('status-loading');
      this.qwenStatusPill.textContent = 'Status: LOADING';
      this.qwenStatusDot.classList.add('status-dot-loading');
    } else if (state === QWEN_STATES.READY) {
      this.qwenStatusPill.classList.add('status-ready');
      this.qwenStatusPill.textContent = 'Status: READY';
      this.qwenStatusDot.classList.add('status-dot-ready');
    } else if (state === QWEN_STATES.THINKING) {
      this.qwenStatusPill.classList.add('status-thinking');
      this.qwenStatusPill.textContent = 'Status: THINKING';
      this.qwenStatusDot.classList.add('status-dot-thinking');
    } else if (state === QWEN_STATES.COMPLETE) {
      this.qwenStatusPill.classList.add('status-complete');
      this.qwenStatusPill.textContent = 'Status: COMPLETE';
      this.qwenStatusDot.classList.add('status-dot-ready');
    } else if (state === QWEN_STATES.ERROR) {
      this.qwenStatusPill.classList.add('status-error');
      this.qwenStatusPill.textContent = 'Status: ERROR';
      this.qwenStatusDot.classList.add('status-dot-error');
    }
  }

  updateQwenDiagnostics(metrics) {
    if (!metrics) metrics = this.qwenAdapter;
    if (this.diagQwenModel) this.diagQwenModel.textContent = metrics.modelName || 'Qwen2.5-1.5B (Quantized Q4_K_M)';
    if (this.diagQwenStatus) this.diagQwenStatus.textContent = this.qwenAdapter.state;
    if (this.diagQwenLoadTime) this.diagQwenLoadTime.textContent = `${this.qwenAdapter.modelLoadSec} s`;
    if (this.diagQwenInferTime) this.diagQwenInferTime.textContent = `${this.qwenAdapter.lastInferenceSec} s`;
    if (this.diagQwenMemory) this.diagQwenMemory.textContent = `${this.qwenAdapter.memoryMB} MB`;
    if (this.diagQwenConfidence) this.diagQwenConfidence.textContent = 'Not reported';
  }

  showEscalationBanner(prev, curr, delta, signalName, text) {
    if (!this.escalationBanner) return;
    if (this.escalationDelta) this.escalationDelta.textContent = `${prev} → ${curr} (+${delta} pts)`;
    if (this.escalationNewEvidence) this.escalationNewEvidence.textContent = signalName;
    if (this.escalationText) this.escalationText.textContent = `"${text}"`;
    this.escalationBanner.style.display = 'flex';
  }

  hideEscalationBanner() {
    if (this.escalationBanner) this.escalationBanner.style.display = 'none';
  }

  renderWhySuspiciousPanel(qwenResult, detectedSignals, evalResult) {
    if (!this.whySuspiciousCard) return;
    if (this.whyStatusTag) this.whyStatusTag.textContent = qwenResult.is_suspicious ? 'THREAT ANALYZED' : 'BENIGN EVALUATED';
    if (this.whySummaryText) this.whySummaryText.textContent = qwenResult.summary;

    if (this.whyTacticsList) {
      this.whyTacticsList.innerHTML = '';
      if (qwenResult.tactics && qwenResult.tactics.length > 0) {
        qwenResult.tactics.forEach(tactic => {
          const item = document.createElement('div');
          item.className = 'why-tactic-item';
          item.innerHTML = `
            <div class="why-tactic-header">
              <span class="why-tactic-name">${tactic.name}</span>
              <div class="why-tactic-tags">
                <span class="badge-rule-evidence">RULE-BASED EVIDENCE</span>
                <span class="badge-ai-explanation">LOCAL AI EXPLANATION</span>
              </div>
            </div>
            <div class="why-tactic-evidence">"${tactic.evidence}"</div>
            <div class="why-tactic-reason">${tactic.reason}</div>
          `;
          this.whyTacticsList.appendChild(item);
        });
      } else {
        const item = document.createElement('div');
        item.className = 'why-tactic-item';
        item.innerHTML = `
          <div class="why-tactic-reason" style="font-style: italic; color: var(--text-secondary);">
            No coercive or deceptive tactics detected in caller dialogue. Legitimate administrative interaction.
          </div>
        `;
        this.whyTacticsList.appendChild(item);
      }
    }

    if (this.whyActionText) this.whyActionText.textContent = qwenResult.recommended_action;
  }

  renderWhySuspiciousFallback(detectedSignals, evalResult) {
    if (!this.whySuspiciousCard) return;
    if (this.whyStatusTag) this.whyStatusTag.textContent = 'RULE-BASED ANALYSIS';

    if (detectedSignals && detectedSignals.length > 0) {
      if (this.whySummaryText) {
        this.whySummaryText.textContent = `ScamShield deterministic rules detected ${detectedSignals.length} pattern(s). Local AI reasoning is inactive.`;
      }
      if (this.whyTacticsList) {
        this.whyTacticsList.innerHTML = '';
        detectedSignals.forEach(sig => {
          const item = document.createElement('div');
          item.className = 'why-tactic-item';
          item.innerHTML = `
            <div class="why-tactic-header">
              <span class="why-tactic-name">${sig.name}</span>
              <div class="why-tactic-tags">
                <span class="badge-rule-evidence">RULE-BASED EVIDENCE</span>
              </div>
            </div>
            <div class="why-tactic-evidence">"${sig.snippet}"</div>
            <div class="why-tactic-reason">Flagged by keyword taxonomy rules (+${sig.weight} pts).</div>
          `;
          this.whyTacticsList.appendChild(item);
        });
      }
      if (this.whyActionText) {
        this.whyActionText.textContent = 'Exercise caution: Do NOT share OTPs, banking passwords, or transfer funds.';
      }
    } else {
      if (this.whySummaryText) {
        this.whySummaryText.textContent = 'No threat indicators detected by deterministic rules.';
      }
      if (this.whyTacticsList) this.whyTacticsList.innerHTML = '';
      if (this.whyActionText) {
        this.whyActionText.textContent = 'Maintain standard phone vigilance.';
      }
    }
  }

  resetWhySuspiciousPanel() {
    if (this.whyStatusTag) this.whyStatusTag.textContent = 'AWAITING DIALOGUE';
    if (this.whySummaryText) {
      this.whySummaryText.textContent = 'Awaiting caller speech to evaluate contextual coercion and tactical patterns.';
    }
    if (this.whyTacticsList) this.whyTacticsList.innerHTML = '';
    if (this.whyActionText) {
      this.whyActionText.textContent = 'Follow standard vigilance: Never transfer money or reveal banking PINs during unexpected phone calls.';
    }
  }

  handleWhisperProgress(percent) {
    this.whisperProgressPercent.textContent = `${percent}%`;
    this.whisperProgressFill.style.width = `${percent}%`;
    this.whisperProgressLabel.textContent = `Loading local model weights into memory (${percent}%)...`;
  }

  setSttMode(sttMode) {
    if (sttMode === 'WHISPER' && !this.whisperEngine.isReady()) {
      if (confirm('Local Whisper model is not loaded yet. Would you like to initialize it now?')) {
        this.loadWhisperModel();
      }
      return;
    }

    this.sttMode = sttMode;
    this.sttWhisperBtn.classList.toggle('active', sttMode === 'WHISPER');
    this.sttDemoBtn.classList.toggle('active', sttMode === 'DETERMINISTIC');

    this.updateSttBadgeLabel();
    this.renderAudioPathFlow();
  }

  updateSttBadgeLabel(isTranscribing = false) {
    if (isTranscribing) {
      this.transcriptSourceBadge.textContent = 'TRANSCRIBING...';
      this.transcriptSourceBadge.className = 'transcript-status transcribing';
      return;
    }

    if (this.sttMode === 'WHISPER') {
      if (this.currentMode === 'ACOUSTIC') {
        this.transcriptSourceBadge.textContent = 'WHISPER — LIVE MICROPHONE';
      } else if (this.currentMode === 'DIRECT_AUDIO') {
        this.transcriptSourceBadge.textContent = 'WHISPER — DIRECT LOCAL AUDIO';
      } else {
        this.transcriptSourceBadge.textContent = 'WHISPER — ON DEVICE';
      }
      this.transcriptSourceBadge.className = 'transcript-status whisper';
    } else {
      this.transcriptSourceBadge.textContent = 'DEMO STT — DETERMINISTIC SCRIPT';
      this.transcriptSourceBadge.className = 'transcript-status';
    }
  }

  setMode(mode) {
    if (this.isSimulating) {
      if (!confirm('Simulation is currently active. Switching mode will reset the session. Continue?')) {
        return;
      }
      this.stopAndPurge();
    }

    this.currentMode = mode;
    this.fallbackBanner.style.display = 'none';

    this.modeAcousticBtn.classList.toggle('active', mode === 'ACOUSTIC');
    this.modeDirectBtn.classList.toggle('active', mode === 'DIRECT_AUDIO');
    this.modeDeterministicBtn.classList.toggle('active', mode === 'DETERMINISTIC');
    if (this.modeInteractiveBtn) {
      this.modeInteractiveBtn.classList.toggle('active', mode === 'INTERACTIVE');
    }

    if (mode === 'INTERACTIVE') {
      this.activeModeText.textContent = 'INTERACTIVE CONVERSATION';
      this.inputStatusText.textContent = 'SIMULATED CONVERSATION — NOT A REAL PHONE CALL';
      this.audioPathDesc.textContent = 'Two-Way Simulated Scam Call: Laptop Mic → Local Whisper → Scam Rules + Qwen → Laptop Speaker';
      if (this.interactiveConversationCard) {
        this.interactiveConversationCard.style.display = 'block';
      }
    } else {
      if (this.interactiveConversationCard) {
        this.interactiveConversationCard.style.display = 'none';
      }
      if (mode === 'ACOUSTIC') {
        this.activeModeText.textContent = 'ACOUSTIC SIMULATION';
        this.inputStatusText.textContent = 'ACOUSTIC SIMULATION — Laptop Mic Monitoring';
        this.audioPathDesc.textContent = 'Real-Time Acoustic Speakerphone Loop';
      } else if (mode === 'DIRECT_AUDIO') {
        this.activeModeText.textContent = 'DIRECT AUDIO SIMULATION';
        this.inputStatusText.textContent = 'DIRECT AUDIO SIMULATION — Local Pipeline Injection';
        this.audioPathDesc.textContent = 'Direct Caller Audio Injection into Local Analysis Engine';
      } else {
        this.activeModeText.textContent = 'DETERMINISTIC DEMO';
        this.inputStatusText.textContent = 'DETERMINISTIC DEMO — Scripted Verification';
        this.audioPathDesc.textContent = 'Deterministic State Machine Verification Pipeline';
      }
    }

    this.updateSttBadgeLabel();
    this.renderAudioPathFlow();
    this.audioEngine.updateDiagnostics();
  }

  setScenario(scenarioKey) {
    if (this.isSimulating) {
      alert('Please stop the current simulation before changing scenarios.');
      return;
    }
    this.activeScenarioKey = scenarioKey;
    this.scenarioCbiBtn.classList.toggle('active', scenarioKey === 'digital_arrest_cbi');
    this.scenarioHindiBtn.classList.toggle('active', scenarioKey === 'fake_police_hindi');
    this.scenarioLegitBtn.classList.toggle('active', scenarioKey === 'legit_bank_call');
    this.resetState();
  }

  renderTacticsGrid() {
    this.tacticsGrid.innerHTML = '';
    Object.entries(TACTICS_CONFIG).forEach(([id, meta]) => {
      const el = document.createElement('div');
      el.className = 'tactic-item';
      el.id = `tactic-${id}`;
      el.innerHTML = `
        <div class="tactic-header">
          <span class="tactic-name">${meta.name}</span>
          <span class="tactic-weight">+${meta.weight} pts</span>
        </div>
        <div class="tactic-snippet">${meta.desc}</div>
      `;
      this.tacticsGrid.appendChild(el);
    });
  }

  renderAudioPathFlow(activeStage = '') {
    this.audioPathFlow.innerHTML = '';
    let nodes = [];

    const sttLabel = this.sttMode === 'WHISPER' ? 'LOCAL WHISPER (WASM)' : 'DETERMINISTIC STT';

    if (this.currentMode === 'INTERACTIVE') {
      nodes = [
        { id: 'user_mic', label: 'USER MIC' },
        { id: 'whisper', label: this.sttMode === 'WHISPER' ? 'LOCAL WHISPER' : 'USER STT' },
        { id: 'engine', label: 'SCAM RULES + RISK' },
        { id: 'qwen', label: 'QWEN / SCRIPT' },
        { id: 'speaker', label: 'LAPTOP SPEAKER 🔊' }
      ];
    } else if (this.currentMode === 'ACOUSTIC') {
      nodes = [
        { id: 'caller_voice', label: 'CALLER VOICE' },
        { id: 'speaker', label: 'LAPTOP SPEAKER' },
        { id: 'room', label: 'ACOUSTIC ROOM' },
        { id: 'mic', label: 'LAPTOP MICROPHONE' },
        { id: 'whisper', label: sttLabel },
        { id: 'engine', label: 'SCAM RULES + RISK' }
      ];
    } else if (this.currentMode === 'DIRECT_AUDIO') {
      nodes = [
        { id: 'caller_wav', label: 'CALLER WAV' },
        { id: 'direct_input', label: 'DIRECT LOCAL INPUT' },
        { id: 'whisper', label: sttLabel },
        { id: 'engine', label: 'SCAM RULES + RISK' }
      ];
    } else {
      nodes = [
        { id: 'script', label: 'SCRIPTED SCENARIO' },
        { id: 'deterministic_runner', label: 'DETERMINISTIC RUNNER' },
        { id: 'engine', label: 'SCAM RULES + RISK' }
      ];
    }

    nodes.forEach((node, index) => {
      const nodeEl = document.createElement('div');
      nodeEl.className = `path-node ${node.id === activeStage ? 'active-stage' : ''}`;
      nodeEl.id = `path-node-${node.id}`;
      nodeEl.textContent = node.label;
      this.audioPathFlow.appendChild(nodeEl);

      if (index < nodes.length - 1) {
        const arrow = document.createElement('span');
        arrow.className = 'path-arrow';
        arrow.textContent = '→';
        this.audioPathFlow.appendChild(arrow);
      }
    });
  }

  highlightAudioPathStage(stageId) {
    document.querySelectorAll('.path-node').forEach(el => el.classList.remove('active-stage'));
    const target = document.getElementById(`path-node-${stageId}`);
    if (target) target.classList.add('active-stage');
  }

  updateAudioLevels(rms, dbfs) {
    const percent = Math.min(100, Math.max(0, rms * 350));
    this.levelFill.style.width = `${percent}%`;
    this.levelValue.textContent = `${percent.toFixed(0)}% (${dbfs.toFixed(1)} dBFS)`;

    if (percent > 65) {
      this.levelFill.style.backgroundColor = 'var(--color-red)';
    } else if (percent > 30) {
      this.levelFill.style.backgroundColor = 'var(--color-amber)';
    } else {
      this.levelFill.style.backgroundColor = 'var(--color-green)';
    }

    const diagLevel = document.getElementById('diagLevel');
    if (diagLevel) diagLevel.textContent = `${dbfs.toFixed(1)} dBFS (RMS: ${(rms * 100).toFixed(1)}%)`;
  }

  setSpeakerIndicator(active) {
    this.speakerIndicator.classList.toggle('active', active);
  }

  setMicIndicator(active) {
    this.micIndicator.classList.toggle('active', active);
  }

  // Simulation State Machine Execution (Section 8)
  async startSimulation() {
    if (this.isSimulating) return;

    if (this.currentMode === 'ACOUSTIC') {
      const micOk = await this.audioEngine.initMicrophone();
      if (!micOk) {
        this.fallbackBanner.style.display = 'flex';
        return;
      }
    }

    this.isSimulating = true;
    this.isPaused = false;
    this.currentTurnIndex = 0;

    this.startSimBtn.style.display = 'none';
    this.pauseSimBtn.style.display = 'inline-flex';
    this.resumeSimBtn.style.display = 'none';
    this.stopSimBtn.style.display = 'inline-flex';
    this.sessionPurgedBanner.style.display = 'none';
    this.whisperFallbackBanner.style.display = 'none';

    this.setProtectionState(true);
    this.resetState();
    this.clearTimeline();

    const scenario = SCENARIOS[this.activeScenarioKey];
    this.logTimeline(`Started ${scenario.name} in ${this.currentMode} mode with STT: ${this.sttMode}.`, 'info');

    if (this.currentMode === 'ACOUSTIC') {
      this.audioEngine.startMonitoring();
      this.setMicIndicator(true);
    } else if (this.currentMode === 'DIRECT_AUDIO') {
      this.audioEngine.startMonitoring();
    }

    this.runTurnSequence();
  }

  pauseSimulation() {
    this.isPaused = true;
    this.pauseSimBtn.style.display = 'none';
    this.resumeSimBtn.style.display = 'inline-flex';
    if (this.audioEngine.currentAudio) {
      this.audioEngine.currentAudio.pause();
    }
    this.logTimeline('Simulation paused by user.', 'info');
  }

  resumeSimulation() {
    this.isPaused = false;
    this.pauseSimBtn.style.display = 'inline-flex';
    this.resumeSimBtn.style.display = 'none';
    if (this.audioEngine.currentAudio) {
      this.audioEngine.currentAudio.play();
    } else {
      this.runTurnSequence();
    }
    this.logTimeline('Simulation resumed.', 'info');
  }

  async runTurnSequence() {
    const scenario = SCENARIOS[this.activeScenarioKey];
    const turns = scenario.turns;

    while (this.currentTurnIndex < turns.length && this.isSimulating) {
      if (this.isPaused) return;

      const turn = turns[this.currentTurnIndex];
      this.progressTimer.textContent = `Turn ${turn.turn} / ${turns.length}`;

      // Step 1: PLAY CALLER AUDIO
      if (this.currentMode === 'ACOUSTIC') {
        this.highlightAudioPathStage('caller_voice');
        this.audioEngine.startMicRecording(); // Accumulate mic audio for Whisper
      } else if (this.currentMode === 'DIRECT_AUDIO') {
        this.highlightAudioPathStage('caller_wav');
      } else {
        this.highlightAudioPathStage('script');
      }

      if (this.currentMode === 'ACOUSTIC' || this.currentMode === 'DIRECT_AUDIO') {
        if (turn.audioFile) {
          const played = await this.audioEngine.playCallerAudio(turn.audioFile);
          if (!played && this.isSimulating) {
            console.warn('Audio playback error; proceeding with synchronized turn flow.');
          }
        } else {
          await new Promise(r => setTimeout(r, 2000));
        }
      } else {
        await new Promise(r => setTimeout(r, 2600));
      }

      if (!this.isSimulating || this.isPaused) return;

      // Step 2: WHISPER TRANSCRIPTION OR DETERMINISTIC SCRIPT (Section 8)
      let actualTranscript = '';
      let sttEngineName = 'DEMO STT — DETERMINISTIC SCRIPT';
      let sourceTag = 'DEMO SCRIPT';

      if (this.sttMode === 'WHISPER' && this.whisperEngine.isReady()) {
        this.highlightAudioPathStage('whisper');
        this.updateSttBadgeLabel(true); // TRANSCRIBING...

        try {
          let pcmToTranscribe = null;

          if (this.currentMode === 'ACOUSTIC') {
            pcmToTranscribe = this.audioEngine.stopMicRecording();
            sttEngineName = 'WHISPER — LIVE MICROPHONE';
            sourceTag = 'WHISPER — LIVE MICROPHONE';
          } else {
            // DIRECT_AUDIO: parse WAV into Float32Array
            if (turn.audioFile) {
              const resp = await fetch(turn.audioFile);
              const arrayBuf = await resp.arrayBuffer();
              pcmToTranscribe = parseWavToFloat32(arrayBuf);
            } else {
              pcmToTranscribe = new Float32Array(16000); // 1s silence fallback
            }
            sttEngineName = 'WHISPER — DIRECT LOCAL AUDIO';
            sourceTag = 'WHISPER — DIRECT LOCAL AUDIO';
          }

          const whisperResult = await this.whisperEngine.transcribe(pcmToTranscribe, {
            language: scenario.language
          });

          actualTranscript = whisperResult.transcript;
          this.updateWhisperDiagnostics(whisperResult);
        } catch (err) {
          console.error('Whisper transcription error:', err);
          this.logTimeline(`Whisper error: ${err.message}. Offering fallback to deterministic script.`, 'info');
          actualTranscript = turn.scriptedText;
          sttEngineName = 'DEMO STT (FALLBACK)';
          sourceTag = 'DEMO FALLBACK';
          this.whisperFallbackBanner.style.display = 'flex';
        }
      } else {
        actualTranscript = turn.scriptedText;
        sttEngineName = 'DEMO STT — DETERMINISTIC SCRIPT';
        sourceTag = 'DEMO SCRIPT';
      }

      this.updateSttBadgeLabel(false);
      this.appendTranscript(turn.speaker, actualTranscript);

      // Step 3: SCAM SIGNAL EXTRACTION AGAINST ACTUAL TRANSCRIPT (Section 12)
      this.highlightAudioPathStage('engine');
      const detectedSignals = extractScamSignals(actualTranscript);

      // Step 4: RISK ENGINE SCORING
      const evalResult = this.riskEngine.evaluateTurn(detectedSignals);
      this.updateRisk(evalResult.prevScore, evalResult.delta, evalResult.newScore, evalResult.explanation);

      // Activate tactics on grid
      evalResult.activeTactics.forEach(id => this.activateTactic(id));

      // Step 4B: LOCAL QWEN SEMANTIC REASONING & EXPLANATION (Phase 4B)
      let qwenResult = null;
      if (this.qwenAdapter && this.qwenAdapter.isReady()) {
        if (this.qwenFallbackBanner) this.qwenFallbackBanner.style.display = 'none';
        try {
          const qwenContext = {
            turn: turn.turn,
            detectedSignals: detectedSignals,
            currentRisk: evalResult.newScore,
            previousRisk: evalResult.prevScore,
            riskDelta: evalResult.delta,
            previousEvidence: evalResult.activeTactics.map(id => TACTICS_CONFIG[id]?.name || id)
          };
          qwenResult = await this.qwenAdapter.analyze(actualTranscript, qwenContext);
          this.renderWhySuspiciousPanel(qwenResult, detectedSignals, evalResult);
          this.updateQwenDiagnostics();
        } catch (qErr) {
          console.error('Qwen analysis exception:', qErr);
          if (this.qwenFallbackBanner) this.qwenFallbackBanner.style.display = 'flex';
          this.renderWhySuspiciousFallback(detectedSignals, evalResult);
        }
      } else {
        this.renderWhySuspiciousFallback(detectedSignals, evalResult);
      }

      // Escalation Explanation Display (Section 12)
      if (qwenResult && qwenResult.escalation_explanation) {
        this.showEscalationBanner(
          evalResult.prevScore,
          evalResult.newScore,
          evalResult.delta,
          detectedSignals.length > 0 ? detectedSignals[0].name : 'Escalation',
          qwenResult.escalation_explanation
        );
      } else {
        this.hideEscalationBanner();
      }

      // Step 5: UPDATE EVIDENCE INSPECTOR (Section 13)
      if (detectedSignals.length > 0) {
        const primarySig = detectedSignals[0];
        this.updateEvidenceInspector(
          primarySig.name,
          primarySig.snippet,
          sourceTag,
          `+${primarySig.weight} pts`,
          'Not reported' // Strictly Section 10
        );
      } else {
        this.updateEvidenceInspector('None', 'Benign customer inquiry pattern.', sourceTag, '+0 pts', 'Not reported');
      }

      // Step 6: TIMELINE EVENT (Section 14)
      const timeStr = new Date().toTimeString().split(' ')[0];
      const audioFileName = turn.audioFile ? turn.audioFile.split('/').pop() : 'SYNTH';
      const sigNames = detectedSignals.map(s => s.name).join(', ') || 'No Threat';
      this.addTimelineEntry(
        timeStr,
        `Turn ${turn.turn} • ${audioFileName}`,
        sttEngineName,
        actualTranscript,
        sigNames,
        `${evalResult.prevScore} → +${evalResult.delta} → ${evalResult.newScore}/100`,
        detectedSignals.length > 0
      );

      // Send derived telemetry only (zero raw PCM)
      this.broadcastTelemetry({
        type: 'RISK_UPDATE',
        riskScore: evalResult.newScore,
        riskLevel: this.getRiskLevel(evalResult.newScore),
        explanation: evalResult.explanation,
        turn: turn.turn,
        transcriptSnippet: `[${turn.speaker}]: ${actualTranscript}`,
        signals: detectedSignals.map(s => s.id)
      });

      // Check Critical Warning Threshold
      if (evalResult.newScore >= 80) {
        if (qwenResult && qwenResult.critical_why) {
          if (this.criticalQwenWhy) {
            this.criticalQwenWhy.textContent = `LOCAL AI EXPLANATION: ${qwenResult.critical_why}`;
            this.criticalQwenWhy.style.display = 'block';
          }
          if (this.modalQwenWhyText) {
            this.modalQwenWhyText.textContent = qwenResult.critical_why;
          }
          if (this.modalQwenWhyBox) {
            this.modalQwenWhyBox.style.display = 'block';
          }
        } else {
          if (this.criticalQwenWhy) this.criticalQwenWhy.style.display = 'none';
          if (this.modalQwenWhyBox) this.modalQwenWhyBox.style.display = 'none';
        }
        this.showCriticalModal(evalResult.newScore, evalResult.activeTactics);
      }

      this.currentTurnIndex++;

      // Inter-turn pause
      if (this.currentTurnIndex < turns.length) {
        await new Promise(r => setTimeout(r, 1500));
      }
    }

    if (this.isSimulating) {
      this.logTimeline('Scenario playback completed.', 'info');
      this.pauseSimBtn.style.display = 'none';
      this.resumeSimBtn.style.display = 'none';
      this.startSimBtn.style.display = 'inline-flex';
      this.startSimBtn.innerHTML = '<span class="btn-icon">↺</span> Replay Simulation';
      this.isSimulating = false;
    }
  }

  updateWhisperDiagnostics(result) {
    if (this.diagWhisperAudioDur) this.diagWhisperAudioDur.textContent = `${result.audioDuration.toFixed(1)} s`;
    if (this.diagWhisperInferTime) this.diagWhisperInferTime.textContent = `${result.inferenceTime.toFixed(1)} s`;
    if (this.diagWhisperRtf) this.diagWhisperRtf.textContent = `${result.realTimeFactor.toFixed(2)}x`;
  }

  updateRisk(prevScore, delta, newScore, explanation) {
    this.riskScoreEl.textContent = newScore;
    this.scoreDeltaCalc.textContent = `${prevScore} → +${delta} → ${newScore}`;
    this.scoreDeltaReason.textContent = explanation;

    const maxOffset = 515.22;
    const progress = Math.min(Math.max(newScore, 0), 100) / 100;
    const offset = maxOffset * (1 - progress);
    this.gaugeFill.style.strokeDashoffset = offset;

    let color = 'var(--color-green)';
    let levelClass = 'level-low';
    let levelLabel = 'LOW RISK';

    if (newScore >= 80) {
      color = 'var(--color-red)';
      levelClass = 'level-critical';
      levelLabel = 'CRITICAL';
      this.criticalBanner.style.display = 'flex';
      this.recText.textContent = 'CRITICAL ALERT: End call immediately. Do NOT transfer funds or disclose OTP.';
    } else if (newScore >= 60) {
      color = 'var(--color-orange)';
      levelClass = 'level-high';
      levelLabel = 'HIGH RISK';
      this.criticalBanner.style.display = 'none';
      this.recText.textContent = 'High-risk scam tactics detected. Verify caller identity independently.';
    } else if (newScore >= 30) {
      color = 'var(--color-amber)';
      levelClass = 'level-suspicious';
      levelLabel = 'SUSPICIOUS';
      this.criticalBanner.style.display = 'none';
      this.recText.textContent = 'Suspicious conversational markers detected. Exercise heightened vigilance.';
    } else {
      color = 'var(--color-green)';
      levelClass = 'level-low';
      levelLabel = 'LOW RISK';
      this.criticalBanner.style.display = 'none';
      this.recText.textContent = explanation || 'Conversation patterns appear normal.';
    }

    this.gaugeFill.style.stroke = color;
    this.riskLevelEl.className = `risk-level-badge ${levelClass}`;
    this.riskLevelEl.textContent = levelLabel;
  }

  getRiskLevel(score) {
    if (score >= 80) return 'CRITICAL';
    if (score >= 60) return 'HIGH';
    if (score >= 30) return 'SUSPICIOUS';
    return 'LOW';
  }

  activateTactic(tacticId) {
    const el = document.getElementById(`tactic-${tacticId}`);
    if (el) {
      el.classList.add('active');
    }
    const activeCount = document.querySelectorAll('.tactic-item.active').length;
    this.activeTacticsCount.textContent = `${activeCount} Active`;
  }

  updateEvidenceInspector(signal, snippet, source, contrib, conf) {
    this.evSignal.textContent = signal;
    this.evSnippet.textContent = `"${snippet}"`;
    this.evSource.textContent = source;
    this.evContrib.textContent = contrib;
    this.evConf.textContent = `(Confidence: ${conf})`;
    if (signal !== 'None') {
      this.evidenceTag.textContent = 'FLAGGED THREAT';
      this.evidenceTag.className = 'evidence-tag flagged';
    } else {
      this.evidenceTag.textContent = 'STANDBY';
      this.evidenceTag.className = 'evidence-tag';
    }
  }

  appendTranscript(speaker, text) {
    const container = document.createElement('div');
    container.style.marginBottom = '6px';

    const spk = document.createElement('span');
    spk.className = 'transcript-speaker';
    spk.textContent = speaker;

    const body = document.createElement('span');
    body.textContent = text;

    container.appendChild(spk);
    container.appendChild(body);
    this.transcriptViewer.appendChild(container);
    this.transcriptViewer.scrollTop = this.transcriptViewer.scrollHeight;
  }

  addTimelineEntry(time, turnInfo, sttEngine, transcript, signals, deltaCalc, isTactic) {
    const emptyNotice = this.timelineList.querySelector('.timeline-empty');
    if (emptyNotice) emptyNotice.remove();

    const entry = document.createElement('div');
    entry.className = `timeline-entry ${isTactic ? 'tactic' : ''}`;
    entry.innerHTML = `
      <div class="timeline-top-row">
        <span class="timeline-turn">${turnInfo}</span>
        <span class="timeline-stt-tag">${sttEngine}</span>
        <span class="timeline-time">${time}</span>
      </div>
      <div class="timeline-transcript">"${transcript}"</div>
      <div class="timeline-signals-row">
        <span class="timeline-signal-tag">${signals}</span>
        <span class="timeline-delta-badge">${deltaCalc}</span>
      </div>
    `;
    this.timelineList.prepend(entry);
  }

  logTimeline(message, type = 'info') {
    const emptyNotice = this.timelineList.querySelector('.timeline-empty');
    if (emptyNotice) emptyNotice.remove();

    const now = new Date();
    const timeStr = now.toTimeString().split(' ')[0];

    const entry = document.createElement('div');
    entry.className = `timeline-entry ${type === 'tactic' ? 'tactic' : ''}`;
    entry.innerHTML = `
      <div class="timeline-top-row">
        <span class="timeline-time">${timeStr}</span>
      </div>
      <div class="timeline-transcript" style="font-style: normal; color: var(--text-secondary);">${message}</div>
    `;
    this.timelineList.prepend(entry);
  }

  clearTimeline() {
    this.timelineList.innerHTML = '<div class="timeline-empty">No scam indicators recorded. Live events will appear chronologically.</div>';
  }

  // Phase 5: Interactive Two-Way Conversation Methods
  setInteractiveState(state, detailText = '') {
    this.interactiveState = state;
    if (!this.convStatePill) return;

    this.convStatePill.className = `conv-state-pill state-${state.toLowerCase()}`;
    const dot = '<span class="status-dot"></span>';

    switch (state) {
      case INTERACTIVE_STATES.IDLE:
        this.convStatePill.innerHTML = `${dot} IDLE — Ready to start`;
        if (this.convMicStatus) this.convMicStatus.textContent = 'Microphone standby';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
      case INTERACTIVE_STATES.LISTENING:
        this.convStatePill.innerHTML = `${dot} LISTENING — Speak now`;
        if (this.convMicStatus) this.convMicStatus.textContent = '● Mic ACTIVE — Voice Activity Detection listening for speech...';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = false;
        break;
      case INTERACTIVE_STATES.TRANSCRIBING:
        this.convStatePill.innerHTML = `${dot} TRANSCRIBING — Local Whisper`;
        if (this.convMicStatus) this.convMicStatus.textContent = '⏳ Mic PAUSED — Transcribing user utterance via Whisper WASM';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
      case INTERACTIVE_STATES.ANALYZING:
        this.convStatePill.innerHTML = `${dot} ANALYZING — ScamShield`;
        if (this.convMicStatus) this.convMicStatus.textContent = '🔍 Evaluating tactics against 12 Indian scam taxonomy rules...';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
      case INTERACTIVE_STATES.RESPONDING:
        this.convStatePill.innerHTML = `${dot} RESPONDING — Simulated caller`;
        if (this.convMicStatus) this.convMicStatus.textContent = '🤖 Generating simulated scammer dialogue (Qwen 1.5B / Scripted Fallback)...';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
      case INTERACTIVE_STATES.SPEAKING:
        this.convStatePill.innerHTML = `${dot} SPEAKING — Microphone Input Ignored`;
        if (this.convMicStatus) this.convMicStatus.textContent = '🔇 Mic MUTED — Feedback suppression active during caller playback';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
      case INTERACTIVE_STATES.ERROR:
        this.convStatePill.innerHTML = `${dot} ERROR — Retry / Exit`;
        if (this.convMicStatus) this.convMicStatus.textContent = detailText || 'An error occurred during interactive conversation';
        if (this.doneSpeakingBtn) this.doneSpeakingBtn.disabled = true;
        break;
    }
  }

  appendDialogueBubble(speaker, text, sourceBadge) {
    if (!this.convStreamBox) return;

    const empty = this.convStreamBox.querySelector('.conv-empty-state');
    if (empty) empty.remove();

    const bubble = document.createElement('div');
    const isUser = speaker.toUpperCase() === 'USER';
    bubble.className = `conv-bubble ${isUser ? 'bubble-user' : 'bubble-scammer'}`;

    bubble.innerHTML = `
      <div class="conv-bubble-header">
        <span class="conv-bubble-speaker">${speaker}</span>
        <span class="conv-bubble-source ${isUser ? 'source-user' : 'source-scammer'}">${sourceBadge}</span>
      </div>
      <div class="conv-bubble-text">"${text}"</div>
    `;

    this.convStreamBox.appendChild(bubble);
    this.convStreamBox.scrollTop = this.convStreamBox.scrollHeight;
  }

  addConversationTurn(speaker, text) {
    this.conversationHistory.push({ speaker, text, timestamp: Date.now() });
    if (this.conversationHistory.length > 6) {
      this.conversationHistory.shift(); // Max 6 turns bounded context
    }
  }

  async startInteractiveCall() {
    if (this.interactiveState !== INTERACTIVE_STATES.IDLE && this.isTurnProcessing) {
      return;
    }

    const micOk = await this.audioEngine.initMicrophone();
    if (!micOk) {
      console.warn('Hardware microphone not granted/available; engaging simulated mic loopback.');
      this.logTimeline('Hardware mic not active; engaging simulated microphone loopback.', 'info');
    }

    // Reset session data
    this.resetState();
    this.clearTimeline();
    this.conversationHistory = [];
    this.interactiveStage = 'AUTHORITY';
    this.isSimulating = true;
    this.isTurnProcessing = false;
    this.setProtectionState(true);

    if (this.convStreamBox) {
      this.convStreamBox.innerHTML = '';
    }

    if (this.startInteractiveBtn) this.startInteractiveBtn.style.display = 'none';
    if (this.stopInteractiveBtn) this.stopInteractiveBtn.style.display = 'inline-flex';

    // Start mic monitoring for waveform
    this.audioEngine.startMonitoring();
    this.setMicIndicator(true);

    // Setup VAD callbacks
    this.audioEngine.onEndOfUtterance = (pcm) => {
      if (this.interactiveState === INTERACTIVE_STATES.LISTENING && !this.isTurnProcessing) {
        this.handleInteractiveUserSpeech(pcm);
      }
    };
    this.audioEngine.onVadStateChange = (vadState) => {
      if (this.interactiveState === INTERACTIVE_STATES.LISTENING) {
        if (vadState === 'USER_SPEAKING') {
          this.convMicStatus.textContent = '🎙️ USER SPEAKING DETECTED... (Speak naturally, pause when done)';
        } else {
          this.convMicStatus.textContent = '● Mic ACTIVE — Voice Activity Detection listening for speech...';
        }
      }
    };

    this.logTimeline('Interactive Simulation started. Call initiated by simulated scammer.', 'info');

    // Turn 0: Scammer Opening Call
    const openingText = "Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi. Your Aadhaar identity has been linked to twenty-four fraudulent bank accounts involved in international money laundering.";

    this.setInteractiveState(INTERACTIVE_STATES.RESPONDING, 'Simulated caller opening');
    this.highlightAudioPathStage('qwen');

    // Append Scammer Bubble
    this.appendDialogueBubble('SIMULATED SCAMMER', openingText, 'SIMULATED SCAMMER — SCRIPTED OPENING');
    this.addConversationTurn('SIMULATED SCAMMER', openingText);
    this.appendTranscript('Caller (Fake CBI Inspector)', openingText);

    // Evaluate scammer speech with Scam Rules + Risk Engine
    this.highlightAudioPathStage('engine');
    const detectedSignals = extractScamSignals(openingText);
    const evalResult = this.riskEngine.evaluateTurn(detectedSignals);
    this.updateRisk(evalResult.prevScore, evalResult.delta, evalResult.newScore, evalResult.explanation);
    evalResult.activeTactics.forEach(id => this.activateTactic(id));

    // Update Evidence Inspector & Timeline
    if (detectedSignals.length > 0) {
      this.updateEvidenceInspector(detectedSignals[0].name, detectedSignals[0].snippet, 'SIMULATED SCAMMER', `+${detectedSignals[0].weight} pts`, 'Not reported');
    }
    this.addTimelineEntry(
      new Date().toTimeString().split(' ')[0],
      'Turn 0 • SCAMMER GREETING',
      'SIMULATED SCAMMER',
      openingText,
      detectedSignals.map(s => s.name).join(', ') || 'None',
      `${evalResult.prevScore} → +${evalResult.delta} → ${evalResult.newScore}/100`,
      true
    );

    // Qwen explanation if ready
    if (this.qwenAdapter && this.qwenAdapter.isReady()) {
      try {
        const qwenRes = await this.qwenAdapter.analyze(openingText, {
          turn: 0,
          detectedSignals,
          currentRisk: evalResult.newScore,
          previousRisk: 0,
          riskDelta: evalResult.delta
        });
        this.renderWhySuspiciousPanel(qwenRes, detectedSignals, evalResult);
      } catch (e) {
        this.renderWhySuspiciousFallback(detectedSignals, evalResult);
      }
    } else {
      this.renderWhySuspiciousFallback(detectedSignals, evalResult);
    }

    // Speak caller opening
    this.highlightAudioPathStage('speaker');
    this.setInteractiveState(INTERACTIVE_STATES.SPEAKING, 'Simulated caller speaking');
    await this.audioEngine.speakText(openingText);

    // Transition to LISTENING for User
    this.highlightAudioPathStage('user_mic');
    this.setInteractiveState(INTERACTIVE_STATES.LISTENING, 'Speak now');
    this.audioEngine.startMicRecording();
  }

  async handleInteractiveUserSpeech(pcmFloat32) {
    if (this.isTurnProcessing) return;
    this.isTurnProcessing = true;
    const turnStartTime = performance.now();

    try {
      // 1. Transcribe User Speech via Local Whisper
      this.setInteractiveState(INTERACTIVE_STATES.TRANSCRIBING, 'Local Whisper');
      this.highlightAudioPathStage('whisper');
      const sttStartTime = performance.now();

      let userTranscript = '';
      let isWhisperActual = false;

      if (this.whisperEngine && this.whisperEngine.isReady()) {
        try {
          const res = await this.whisperEngine.transcribe(pcmFloat32, { language: 'en' });
          userTranscript = (res.transcript || '').trim();
          this.updateWhisperDiagnostics(res);
          isWhisperActual = true;
        } catch (sttErr) {
          console.warn('Whisper transcription error in interactive mode:', sttErr);
          userTranscript = 'Why are you calling me? Can you send me an official notice?';
        }
      } else {
        userTranscript = 'Why are you calling me? Can you send me an official notice?';
      }

      const userSttDuration = (performance.now() - sttStartTime) / 1000;
      if (this.diagUserSttTime) {
        this.diagUserSttTime.textContent = `${userSttDuration.toFixed(2)} s`;
      }

      if (!userTranscript || userTranscript.length < 3) {
        userTranscript = 'Why are you calling me?';
      }

      // 2. Add User Dialogue Bubble
      this.appendDialogueBubble('USER', userTranscript, isWhisperActual ? 'WHISPER — ON DEVICE' : 'USER (LOCAL CAPTURE)');
      this.addConversationTurn('USER', userTranscript);
      this.appendTranscript('User', userTranscript);

      // 3. Analyzing & Responding
      this.setInteractiveState(INTERACTIVE_STATES.ANALYZING, 'ScamShield rules');
      this.highlightAudioPathStage('engine');

      this.setInteractiveState(INTERACTIVE_STATES.RESPONDING, 'Simulated caller');
      this.highlightAudioPathStage('qwen');

      const qwenGenStartTime = performance.now();
      const qwenResp = await this.qwenAdapter.generateScammerResponse(userTranscript, {
        history: this.conversationHistory,
        stage: this.interactiveStage,
        riskScore: this.riskEngine.score
      });
      const qwenGenDuration = (performance.now() - qwenGenStartTime) / 1000;
      if (this.diagQwenRespTime) {
        this.diagQwenRespTime.textContent = `${qwenGenDuration.toFixed(2)} s`;
      }

      this.interactiveStage = qwenResp.stage;
      if (this.convStageBadge) {
        this.convStageBadge.textContent = `Stage: ${this.interactiveStage}`;
      }

      const scammerText = qwenResp.text;
      const isQwenGenerated = qwenResp.method === 'LOCAL QWEN';

      // 4. Add Scammer Dialogue Bubble
      this.appendDialogueBubble(
        'SIMULATED SCAMMER',
        scammerText,
        isQwenGenerated ? 'SIMULATED SCAMMER — LOCAL QWEN' : 'SIMULATED SCAMMER — SCRIPTED FALLBACK'
      );
      this.addConversationTurn('SIMULATED SCAMMER', scammerText);
      this.appendTranscript('Caller (Fake CBI Inspector)', scammerText);

      // 5. Run Scam Rules & Deterministic Risk Engine on Scammer Speech
      this.highlightAudioPathStage('engine');
      const detectedSignals = extractScamSignals(scammerText);
      const evalResult = this.riskEngine.evaluateTurn(detectedSignals);
      this.updateRisk(evalResult.prevScore, evalResult.delta, evalResult.newScore, evalResult.explanation);
      evalResult.activeTactics.forEach(id => this.activateTactic(id));

      if (detectedSignals.length > 0) {
        this.updateEvidenceInspector(detectedSignals[0].name, detectedSignals[0].snippet, isQwenGenerated ? 'LOCAL QWEN' : 'SCRIPTED FALLBACK', `+${detectedSignals[0].weight} pts`, 'Not reported');
      }

      this.addTimelineEntry(
        new Date().toTimeString().split(' ')[0],
        `Interactive Turn • ${this.interactiveStage}`,
        isQwenGenerated ? 'LOCAL QWEN' : 'SCRIPTED FALLBACK',
        scammerText,
        detectedSignals.map(s => s.name).join(', ') || 'Escalation Pressure',
        `${evalResult.prevScore} → +${evalResult.delta} → ${evalResult.newScore}/100`,
        detectedSignals.length > 0
      );

      // Semantic Explanation via Qwen
      if (this.qwenAdapter && this.qwenAdapter.isReady()) {
        try {
          const qwenAnalysis = await this.qwenAdapter.analyze(scammerText, {
            turn: this.conversationHistory.length,
            detectedSignals,
            currentRisk: evalResult.newScore,
            previousRisk: evalResult.prevScore,
            riskDelta: evalResult.delta
          });
          this.renderWhySuspiciousPanel(qwenAnalysis, detectedSignals, evalResult);
        } catch (e) {
          this.renderWhySuspiciousFallback(detectedSignals, evalResult);
        }
      } else {
        this.renderWhySuspiciousFallback(detectedSignals, evalResult);
      }

      // 6. Check Critical Intervention Threshold (Risk >= 80)
      if (evalResult.newScore >= 80) {
        this.showCriticalIntervention(evalResult.newScore, evalResult.activeTactics);
        this.setInteractiveState(INTERACTIVE_STATES.IDLE, 'Critical intervention triggered');
        this.isTurnProcessing = false;
        return; // Halt conversational loop immediately!
      }

      // 7. Local Voice Output
      this.highlightAudioPathStage('speaker');
      this.setInteractiveState(INTERACTIVE_STATES.SPEAKING, 'Simulated caller (Microphone Input Ignored)');
      const ttsStartTime = performance.now();
      await this.audioEngine.speakText(scammerText);
      const ttsDuration = (performance.now() - ttsStartTime) / 1000;
      if (this.diagVoiceOutputTime) {
        this.diagVoiceOutputTime.textContent = `${ttsDuration.toFixed(2)} s`;
      }

      const totalTurnDuration = (performance.now() - turnStartTime) / 1000;
      if (this.diagTotalTurnLatency) {
        this.diagTotalTurnLatency.textContent = `${totalTurnDuration.toFixed(2)} s`;
      }

      // 8. Return to Listening
      if (this.isSimulating) {
        this.highlightAudioPathStage('user_mic');
        this.setInteractiveState(INTERACTIVE_STATES.LISTENING, 'Speak now');
        this.audioEngine.startMicRecording();
      }
    } catch (turnErr) {
      console.error('Interactive turn error:', turnErr);
      this.setInteractiveState(INTERACTIVE_STATES.ERROR, turnErr.message);
    } finally {
      this.isTurnProcessing = false;
    }
  }

  showCriticalIntervention(score, activeTactics) {
    this.modalScorePill.textContent = `CRITICAL RISK: ${score} / 100 — POSSIBLE SCAM`;
    this.modalTacticsList.innerHTML = '';

    activeTactics.forEach(id => {
      const cfg = TACTICS_CONFIG[id];
      const li = document.createElement('li');
      li.textContent = `⚠ ${cfg ? cfg.name : id} (${cfg ? cfg.desc : ''})`;
      this.modalTacticsList.appendChild(li);
    });

    if (this.modalQwenWhyBox) {
      this.modalQwenWhyBox.style.display = 'block';
    }
    if (this.modalQwenWhyText) {
      this.modalQwenWhyText.textContent = 'The caller has combined authority impersonation, legal threats, and coercive pressure. Digital arrest is not a legitimate legal procedure. Law enforcement never conducts interrogations or demands financial deposits over phone calls or video.';
    }

    this.criticalModal.style.display = 'flex';
    this.logTimeline('CRITICAL SAFETY INTERVENTION: Conversational loop interrupted. Immediate disconnection recommended.', 'tactic');
  }

  handleManualDoneSpeaking() {
    if (this.interactiveState === INTERACTIVE_STATES.LISTENING && !this.isTurnProcessing) {
      const pcm = this.audioEngine.stopMicRecording();
      this.handleInteractiveUserSpeech(pcm);
    }
  }

  toggleAutoVad() {
    this.isVadAuto = !this.isVadAuto;
    this.audioEngine.vadEnabled = this.isVadAuto;
    if (this.toggleVadBtn) {
      this.toggleVadBtn.classList.toggle('active', this.isVadAuto);
      this.toggleVadBtn.textContent = this.isVadAuto ? '⚡ VAD: Auto' : '⚡ VAD: Manual';
    }
    this.logTimeline(`Voice Activity Detection switched to ${this.isVadAuto ? 'AUTO' : 'MANUAL'}.`, 'info');
  }

  showCriticalModal(score, activeTactics) {
    this.modalScorePill.textContent = `Risk Score: ${score} / 100`;
    this.modalTacticsList.innerHTML = '';

    activeTactics.forEach(id => {
      const cfg = TACTICS_CONFIG[id];
      const li = document.createElement('li');
      li.textContent = `✓ ${cfg ? cfg.name : id} (${cfg ? cfg.desc : ''})`;
      this.modalTacticsList.appendChild(li);
    });

    this.criticalModal.style.display = 'flex';
  }

  stopAndPurge() {
    this.isSimulating = false;
    this.isPaused = false;
    this.currentTurnIndex = 0;
    this.isTurnProcessing = false;
    this.conversationHistory = [];

    this.audioEngine.stopAll();
    this.setSpeakerIndicator(false);
    this.setMicIndicator(false);
    this.setProtectionState(false);
    this.resetState();

    if (this.startInteractiveBtn) this.startInteractiveBtn.style.display = 'inline-flex';
    if (this.stopInteractiveBtn) this.stopInteractiveBtn.style.display = 'none';
    if (this.convStreamBox) {
      this.convStreamBox.innerHTML = '<div class="conv-empty-state">Interactive conversation session purged. Tap "Start Interactive Call" to begin a new simulation.</div>';
    }
    this.setInteractiveState(INTERACTIVE_STATES.IDLE);

    if (this.qwenAdapter) {
      this.qwenAdapter.unload();
      if (this.loadQwenBtn) this.loadQwenBtn.style.display = 'inline-flex';
      if (this.unloadQwenBtn) this.unloadQwenBtn.style.display = 'none';
      if (this.qwenReadyNote) this.qwenReadyNote.style.display = 'none';
    }

    this.startSimBtn.style.display = 'inline-flex';
    this.startSimBtn.innerHTML = '<span class="btn-icon">▶</span> Start Simulation';
    this.pauseSimBtn.style.display = 'none';
    this.resumeSimBtn.style.display = 'none';
    this.stopSimBtn.style.display = 'none';
    this.progressTimer.textContent = '00:00 / 00:00';

    this.sessionPurgedBanner.style.display = 'block';
    setTimeout(() => {
      this.sessionPurgedBanner.style.display = 'none';
    }, 4500);

    this.logTimeline('SESSION PURGED: Audio buffers discarded, transient analysis memory zeroed.', 'info');

    this.broadcastTelemetry({
      type: 'SESSION_CLEARED',
      timestamp: Date.now()
    });
  }

  setProtectionState(active) {
    if (active) {
      this.protectionBadge.textContent = 'PROTECTION ACTIVE';
      this.protectionBadge.className = 'badge active';
      this.recText.textContent = 'Active call protection engaged. Analyzing acoustic speech locally for scam patterns.';
    } else {
      this.protectionBadge.textContent = 'PROTECTION OFF';
      this.protectionBadge.className = 'badge';
      this.recText.textContent = 'Call protection inactive. Select a simulation mode and tap Start.';
    }
  }

  resetState() {
    this.riskEngine.reset();
    this.updateRisk(0, 0, 0, 'No threat detected');
    this.activeTacticsCount.textContent = '0 Active';
    document.querySelectorAll('.tactic-item').forEach(el => el.classList.remove('active'));
    this.updateEvidenceInspector('None', 'Waiting for caller speech...', 'STANDBY', '+0 pts', 'Not reported');
    this.transcriptViewer.innerHTML = '<div class="transcript-notice">Caller audio is processed locally. Detected statements and evidence keywords appear in this pane.</div>';
    this.criticalBanner.style.display = 'none';
    if (this.criticalQwenWhy) this.criticalQwenWhy.style.display = 'none';
    if (this.modalQwenWhyBox) this.modalQwenWhyBox.style.display = 'none';
    this.hideEscalationBanner();
    this.resetWhySuspiciousPanel();
    this.scoreDeltaCalc.textContent = '0 → +0 → 0';
    this.scoreDeltaReason.textContent = 'No threat detected';
    this.conversationHistory = [];
    this.interactiveStage = 'AUTHORITY';
    if (this.convStageBadge) this.convStageBadge.textContent = 'Stage: Authority Impersonation';
    this.updateSttBadgeLabel();
    this.renderAudioPathFlow();
  }

  // WebSocket Companion Communication (Optional Laptop-to-Phone Telemetry)
  autoConnectWs() {
    setTimeout(() => this.connectWs(), 600);
  }

  connectWs() {
    const url = this.wsUrlInput.value.trim();
    if (!url) return;

    if (this.ws) {
      try { this.ws.close(); } catch (e) {}
    }

    try {
      this.ws = new WebSocket(url);
      this.ws.onopen = () => {
        this.connectionPill.classList.add('connected');
        this.connectionPill.querySelector('.status-label').textContent = 'PHONE CONNECTED';
        this.logTimeline('WebSocket session established with mobile companion.', 'info');
      };
      this.ws.onclose = () => {
        this.connectionPill.classList.remove('connected');
        this.connectionPill.querySelector('.status-label').textContent = 'STANDALONE LAPTOP';
      };
      this.ws.onerror = () => {
        this.connectionPill.classList.remove('connected');
        this.connectionPill.querySelector('.status-label').textContent = 'STANDALONE LAPTOP';
      };
    } catch (err) {
      this.connectionPill.classList.remove('connected');
      this.connectionPill.querySelector('.status-label').textContent = 'STANDALONE LAPTOP';
    }
  }

  broadcastTelemetry(payload) {
    // SECURITY GUARANTEE: Never include raw PCM audio or byte arrays in telemetry
    delete payload.rawPcm;
    delete payload.audioBytes;
    delete payload.audioBuffer;

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(payload));
      } catch (e) {
        console.warn('Failed to send telemetry:', e);
      }
    }
  }
}

// Initialize on page load
window.addEventListener('DOMContentLoaded', () => {
  window.dashboard = new DashboardController();
});
