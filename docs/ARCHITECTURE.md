# ScamShield Technical Architecture

ScamShield implements a clean, layered architecture separating core scam reasoning and privacy contracts from Android UI and hardware drivers.

```mermaid
graph TD
    subgraph "Acoustic Intake"
        SPK[Phone Speaker Output] -->|Ambient Acoustic Coupling| MIC[Device Microphone]
        MIC -->|On-Demand Capture| REC[AudioRecord Service]
        REC -->|16kHz PCM Frames| RBUF[RollingAudioBuffer]
    end

    subgraph "Local On-Device AI Engine"
        RBUF -->|3-5s Window Chunks| STT[LocalSpeechEngine / Whisper]
        STT -->|Transcribed Text| EXP[RollingContextManager 30-60s]
        EXP --> SIG[ScamSignalExtractor 12 Tactic Taxonomies]
        SIG --> RISK[Deterministic RiskEngine]
        RISK --> LLM[LocalLLMEngine / Qwen Explainer]
    end

    subgraph "Presentation & Safety Alerts"
        RISK -->|Reactive StateFlow| UI[Jetpack Compose HUD]
        RISK -->|Score >= 80| HAPTIC[Haptic Vibrator & Critical Overlay]
        UI -->|Opt-in Filtered DTO| SOCK[Local WebSocket Server]
    end

    subgraph "External Office Kit (No Raw Audio)"
        SOCK -->|JSON Risk Telemetry| DASH[Laptop Command Center]
    end
```

---

## Module Breakdown

### 1. Domain Layer (`domain/`)
- **`model/ScamSignal`**: Evidence dataclass with ID, type taxonomy, confidence score, severity weight, matched snippet, and timestamp.
- **`model/RiskScore`**: Aggregate risk assessment containing score (0-100), confidence multiplier, level (`LOW`, `SUSPICIOUS`, `HIGH`, `CRITICAL`), active signals, and explainable summary.
- **`model/PrivacyState`**: Formal state machine tracking mic status, buffer allocation, network isolation, and export privileges.

### 2. Signal Extraction Engine (`ai/signal/`)
Implements rule-based and semantic pattern matchers tailored to Indian cyber crime syndicates:
- Digital arrest schemes (fake CBI / Police / Enforcement Directorate / Customs / Supreme Court).
- Aadhaar / PAN illegal shipment linkage threats.
- Video call isolation & confinement ("do not cut call", "keep camera on").
- Safe account transfer scams ("move funds to RBI verification account").
- Remote desktop takeover (AnyDesk, QuickSupport, TeamViewer).
- Credential harvesting (OTP, UPI PIN, ATM PIN).
- Multilingual and Hinglish lexicons.

### 3. Explainable Risk Engine (`ai/risk/`)
- Heuristic weighted accumulator with asymptotic ceiling (Max: 100).
- Temporal persistence: filters out transient false-positives (e.g. casual mention of "bank") by demanding multiple independent tactical vectors across a time window.
- Confidence scaling: increases with corroborating evidence.

### 4. Audio & Privacy Layer (`audio/`, `privacy/`)
- Zero-disk ephemeral buffer (`RollingAudioBuffer`).
- Strict lifecycle binds microphone hardware activation to the user's explicit foreground UI intent.
- Synchronous destruction upon `STOP_PROTECTION`.

### 5. Network & Companion Protocol (`network/`)
- Embedded lightweight WebSocket server (`port 8765`).
- Telemetry payload sanitizer strips all PII and acoustic data.
