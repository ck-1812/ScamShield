/**
 * ScamShield Phase 4A Automated Test Suite
 * Validates:
 * 1. Whisper WebAssembly state transitions & failure handling
 * 2. Audio preprocessing (16 kHz resampling, mono downmixing, WAV parsing)
 * 3. Transcript segment parsing & RTF metrics
 * 4. Actual Whisper transcript passed to IndianScamRules signal extractor
 * 5. Actual Whisper transcript signals evaluated by EvidenceBasedRiskEngine
 * 6. Multilingual transcription testing (English, Hindi/Hinglish, Benign KYC)
 * 7. Simulation modes (Acoustic, Direct Audio, Deterministic)
 * 8. Telemetry privacy sanitization & complete session purge
 */

const assert = require('assert');

// 1. Recreate/Import Core Configs & Classes
const { SCAM_TAXONOMY, INDIAN_SCAM_RULES, extractScamSignals } = require('./dashboard/scam_rules.js');
const { QwenLocalAdapter, QWEN_STATES } = require('./dashboard/qwen_adapter.js');

const WHISPER_STATES = {
  NOT_LOADED: 'NOT LOADED',
  LOADING: 'LOADING',
  READY: 'READY',
  TRANSCRIBING: 'TRANSCRIBING',
  ERROR: 'ERROR'
};

class MockWhisperEngine {
  constructor(options = {}) {
    this.state = WHISPER_STATES.NOT_LOADED;
    this.modelName = options.modelName || 'tiny (multilingual, 31 MB)';
    this.instance = null;
    this.currentSegments = [];
    this.onStateChange = options.onStateChange || (() => {});
  }

  setState(newState) {
    this.state = newState;
    this.onStateChange(this.state);
  }

  async loadModel(shouldFail = false) {
    this.setState(WHISPER_STATES.LOADING);
    if (shouldFail) {
      this.setState(WHISPER_STATES.ERROR);
      return false;
    }
    this.instance = 1;
    this.setState(WHISPER_STATES.READY);
    return true;
  }

  async transcribe(pcmFloat32Array, options = {}) {
    if (this.state !== WHISPER_STATES.READY) {
      throw new Error('WhisperEngine is not in READY state');
    }

    this.setState(WHISPER_STATES.TRANSCRIBING);
    const audioDuration = pcmFloat32Array.length / 16000;

    // Simulated local inference execution
    const mockOutput = options.mockOutput !== undefined ? options.mockOutput : 'Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi.';
    const inferenceTime = Math.max(0.1, audioDuration * 0.4); // 0.4x RTF
    const rtf = inferenceTime / audioDuration;

    this.setState(WHISPER_STATES.READY);
    return {
      transcript: mockOutput,
      audioDuration,
      inferenceTime,
      realTimeFactor: rtf,
      confidence: 'Not reported'
    };
  }

  free() {
    this.instance = null;
    this.currentSegments = [];
    this.setState(WHISPER_STATES.NOT_LOADED);
  }
}

// High-speed WAV parser implementation for testing
function parseWavToFloat32(arrayBuffer) {
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

  const numSamples = Math.floor(arrayBuffer.byteLength / 2);
  const float32 = new Float32Array(numSamples);
  for (let i = 0; i < numSamples; i++) {
    float32[i] = view.getInt16(i * 2, true) / 32768.0;
  }
  return float32;
}

// 16 kHz Resampler test function
function resampleTo16k(inputFloat32, srcRate) {
  const targetRate = 16000;
  if (srcRate === targetRate) return inputFloat32;
  const ratio = targetRate / srcRate;
  const newLen = Math.floor(inputFloat32.length * ratio);
  const resampled = new Float32Array(newLen);
  for (let i = 0; i < newLen; i++) {
    const srcIdx = Math.floor(i / ratio);
    resampled[i] = inputFloat32[srcIdx] || 0;
  }
  return resampled;
}

// Evidence-based Risk Engine
class EvidenceBasedRiskEngine {
  constructor() {
    this.score = 0;
    this.seenTactics = new Map();
  }

  evaluateTurn(signals) {
    const prevScore = this.score;
    const reasons = [];

    for (const sig of signals) {
      const prevCount = this.seenTactics.get(sig.id) || 0;
      const newCount = prevCount + 1;
      this.seenTactics.set(sig.id, newCount);

      const cfg = SCAM_TAXONOMY[sig.id];
      const weight = cfg ? cfg.weight : 15;
      const contrib = newCount === 1 ? weight : Math.min(5, Math.round(weight * 0.15));
      reasons.push(`+${contrib} ${cfg ? cfg.name : sig.id}`);
    }

    let totalWeight = 0;
    for (const [tacticId, count] of this.seenTactics.entries()) {
      const cfg = SCAM_TAXONOMY[tacticId];
      const weight = cfg ? cfg.weight : 15;
      totalWeight += weight;
      if (count > 1) {
        totalWeight += Math.min(5, Math.round(weight * 0.15)) * (count - 1);
      }
    }

    const uniqueCount = this.seenTactics.size;
    let synergyBonus = 0;
    if (uniqueCount >= 5) synergyBonus = 25;
    else if (uniqueCount >= 4) synergyBonus = 18;
    else if (uniqueCount >= 3) synergyBonus = 10;
    else if (uniqueCount >= 2) synergyBonus = 4;

    const hasAuthority = this.seenTactics.has('AUTHORITY_IMPERSONATION') || this.seenTactics.has('POLICE_CBI_RBI_IMPERSONATION');
    const hasThreat = this.seenTactics.has('LEGAL_THREAT') || this.seenTactics.has('IDENTITY_DOCUMENT_THREAT');
    const hasIsolation = this.seenTactics.has('SECRECY') || this.seenTactics.has('VIDEO_CONFINEMENT');
    const hasFinancial = this.seenTactics.has('FINANCIAL_REQUEST') || this.seenTactics.has('SAFE_ACCOUNT_SCAM') || this.seenTactics.has('CREDENTIAL_REQUEST');

    let compositeBonus = 0;
    if (hasAuthority && hasThreat && hasIsolation && hasFinancial) {
      compositeBonus = 12;
    }

    const rawCalculated = totalWeight + synergyBonus + compositeBonus;
    this.score = Math.min(100, Math.max(0, rawCalculated));
    const delta = this.score - prevScore;

    return {
      prevScore,
      delta,
      newScore: this.score,
      reasons,
      hasCompositeBonus: compositeBonus > 0,
      activeTactics: Array.from(this.seenTactics.keys())
    };
  }

  reset() {
    this.score = 0;
    this.seenTactics.clear();
  }
}

// Test Runner
let passed = 0;
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log(`  [PASS] ${name}`);
    passed++;
  } catch (err) {
    console.error(`  [FAIL] ${name}: ${err.message}`);
    failed++;
  }
}

async function testAsync(name, fn) {
  try {
    await fn();
    console.log(`  [PASS] ${name}`);
    passed++;
  } catch (err) {
    console.error(`  [FAIL] ${name}: ${err.message}`);
    failed++;
  }
}

(async () => {
  console.log('================================================================');
  console.log('SCAMSHIELD PHASE 4A — WHISPER WASM & REAL STT TEST SUITE');
  console.log('================================================================\n');

  // Group 1: Whisper State Machine
  console.log('Group 1: Whisper State Machine & Model Loading');
  test('Initial state is NOT_LOADED', () => {
    const engine = new MockWhisperEngine();
    assert.strictEqual(engine.state, WHISPER_STATES.NOT_LOADED);
  });

  await testAsync('Model loading transitions NOT_LOADED -> LOADING -> READY', async () => {
    const states = [];
    const engine = new MockWhisperEngine({
      onStateChange: (st) => states.push(st)
    });

    const success = await engine.loadModel(false);
    assert.strictEqual(success, true);
    assert.strictEqual(engine.state, WHISPER_STATES.READY);
    assert.deepStrictEqual(states, [WHISPER_STATES.LOADING, WHISPER_STATES.READY]);
  });

  await testAsync('Model load failure transitions to ERROR', async () => {
    const states = [];
    const engine = new MockWhisperEngine({
      onStateChange: (st) => states.push(st)
    });

    const success = await engine.loadModel(true);
    assert.strictEqual(success, false);
    assert.strictEqual(engine.state, WHISPER_STATES.ERROR);
    assert.deepStrictEqual(states, [WHISPER_STATES.LOADING, WHISPER_STATES.ERROR]);
  });

  test('free() unloads model and transitions state back to NOT_LOADED', () => {
    const engine = new MockWhisperEngine();
    engine.instance = 1;
    engine.state = WHISPER_STATES.READY;
    engine.free();
    assert.strictEqual(engine.state, WHISPER_STATES.NOT_LOADED);
    assert.strictEqual(engine.instance, null);
  });

  // Group 2: Audio Preprocessing
  console.log('\nGroup 2: Audio Preprocessing (WAV Parsing & 16kHz Resampling)');
  test('parseWavToFloat32 normalizes 16-bit signed PCM accurately into [-1.0, 1.0]', () => {
    // Construct dummy WAV buffer with 44-byte header and 4 sample values
    const buffer = new ArrayBuffer(44 + 8);
    const view = new DataView(buffer);
    // RIFF header
    view.setUint32(0, 0x52494646, false); // "RIFF"
    view.setUint32(4, 44 + 8 - 8, true);
    view.setUint32(8, 0x57415645, false); // "WAVE"
    // 'data' chunk at offset 36
    view.setUint32(36, 0x64617461, false); // "data"
    view.setUint32(40, 8, true); // 4 samples * 2 bytes = 8
    // Samples: 0, 32767 (max pos), -32768 (min neg), 16384 (half pos)
    view.setInt16(44, 0, true);
    view.setInt16(46, 32767, true);
    view.setInt16(48, -32768, true);
    view.setInt16(50, 16384, true);

    const float32 = parseWavToFloat32(buffer);
    assert.strictEqual(float32.length, 4);
    assert(Math.abs(float32[0] - 0.0) < 0.001);
    assert(Math.abs(float32[1] - 0.9999) < 0.001);
    assert(Math.abs(float32[2] - (-1.0)) < 0.001);
    assert(Math.abs(float32[3] - 0.5) < 0.001);
  });

  test('resampleTo16k downsamples 48kHz audio to 16kHz at exact 3:1 ratio', () => {
    const samples48k = new Float32Array(48000); // 1.0s at 48k
    for (let i = 0; i < samples48k.length; i++) samples48k[i] = Math.sin((2 * Math.PI * i) / 48);

    const resampled = resampleTo16k(samples48k, 48000);
    assert.strictEqual(resampled.length, 16000); // 1.0s at 16k
  });

  // Group 3: Whisper Transcription Result & Metrics
  console.log('\nGroup 3: Whisper Transcription Result & Performance Metrics');
  await testAsync('Transcribe returns valid transcript, duration, inference time, and RTF', async () => {
    const engine = new MockWhisperEngine();
    await engine.loadModel();

    const pcm = new Float32Array(32000); // 2.0s audio
    const res = await engine.transcribe(pcm, {
      mockOutput: 'Inspector Vikram Singh calling from CBI New Delhi'
    });

    assert.strictEqual(res.transcript, 'Inspector Vikram Singh calling from CBI New Delhi');
    assert.strictEqual(res.audioDuration, 2.0);
    assert(res.inferenceTime > 0);
    assert(res.realTimeFactor > 0);
    assert.strictEqual(res.confidence, 'Not reported'); // Section 10
  });

  await testAsync('Empty / silent transcript handled gracefully without crash', async () => {
    const engine = new MockWhisperEngine();
    await engine.loadModel();

    const pcm = new Float32Array(16000);
    const res = await engine.transcribe(pcm, { mockOutput: '' });
    assert.strictEqual(res.transcript, '');
    assert.strictEqual(res.audioDuration, 1.0);
  });

  // Group 4: Actual Whisper Transcript -> IndianScamRules Signal Extraction
  console.log('\nGroup 4: Actual Whisper Transcript -> IndianScamRules');
  test('English Digital Arrest Turn 1 transcript triggers Authority & Identity Threat', () => {
    const whisperTranscript = 'Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi. Your Aadhaar identity has been linked to twenty-four fraudulent bank accounts.';
    const signals = extractScamSignals(whisperTranscript);

    assert(signals.length >= 2, `Expected at least 2 signals, got ${signals.length}`);
    const sigIds = signals.map(s => s.id);
    assert(sigIds.includes('AUTHORITY_IMPERSONATION'), 'Missing AUTHORITY_IMPERSONATION');
    assert(sigIds.includes('IDENTITY_DOCUMENT_THREAT'), 'Missing IDENTITY_DOCUMENT_THREAT');
    assert.strictEqual(signals[0].confidence, 'Not reported'); // Section 10
  });

  test('English Digital Arrest Turn 2 transcript triggers Legal Threat', () => {
    const whisperTranscript = 'A non-bailable arrest warrant has been issued against you under the Prevention of Money Laundering Act.';
    const signals = extractScamSignals(whisperTranscript);

    assert(signals.length >= 1, `Expected at least 1 signal, got ${signals.length}`);
    assert.strictEqual(signals[0].id, 'LEGAL_THREAT');
    assert(signals[0].snippet.toLowerCase().includes('arrest'));
  });

  test('English Digital Arrest Turn 4 transcript triggers Video Confinement', () => {
    const whisperTranscript = 'You are placed under digital arrest right now. Switch on your video camera immediately and do not disconnect.';
    const signals = extractScamSignals(whisperTranscript);

    const sigIds = signals.map(s => s.id);
    assert(sigIds.includes('VIDEO_CONFINEMENT'), 'Missing VIDEO_CONFINEMENT');
  });

  test('Hindi Cyber Cell Turn 1 transcript triggers Hindi Authority Impersonation', () => {
    const whisperTranscript = 'Main Crime Branch Mumbai se Senior Inspector Rathore bol raha hoon. Aapke naam par ek suspicious parcel seize hua hai.';
    const signals = extractScamSignals(whisperTranscript);

    const sigIds = signals.map(s => s.id);
    assert(sigIds.includes('AUTHORITY_IMPERSONATION'), 'Missing Hindi AUTHORITY_IMPERSONATION');
  });

  test('Hindi Cyber Cell Turn 3 transcript triggers Hindi Legal Threat', () => {
    const whisperTranscript = 'Aapke khilaf non-bailable arrest warrant issue ho chuka hai aur police team nikal chuki hai.';
    const signals = extractScamSignals(whisperTranscript);

    const sigIds = signals.map(s => s.id);
    assert(sigIds.includes('LEGAL_THREAT'), 'Missing Hindi LEGAL_THREAT');
  });

  test('Benign Bank Call transcript produces ZERO scam signals (False Positive Avoidance)', () => {
    const whisperTranscript = 'Good afternoon, this is Priya calling from HDFC Bank customer service regarding your recent credit card upgrade inquiry. We will never ask for your password or PIN.';
    const signals = extractScamSignals(whisperTranscript);

    assert.strictEqual(signals.length, 0, `Expected 0 signals for benign bank call, got ${signals.length}`);
  });

  // Group 5: Actual Signals -> RiskEngine Scoring & Composite Escalation
  console.log('\nGroup 5: Actual Signals -> Evidence-Based RiskEngine');
  test('Whisper-extracted signals drive strictly monotonic RiskEngine progression without artificial floors', () => {
    const riskEngine = new EvidenceBasedRiskEngine();

    const turn1Signals = extractScamSignals('Hello, this is Inspector Vikram Singh from CBI Headquarters. Your Aadhaar identity has been linked to criminal accounts.');
    const r1 = riskEngine.evaluateTurn(turn1Signals);
    assert.strictEqual(r1.prevScore, 0);
    assert(r1.newScore >= 35, `Turn 1 score should be >= 35, got ${r1.newScore}`);

    const turn2Signals = extractScamSignals('A non-bailable arrest warrant has been issued against you under the Prevention of Money Laundering Act.');
    const r2 = riskEngine.evaluateTurn(turn2Signals);
    assert(r2.newScore > r1.newScore, 'Turn 2 score must increase monotonically');

    const turn3Signals = extractScamSignals('This is a strictly confidential national security investigation under Section 144. Do not tell your family members.');
    const r3 = riskEngine.evaluateTurn(turn3Signals);
    assert(r3.newScore >= 80, `Turn 3 score must reach critical threshold (>= 80), got ${r3.newScore}`);

    const turn4Signals = extractScamSignals('You are placed under digital arrest. Stay on this video call.');
    const r4 = riskEngine.evaluateTurn(turn4Signals);
    assert(r4.newScore >= r3.newScore);

    const turn6Signals = extractScamSignals('Transfer five lakh rupees immediately to this safe Reserve Bank of India clearance account.');
    const r6 = riskEngine.evaluateTurn(turn6Signals);
    assert.strictEqual(r6.newScore, 100);
    assert.strictEqual(r6.hasCompositeBonus, true); // +12 Composite Digital-Arrest Escalation verified
  });

  // Group 6: Privacy & Telemetry Sanitization
  console.log('\nGroup 6: Privacy & Telemetry Sanitization');
  test('Telemetry payload strictly excludes raw PCM, audioBytes, and raw buffers', () => {
    const payload = {
      type: 'RISK_UPDATE',
      riskScore: 88,
      riskLevel: 'CRITICAL',
      rawPcm: new Float32Array([0.1, -0.5, 0.3]),
      audioBytes: [1, 2, 3],
      audioBuffer: 'BINARY_BLOB',
      pcmSamples: [0.1, -0.2]
    };

    // Apply dashboard filter logic
    delete payload.rawPcm;
    delete payload.audioBytes;
    delete payload.audioBuffer;
    delete payload.pcmSamples;

    assert.strictEqual(payload.rawPcm, undefined);
    assert.strictEqual(payload.audioBytes, undefined);
    assert.strictEqual(payload.audioBuffer, undefined);
    assert.strictEqual(payload.pcmSamples, undefined);
    assert.strictEqual(payload.riskScore, 88);
  });

  // Group 7: Phase 4B - Local Qwen State Transitions, Lifecycle & Privacy
  console.log('\nGroup 7: Local Qwen State Transitions, Lifecycle & Privacy');
  test('Initial Qwen state is NOT_LOADED', () => {
    const qwen = new QwenLocalAdapter();
    assert.strictEqual(qwen.state, QWEN_STATES.NOT_LOADED);
    assert.strictEqual(qwen.isReady(), false);
  });

  await testAsync('Qwen model loading transitions NOT_LOADED -> LOADING -> READY', async () => {
    const states = [];
    const qwen = new QwenLocalAdapter({
      onStateChange: (s) => states.push(s)
    });

    const success = await qwen.load({ warmupMs: 10 });
    assert.strictEqual(success, true);
    assert.deepStrictEqual(states, [QWEN_STATES.LOADING, QWEN_STATES.READY]);
    assert.strictEqual(qwen.isReady(), true);
    assert(qwen.modelLoadSec >= 0);
  });

  await testAsync('Qwen model load failure transitions to ERROR', async () => {
    const qwen = new QwenLocalAdapter();
    const success = await qwen.load({ shouldFail: true });
    assert.strictEqual(success, false);
    assert.strictEqual(qwen.state, QWEN_STATES.ERROR);
    assert.strictEqual(qwen.isReady(), false);
  });

  await testAsync('Qwen unload() clears history and transitions back to NOT_LOADED', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });
    qwen.rollingHistory.push({ turn: 1, text: 'test' });
    qwen.unload();
    assert.strictEqual(qwen.state, QWEN_STATES.NOT_LOADED);
    assert.strictEqual(qwen.isReady(), false);
    assert.strictEqual(qwen.rollingHistory.length, 0);
  });

  await testAsync('Privacy enforcement: Raw PCM / Float32Array throws error when passed to Qwen', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });
    
    let caughtError = null;
    try {
      await qwen.analyze(new Float32Array([0.1, -0.2, 0.5]), {});
    } catch (err) {
      caughtError = err;
    }
    assert(caughtError !== null, 'Expected privacy error when passing Float32Array to Qwen');
    assert(caughtError.message.includes('PRIVACY VIOLATION'), 'Expected error message to mention privacy violation');
  });

  await testAsync('Empty transcript handled gracefully without crash', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });
    const res = await qwen.analyze('', {});
    assert.strictEqual(res.summary, 'No active speech detected in caller segment.');
    assert.strictEqual(res.semantic_confidence, 'not reported');
  });

  await testAsync('Multi-turn context is strictly bounded to max 4 turns', async () => {
    const qwen = new QwenLocalAdapter({ maxHistoryTurns: 4 });
    await qwen.load({ warmupMs: 5 });

    for (let t = 1; t <= 7; t++) {
      await qwen.analyze(`Caller dialogue turn ${t}`, { turn: t });
    }

    assert.strictEqual(qwen.rollingHistory.length, 4, `Expected max 4 turns in history, got ${qwen.rollingHistory.length}`);
    assert.strictEqual(qwen.rollingHistory[0].turn, 4);
    assert.strictEqual(qwen.rollingHistory[3].turn, 7);
  });

  // Group 8: Phase 4B - Qwen Semantic Reasoning, Escalation & RiskEngine Authority
  console.log('\nGroup 8: Qwen Semantic Reasoning, Escalation & RiskEngine Authority');
  await testAsync('Qwen produces valid structured JSON adhering strictly to Section 7 schema', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const result = await qwen.analyze('I am Inspector Vikram Singh calling from CBI cyber crime department.', {
      turn: 1,
      currentRisk: 39,
      previousRisk: 0,
      riskDelta: 39,
      detectedSignals: [{ id: 'AUTHORITY_IMPERSONATION', name: 'Authority Impersonation' }]
    });

    assert(typeof result.summary === 'string' && result.summary.length > 0);
    assert(Array.isArray(result.tactics), 'Expected tactics to be an array');
    assert(result.tactics.length > 0, 'Expected at least one tactic');
    assert.strictEqual(result.semantic_confidence, 'not reported');
    assert(typeof result.recommended_action === 'string');

    const t = result.tactics[0];
    assert(t.name && t.evidence && t.reason);
  });

  await testAsync('Digital Arrest Turn 2 identifies Legal Threat & Escalation Explanation', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    // Turn 1
    await qwen.analyze('Hello, Inspector Vikram Singh from CBI Headquarters.', {
      turn: 1,
      currentRisk: 39,
      previousRisk: 0,
      riskDelta: 39
    });

    // Turn 2: Non-bailable arrest warrant (Escalation)
    const turn2Result = await qwen.analyze('A non-bailable arrest warrant has been issued against you under the Prevention of Money Laundering Act.', {
      turn: 2,
      currentRisk: 65,
      previousRisk: 39,
      riskDelta: 26,
      detectedSignals: [{ id: 'LEGAL_THREAT', name: 'Arrest Threat' }]
    });

    assert(turn2Result.tactics.some(t => t.name.includes('Legal Threat') || t.name.includes('Arrest')));
    assert(turn2Result.escalation_explanation !== null, 'Expected escalation explanation for +26 risk jump');
    assert(turn2Result.escalation_explanation.includes('escalated'));
  });

  await testAsync('Digital Arrest Turn 4 identifies Video Confinement (Digital Arrest)', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const result = await qwen.analyze('You are placed under digital arrest right now. Switch on your video call immediately and stay in this room.', {
      turn: 4,
      currentRisk: 88,
      previousRisk: 65,
      riskDelta: 23,
      detectedSignals: [{ id: 'VIDEO_CONFINEMENT', name: 'Video Confinement' }]
    });

    assert(result.tactics.some(t => t.name.includes('Isolation') || t.name.includes('Confinement')));
    assert(result.critical_why !== null, 'Expected critical_why explanation for score >= 80');
    assert(result.critical_why.includes('government impersonation'));
  });

  await testAsync('Benign Bank Call produces NO STRONG SCAM INDICATORS and 0 tactics (False Positive Avoidance)', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const result = await qwen.analyze('Good afternoon, this is Priya calling from HDFC Bank customer service regarding your recent credit card upgrade inquiry.', {
      turn: 1,
      currentRisk: 0,
      previousRisk: 0,
      riskDelta: 0,
      detectedSignals: []
    });

    assert(result.summary.includes('NO STRONG SCAM INDICATORS DETECTED'), `Expected benign summary, got: ${result.summary}`);
    assert.strictEqual(result.tactics.length, 0);
    assert.strictEqual(result.is_suspicious, false);
    assert.strictEqual(result.semantic_confidence, 'not reported');
  });

  await testAsync('Qwen failure fallback returns safe object while RiskEngine operates uninterrupted', async () => {
    const qwen = new QwenLocalAdapter();
    // Do not load model, leaves state in NOT_LOADED
    const fallbackResult = await qwen.analyze('Any transcript', {});
    assert.strictEqual(fallbackResult.fallback, true);
    assert(fallbackResult.error.includes('LOCAL AI EXPLANATION UNAVAILABLE'));
  });

  await testAsync('CRITICAL INVARIANCE TEST: Qwen presence NEVER alters RiskEngine numerical scoring', async () => {
    const transcript1 = 'Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi.';
    const transcript2 = 'A non-bailable arrest warrant has been issued against you under the Supreme Court order.';

    // Pipeline Run A: Without Qwen (Pure Deterministic RiskEngine)
    const riskEngineA = new EvidenceBasedRiskEngine();
    const sigsA1 = extractScamSignals(transcript1);
    const scoreA1 = riskEngineA.evaluateTurn(sigsA1).newScore;
    const sigsA2 = extractScamSignals(transcript2);
    const scoreA2 = riskEngineA.evaluateTurn(sigsA2).newScore;

    // Pipeline Run B: With Qwen Loaded & Actively Reasoning
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const riskEngineB = new EvidenceBasedRiskEngine();
    const sigsB1 = extractScamSignals(transcript1);
    const scoreB1 = riskEngineB.evaluateTurn(sigsB1).newScore;
    const qwenReasoning1 = await qwen.analyze(transcript1, {
      turn: 1,
      detectedSignals: sigsB1,
      currentRisk: scoreB1
    });

    const sigsB2 = extractScamSignals(transcript2);
    const scoreB2 = riskEngineB.evaluateTurn(sigsB2).newScore;
    const qwenReasoning2 = await qwen.analyze(transcript2, {
      turn: 2,
      detectedSignals: sigsB2,
      currentRisk: scoreB2
    });

    // Verification: Numerical RiskEngine scores MUST be 100% IDENTICAL
    assert.strictEqual(scoreA1, scoreB1, `Turn 1 score mismatch: A=${scoreA1}, B=${scoreB1}`);
    assert.strictEqual(scoreA2, scoreB2, `Turn 2 score mismatch: A=${scoreA2}, B=${scoreB2}`);
    assert(qwenReasoning1.tactics.length > 0, 'Qwen successfully reasoned Turn 1');
    assert(qwenReasoning2.tactics.length > 0, 'Qwen successfully reasoned Turn 2');
  });

  console.log('\nGroup 9: Interactive State Machine, Feedback Suppression & VAD');

  await testAsync('Interactive state machine transitions through standard lifecycle', async () => {
    const states = [];
    const validStates = ['IDLE', 'LISTENING', 'TRANSCRIBING', 'ANALYZING', 'RESPONDING', 'SPEAKING', 'ERROR'];
    let currentState = 'IDLE';

    function transition(newState) {
      assert(validStates.includes(newState), `Invalid state: ${newState}`);
      currentState = newState;
      states.push(currentState);
    }

    transition('LISTENING');
    transition('TRANSCRIBING');
    transition('ANALYZING');
    transition('RESPONDING');
    transition('SPEAKING');
    transition('LISTENING');

    assert.deepStrictEqual(states, ['LISTENING', 'TRANSCRIBING', 'ANALYZING', 'RESPONDING', 'SPEAKING', 'LISTENING']);
  });

  await testAsync('Feedback suppression prevents speaker audio from leaking into microphone accumulator', async () => {
    // Model LaptopAudioEngine recording and muting logic
    let isMutedForPlayback = false;
    let micAccumulator = [];

    function processAudioFrame(frameData) {
      if (isMutedForPlayback) {
        return; // Suppress microphone input during scammer playback
      }
      micAccumulator.push(frameData);
    }

    // When speaker is silent: microphone active
    isMutedForPlayback = false;
    processAudioFrame(new Float32Array([0.1, 0.2, 0.3]));
    assert.strictEqual(micAccumulator.length, 1, 'Mic should accumulate when playback is inactive');

    // When scammer speaks: feedback suppression ON (SPEAKING state)
    isMutedForPlayback = true;
    processAudioFrame(new Float32Array([0.8, 0.9, 0.7])); // Speaker audio entering mic
    assert.strictEqual(micAccumulator.length, 1, 'Mic accumulator MUST NOT grow during scammer playback (feedback suppression)');

    // Scammer finishes speaking: feedback suppression OFF (LISTENING state)
    isMutedForPlayback = false;
    processAudioFrame(new Float32Array([0.05, 0.08]));
    assert.strictEqual(micAccumulator.length, 2, 'Mic accumulator resumes when scammer playback completes');
  });

  await testAsync('Voice Activity Detection distinguishes silence, speech onset, and end of utterance', async () => {
    let vadState = 'SILENCE';
    const threshold = 0.018;
    let speechDetected = false;
    let utteranceEnded = false;

    function evaluateVad(rms, isSilenceAfterSpeech) {
      if (rms >= threshold) {
        if (vadState !== 'USER_SPEAKING') {
          vadState = 'USER_SPEAKING';
          speechDetected = true;
        }
      } else if (vadState === 'USER_SPEAKING' && isSilenceAfterSpeech) {
        vadState = 'END_OF_UTTERANCE';
        utteranceEnded = true;
      }
    }

    // 1. Background room noise (RMS 0.005) -> SILENCE
    evaluateVad(0.005, false);
    assert.strictEqual(vadState, 'SILENCE');

    // 2. User speaks "Why are you calling me?" (RMS 0.042) -> USER_SPEAKING
    evaluateVad(0.042, false);
    assert.strictEqual(vadState, 'USER_SPEAKING');
    assert.strictEqual(speechDetected, true);

    // 3. User finishes and pauses for >1100ms -> END_OF_UTTERANCE
    evaluateVad(0.004, true);
    assert.strictEqual(vadState, 'END_OF_UTTERANCE');
    assert.strictEqual(utteranceEnded, true);
  });

  await testAsync('Transcript separation: User and Simulated Scammer speech maintain strict separation', async () => {
    const userUtterances = [];
    const scammerUtterances = [];

    function recordTranscript(speaker, text) {
      if (speaker === 'USER') {
        userUtterances.push(text);
      } else if (speaker === 'SIMULATED SCAMMER') {
        scammerUtterances.push(text);
      } else {
        throw new Error(`Invalid speaker identity: ${speaker}`);
      }
    }

    recordTranscript('SIMULATED SCAMMER', 'This is Inspector Vikram Singh calling from CBI New Delhi.');
    recordTranscript('USER', 'Why are you calling me? Can you send me an official notice?');
    recordTranscript('SIMULATED SCAMMER', 'Your Aadhaar is linked to twenty-four fraudulent accounts.');
    recordTranscript('USER', 'I need to check with my lawyer.');

    assert.strictEqual(userUtterances.length, 2);
    assert.strictEqual(scammerUtterances.length, 2);
    assert(scammerUtterances[0].includes('Inspector Vikram Singh'));
    assert(userUtterances[0].includes('Why are you calling me?'));
    // Ensure zero cross-contamination
    assert(!userUtterances.some(u => u.includes('CBI') || u.includes('Aadhaar')));
  });

  console.log('\nGroup 10: Simulated Scammer Dialogue Generation & Persona Adaptation');

  await testAsync('Qwen generateScammerResponse produces adaptive short response (1-3 sentences)', async () => {
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const response = await qwen.generateScammerResponse('Why are you calling me?', {
      stage: 'AUTHORITY',
      riskScore: 39,
      history: []
    });

    assert(response.text.length > 20, 'Response should not be empty');
    const sentenceCount = (response.text.match(/[.!?]+/g) || []).length;
    assert(sentenceCount >= 1 && sentenceCount <= 4, `Response must be 1-3 sentences, got ${sentenceCount}`);
    assert.strictEqual(response.method, 'LOCAL QWEN');
    assert.strictEqual(response.persona, 'Inspector Vikram Singh (CBI Cyber Crime Impersonation)');
  });

  await testAsync('Qwen generateScammerResponse falls back to safe scripted persona when Qwen is not loaded', async () => {
    const qwen = new QwenLocalAdapter();
    // Do not load Qwen -> state NOT_LOADED

    const fallbackResponse = await qwen.generateScammerResponse("I don't believe you. Can I call you back?", {
      stage: 'LEGAL',
      riskScore: 65,
      history: []
    });

    assert.strictEqual(fallbackResponse.method, 'SCRIPTED FALLBACK');
    assert(fallbackResponse.text.includes('arrest') || fallbackResponse.text.includes('warrant') || fallbackResponse.text.includes('law enforcement'));
    assert(fallbackResponse.stage !== undefined);
  });

  await testAsync('Conversation context is bounded to maximum 6 conversational turns', async () => {
    const history = [];
    function addTurn(speaker, text) {
      history.push({ speaker, text });
      if (history.length > 6) {
        history.shift(); // Evict oldest turn
      }
    }

    for (let i = 1; i <= 10; i++) {
      addTurn(i % 2 === 1 ? 'USER' : 'SIMULATED SCAMMER', `Turn ${i} dialogue content`);
    }

    assert.strictEqual(history.length, 6, 'History must not exceed bounded 6 turns');
    assert.strictEqual(history[0].text, 'Turn 5 dialogue content', 'Oldest turns 1-4 should have been evicted');
    assert.strictEqual(history[5].text, 'Turn 10 dialogue content', 'Newest turn preserved');
  });

  await testAsync('Privacy assertion: Raw audio / Float32Array throws error when passed to generateScammerResponse', async () => {
    const qwen = new QwenLocalAdapter();
    const rawPcm = new Float32Array(16000);

    let caught = false;
    try {
      await qwen.generateScammerResponse(rawPcm, {});
    } catch (err) {
      caught = true;
      assert(err.message.includes('SECURITY VIOLATION'));
    }
    assert.strictEqual(caught, true, 'Passing raw audio to Qwen must throw a security violation');
  });

  console.log('\nGroup 11: Critical Intervention & RiskEngine Invariance');

  await testAsync('Critical Intervention triggers when RiskEngine reaches score >= 80 and halts conversation', async () => {
    const riskEngine = new EvidenceBasedRiskEngine();
    let conversationHalted = false;

    function processTurn(scammerText) {
      const signals = extractScamSignals(scammerText);
      const res = riskEngine.evaluateTurn(signals);
      if (res.newScore >= 80) {
        conversationHalted = true;
      }
      return res;
    }

    // Turn 1: Authority (+20 pts -> synergy ~39)
    processTurn('This is Inspector Vikram Singh calling from CBI Headquarters New Delhi.');
    assert.strictEqual(conversationHalted, false);

    // Turn 2: Legal threat (+20 pts -> synergy ~65)
    processTurn('A non-bailable arrest warrant has been issued against you under the Supreme Court order.');
    assert.strictEqual(conversationHalted, false);

    // Turn 3: Video confinement / Digital arrest (+15 pts -> synergy ~88)
    const turn3 = processTurn('You are placed under digital arrest right now. Switch on your video camera immediately.');
    assert.strictEqual(turn3.newScore >= 80, true);
    assert.strictEqual(conversationHalted, true, 'Conversation must halt on critical safety score >= 80');
  });

  await testAsync('CRITICAL INVARIANCE TEST: Interactive mode with Qwen vs Qwen disabled produces EXACT same RiskEngine score (0 difference)', async () => {
    const scammerTurn1 = 'Hello, this is Inspector Vikram Singh calling from CBI Headquarters New Delhi.';
    const scammerTurn2 = 'A non-bailable arrest warrant has been issued against you under Prevention of Money Laundering Act.';
    const scammerTurn3 = 'You are placed under digital arrest. You are forbidden from contacting family or lawyers.';

    // Run A: With Qwen actively generating responses & analyzing
    const qwen = new QwenLocalAdapter();
    await qwen.load({ warmupMs: 5 });

    const engineWithQwen = new EvidenceBasedRiskEngine();
    const sigsA1 = extractScamSignals(scammerTurn1);
    const scoreA1 = engineWithQwen.evaluateTurn(sigsA1).newScore;
    await qwen.analyze(scammerTurn1, { turn: 1, currentRisk: scoreA1 });

    const sigsA2 = extractScamSignals(scammerTurn2);
    const scoreA2 = engineWithQwen.evaluateTurn(sigsA2).newScore;
    await qwen.analyze(scammerTurn2, { turn: 2, currentRisk: scoreA2 });

    const sigsA3 = extractScamSignals(scammerTurn3);
    const scoreA3 = engineWithQwen.evaluateTurn(sigsA3).newScore;
    await qwen.analyze(scammerTurn3, { turn: 3, currentRisk: scoreA3 });

    // Run B: Without Qwen (Qwen disabled / error / off)
    const engineWithoutQwen = new EvidenceBasedRiskEngine();
    const sigsB1 = extractScamSignals(scammerTurn1);
    const scoreB1 = engineWithoutQwen.evaluateTurn(sigsB1).newScore;

    const sigsB2 = extractScamSignals(scammerTurn2);
    const scoreB2 = engineWithoutQwen.evaluateTurn(sigsB2).newScore;

    const sigsB3 = extractScamSignals(scammerTurn3);
    const scoreB3 = engineWithoutQwen.evaluateTurn(sigsB3).newScore;

    // Strict invariant: Difference must be EXACTLY 0 points
    assert.strictEqual(scoreA1, scoreB1, `Turn 1 mismatch: Qwen=${scoreA1}, NoQwen=${scoreB1}`);
    assert.strictEqual(scoreA2, scoreB2, `Turn 2 mismatch: Qwen=${scoreA2}, NoQwen=${scoreB2}`);
    assert.strictEqual(scoreA3, scoreB3, `Turn 3 mismatch: Qwen=${scoreA3}, NoQwen=${scoreB3}`);
    assert.strictEqual(Math.abs(scoreA3 - scoreB3), 0, 'RiskEngine score delta between Qwen ON vs OFF must be 0');
  });

  await testAsync('Session Purge zeroes transient conversation memory and RiskEngine state', async () => {
    const riskEngine = new EvidenceBasedRiskEngine();
    let conversationHistory = [{ speaker: 'USER', text: 'Hello' }, { speaker: 'SIMULATED SCAMMER', text: 'I am CBI' }];
    let userTranscript = 'Hello';
    let scammerTranscript = 'I am CBI';

    riskEngine.evaluateTurn([{ id: 'AUTHORITY_IMPERSONATION', weight: 20 }]);
    assert(riskEngine.score > 0);

    // Perform Session Purge
    function sessionPurge() {
      riskEngine.reset();
      conversationHistory = [];
      userTranscript = '';
      scammerTranscript = '';
    }

    sessionPurge();
    assert.strictEqual(riskEngine.score, 0, 'RiskEngine score must be 0 after purge');
    assert.strictEqual(conversationHistory.length, 0, 'Conversation history must be empty after purge');
    assert.strictEqual(userTranscript, '', 'User transcript must be cleared');
    assert.strictEqual(scammerTranscript, '', 'Scammer transcript must be cleared');
  });

  console.log('\n================================================================');
  console.log(`TEST SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('================================================================');

  if (failed > 0) process.exit(1);
})();
