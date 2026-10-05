package com.scamshield.core.privacy

import com.scamshield.core.ai.risk.RiskEngine

class SessionDataCleaner(
    private val privacyManager: PrivacyManager,
    private val transcriptBuffer: RollingTranscriptBuffer,
    private val riskEngine: RiskEngine
) {
    /**
     * Executes immediate obliteration of all session-related memory:
     * - Clears rolling transcript buffers
     * - Resets signal history and risk engine state
     * - Updates PrivacyManager state to sessionCleared
     */
    fun deleteAllSessionData() {
        transcriptBuffer.clear()
        riskEngine.reset()
        privacyManager.purgeSessionData()
    }
}
