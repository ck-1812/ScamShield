package com.scamshield.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scamshield.app.audio.AudioCaptureService
import com.scamshield.app.audio.AudioProtectionService
import com.scamshield.app.network.WebSocketDashboardServer
import com.scamshield.core.ai.llm.MockLocalLLMEngine
import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.ai.stt.LocalSpeechEngine
import com.scamshield.core.ai.stt.MockSpeechEngine
import com.scamshield.core.domain.model.*
import com.scamshield.core.network.dto.DashboardPayload
import com.scamshield.core.network.dto.DashboardPayloadSanitizer
import com.scamshield.core.privacy.PrivacyManager
import com.scamshield.core.privacy.RollingTranscriptBuffer
import com.scamshield.core.privacy.SessionDataCleaner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val signalExtractor = ScamSignalExtractor()
    private val riskEngine = RiskEngine()
    private val transcriptBuffer = RollingTranscriptBuffer()
    private val privacyManager = PrivacyManager()
    private val sessionCleaner = SessionDataCleaner(privacyManager, transcriptBuffer, riskEngine)
    private val speechEngine: LocalSpeechEngine = MockSpeechEngine()
    private val llmEngine = MockLocalLLMEngine()

    val dashboardTransport = WebSocketDashboardServer(application)

    private val audioCapture = AudioCaptureService(application) { rawPcmBytes ->
        processAudioChunk(rawPcmBytes)
    }

    private val _currentRiskScore = MutableStateFlow(RiskScore.SAFE)
    val currentRiskScore: StateFlow<RiskScore> = _currentRiskScore.asStateFlow()

    private val _recentTranscripts = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val recentTranscripts: StateFlow<List<TranscriptSegment>> = _recentTranscripts.asStateFlow()

    private val _activeSession = MutableStateFlow<ProtectionSession?>(null)
    val activeSession: StateFlow<ProtectionSession?> = _activeSession.asStateFlow()

    private val _scanResult = MutableStateFlow<ScamLensResult?>(null)
    val scanResult: StateFlow<ScamLensResult?> = _scanResult.asStateFlow()

    private val _shareTranscriptWithDashboard = MutableStateFlow(false)
    val shareTranscriptWithDashboard: StateFlow<Boolean> = _shareTranscriptWithDashboard.asStateFlow()

    val privacyState: StateFlow<PrivacyState> = privacyManager.privacyState

    init {
        // Start local WebSocket companion server for laptop dashboard
        dashboardTransport.start(port = 8765)
    }

    fun startCallProtection() {
        privacyManager.activateProtection()
        speechEngine.start()
        audioCapture.startCapture(viewModelScope)
        AudioProtectionService.start(getApplication())

        _activeSession.value = ProtectionSession(
            startTime = System.currentTimeMillis(),
            active = true
        )

        dashboardTransport.broadcast(DashboardPayloadSanitizer.createProtectionStarted())
    }

    fun stopCallProtection() {
        audioCapture.stopCapture()
        speechEngine.stop()
        privacyManager.stopProtection()
        AudioProtectionService.stop(getApplication())

        val currentScore = _currentRiskScore.value
        val signals = currentScore.activeSignals

        _activeSession.value = _activeSession.value?.copy(
            endTime = System.currentTimeMillis(),
            active = false,
            peakRiskScore = currentScore,
            signals = signals
        )

        dashboardTransport.broadcast(DashboardPayloadSanitizer.createProtectionStopped(currentScore))
    }

    private fun processAudioChunk(rawPcmBytes: ByteArray) {
        viewModelScope.launch {
            val transcribedText = speechEngine.transcribe(rawPcmBytes)
            if (transcribedText.isBlank()) return@launch

            val timestamp = System.currentTimeMillis()
            val segment = TranscriptSegment(text = transcribedText, timestamp = timestamp)
            transcriptBuffer.addSegment(segment, timestamp)
            _recentTranscripts.value = transcriptBuffer.getActiveSegments(timestamp)

            // Extract scam signals
            val extractedSignals = signalExtractor.extractSignals(transcribedText, timestamp)
            val updatedScore = riskEngine.processSignals(extractedSignals, timestamp)
            _currentRiskScore.value = updatedScore

            // Update session
            _activeSession.value = _activeSession.value?.let { current ->
                val highest = if (updatedScore.score > current.peakRiskScore.score) updatedScore else current.peakRiskScore
                current.copy(
                    peakRiskScore = highest,
                    signals = (current.signals + extractedSignals).distinctBy { it.type }
                )
            }

            // Broadcast to Laptop Dashboard (Sanitized telemetry only!)
            val payload = DashboardPayloadSanitizer.createRiskUpdate(
                score = updatedScore,
                protectionActive = true,
                shareTranscript = _shareTranscriptWithDashboard.value,
                latestTranscriptSnippet = transcribedText
            )
            dashboardTransport.broadcast(payload)
        }
    }

    fun analyzeScamLensNotice(text: String) {
        val signals = signalExtractor.extractSignals(text)
        val score = riskEngine.evaluateSignals(signals)
        val reasons = signals.map { "${it.type.displayName}: \"${it.evidenceText}\"" }
        val recommendedAction = if (score.level == RiskLevel.CRITICAL) {
            "CRITICAL WARNING: This notice contains coercive scam markers (CBI impersonation / threat / illegal payment demands). Do NOT transfer funds or call unverified numbers. Report to cybercrime.gov.in."
        } else {
            "Caution: Review official government seals and cross-check transaction IDs directly with your bank or local authority before acting."
        }

        _scanResult.value = ScamLensResult(
            extractedText = text,
            riskScore = score,
            detectedTactics = signals,
            reasons = reasons,
            recommendedAction = recommendedAction
        )
    }

    fun setTranscriptSharing(enabled: Boolean) {
        _shareTranscriptWithDashboard.value = enabled
        privacyManager.setTranscriptSharing(enabled)
    }

    fun deleteAllSessionData() {
        sessionCleaner.deleteAllSessionData()
        _currentRiskScore.value = RiskScore.SAFE
        _recentTranscripts.value = emptyList()
        _activeSession.value = null

        dashboardTransport.broadcast(
            DashboardPayload(
                type = "SESSION_CLEARED",
                riskScore = 0,
                riskLevel = "LOW",
                signals = emptyList(),
                protectionActive = false,
                explanation = "Memory wiped."
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioCapture.stopCapture()
        dashboardTransport.stop()
    }
}
