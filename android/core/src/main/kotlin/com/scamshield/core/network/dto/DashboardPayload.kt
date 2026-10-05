package com.scamshield.core.network.dto

data class DashboardPayload(
    val type: String, // "RISK_UPDATE", "SIGNAL_DETECTED", "PROTECTION_STARTED", "PROTECTION_STOPPED", "SESSION_CLEARED"
    val riskScore: Int,
    val riskLevel: String,
    val signals: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val protectionActive: Boolean,
    val explanation: String = "",
    val transcriptSnippet: String? = null // ONLY populated when user explicitly enables transcript sharing
)
