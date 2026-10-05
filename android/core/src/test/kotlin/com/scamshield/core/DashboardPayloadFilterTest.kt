package com.scamshield.core

import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.ScamType
import com.scamshield.core.network.dto.DashboardPayloadSanitizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DashboardPayloadFilterTest {

    @Test
    fun `transcript snippet is null when sharing is disabled`() {
        val score = RiskScore(
            score = 85,
            confidence = 0.95f,
            level = RiskLevel.CRITICAL,
            activeSignals = listOf(
                ScamSignal(
                    type = ScamType.AUTHORITY_IMPERSONATION,
                    confidence = 0.9f,
                    severity = 20,
                    evidenceText = "CBI officer"
                )
            ),
            explanation = "Critical threat"
        )

        val payload = DashboardPayloadSanitizer.createRiskUpdate(
            score = score,
            protectionActive = true,
            shareTranscript = false,
            latestTranscriptSnippet = "Secret text that must not leak"
        )

        assertEquals(85, payload.riskScore)
        assertEquals("CRITICAL", payload.riskLevel)
        assertTrue(payload.protectionActive)
        assertNull(payload.transcriptSnippet, "Transcript must be omitted when shareTranscript is false")
    }

    @Test
    fun `transcript snippet is included ONLY when explicit user consent is true`() {
        val score = RiskScore(
            score = 65,
            confidence = 0.9f,
            level = RiskLevel.HIGH,
            activeSignals = emptyList(),
            explanation = "High risk"
        )

        val payload = DashboardPayloadSanitizer.createRiskUpdate(
            score = score,
            protectionActive = true,
            shareTranscript = true,
            latestTranscriptSnippet = "Aadhaar linked to parcel"
        )

        assertEquals("Aadhaar linked to parcel", payload.transcriptSnippet)
    }
}
