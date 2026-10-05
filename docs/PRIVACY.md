# ScamShield Privacy Architecture & Principles

> **Core Tenet**: Privacy is a primary product feature. ScamShield does not actively capture microphone input while Protection is OFF, never persists raw audio to storage, and never relies on external cloud APIs.

---

## 1. The Twenty Privacy Rules (Implemented in Code)

1. **RULE 1**: Protection is OFF by default.
2. **RULE 2**: ScamShield does not actively capture microphone input while Protection is OFF.
3. **RULE 3**: Microphone permission must NOT be requested on first launch.
4. **RULE 4**: Request `RECORD_AUDIO` strictly when the user explicitly enters call protection.
5. **RULE 5**: Capture begins only after explicit user action and permission.
6. **RULE 6**: Stopping protection stops ScamShield's audio capture immediately.
7. **RULE 7**: Raw audio must not be intentionally persisted to permanent storage.
8. **RULE 8**: Use a short in-memory processing buffer where required (bounded at 4 seconds).
9. **RULE 9**: Application-owned temporary audio buffers are overwritten/discarded after processing and are not intentionally persisted.
10. **RULE 10**: Do not send raw audio to the laptop dashboard.
11. **RULE 11**: Do not send raw audio to cloud APIs.
12. **RULE 12**: Do not send conversation transcripts to external AI APIs.
13. **RULE 13**: No OpenAI API.
14. **RULE 14**: No Google Speech API.
15. **RULE 15**: No Azure Speech API.
16. **RULE 16**: No AWS transcription API.
17. **RULE 17**: Local/open-source AI may be integrated where practical.
18. **RULE 18**: Transcript sharing to the dashboard is OFF by default.
19. **RULE 19**: Any export/save/share action requires explicit user action.
20. **RULE 20**: Session deletion clears the application's stored/retained session information.

---

## 2. Technical Honesty & Phrasing Standards

- **Microphone Hardware**: We state: *"ScamShield does not capture microphone input while Protection is OFF."* We do **not** falsely claim the physical microphone hardware chip is powered down.
- **Buffer Oblivion**: We state: *"Application-owned temporary audio buffers are overwritten/discarded after processing and are not intentionally persisted."* We do **not** claim physical silicon RAM is erased beyond standard JVM garbage collection and explicit array zeroing.
- **Call-Audio Ingestion**: We state: *"Prototype call analysis uses speakerphone acoustic capture."* We do **not** claim to intercept raw cellular telephone streams.

---

## 3. Privacy State Machine

```
              ┌─────────────────────────────────────────┐
              │             PROTECTION_OFF              │
              │  - ScamShield does not capture audio    │
              │  - In-memory buffer: None               │
              │  - Network telemetry: Idle              │
              └────────────────────┬────────────────────┘
                                   │
                  User enters "Protect a call" screen
                                   │
                                   ▼
              ┌─────────────────────────────────────────┐
              │    PROTECTION_REQUESTING_PERMISSION     │
              │  - Displays speakerphone notice         │
              │  - Requests RECORD_AUDIO only if needed │
              └────────────────────┬────────────────────┘
                                   │
                      Permission granted & User arms
                                   │
                                   ▼
              ┌─────────────────────────────────────────┐
              │            PROTECTION_ACTIVE            │
              │  - AudioRecord capturing speaker voice  │
              │  - In-Memory Rolling Buffer (≤ 4 sec)   │
              │  - Incremental chunk transcription      │
              │  - On-Device rule-based signal engine   │
              └────────────────────┬────────────────────┘
                                   │
                   User taps "STOP PROTECTION"
                                   │
                                   ▼
              ┌─────────────────────────────────────────┐
              │           PROTECTION_STOPPED            │
              │  - AudioRecord stopped immediately      │
              │  - Active audio buffers discarded       │
              │  - Incident Summary displayed           │
              │  - Optional opt-in saving               │
              └────────────────────┬────────────────────┘
                                   │
                   User taps "DELETE SESSION"
                                   │
                                   ▼
              ┌─────────────────────────────────────────┐
              │             SESSION_PURGED              │
              │  - In-memory transcripts cleared        │
              │  - Signal history reset                 │
              │  - Returns to PROTECTION_OFF            │
              └─────────────────────────────────────────┘
```

---

## 4. Laptop Dashboard Payload Sanitization

The `DashboardTransport` enforces strict data sanitization before transmitting over local Wi-Fi:

| Telemetry Field | Included by Default? | Rationale |
|---|---|---|
| **Raw Microphone Audio** | ❌ **NEVER** | Explicitly blocked by code architecture. |
| **Speaker Voice Embeddings**| ❌ **NEVER** | Not generated or transmitted. |
| **Risk Score (0–100)** | ✅ **YES** | Numeric integer derived locally on phone. |
| **Detected Scam Signals** | ✅ **YES** | Tactical enums (e.g. `AUTHORITY_IMPERSONATION`). |
| **Event Timestamp** | ✅ **YES** | Local Unix timestamp of event. |
| **Transcript Snippet** | ⚠️ **OFF BY DEFAULT** | Sent ONLY if user explicitly enables toggle in Settings. |
