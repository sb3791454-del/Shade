package com.moh.sh.app.shade.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.moh.sh.app.shade.R
import com.moh.sh.app.shade.presentation.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-enforce Device Owner policies upon boot
            com.moh.sh.app.shade.security.ProtectionPolicyManager.enforceDeviceOwnerPolicies(context)

            // Re-verify recovery state integrity and handle fail-safe maintenance relock
            com.moh.sh.app.shade.security.TrustedRecoveryManager.initialize(context)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "shade_persistence_channel"
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_persistence),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_persistence_desc)
            }
            notificationManager.createNotificationChannel(channel)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.reboot_notification_title))
                .setContentText(context.getString(R.string.reboot_notification_desc))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        }
    }
}
