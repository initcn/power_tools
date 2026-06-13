package com.initcn.powertools.feature.vault.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.StorageAccessManager
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerAlertDialog
import com.initcn.powertools.core.utils.KeepScreenSecure
import com.initcn.powertools.core.utils.UiText
import com.initcn.powertools.feature.vault.data.VaultFileEntity
import com.initcn.powertools.feature.vault.domain.auth.BiometricAuthenticator
import com.initcn.powertools.feature.vault.presentation.components.VaultHomeTab
import com.initcn.powertools.feature.vault.presentation.components.VaultSettingsTab
import kotlinx.coroutines.launch

@Composable
fun VaultRoute(
    onNavigateBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    KeepScreenSecure()
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasDocumentsAccess by remember {
        mutableStateOf(StorageAccessManager.getPersistedUri(context, StorageAccessManager.KEY_DOCUMENTS_URI) != null)
    }

    val safLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            StorageAccessManager.savePersistedUri(context, StorageAccessManager.KEY_DOCUMENTS_URI, it)
            hasDocumentsAccess = true
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onEvent(VaultEvent.ImportFile(it)) }
    }

    val hasEscrowBackup = remember(state.isVaultSetup, state.isUnlocked, hasDocumentsAccess, state.showDeleteDialog) {
        viewModel.hasEscrowBackup()
    }

    val launchBiometricUnlock = {
        BiometricAuthenticator.authenticate(
            activity = context as FragmentActivity,
            onSuccess = { viewModel.onEvent(VaultEvent.ProcessBiometricUnlock) },
            onError = { viewModel.onEvent(VaultEvent.SetError(UiText.DynamicString(it))) }
        )
    }

    val launchBiometricEnable = {
        BiometricAuthenticator.authenticate(
            activity = context as FragmentActivity,
            onSuccess = { viewModel.onEvent(VaultEvent.EnableBiometrics) },
            onError = { viewModel.onEvent(VaultEvent.SetError(UiText.DynamicString(it))) }
        )
    }

    val viewFile = { file: VaultFileEntity ->
        try {
            val uri = DocumentsContract.buildDocumentUri("com.initcn.powertools.vault.documents", file.id)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onEvent(VaultEvent.SetError(UiText.StringResource(R.string.error_no_app_to_view)))
        } catch (e: Exception) {
            viewModel.onEvent(VaultEvent.SetError(UiText.StringResource(R.string.error_failed_to_open)))
        }
    }

    if (!hasDocumentsAccess) {
        PowerAlertDialog(
            title = stringResource(R.string.vault_storage_access),
            icon = Icons.Default.Security,
            confirmText = stringResource(R.string.grant_permission),
            dismissText = stringResource(R.string.go_back),
            onConfirm = { safLauncher.launch(null) },
            onDismiss = onNavigateBack,
            content = {
                Text(stringResource(R.string.vault_storage_access_desc))
            }
        )
    } else {
        VaultScreen(
            state = state,
            hasEscrowBackup = hasEscrowBackup,
            onEvent = viewModel::onEvent,
            onLaunchFilePicker = { filePickerLauncher.launch("*/*") },
            onBiometricUnlock = launchBiometricUnlock,
            onBiometricEnable = launchBiometricEnable,
            onViewFile = viewFile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    state: VaultUiState,
    hasEscrowBackup: Boolean,
    onEvent: (VaultEvent) -> Unit,
    onLaunchFilePicker: () -> Unit,
    onBiometricUnlock: () -> Unit,
    onBiometricEnable: () -> Unit,
    onViewFile: (VaultFileEntity) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    if (state.showDeleteDialog) {
        PowerAlertDialog(
            title = stringResource(R.string.delete_vault),
            text = stringResource(R.string.delete_vault_desc),
            confirmText = "Permanently Delete",
            icon = Icons.Default.Warning,
            isDestructive = true,
            onDismiss = { onEvent(VaultEvent.ToggleDeleteDialog(false)) },
            onConfirm = { onEvent(VaultEvent.DeleteEntireVault) }
        )
    }

    state.fileToDelete?.let { _ ->
        PowerAlertDialog(
            title = stringResource(R.string.delete_file_title),
            text = stringResource(R.string.delete_file_desc),
            confirmText = stringResource(R.string.delete),
            icon = Icons.Default.Delete,
            isDestructive = true,
            onDismiss = { onEvent(VaultEvent.ToggleFileDeleteDialog(null)) },
            onConfirm = { onEvent(VaultEvent.DeleteFile) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.secure_vault)) },
                actions = {
                    if (state.isUnlocked) {
                        IconButton(onClick = { onEvent(VaultEvent.LockVault) }) {
                            Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock_vault))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isUnlocked && pagerState.currentPage == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onLaunchFilePicker,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import File")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = Pair(state.isLoading, state.isUnlocked),
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "VaultStateTransition"
            ) { (isLoading, isUnlocked) ->

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!isUnlocked) {
                    // LOCKED STATE: Login / Setup Flow
                    Column(
                        modifier = Modifier
                            .padding(Dimens.MD)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!state.isVaultSetup && hasEscrowBackup) {
                            Text(stringResource(R.string.reinstall_detected), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.restore_pin_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Dimens.SM)
                            )
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text(stringResource(R.string.enter_previous_pin)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.RestoreVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.restore_open_vault)) }
                            Spacer(modifier = Modifier.height(Dimens.LG))
                            TextButton(
                                onClick = { onEvent(VaultEvent.ToggleDeleteDialog(true)) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text(stringResource(R.string.forgot_pin_reset)) }

                        } else if (!state.isVaultSetup) {
                            Text(stringResource(R.string.setup_vault_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text(stringResource(R.string.enter_pin)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinConfirmInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinConfirmInput(it)) },
                                label = { Text(stringResource(R.string.confirm_pin)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.SetupVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.create_vault)) }

                        } else {
                            Text(stringResource(R.string.enter_pin_pass), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text(stringResource(R.string.master_password)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.UnlockVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.unlock_vault)) }

                            if (state.isBiometricEnabled) {
                                Spacer(modifier = Modifier.height(Dimens.SM))
                                Button(
                                    onClick = onBiometricUnlock,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(Modifier.width(Dimens.SM))
                                    Text(stringResource(R.string.unlock_biometrics))
                                }
                            }
                        }

                        state.errorMessage?.let {
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            Text(it.asString(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    // UNLOCKED STATE: Split Pager Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                            val tabs = listOf(stringResource(R.string.tab_home), stringResource(R.string.tab_settings))
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(title) }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            when (page) {
                                0 -> VaultHomeTab(state, onViewFile, onEvent)
                                1 -> VaultSettingsTab(state, onBiometricEnable, onEvent)
                            }
                        }
                    }
                }
            }
        }
    }
}