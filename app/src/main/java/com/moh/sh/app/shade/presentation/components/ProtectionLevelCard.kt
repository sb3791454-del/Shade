package com.moh.sh.app.shade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moh.sh.app.shade.R
import com.moh.sh.app.shade.security.ProtectionLevel
import com.moh.sh.app.shade.security.RecoveryState
import com.moh.sh.app.shade.security.TrustedRecoveryManager

@Composable
fun ProtectionLevelCard(
    protectionLevel: ProtectionLevel,
    recoveryState: RecoveryState,
    coolingOffRemainingSeconds: Long,
    maintenanceRemainingSeconds: Long,
    isCredentialConfigured: Boolean,
    configuredCoolingOffHours: Int,
    isFriendKeyMode: Boolean,
    onRequestDeviceAdmin: () -> Unit,
    onSetupRecoveryCredential: (credential: String, hours: Int, isFriend: Boolean) -> Unit,
    onVerifyAndRequestUnlock: (credential: String) -> Unit,
    onCancelUnlockRequest: () -> Unit,
    onRelockImmediately: () -> Unit
) {
    var showSetupDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showProvisioningHint by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(icon = Icons.Outlined.Security)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.protection_level_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (protectionLevel) {
                            ProtectionLevel.DEVICE_OWNER -> stringResource(R.string.protection_level_device_owner)
                            ProtectionLevel.DEVICE_ADMIN -> stringResource(R.string.protection_level_device_admin)
                            ProtectionLevel.NONE -> stringResource(R.string.protection_level_none)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (protectionLevel) {
                            ProtectionLevel.DEVICE_OWNER -> Color(0xFF60A5FA)
                            ProtectionLevel.DEVICE_ADMIN -> MaterialTheme.colorScheme.primary
                            ProtectionLevel.NONE -> MaterialTheme.colorScheme.outline
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = when (protectionLevel) {
                            ProtectionLevel.DEVICE_OWNER -> stringResource(R.string.protection_level_device_owner_desc)
                            ProtectionLevel.DEVICE_ADMIN -> stringResource(R.string.protection_level_device_admin_desc)
                            ProtectionLevel.NONE -> stringResource(R.string.protection_level_none_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    if (protectionLevel == ProtectionLevel.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRequestDeviceAdmin,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.device_admin_enable_action))
                        }
                    }

                    if (protectionLevel != ProtectionLevel.DEVICE_OWNER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showProvisioningHint = !showProvisioningHint }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "How to activate Level 2 (Device Owner)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = showProvisioningHint) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.device_owner_provisioning_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trusted Recovery Sub-section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.recovery_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                !isCredentialConfigured -> {
                    Text(
                        text = "Set up a recovery credential and cooling-off waiting period to prevent impulsive uninstallation or deactivation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showSetupDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.recovery_setup_btn))
                    }
                }

                recoveryState == RecoveryState.LOCKED -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.recovery_state_locked) +
                                            " • Delay: ${configuredCoolingOffHours}h" +
                                            if (isFriendKeyMode) " (Friend Key)" else " (PIN)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { showUnlockDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.recovery_request_unlock_btn))
                            }
                        }
                    }
                }

                recoveryState == RecoveryState.COOLING_OFF -> {
                    val hrs = coolingOffRemainingSeconds / 3600
                    val mins = (coolingOffRemainingSeconds % 3600) / 60
                    val secs = coolingOffRemainingSeconds % 60
                    val timeFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF451A03),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.recovery_state_cooling_off, timeFormatted),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.recovery_state_cooling_off_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFEF3C7)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onCancelUnlockRequest,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.recovery_cancel_btn))
                            }
                        }
                    }
                }

                recoveryState == RecoveryState.MAINTENANCE -> {
                    val mins = maintenanceRemainingSeconds / 60
                    val secs = maintenanceRemainingSeconds % 60
                    val timeFormatted = String.format("%02d:%02d", mins, secs)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF064E3B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.recovery_state_maintenance, timeFormatted),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.recovery_state_maintenance_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1FAE5)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onRelockImmediately,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF047857),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(R.string.recovery_relock_btn))
                            }
                        }
                    }
                }
            }
        }
    }

    // Setup Dialog
    if (showSetupDialog) {
        SetupRecoveryDialog(
            initialHours = configuredCoolingOffHours,
            onDismiss = { showSetupDialog = false },
            onConfirm = { credential, hours, isFriend ->
                onSetupRecoveryCredential(credential, hours, isFriend)
                showSetupDialog = false
            }
        )
    }

    // Unlock Dialog
    if (showUnlockDialog) {
        UnlockRequestDialog(
            configuredHours = configuredCoolingOffHours,
            onDismiss = { showUnlockDialog = false },
            onConfirm = { credential ->
                onVerifyAndRequestUnlock(credential)
                showUnlockDialog = false
            }
        )
    }
}

@Composable
private fun SetupRecoveryDialog(
    initialHours: Int,
    onDismiss: () -> Unit,
    onConfirm: (credential: String, hours: Int, isFriend: Boolean) -> Unit
) {
    var selectedHours by remember { mutableIntStateOf(initialHours) }
    var useFriendKey by remember { mutableStateOf(false) }
    var generatedFriendKey by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.setup_recovery_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.setup_recovery_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Select Delay
                Text(
                    text = stringResource(R.string.cooling_off_delay_label) + ":",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1h", 6 to "6h", 12 to "12h", 24 to "24h", 48 to "48h", 168 to "7d").forEach { (h, label) ->
                        val isSel = selectedHours == h
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable { selectedHours = h }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector (PIN vs Friend Key)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            useFriendKey = false
                            generatedFriendKey = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!useFriendKey) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Text("Recovery PIN", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            useFriendKey = true
                            if (generatedFriendKey.isEmpty()) {
                                generatedFriendKey = TrustedRecoveryManager.generateRandomFriendKey()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (useFriendKey) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                    ) {
                        Text("Friend Key", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!useFriendKey) {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it },
                        label = { Text("Set Recovery PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = stringResource(R.string.friend_key_generated),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedFriendKey,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.friend_key_copy_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalCred = if (useFriendKey) generatedFriendKey else enteredPin
                    if (finalCred.isNotBlank()) {
                        onConfirm(finalCred, selectedHours, useFriendKey)
                    }
                },
                enabled = if (useFriendKey) generatedFriendKey.isNotBlank() else enteredPin.length >= 4
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun UnlockRequestDialog(
    configuredHours: Int,
    onDismiss: () -> Unit,
    onConfirm: (credential: String) -> Unit
) {
    var enteredCredential by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.unlock_request_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.unlock_request_desc, configuredHours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = enteredCredential,
                    onValueChange = { enteredCredential = it },
                    label = { Text(stringResource(R.string.enter_credential_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (enteredCredential.isNotBlank()) {
                        onConfirm(enteredCredential)
                    }
                },
                enabled = enteredCredential.isNotBlank()
            ) {
                Text("Start Waiting Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
