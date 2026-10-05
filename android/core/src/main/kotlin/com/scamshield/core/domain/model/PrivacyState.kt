package com.scamshield.core.domain.model

enum class ProtectionStatus {
    OFF,
    REQUESTING_PERMISSION,
    ACTIVE,
    STOPPED,
    PURGED
}

data class PrivacyState(
    val status: ProtectionStatus = ProtectionStatus.OFF,
    val protectionActive: Boolean = false,
    val microphoneActive: Boolean = false,
    val audioStored: Boolean = false,
    val networkProcessing: Boolean = false,
    val dashboardSharing: Boolean = false,
    val transcriptSharing: Boolean = false,
    val sessionCleared: Boolean = true,
    val lastCleanedTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = PrivacyState(
            status = ProtectionStatus.OFF,
            protectionActive = false,
            microphoneActive = false,
            audioStored = false,
            networkProcessing = false,
            dashboardSharing = false,
            transcriptSharing = false,
            sessionCleared = true
        )

        val ACTIVE_PROTECTION = PrivacyState(
            status = ProtectionStatus.ACTIVE,
            protectionActive = true,
            microphoneActive = true,
            audioStored = false, // Ephemeral in-memory buffer only
            networkProcessing = false, // Local processing
            dashboardSharing = true, // Derived metrics only
            transcriptSharing = false, // Explicit opt-in required
            sessionCleared = false
        )
    }
}
