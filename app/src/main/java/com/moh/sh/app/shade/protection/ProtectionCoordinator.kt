package com.moh.sh.app.shade.protection

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.moh.sh.app.shade.R
import com.moh.sh.app.shade.service.OverlayManager
import com.moh.sh.app.shade.service.ShadeAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ProtectionCoordinator {

    private const val TAG = "ProtectionCoordinator"
    private const val TEMPORAL_WINDOW_MS = 750L
    private const val REQUIRED_CONSECUTIVE_FRAMES = 2

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _cooldownRemainingSeconds = MutableStateFlow(0)
    val cooldownRemainingSeconds: StateFlow<Int> = _cooldownRemainingSeconds.asStateFlow()

    private val _isCooldownActive = MutableStateFlow(false)
    val isCooldownActive: StateFlow<Boolean> = _isCooldownActive.asStateFlow()

    private val _isShieldActive = MutableStateFlow(false)
    val isShieldActive: StateFlow<Boolean> = _isShieldActive.asStateFlow()

    private val _lastFlaggedPackage = MutableStateFlow<String?>(null)
    val lastFlaggedPackage: StateFlow<String?> = _lastFlaggedPackage.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<ProtectionEvent>>(emptyList())
    val recentEvents: StateFlow<List<ProtectionEvent>> = _recentEvents.asStateFlow()

    private var cooldownJob: Job? = null
    private val recentDetectionTimestamps = mutableListOf<Long>()
    private var lastEnforcementTime = 0L

    fun onExplicitFrameDetected(
        context: Context,
        confidence: Float,
        useTemporalConfirmation: Boolean,
        cooldownMinutes: Int,
        autoRedirectHome: Boolean
    ): Boolean {
        val now = System.currentTimeMillis()

        // Clean out older detections outside the temporal window
        recentDetectionTimestamps.removeAll { now - it > TEMPORAL_WINDOW_MS }
        recentDetectionTimestamps.add(now)

        val isConfirmed = if (useTemporalConfirmation) {
            recentDetectionTimestamps.size >= REQUIRED_CONSECUTIVE_FRAMES
        } else {
            true
        }

        if (!isConfirmed) {
            Log.d(TAG, "Temporal frame candidate detected (${recentDetectionTimestamps.size}/$REQUIRED_CONSECUTIVE_FRAMES). Waiting for confirmation.")
            return false
        }

        // Prevent repeated re-trigger spam within 1.5 seconds
        if (now - lastEnforcementTime < 1500L) {
            return true
        }
        lastEnforcementTime = now
        recentDetectionTimestamps.clear()

        Log.w(TAG, "Explicit visual content confirmed! Confidence: ${(confidence * 100).toInt()}%")

        val currentPkg = ShadeAccessibilityService.currentForegroundPackage.value
        _lastFlaggedPackage.value = currentPkg

        // 1. Immediately activate emergency full-screen protective shield
        activateProtectiveShield()

        // 2. Prevent user from continuing to view by redirecting Home
        if (autoRedirectHome) {
            mainHandler.postDelayed({
                val navigatedHome = ShadeAccessibilityService.navigateHome()
                if (!navigatedHome) {
                    // Fallback to back button
                    ShadeAccessibilityService.navigateBack()
                }
            }, 100)
        }

        // 3. Start cooldown timer
        startCooldown(cooldownMinutes)

        // 4. Show warning toast & alert
        mainHandler.post {
            Toast.makeText(
                context,
                context.getString(R.string.explicit_content_blocked_warning, cooldownMinutes),
                Toast.LENGTH_LONG
            ).show()
        }

        // 5. Record minimal local event info
        val event = ProtectionEvent(
            confidencePercent = (confidence * 100).toInt(),
            packageName = currentPkg
        )
        val updated = listOf(event) + _recentEvents.value.take(49)
        _recentEvents.value = updated

        return true
    }

    fun onForegroundPackageChanged(packageName: String) {
        if (_isCooldownActive.value) {
            val flagged = _lastFlaggedPackage.value
            if (flagged != null && flagged == packageName) {
                Log.w(TAG, "User re-entered flagged app ($packageName) during cooldown. Redirecting to Home.")
                activateProtectiveShield()
                mainHandler.postDelayed({
                    ShadeAccessibilityService.navigateHome()
                }, 150)
            }
        }
    }

    private fun activateProtectiveShield() {
        _isShieldActive.value = true
        OverlayManager.setEmergencyShield(true)
        scope.launch {
            // Keep shield visible for at least 3 seconds or until app switches
            delay(3000)
            _isShieldActive.value = false
            OverlayManager.setEmergencyShield(false)
        }
    }

    fun startCooldown(minutes: Int) {
        val totalSeconds = (minutes.coerceAtLeast(1)) * 60
        _cooldownRemainingSeconds.value = totalSeconds
        _isCooldownActive.value = true

        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            while (isActive && _cooldownRemainingSeconds.value > 0) {
                delay(1000)
                val remaining = _cooldownRemainingSeconds.value - 1
                _cooldownRemainingSeconds.value = remaining
                if (remaining <= 0) {
                    _isCooldownActive.value = false
                    _lastFlaggedPackage.value = null
                    Log.d(TAG, "Cooldown timer expired. Normal operation restored.")
                }
            }
        }
    }

    fun resetCooldown() {
        cooldownJob?.cancel()
        _cooldownRemainingSeconds.value = 0
        _isCooldownActive.value = false
        _lastFlaggedPackage.value = null
        OverlayManager.setEmergencyShield(false)
        _isShieldActive.value = false
    }
}
