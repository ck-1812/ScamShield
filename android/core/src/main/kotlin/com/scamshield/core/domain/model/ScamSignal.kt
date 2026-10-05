package com.scamshield.core.domain.model

import java.util.UUID

data class ScamSignal(
    val id: String = UUID.randomUUID().toString(),
    val type: ScamType,
    val confidence: Float,
    val severity: Int,
    val evidenceText: String,
    val timestamp: Long = System.currentTimeMillis()
)
