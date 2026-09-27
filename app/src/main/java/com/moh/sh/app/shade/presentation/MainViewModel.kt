package com.moh.sh.app.shade.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moh.sh.app.shade.util.PreferenceManager
import android.os.Build
import com.moh.sh.app.shade.detection.DEFAULT_CONFIDENCE_PERCENT
import com.moh.sh.app.shade.detection.DEFAULT_DOWNSAMPLE_FACTOR
import com.moh.sh.app.shade.service.CaptureState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.moh.sh.app.shade.service.ScreenCaptureService
import org.koin.android.annotation.KoinViewModel

data class MainUiState(
    val captureState: CaptureState = CaptureState.IDLE,
    val confidence: Float = DEFAULT_CONFIDENCE_PERCENT,
    val performanceModeEnabled: Boolean = false,
    val detailedModeEnabled: Boolean = false,
    val overlayOpacity: Float = 100f,
    val fullScreenModeEnabled: Boolean = false,
    val pixelationLevel: Int = DEFAULT_DOWNSAMPLE_FACTOR,
    val isAccessibilityEnabled: Boolean = false,
    val isSingleAppRecordingSupported: Boolean = true,
    val showOnboardingFlow: Boolean = false,
    val showUnsupportedDeviceDialog: Boolean = false,
    val shouldShowSingleAppCaptureTipOnStart: Boolean = false,
    val showSingleAppCaptureTipDialog: Boolean = false,
    val autoStartApps: Set<String> = emptySet(),
    val showAppSelectionDialog: Boolean = false,
    val cooldownMinutes: Int = 10,
    val autoRedirectHome: Boolean = true,
    val temporalConfirmationEnabled: Boolean = true,
    val cooldownRemainingSeconds: Int = 0,
    val isCooldownActive: Boolean = false,
    val isSurfaceRestricted: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val showCoverageDialog: Boolean = false,
    val protectionLevel: com.moh.sh.app.shade.security.ProtectionLevel = com.moh.sh.app.shade.security.ProtectionLevel.NONE,
    val recoveryState: com.moh.sh.app.shade.security.RecoveryState = com.moh.sh.app.shade.security.RecoveryState.LOCKED,
    val recoveryCoolingOffRemainingSeconds: Long = 0L,
    val recoveryMaintenanceRemainingSeconds: Long = 0L,
    val isRecoveryCredentialConfigured: Boolean = false,
    val configuredCoolingOffHours: Int = 24,
    val isFriendKeyMode: Boolean = false
)

@KoinViewModel
class MainViewModel(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        val isSingleAppRecordingSupported = isSingleAppRecordingSupported()
        _uiState.update { it.copy(isSingleAppRecordingSupported = isSingleAppRecordingSupported) }

        if (!isSingleAppRecordingSupported) {
            updateFullScreenMode(true)
        }

        viewModelScope.launch {
            val hasCompletedOnboarding = preferenceManager.hasCompletedOnboardingFlow()
            val hasShownUnsupportedDialog = preferenceManager.hasShownUnsupportedDeviceDialog()
            val hasSeenSingleAppCaptureTip = preferenceManager.hasSeenSingleAppCaptureTip()
            _uiState.update {
                it.copy(
                    showOnboardingFlow = !hasCompletedOnboarding,
                    showUnsupportedDeviceDialog = !isSingleAppRecordingSupported && !hasShownUnsupportedDialog,
                    shouldShowSingleAppCaptureTipOnStart = isSingleAppRecordingSupported && !hasSeenSingleAppCaptureTip
                )
            }
        }

        viewModelScope.launch {
            preferenceManager.confidencePercentFlow.collectLatest { value ->
                _uiState.update { state -> state.copy(confidence = value) }
            }
        }

        viewModelScope.launch {
            preferenceManager.performanceModeFlow.collectLatest { enabled ->
                _uiState.update { state -> state.copy(performanceModeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            preferenceManager.overlayOpacityFlow.collectLatest { value ->
                _uiState.update { state -> state.copy(overlayOpacity = value) }
            }
        }

        viewModelScope.launch {
            preferenceManager.fullScreenModeFlow.collectLatest { enabled ->
                _uiState.update { state -> state.copy(fullScreenModeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            preferenceManager.autoStartAppsFlow.collectLatest { apps ->
                _uiState.update { state -> state.copy(autoStartApps = apps) }
            }
        }

        viewModelScope.launch {
            preferenceManager.pixelationLevelFlow.collectLatest { level ->
                _uiState.update { state -> state.copy(pixelationLevel = level) }
            }
        }

        viewModelScope.launch {
            preferenceManager.detailedModeFlow.collectLatest { enabled ->
                _uiState.update { state -> state.copy(detailedModeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            ScreenCaptureService.captureStateFlow.collectLatest { captureState ->
                _uiState.update { state -> state.copy(captureState = captureState) }
            }
        }

        viewModelScope.launch {
            ScreenCaptureService.isSurfaceRestrictedFlow.collectLatest { isRestricted ->
                _uiState.update { state -> state.copy(isSurfaceRestricted = isRestricted) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.protection.ProtectionCoordinator.cooldownRemainingSeconds.collectLatest { remaining ->
                _uiState.update { state -> state.copy(cooldownRemainingSeconds = remaining) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.protection.ProtectionCoordinator.isCooldownActive.collectLatest { active ->
                _uiState.update { state -> state.copy(isCooldownActive = active) }
            }
        }

        viewModelScope.launch {
            preferenceManager.cooldownMinutesFlow.collectLatest { minutes ->
                _uiState.update { state -> state.copy(cooldownMinutes = minutes) }
            }
        }

        viewModelScope.launch {
            preferenceManager.autoRedirectHomeFlow.collectLatest { enabled ->
                _uiState.update { state -> state.copy(autoRedirectHome = enabled) }
            }
        }

        viewModelScope.launch {
            preferenceManager.temporalConfirmationFlow.collectLatest { enabled ->
                _uiState.update { state -> state.copy(temporalConfirmationEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.recoveryState.collectLatest { state ->
                _uiState.update { it.copy(recoveryState = state) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.coolingOffRemainingSeconds.collectLatest { sec ->
                _uiState.update { it.copy(recoveryCoolingOffRemainingSeconds = sec) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.maintenanceRemainingSeconds.collectLatest { sec ->
                _uiState.update { it.copy(recoveryMaintenanceRemainingSeconds = sec) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.isCredentialConfigured.collectLatest { configured ->
                _uiState.update { it.copy(isRecoveryCredentialConfigured = configured) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.configuredCoolingOffHours.collectLatest { hrs ->
                _uiState.update { it.copy(configuredCoolingOffHours = hrs) }
            }
        }

        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.isFriendKeyMode.collectLatest { isFriend ->
                _uiState.update { it.copy(isFriendKeyMode = isFriend) }
            }
        }
    }

    fun updateConfidence(value: Float) {
        _uiState.update { it.copy(confidence = value) }
        viewModelScope.launch {
            preferenceManager.saveConfidencePercent(value)
        }
    }

    fun updatePowerMode(enabled: Boolean) {
        _uiState.update { it.copy(performanceModeEnabled = enabled) }
        viewModelScope.launch {
            preferenceManager.setPowerMode(enabled)
        }
    }

    fun updateOverlayOpacity(value: Float) {
        _uiState.update { it.copy(overlayOpacity = value) }
        viewModelScope.launch {
            preferenceManager.setOverlayOpacity(value)
        }
    }

    fun updateFullScreenMode(enabled: Boolean) {
        _uiState.update { it.copy(fullScreenModeEnabled = enabled) }
        viewModelScope.launch {
            preferenceManager.setFullScreenMode(enabled)
        }
    }

    fun updateAccessibilityEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
    }

    fun completeOnboardingFlow() {
        _uiState.update { it.copy(showOnboardingFlow = false) }
        viewModelScope.launch {
            preferenceManager.setHasCompletedOnboardingFlow(true)
        }
    }

    fun dismissUnsupportedDeviceDialog() {
        _uiState.update { it.copy(showUnsupportedDeviceDialog = false) }
        viewModelScope.launch {
            preferenceManager.setHasShownUnsupportedDeviceDialog(true)
        }
    }

    fun showSingleAppCaptureTipDialog() {
        _uiState.update { it.copy(showSingleAppCaptureTipDialog = true) }
    }

    fun acknowledgeSingleAppCaptureTip() {
        _uiState.update {
            it.copy(
                showSingleAppCaptureTipDialog = false,
                shouldShowSingleAppCaptureTipOnStart = false
            )
        }
        viewModelScope.launch {
            preferenceManager.setHasSeenSingleAppCaptureTip(true)
        }
    }

    fun dismissSingleAppCaptureTip() {
        _uiState.update {
            it.copy(
                showSingleAppCaptureTipDialog = false,
                shouldShowSingleAppCaptureTipOnStart = false
            )
        }
    }

    fun updateAutoStartApps(apps: Set<String>) {
        viewModelScope.launch {
            preferenceManager.setAutoStartApps(apps)
        }
        _uiState.update { it.copy(showAppSelectionDialog = false) }
    }

    fun toggleAppSelectionDialog(show: Boolean) {
        _uiState.update { it.copy(showAppSelectionDialog = show) }
    }

    fun updatePixelationLevel(value: Int) {
        _uiState.update { it.copy(pixelationLevel = value) }
        viewModelScope.launch {
            preferenceManager.setPixelationLevel(value)
        }
    }

    fun updateDetailedMode(enabled: Boolean) {
        _uiState.update { it.copy(detailedModeEnabled = enabled) }
        viewModelScope.launch {
            preferenceManager.setDetailedMode(enabled)
        }
    }

    fun updateCooldownMinutes(minutes: Int) {
        _uiState.update { it.copy(cooldownMinutes = minutes) }
        viewModelScope.launch {
            preferenceManager.setCooldownMinutes(minutes)
        }
    }

    fun updateAutoRedirectHome(enabled: Boolean) {
        _uiState.update { it.copy(autoRedirectHome = enabled) }
        viewModelScope.launch {
            preferenceManager.setAutoRedirectHome(enabled)
        }
    }

    fun updateTemporalConfirmation(enabled: Boolean) {
        _uiState.update { it.copy(temporalConfirmationEnabled = enabled) }
        viewModelScope.launch {
            preferenceManager.setTemporalConfirmation(enabled)
        }
    }

    fun resetCooldown() {
        com.moh.sh.app.shade.protection.ProtectionCoordinator.resetCooldown()
    }

    fun toggleCoverageDialog(show: Boolean) {
        _uiState.update { it.copy(showCoverageDialog = show) }
    }

    fun updateDeviceAdminState(active: Boolean) {
        _uiState.update { it.copy(isDeviceAdminActive = active) }
    }

    fun updateBatteryOptimizationState(ignored: Boolean) {
        _uiState.update { it.copy(isBatteryOptimizationIgnored = ignored) }
    }

    fun updateProtectionLevel(level: com.moh.sh.app.shade.security.ProtectionLevel) {
        _uiState.update { it.copy(protectionLevel = level) }
    }

    fun setupRecoveryCredential(context: android.content.Context, credential: String, hours: Int, isFriend: Boolean) {
        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.setupCredential(
                context = context,
                credential = credential.toCharArray(),
                coolingOffHours = hours,
                isFriendKey = isFriend
            )
        }
    }

    fun verifyAndRequestUnlock(context: android.content.Context, credential: String) {
        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.verifyAndRequestUnlock(
                context = context,
                credential = credential.toCharArray()
            )
        }
    }

    fun cancelUnlockRequest(context: android.content.Context) {
        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.cancelUnlockRequest(context)
        }
    }

    fun relockImmediately(context: android.content.Context) {
        viewModelScope.launch {
            com.moh.sh.app.shade.security.TrustedRecoveryManager.relockImmediately(context)
        }
    }

    private fun isSingleAppRecordingSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 35 ||
                (Build.VERSION.SDK_INT == 34 && Build.MANUFACTURER.equals("Google", ignoreCase = true))
    }
}
