package com.moh.sh.app.shade.presentation

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.app.admin.DevicePolicyManager
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.moh.sh.app.shade.R
import com.moh.sh.app.shade.presentation.components.AntiBypassCard
import com.moh.sh.app.shade.presentation.components.AutoStartAppsCard
import com.moh.sh.app.shade.presentation.components.CaptureStatusCard
import com.moh.sh.app.shade.presentation.components.CooldownTimerCard
import com.moh.sh.app.shade.presentation.components.CoverageDialog
import com.moh.sh.app.shade.presentation.components.DetectionConfidenceCard
import com.moh.sh.app.shade.presentation.components.GitHubFooter
import com.moh.sh.app.shade.presentation.components.ModelLoadingDialog
import com.moh.sh.app.shade.presentation.components.OverlayOpacityCard
import com.moh.sh.app.shade.presentation.components.PixelationLevelCard
import com.moh.sh.app.shade.presentation.components.PreviewDialog
import com.moh.sh.app.shade.presentation.components.ProtectionLevelCard
import com.moh.sh.app.shade.presentation.components.SingleAppCaptureTipDialog
import com.moh.sh.app.shade.presentation.components.SettingsSectionHeader
import com.moh.sh.app.shade.presentation.components.SettingsToggleCard
import com.moh.sh.app.shade.presentation.components.UnsupportedBanner
import com.moh.sh.app.shade.presentation.components.UnsupportedDialog
import com.moh.sh.app.shade.receiver.ShadeDeviceAdminReceiver
import com.moh.sh.app.shade.security.ProtectionLevel
import com.moh.sh.app.shade.security.ProtectionPolicyManager
import com.moh.sh.app.shade.security.TrustedRecoveryManager
import com.moh.sh.app.shade.service.CaptureState
import com.moh.sh.app.shade.service.ShadeAccessibilityService
import com.moh.sh.app.shade.presentation.theme.ShadeTheme
import com.moh.sh.app.shade.util.ScreenCaptureManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    private lateinit var screenCaptureManager: ScreenCaptureManager

    private var permissionState by mutableStateOf(PermissionState())

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updatePermissionStates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        screenCaptureManager = ScreenCaptureManager(
            activity = this,
            onCapturePermissionDenied = {
                Toast.makeText(
                    this,
                    R.string.screen_capture_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        updatePermissionStates()
        TrustedRecoveryManager.initialize(applicationContext)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            ShadeTheme {
                if (!permissionState.allGranted) {
                    PermissionSetupScreen(
                        permissionState = permissionState,
                        onAccessibilityClick = { openAccessibilitySettings() },
                        onNotificationClick = { requestNotificationPermission() }
                    )
                } else if (uiState.showOnboardingFlow) {
                    OnboardingFlowScreen(
                        uiState = uiState,
                        onConfidenceChanged = { value -> viewModel.updateConfidence(value) },
                        onOverlayOpacityChanged = { value -> viewModel.updateOverlayOpacity(value) },
                        onPixelationLevelChanged = { value -> viewModel.updatePixelationLevel(value) },
                        onDetailedModeChanged = { enabled -> viewModel.updateDetailedMode(enabled) },
                        onDone = { viewModel.completeOnboardingFlow() }
                    )
                } else {
                    MainScreen(
                        uiState = uiState,
                        onStartCapture = {
                            if (uiState.shouldShowSingleAppCaptureTipOnStart) {
                                viewModel.showSingleAppCaptureTipDialog()
                            } else {
                                startScreenCapture()
                            }
                        },
                        onStopCapture = { stopScreenCapture() },
                        onConfidenceChanged = { value -> viewModel.updateConfidence(value) },
                        onPerformanceModeChanged = { enabled -> viewModel.updatePowerMode(enabled) },
                        onDetailedModeChanged = { enabled -> viewModel.updateDetailedMode(enabled) },
                        onOverlayOpacityChanged = { value -> viewModel.updateOverlayOpacity(value) },
                        onPixelationLevelChanged = { value -> viewModel.updatePixelationLevel(value) },
                        onFullScreenModeChanged = { enabled ->
                            viewModel.updateFullScreenMode(
                                enabled
                            )
                        },
                        onAutoStartAppsClick = { viewModel.toggleAppSelectionDialog(true) },
                        onAutoStartAppsChanged = { apps -> viewModel.updateAutoStartApps(apps) },
                        onDismissAppSelectionDialog = { viewModel.toggleAppSelectionDialog(false) },
                        onDismissUnsupportedDialog = { viewModel.dismissUnsupportedDeviceDialog() },
                        onDismissSingleAppCaptureTipDialog = { viewModel.dismissSingleAppCaptureTip() },
                        onContinueSingleAppCaptureTipDialog = {
                            viewModel.acknowledgeSingleAppCaptureTip()
                            startScreenCapture()
                        },
                        onCooldownMinutesChanged = { mins -> viewModel.updateCooldownMinutes(mins) },
                        onAutoRedirectHomeChanged = { enabled -> viewModel.updateAutoRedirectHome(enabled) },
                        onTemporalConfirmationChanged = { enabled -> viewModel.updateTemporalConfirmation(enabled) },
                        onResetCooldown = { viewModel.resetCooldown() },
                        onViewCoverageDetails = { viewModel.toggleCoverageDialog(true) },
                        onDismissCoverageDialog = { viewModel.toggleCoverageDialog(false) },
                        onRequestDeviceAdmin = { requestDeviceAdmin() },
                        onRequestIgnoreBatteryOptimization = { requestIgnoreBatteryOptimization() },
                        onSetupRecoveryCredential = { cred, hours, isFriend ->
                            viewModel.setupRecoveryCredential(applicationContext, cred, hours, isFriend)
                        },
                        onVerifyAndRequestUnlock = { cred ->
                            viewModel.verifyAndRequestUnlock(applicationContext, cred)
                        },
                        onCancelUnlockRequest = {
                            viewModel.cancelUnlockRequest(applicationContext)
                        },
                        onRelockImmediately = {
                            viewModel.relockImmediately(applicationContext)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        val level = ProtectionPolicyManager.getProtectionLevel(this)
        viewModel.updateProtectionLevel(level)
        if (level == ProtectionLevel.DEVICE_OWNER) {
            ProtectionPolicyManager.enforceDeviceOwnerPolicies(this)
        }
        viewModel.updateDeviceAdminState(isDeviceAdminActive())
        viewModel.updateBatteryOptimizationState(isBatteryOptimizationIgnored())
    }

    private fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, ShadeDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    private fun requestDeviceAdmin() {
        val adminComponent = ComponentName(this, ShadeDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_description))
        }
        startActivity(intent)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallback)
        }
    }

    private fun updatePermissionStates() {
        permissionState = PermissionState(
            accessibilityGranted = isAccessibilityServiceEnabled(),
            notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
        viewModel.updateAccessibilityEnabled(permissionState.accessibilityGranted)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val serviceName = ComponentName(this, ShadeAccessibilityService::class.java)
        return enabledServices.any {
            ComponentName(
                it.resolveInfo.serviceInfo.packageName,
                it.resolveInfo.serviceInfo.name
            ) == serviceName
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startScreenCapture() {
        screenCaptureManager.requestScreenCapturePermission()
    }

    private fun stopScreenCapture() {
        screenCaptureManager.stopScreenCapture()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onConfidenceChanged: (Float) -> Unit,
    onPerformanceModeChanged: (Boolean) -> Unit,
    onDetailedModeChanged: (Boolean) -> Unit,
    onOverlayOpacityChanged: (Float) -> Unit,
    onPixelationLevelChanged: (Int) -> Unit,
    onFullScreenModeChanged: (Boolean) -> Unit,
    onAutoStartAppsClick: () -> Unit,
    onAutoStartAppsChanged: (Set<String>) -> Unit,
    onDismissAppSelectionDialog: () -> Unit,
    onDismissUnsupportedDialog: () -> Unit,
    onDismissSingleAppCaptureTipDialog: () -> Unit,
    onContinueSingleAppCaptureTipDialog: () -> Unit,
    onCooldownMinutesChanged: (Int) -> Unit,
    onAutoRedirectHomeChanged: (Boolean) -> Unit,
    onTemporalConfirmationChanged: (Boolean) -> Unit,
    onResetCooldown: () -> Unit,
    onViewCoverageDetails: () -> Unit,
    onDismissCoverageDialog: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit,
    onSetupRecoveryCredential: (credential: String, hours: Int, isFriend: Boolean) -> Unit,
    onVerifyAndRequestUnlock: (credential: String) -> Unit,
    onCancelUnlockRequest: () -> Unit,
    onRelockImmediately: () -> Unit
) {
    if (uiState.showUnsupportedDeviceDialog) {
        UnsupportedDialog(onDismiss = onDismissUnsupportedDialog)
    }

    if (uiState.showSingleAppCaptureTipDialog) {
        SingleAppCaptureTipDialog(
            onDismiss = onDismissSingleAppCaptureTipDialog,
            onContinue = onContinueSingleAppCaptureTipDialog
        )
    }

    if (uiState.showCoverageDialog) {
        CoverageDialog(onDismiss = onDismissCoverageDialog)
    }

    if (uiState.captureState == CaptureState.INITIALIZING) {
        val firstLoadMessageRes = when {
            uiState.detailedModeEnabled -> R.string.model_loading_first_time_detailed
            uiState.performanceModeEnabled -> R.string.model_loading_first_time_large
            else -> R.string.model_loading_first_time_normal
        }
        ModelLoadingDialog(
            firstLoadMessageRes = firstLoadMessageRes
        )
    }

    if (uiState.showAppSelectionDialog) {
        AppSelectionDialog(
            selectedApps = uiState.autoStartApps,
            onDismiss = onDismissAppSelectionDialog,
            onConfirm = onAutoStartAppsChanged
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isSingleAppRecordingSupported) {
                UnsupportedBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            CaptureStatusCard(
                captureState = uiState.captureState,
                isSurfaceRestricted = uiState.isSurfaceRestricted,
                isCooldownActive = uiState.isCooldownActive,
                cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
                onStartCapture = onStartCapture,
                onStopCapture = onStopCapture,
                onViewCoverageDetails = onViewCoverageDetails
            )

            Spacer(modifier = Modifier.height(12.dp))

            // System Protection Level & Trusted Recovery Section
            ProtectionLevelCard(
                protectionLevel = uiState.protectionLevel,
                recoveryState = uiState.recoveryState,
                coolingOffRemainingSeconds = uiState.recoveryCoolingOffRemainingSeconds,
                maintenanceRemainingSeconds = uiState.recoveryMaintenanceRemainingSeconds,
                isCredentialConfigured = uiState.isRecoveryCredentialConfigured,
                configuredCoolingOffHours = uiState.configuredCoolingOffHours,
                isFriendKeyMode = uiState.isFriendKeyMode,
                onRequestDeviceAdmin = onRequestDeviceAdmin,
                onSetupRecoveryCredential = onSetupRecoveryCredential,
                onVerifyAndRequestUnlock = onVerifyAndRequestUnlock,
                onCancelUnlockRequest = onCancelUnlockRequest,
                onRelockImmediately = onRelockImmediately
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Anti-Porn Protection Section
            SettingsSectionHeader(stringResource(R.string.settings_section_protection))

            CooldownTimerCard(
                cooldownMinutes = uiState.cooldownMinutes,
                isCooldownActive = uiState.isCooldownActive,
                cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
                autoRedirectHome = uiState.autoRedirectHome,
                temporalConfirmationEnabled = uiState.temporalConfirmationEnabled,
                onCooldownMinutesChanged = onCooldownMinutesChanged,
                onAutoRedirectHomeChanged = onAutoRedirectHomeChanged,
                onTemporalConfirmationChanged = onTemporalConfirmationChanged,
                onResetCooldown = onResetCooldown
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Detection Settings Section
            SettingsSectionHeader(stringResource(R.string.settings_section_detection))

            DetectionConfidenceCard(
                confidence = uiState.confidence,
                onConfidenceChanged = onConfidenceChanged
            )

            OverlayOpacityCard(
                opacity = uiState.overlayOpacity,
                onOpacityChanged = onOverlayOpacityChanged
            )

            PixelationLevelCard(
                pixelationLevel = uiState.pixelationLevel,
                onPixelationLevelChanged = onPixelationLevelChanged
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Anti-Bypass & Persistence Section
            SettingsSectionHeader(stringResource(R.string.settings_section_anti_bypass))

            AntiBypassCard(
                isDeviceAdminActive = uiState.isDeviceAdminActive,
                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                onRequestDeviceAdmin = onRequestDeviceAdmin,
                onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced Section
            SettingsSectionHeader(stringResource(R.string.settings_section_advanced))

            SettingsToggleCard(
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                title = stringResource(R.string.detailed_mode),
                description = stringResource(R.string.detailed_mode_description),
                checked = uiState.detailedModeEnabled,
                onCheckedChange = onDetailedModeChanged,
                infoDialog = { onDismiss ->
                    PreviewDialog(
                        firstImageRes = R.drawable.full_low_pixelation_example,
                        firstLabelRes = R.string.normal_mode,
                        secondImageRes = R.drawable.detailed_mode_example,
                        secondLabelRes = R.string.detailed_mode,
                        onDismiss = onDismiss
                    )
                }
            )

            SettingsToggleCard(
                icon = Icons.Outlined.Speed,
                title = stringResource(R.string.power_mode),
                description = stringResource(R.string.power_mode_description),
                checked = uiState.performanceModeEnabled,
                onCheckedChange = onPerformanceModeChanged,
                enabled = !uiState.detailedModeEnabled
            )

            SettingsToggleCard(
                icon = Icons.Outlined.Fullscreen,
                title = stringResource(R.string.full_screen_mode),
                description = stringResource(R.string.full_screen_mode_description),
                checked = uiState.fullScreenModeEnabled,
                onCheckedChange = onFullScreenModeChanged,
                enabled = uiState.isSingleAppRecordingSupported
            )

            AutoStartAppsCard(
                selectedCount = uiState.autoStartApps.size,
                onClick = onAutoStartAppsClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            GitHubFooter()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
