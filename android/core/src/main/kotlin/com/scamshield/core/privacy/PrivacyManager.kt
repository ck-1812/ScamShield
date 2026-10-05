package com.scamshield.core.privacy

import com.scamshield.core.domain.model.PrivacyState
import com.scamshield.core.domain.model.ProtectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrivacyManager {

    private val _privacyState = MutableStateFlow(PrivacyState.DEFAULT)
    val privacyState: StateFlow<PrivacyState> = _privacyState.asStateFlow()

    /**
     * Flow: User is on Protect Call screen preparing to arm protection.
     */
    fun startPermissionRequest() {
        _privacyState.value = _privacyState.value.copy(
            status = ProtectionStatus.REQUESTING_PERMISSION
        )
    }

    /**
     * RULE 2 & 5: Microphone capture begins only after explicit user action and granted permission.
     */
    fun activateProtection() {
        _privacyState.value = _privacyState.value.copy(
            status = ProtectionStatus.ACTIVE,
            protectionActive = true,
            microphoneActive = true,
            audioStored = false,
            networkProcessing = false,
            dashboardSharing = true,
            sessionCleared = false
        )
    }

    /**
     * RULE 6: Stopping protection stops ScamShield's audio capture immediately.
     */
    fun stopProtection() {
        _privacyState.value = _privacyState.value.copy(
            status = ProtectionStatus.STOPPED,
            protectionActive = false,
            microphoneActive = false,
            audioStored = false
        )
    }

    /**
     * RULE 18: Transcript sharing to dashboard is OFF by default.
     */
    fun setTranscriptSharing(enabled: Boolean) {
        _privacyState.value = _privacyState.value.copy(
            transcriptSharing = enabled
        )
    }

    /**
     * RULE 20: Session deletion clears the application's stored/retained session information.
     */
    fun purgeSessionData() {
        _privacyState.value = PrivacyState.DEFAULT.copy(
            status = ProtectionStatus.PURGED,
            sessionCleared = true,
            lastCleanedTimestamp = System.currentTimeMillis()
        )
    }
}
