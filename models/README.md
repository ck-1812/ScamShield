# ScamShield - On-Device AI Models Setup Guide

ScamShield is engineered with a **local-first, privacy-by-design** architecture. Raw phone call audio and transcripts **never** leave your device. All inference runs locally on the edge.

This directory outlines the supported on-device models, hardware requirements, file placements, and offline quantizations.

---

## 1. Supported Architecture Overview

| Pipeline Stage | Model Family | Preferred Hackathon Format | Size / Memory | Offline Inference Engine |
|---|---|---|---|---|
| **Speech-to-Text (STT)** | OpenAI Whisper (Tiny / Base) | GGUF / ONNX / TFLite (INT8) | ~39 MB (Tiny) / ~140 MB (Base) | `whisper.cpp` / ONNX Runtime Mobile |
| **Semantic Scam Reasoning** | Qwen 2.5 (0.5B / 1.5B Instruct) | GGUF (Q4_K_M) | ~350 MB (0.5B) / ~980 MB (1.5B) | `llama.cpp` Android / ExecuTorch |
| **Heuristic Risk Engine** | Deterministic Signal Extractor | Kotlin native | < 1 MB | Instant, deterministic on-device |
| **Visual Notice OCR (ScamLens)** | ML Kit Text Recognition / Tesseract | On-Device TFLite | ~12 MB | Google ML Kit on-device (zero cloud) |

---

## 2. Directory Placement

When deploying full weights on a physical device, place the quantized files under Android's internal files directory (`/data/data/com.scamshield.app/files/models/`) or bundled in `app/src/main/assets/models/`:

```
models/
├── stt/
│   ├── whisper-tiny.bin            # whisper.cpp 16-bit / q5_1 quantized model (English + Multilingual)
│   └── whisper-base.bin            # Optional higher accuracy multilingual base model
└── llm/
    ├── qwen2.5-0.5b-instruct-q4_k_m.gguf   # Ultra-lightweight conversational reasoning for budget phones
    └── qwen2.5-1.5b-instruct-q4_k_m.gguf   # High precision scam explanation model
```

---

## 3. How to Download the Quantized Weights

### Whisper Tiny (Multilingual / English)
```bash
# Using huggingface-cli or curl:
curl -L -o whisper-tiny.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

### Qwen 2.5 1.5B Instruct (GGUF Q4_K_M)
```bash
curl -L -o qwen2.5-1.5b-instruct-q4_k_m.gguf \
  https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf
```

---

## 4. Development & Demo Mode

ScamShield includes a zero-dependency **Hybrid AI Adapter**:
1. **Real Engine Mode**: Loads the local JNI/C++ `whisper.cpp` and `llama.cpp` bindings if weights exist on disk.
2. **Demo Mode / Simulation Mode**: When models are not installed or during rapid hackathon jury presentations, ScamShield operates using its high-speed rule-based semantic extractor and deterministic simulator.
3. **Honest Labeling**: The UI transparently indicates **"DEMO SIMULATION"** or **"ON-DEVICE HYBRID ENGINE"** so that judges and users always know the exact operational state.
