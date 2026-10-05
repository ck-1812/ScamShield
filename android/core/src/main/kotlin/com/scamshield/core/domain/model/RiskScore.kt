package com.scamshield.core.domain.model

data class RiskScore(
    val score: Int,
    val confidence: Float,
    val level: RiskLevel = RiskLevel.fromScore(score),
    val activeSignals: List<ScamSignal> = emptyList(),
    val explanation: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val SAFE = RiskScore(
            score = 0,
            confidence = 1.0f,
            level = RiskLevel.LOW,
            activeSignals = emptyList(),
            explanation = "Conversation is normal. No scam indicators detected."
        )
    }
}
