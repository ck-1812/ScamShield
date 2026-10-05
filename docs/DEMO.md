# ScamShield Hackathon Live Demo Script & Runbook

This guide instructs judges, developers, and presenters on executing the **two-person scam simulation** and showcasing ScamShield's privacy-first on-device detection.

---

## 1. Setup Checklist

1. **Android Device or Emulator**: Launch the ScamShield app.
2. **Laptop Command Center**: Open `dashboard/` in a modern browser (`http://localhost:5173`).
3. **Local Wi-Fi Network**: Ensure phone and laptop are on the same Wi-Fi network (or configure the phone hotspot). Note the Phone's IP address displayed on the ScamShield Settings screen.
4. **Speakerphone**: If testing with live microphone acoustic audio, enable speakerphone on the caller device.

---

## 2. Walkthrough: The "Digital Arrest" Simulation

### Step 1: Baseline Safety (Off State)
- Show the app home screen.
- Highlight the **PROTECTION OFF** indicator.
- Point out: *Zero microphone capture, zero background polling, zero cloud transmissions.*

### Step 2: On-Demand Activation
- Tap **[ 🛡 PROTECT A CALL ]**.
- Emphasize the clear banner:
  - `Microphone: ACTIVE`
  - `Processing: ON-DEVICE`
  - `Network: NOT REQUIRED`
  - Notice: *"Speakerphone required for prototype call analysis."*

### Step 3: Audible Caller Voice & Acoustic Pipeline
Launch **Demo Mode** and select **"Digital Arrest (CBI)"**:
1. Tap **[ START LIVE DEMO (PLAY VOICE) ]**.
2. **Real Audible Caller Voice**: The scripted scam caller begins speaking aloud through the phone speaker:
   - Statement 1: *"Hello, I am calling from the CBI cyber crime department."*
   - Statement 2: *"Your Aadhaar has been linked to a money laundering case."*
   - Statement 3: *"You will be arrested if you do not cooperate."*
   - Statement 4: *"Do not tell your family about this investigation."*
   - Statement 5: *"Stay on this video call."*
   - Statement 6: *"You need to transfer two lakh rupees to a safe RBI account immediately."*
3. **Simultaneous Acoustic Microphone Capture**: The device microphone captures the room sound from the speaker.
4. **Live Microphone Audio Monitor**:
   - `Speaker: ● ACTIVE`
   - `Mic: ● LISTENING`
   - Real-time animated waveform bars react to the actual microphone PCM input level!
   - `● INPUT DETECTED (Acoustic sound picked up)` displays when voice is heard.
5. **Progressive Risk Escalation**:
   - Turn 1: `AUTHORITY_IMPERSONATION` (+20) -> Score ~20 (Low/Suspicious)
   - Turn 2: `IDENTITY_DOCUMENT_THREAT` (+15) -> Score ~35 (Suspicious)
   - Turn 3: `LEGAL_THREAT` (+20) -> Score ~55 (Suspicious/High)
   - Turn 4: `SECRECY` (+15) -> Score ~70 (High)
   - Turn 5: `VIDEO_CONFINEMENT` (+15) -> Score ~85 (Critical)
   - Turn 6: `FINANCIAL_REQUEST` + `SAFE_ACCOUNT_SCAM` + `URGENCY` -> Score ~98 (CRITICAL)

### Step 4: Critical Alarm & Haptic Alert
- Phone triggers haptic vibration pulses.
- Intervention dialog appears:
  ```
  HIGH-RISK SCAM DETECTED
  Risk Score: 98 / 100
  Why ScamShield intervened:
  ✓ Authority impersonation
  ✓ Arrest threat
  ✓ Secrecy request
  ✓ Video confinement
  ✓ Money transfer request

  DO NOT TRANSFER MONEY
  [ STOP PROTECTION ]
  [ I'M NOT SENDING MONEY ]  [ SAVE INCIDENT ]
  ```
- Laptop Command Center simultaneously updates with **CRITICAL 98/100**, listing the tactics and timestamped audit log.

### Step 5: False-Positive Benchmark Demo
1. In Demo Mode, switch scenario to **"Legit Call (Bank)"**.
2. Tap **[ START LIVE DEMO ]**.
3. The speaker plays: *"Hello, this is City Bank calling. Your branch is open until 5 PM today for KYC updates. No action is required over the phone."*
4. App identifies zero threats, maintaining **LOW RISK (0/100)**.
5. Demonstrates to judges that ScamShield does NOT blindly trigger on keywords like "bank" or "KYC" without coercive pressure tactics.

### Step 6: Incident Resolution & Session Purge
- Tap **[ STOP PROTECTION ]** (or **[ STOP SIMULATION ]**).
- App immediately terminates speaker playback, stops microphone capture, and purges temporary rolling memory buffers.
- Return to **Privacy Center**: confirm that application-owned session data is cleared with zero raw audio ever transmitted.
