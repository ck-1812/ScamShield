package com.scamshield.core.domain.model

data class ScamLensResult(
    val extractedText: String,
    val riskScore: RiskScore,
    val detectedTactics: List<ScamSignal>,
    val reasons: List<String>,
    val recommendedAction: String,
    val timestamp: Long = System.currentTimeMillis()
)
