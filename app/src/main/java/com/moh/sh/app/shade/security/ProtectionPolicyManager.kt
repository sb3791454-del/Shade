package com.moh.sh.app.shade.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.moh.sh.app.shade.receiver.ShadeDeviceAdminReceiver

enum class ProtectionLevel {
    NONE,
    DEVICE_ADMIN,
    DEVICE_OWNER
}

object ProtectionPolicyManager {
    private const val TAG = "ProtectionPolicyManager"

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, ShadeDeviceAdminReceiver::class.java)
    }

    fun getProtectionLevel(context: Context): ProtectionLevel {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return ProtectionLevel.NONE
        val admin = getAdminComponent(context)

        return when {
            dpm.isDeviceOwnerApp(context.packageName) -> ProtectionLevel.DEVICE_OWNER
            dpm.isAdminActive(admin) -> ProtectionLevel.DEVICE_ADMIN
            else -> ProtectionLevel.NONE
        }
    }

    /**
     * Enforces enterprise Device Owner policies.
     * Called whenever Device Owner is detected, upon boot, and on provisioning.
     * Note: Device Owner and uninstall blocking are NEVER removed during maintenance.
     */
    fun enforceDeviceOwnerPolicies(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return
        val admin = getAdminComponent(context)

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            try {
                // Official AOSP Enterprise API: Block uninstalling this package
                dpm.setUninstallBlocked(admin, context.packageName, true)
                Log.d(TAG, "Device Owner: setUninstallBlocked(true) enforced")

                // Optionally prevent accidental factory reset from the Android Settings UI
                // Note: Hardware bootloader recovery mode remains intact as required.
                try {
                    dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
                    Log.d(TAG, "Device Owner: DISALLOW_FACTORY_RESET in Settings enforced")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set DISALLOW_FACTORY_RESET: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enforce Device Owner policies: ${e.message}", e)
            }
        }
    }
}
