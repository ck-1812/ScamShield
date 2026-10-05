package com.scamshield.core

import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.domain.model.TranscriptSegment
import com.scamshield.core.privacy.PrivacyManager
import com.scamshield.core.privacy.RollingTranscriptBuffer
import com.scamshield.core.privacy.SessionDataCleaner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrivacyStateTest {

    private lateinit var privacyManager: PrivacyManager
    private lateinit var transcriptBuffer: RollingTranscriptBuffer
    private lateinit var riskEngine: RiskEngine
    private lateinit var cleaner: SessionDataCleaner

    @BeforeEach
    fun setUp() {
        privacyManager = PrivacyManager()
        transcriptBuffer = RollingTranscriptBuffer(maxWindowDurationMs = 1000L) // 1 second test window
        riskEngine = RiskEngine()
        cleaner = SessionDataCleaner(privacyManager, transcriptBuffer, riskEngine)
    }

    @Test
    fun `default state is OFF with no microphone or audio recording`() {
        val state = privacyManager.privacyState.value
        assertEquals(com.scamshield.core.domain.model.ProtectionStatus.OFF, state.status)
        assertFalse(state.protectionActive)
        assertFalse(state.microphoneActive)
        assertFalse(state.audioStored)
        assertFalse(state.networkProcessing)
        assertFalse(state.transcriptSharing)
    }

    @Test
    fun `activating protection turns mic on locally without network upload`() {
        privacyManager.activateProtection()
        val state = privacyManager.privacyState.value

        assertEquals(com.scamshield.core.domain.model.ProtectionStatus.ACTIVE, state.status)
        assertTrue(state.protectionActive)
        assertTrue(state.microphoneActive)
        assertFalse(state.audioStored)
        assertFalse(state.networkProcessing)
    }

    @Test
    fun `stopping protection stops microphone immediately`() {
        privacyManager.activateProtection()
        privacyManager.stopProtection()
        val state = privacyManager.privacyState.value

        assertEquals(com.scamshield.core.domain.model.ProtectionStatus.STOPPED, state.status)
        assertFalse(state.protectionActive)
        assertFalse(state.microphoneActive)
    }

    @Test
    fun `transcript window discards expired entries`() {
        val now = 10000L
        transcriptBuffer.addSegment(TranscriptSegment(text = "Old segment", timestamp = now - 2000L), now)
        transcriptBuffer.addSegment(TranscriptSegment(text = "Recent segment", timestamp = now), now)

        val active = transcriptBuffer.getActiveSegments(now)
        assertEquals(1, active.size)
        assertEquals("Recent segment", active.first().text)
    }

    @Test
    fun `delete all session data wipes everything`() {
        val now = 10000L
        transcriptBuffer.addSegment(TranscriptSegment(text = "Sensitive data", timestamp = now), now)
        privacyManager.activateProtection()

        cleaner.deleteAllSessionData()

        assertEquals(0, transcriptBuffer.size())
        val state = privacyManager.privacyState.value
        assertFalse(state.protectionActive)
        assertTrue(state.sessionCleared)
    }
}
