package com.initcn.powertools.core.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens

@Composable
fun FeaturePermissionGuard(
    requiredPermissions: List<RequiredPermission>,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var permissionCheckTrigger by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        permissionCheckTrigger++
        onPauseOrDispose { }
    }
    
    val missingPermission = remember(permissionCheckTrigger, requiredPermissions) {
        requiredPermissions.firstOrNull { !it.checkIsGranted(context) }
    }

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionCheckTrigger++
    }

    if (missingPermission != null) {
        PowerAlertDialog(
            title = missingPermission.title,
            icon = Icons.Default.Security,
            confirmText = if (missingPermission.adbCommand != null) "I Understand" else "Grant Permission",
            dismissText = "Go Back",
            onConfirm = {
                when {
                    missingPermission.manifestString != null -> {
                        runtimeLauncher.launch(missingPermission.manifestString)
                    }
                    missingPermission.settingsAction != null -> {
                        try {
                            val intent = Intent(missingPermission.settingsAction)
                            if (missingPermission == RequiredPermission.WRITE_SYSTEM_SETTINGS ||
                                missingPermission == RequiredPermission.ALL_FILES_ACCESS) {
                                intent.data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                    missingPermission.adbCommand != null -> {
                        onNavigateBack()
                    }
                }
            },
            onDismiss = onNavigateBack,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SM)) {
                    Text(text = missingPermission.description)
                    if (missingPermission.adbCommand != null) {
                        Spacer(modifier = Modifier.height(Dimens.SM))
                        Text(
                            text = "Execute via ADB:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = Dimens.SM),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = missingPermission.adbCommand,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f).padding(vertical = Dimens.SM)
                                )
                                IconButton(
                                    onClick = {
                                        // Use native Android ClipboardManager
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ADB Command", missingPermission.adbCommand)
                                        clipboardManager.setPrimaryClip(clip)

                                        Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Command")
                                }
                            }
                        }
                    }
                }
            }
        )
    } else {
        content()
    }
}