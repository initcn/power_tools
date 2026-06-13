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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.core.permissions.StorageAccessManager
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerAlertDialog
import com.initcn.powertools.core.utils.KeepScreenSecure
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
            onError = { viewModel.onEvent(VaultEvent.SetError(it)) }
        )
    }

    val launchBiometricEnable = {
        BiometricAuthenticator.authenticate(
            activity = context as FragmentActivity,
            onSuccess = { viewModel.onEvent(VaultEvent.EnableBiometrics) },
            onError = { viewModel.onEvent(VaultEvent.SetError(it)) }
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
            viewModel.onEvent(VaultEvent.SetError("No app installed to view this file type."))
        } catch (e: Exception) {
            viewModel.onEvent(VaultEvent.SetError("Failed to open file."))
        }
    }

    if (!hasDocumentsAccess) {
        PowerAlertDialog(
            title = "Vault Storage Access",
            icon = Icons.Default.Security,
            confirmText = "Grant Permission",
            dismissText = "Go Back",
            onConfirm = { safLauncher.launch(null) },
            onDismiss = onNavigateBack,
            content = {
                Text("Required to manage files inside your secure vault. Please select or create a folder (e.g., 'PowerToolsVault') and click 'Use this folder'.")
            }
        )
    } else {
        VaultScreen(
            state = state,
            hasEscrowBackup = hasEscrowBackup,
            onNavigateBack = onNavigateBack,
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
    onNavigateBack: () -> Unit,
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
            title = "Delete File?",
            text = "Are you sure you want to permanently delete this file? This cannot be undone.",
            confirmText = "Delete",
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
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isUnlocked) {
                        IconButton(onClick = { onEvent(VaultEvent.LockVault) }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
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
                            Text("Reinstall Detected", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Enter your previous Vault PIN to restore your keys and access your encrypted files.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Dimens.SM)
                            )
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text("Enter Previous PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.RestoreVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Restore & Open Vault") }
                            Spacer(modifier = Modifier.height(Dimens.LG))
                            TextButton(
                                onClick = { onEvent(VaultEvent.ToggleDeleteDialog(true)) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Forgot PIN? Factory Reset Vault") }

                        } else if (!state.isVaultSetup) {
                            Text(stringResource(R.string.setup_vault_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text("Enter PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinConfirmInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinConfirmInput(it)) },
                                label = { Text("Confirm PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.SetupVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Create Vault") }

                        } else {
                            Text(stringResource(R.string.enter_pin_pass), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onEvent(VaultEvent.UpdatePinInput(it)) },
                                label = { Text("Master Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.MD))
                            Button(
                                onClick = { onEvent(VaultEvent.UnlockVault) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Unlock Vault") }

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
                                    Text("Unlock with Biometrics")
                                }
                            }
                        }

                        state.errorMessage?.let {
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    // UNLOCKED STATE: Split Pager Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                            val tabs = listOf("Home", "Settings")
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