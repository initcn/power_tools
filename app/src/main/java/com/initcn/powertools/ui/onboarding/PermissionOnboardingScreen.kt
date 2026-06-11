package com.initcn.powertools.ui.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.initcn.powertools.R
import com.initcn.powertools.permissions.RequiredPermission
import com.initcn.powertools.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun PermissionOnboardingScreen(
    missingPermissions: List<RequiredPermission>,
    modifier: Modifier = Modifier,
    optionalPermissions: List<RequiredPermission> = emptyList()
) {
    val context = LocalContext.current

    val clipboardManager = remember {
        context.getSystemService(ClipboardManager::class.java)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val coroutineScope = rememberCoroutineScope()

    val setupTitle = stringResource(R.string.powertools_setup)
    val grantPermissionsText = stringResource(R.string.grant_permissions)
    val requiredPermissionsText = stringResource(R.string.required_permissions)
    val optionalFeaturesText = stringResource(R.string.optional_features)
    val optionalPermissionsDescription =
        stringResource(R.string.optional_permissions_description)

    val tapOpenSettingsText =
        stringResource(R.string.tap_open_settings)

    val tapGrantFilesAccessText =
        stringResource(R.string.tap_grant_files_access)

    val copyAdbCommandText =
        stringResource(R.string.copy_adb_command)

    val adbCommandCopiedText =
        stringResource(R.string.adb_command_copied)

    Scaffold(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
                .verticalScroll(
                    rememberScrollState()
                ),
            verticalArrangement = Arrangement.spacedBy(
                Dimens.MD
            )
        ) {

            Text(
                text = setupTitle,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = grantPermissionsText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (missingPermissions.isNotEmpty()) {

                Text(
                    text = requiredPermissionsText,
                    style = MaterialTheme.typography.titleLarge
                )

                missingPermissions.forEach { permission ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                when (permission) {

                                    RequiredPermission.WRITE_SYSTEM_SETTINGS -> {
                                        Intent(
                                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        ).apply {
                                            addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                            )
                                        }.also(
                                            context::startActivity
                                        )
                                    }

                                    RequiredPermission.ALL_FILES_ACCESS -> {
                                        if (
                                            Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES.R
                                        ) {
                                            try {
                                                Intent(
                                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    "package:${context.packageName}".toUri()
                                                ).apply {
                                                    addFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                    )
                                                }.also(
                                                    context::startActivity
                                                )
                                            } catch (_: Exception) {
                                                Intent(
                                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                                ).apply {
                                                    addFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                    )
                                                }.also(
                                                    context::startActivity
                                                )
                                            }
                                        }
                                    }

                                    RequiredPermission.READ_CALL_LOG -> {
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }.also(context::startActivity)
                                    }

                                    RequiredPermission.WRITE_SECURE_SETTINGS -> {
                                        // Granted through ADB only.
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation =
                                Dimens.ElevationSM
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.MD),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    Dimens.SM
                                )
                        ) {

                            Text(
                                text = permission.title,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                text = permission.description,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )

                            when (permission) {

                                RequiredPermission.WRITE_SYSTEM_SETTINGS -> {
                                    Text(
                                        text = tapOpenSettingsText,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                RequiredPermission.ALL_FILES_ACCESS -> {
                                    Text(
                                        text = tapGrantFilesAccessText,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                RequiredPermission.READ_CALL_LOG -> {
                                    Text(
                                        text = "Tap to open App Settings and grant Call Log access.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                RequiredPermission.WRITE_SECURE_SETTINGS -> Unit
                            }
                        }
                    }
                }
            }

            if (optionalPermissions.isNotEmpty()) {

                Text(
                    text = optionalFeaturesText,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = optionalPermissionsDescription,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                optionalPermissions.forEach { permission ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation =
                                Dimens.ElevationSM
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.MD),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    Dimens.SM
                                )
                        ) {

                            Text(
                                text = permission.title,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                text = permission.description,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )

                            permission.adbCommand?.let { command ->

                                Card(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant
                                        )
                                ) {

                                    Text(
                                        text = command,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                Dimens.MD
                                            ),
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {

                                        clipboardManager?.setPrimaryClip(
                                            ClipData.newPlainText(
                                                copyAdbCommandText,
                                                command
                                            )
                                        )

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message =
                                                    adbCommandCopiedText
                                            )
                                        }
                                    }
                                ) {

                                    Text(
                                        text =
                                            copyAdbCommandText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}