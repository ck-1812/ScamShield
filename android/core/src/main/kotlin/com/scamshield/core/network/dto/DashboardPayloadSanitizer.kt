package com.scamshield.core.network.dto

import com.scamshield.core.domain.model.RiskScore

object DashboardPayloadSanitizer {

    /**
     * Sanitizes and packages outgoing telemetry for the Laptop Dashboard.
     * Guarantees:
     * - ZERO raw audio bytes
     * - ZERO speaker voice prints
     * - Transcript snippet is included ONLY if explicit user consent [shareTranscript] is true
     */
    fun createRiskUpdate(
        score: RiskScore,
        protectionActive: Boolean,
        shareTranscript: Boolean,
        latestTranscriptSnippet: String? = null
    ): DashboardPayload {
        return DashboardPayload(
            type = "RISK_UPDATE",
            riskScore = score.score,
            riskLevel = score.level.name,
            signals = score.activeSignals.map { it.type.name },
            timestamp = score.timestamp,
            protectionActive = protectionActive,
            explanation = score.explanation,
            transcriptSnippet = if (shareTranscript) latestTranscriptSnippet else null
        )
    }

    fun createProtectionStarted(): DashboardPayload {
        return DashboardPayload(
            type = "PROTECTION_STARTED",
            riskScore = 0,
            riskLevel = "LOW",
            signals = emptyList(),
            timestamp = System.currentTimeMillis(),
            protectionActive = true,
            explanation = "Protection session activated by user.",
            transcriptSnippet = null
        )
    }

    fun createProtectionStopped(finalScore: RiskScore): DashboardPayload {
        return DashboardPayload(
            type = "PROTECTION_STOPPED",
            riskScore = finalScore.score,
            riskLevel = finalScore.level.name,
            signals = finalScore.activeSignals.map { it.type.name },
            timestamp = System.currentTimeMillis(),
            protectionActive = false,
            explanation = "Protection session terminated.",
            transcriptSnippet = null
        )
    }
}
