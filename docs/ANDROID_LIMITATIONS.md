# Android Platform Call-Audio Realities & Known Prototype Limitations

## 1. The Cellular Call Audio Isolation Boundary

In standard consumer Android (Android 6.0 through Android 15+), third-party applications running in the user application sandbox cannot directly tap into the raw internal telephony audio stream (uplink or downlink).

### Architectural Rationale:
- **Telephony Audio Routing**: The cellular modem routes audio directly into hardware Audio HAL DSP codecs managed by the `rild` / telephony subsystem, isolated by Linux kernel SELinux policies.
- **Privacy Protections**: `android.media.AudioRecord` restricts `AudioSource.VOICE_CALL`, `VOICE_DOWNLINK`, and `VOICE_UPLINK` to system applications holding privileged permissions (`android.permission.CAPTURE_AUDIO_OUTPUT`) and platform signature keys.

---

## 2. ScamShield's Legitimate Prototype Approach

ScamShield refuses to employ malicious workarounds, root exploits, accessibility service abuse, or covert recording hacks. 

Instead, the prototype uses an **honest, privacy-respecting acoustic coupling pipeline**:

```
Caller Voice
    ↓
Cellular Network / VoIP
    ↓
Phone Loudspeaker (Speakerphone)
    ↓
Acoustic Sound Waves
    ↓
Device Primary Microphone
    ↓
AudioRecord (MediaRecorder.AudioSource.VOICE_RECOGNITION)
    ↓
ScamShield In-Memory Processing
```

### Mandatory User Requirement:
- **Speakerphone Engagement**: The user or demo operator must engage the device speakerphone so the outgoing sound from the speaker is acoustically captured by the phone's ambient microphone.
- **In-App Transparency**: Every relevant screen clearly displays:
  > *"Speakerphone required for prototype call analysis."*

---

## 3. OEM & Hardware Audio Routing Caveats

1. **Hardware Acoustic Echo Cancellation (AEC)**:
   - When speakerphone is active during an actual phone call, many modern Android devices (e.g. Google Pixel, Samsung Galaxy, iQOO) engage hardware DSP echo cancellation to prevent the caller from hearing their own voice echo.
   - If AEC aggressively suppresses the speaker output from being passed back to `AudioRecord`, the microphone buffer may receive low volume or silence.
   - **Prototype Fallback**: ScamShield checks buffer root-mean-square (RMS) energy. If input is completely silent during active protection, the UI displays a notification (*"Microphone input low or unavailable. Verify speakerphone volume."*) rather than fabricating transcription.

2. **Foreground Service Policies (Android 14+ / API 34)**:
   - Android 14 strictly enforces that services with `foregroundServiceType="microphone"` cannot be initiated from an arbitrary background state.
   - ScamShield correctly launches `AudioProtectionService` **strictly while the user is actively viewing the ScamShield application in the foreground** and taps `[ PROTECT THIS CALL ]`.

3. **Bluetooth & Headset Behavior**:
   - If the user is wearing Bluetooth earbuds or a wired headset, audio bypasses the external speaker entirely. Acoustic capture is impossible in this configuration. The user must switch to speakerphone for prototype call analysis.

---

## 4. Ethical & Legal Positioning

- ScamShield is an **assistive consumer safety layer**, not a wiretapping or forensic intercept tool.
- The prototype never covertly monitors calls.
- Protection is **OFF by default** and halts immediately when the user taps `[ STOP PROTECTION ]`.
- No raw audio is ever persisted to permanent disk or transmitted to cloud servers.
