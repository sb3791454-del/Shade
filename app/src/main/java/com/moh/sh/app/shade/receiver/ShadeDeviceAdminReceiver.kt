package com.moh.sh.app.shade.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.moh.sh.app.shade.R
import com.moh.sh.app.shade.security.ProtectionPolicyManager

class ShadeDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        ProtectionPolicyManager.enforceDeviceOwnerPolicies(context)
        Toast.makeText(context, R.string.device_admin_enabled_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        ProtectionPolicyManager.enforceDeviceOwnerPolicies(context)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.device_admin_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, R.string.device_admin_disabled_toast, Toast.LENGTH_SHORT).show()
    }
}

