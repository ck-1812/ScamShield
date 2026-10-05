package com.scamshield.core.domain.model

enum class RiskLevel(val label: String, val minScore: Int, val maxScore: Int, val description: String) {
    LOW(
        label = "LOW",
        minScore = 0,
        maxScore = 29,
        description = "Low risk detected. Conversation appears normal."
    ),
    SUSPICIOUS(
        label = "SUSPICIOUS",
        minScore = 30,
        maxScore = 59,
        description = "Suspicious conversation patterns detected. Exercise caution."
    ),
    HIGH(
        label = "HIGH",
        minScore = 60,
        maxScore = 79,
        description = "High-risk scam indicators detected. Do not share sensitive details."
    ),
    CRITICAL(
        label = "CRITICAL",
        minScore = 80,
        maxScore = 100,
        description = "Possible scam in progress. Do not transfer funds or share credentials."
    );

    companion object {
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped >= 80 -> CRITICAL
                clamped >= 60 -> HIGH
                clamped >= 30 -> SUSPICIOUS
                else -> LOW
            }
        }
    }
}
