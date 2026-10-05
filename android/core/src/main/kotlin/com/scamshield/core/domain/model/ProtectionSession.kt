package com.scamshield.core.domain.model

import java.util.UUID

data class ProtectionSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val active: Boolean = true,
    val peakRiskScore: RiskScore = RiskScore.SAFE,
    val signals: List<ScamSignal> = emptyList(),
    val transcriptSnippetsCount: Int = 0,
    val userSaved: Boolean = false,
    val userNotes: String = ""
)
