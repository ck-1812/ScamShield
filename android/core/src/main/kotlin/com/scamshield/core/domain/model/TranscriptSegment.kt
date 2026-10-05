package com.scamshield.core.domain.model

import java.util.UUID

data class TranscriptSegment(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val speaker: String = "Caller",
    val confidence: Float = 0.95f
)
