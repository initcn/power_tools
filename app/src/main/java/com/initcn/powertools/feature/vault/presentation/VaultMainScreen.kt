package com.initcn.powertools.feature.vault.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.core.permissions.StorageAccessManager
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerAlertDialog
import com.initcn.powertools.feature.vault.domain.VaultNameEncryptor
import com.initcn.powertools.feature.vault.domain.auth.BiometricAuthenticator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    onNavigateBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
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
        uri?.let {
            viewModel.onEvent(VaultEvent.ImportFile(it))
        }
    }

    // ADD state.showDeleteDialog to the remember keys
    val hasEscrowBackup = remember(state.isVaultSetup, state.isUnlocked, hasDocumentsAccess, state.showDeleteDialog) {
        viewModel.hasEscrowBackup()
    }

    if (state.showDeleteDialog) {
        PowerAlertDialog(
            title = "Delete Entire Vault?",
            text = "This action is permanent and cannot be undone. All encrypted files, your Master Key, and your PIN will be permanently destroyed.",
            confirmText = "Permanently Delete",
            icon = Icons.Default.Warning,
            isDestructive = true,
            onDismiss = { viewModel.onEvent(VaultEvent.ToggleDeleteDialog(false)) },
            onConfirm = { viewModel.onEvent(VaultEvent.DeleteEntireVault) }
        )
    }

    state.fileToDelete?.let { _ ->
        PowerAlertDialog(
            title = "Delete File?",
            text = "Are you sure you want to permanently delete this file? This cannot be undone.",
            confirmText = "Delete",
            icon = Icons.Default.Delete,
            isDestructive = true,
            onDismiss = { viewModel.onEvent(VaultEvent.ToggleFileDeleteDialog(null)) },
            onConfirm = { viewModel.onEvent(VaultEvent.DeleteFile) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isUnlocked) {
                        IconButton(onClick = { viewModel.onEvent(VaultEvent.LockVault) }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasDocumentsAccess && state.isUnlocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
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
                targetState = Triple(state.isLoading, hasDocumentsAccess, state.isUnlocked),
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "VaultStateTransition"
            ) { (isLoading, hasAccess, isUnlocked) ->
                Column(
                    modifier = Modifier
                        .padding(Dimens.MD)
                        .fillMaxSize(),
                    verticalArrangement = if (isUnlocked || !hasAccess) Arrangement.Top else Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        !hasAccess -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(Dimens.LG),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.SM))
                                    Text(
                                        text = "Vault Storage Access Required",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "To securely store your encrypted files, please select or create a folder (e.g., 'PowerToolsVault') and click 'Use this folder'.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Dimens.SM)
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.SM))
                                    Button(onClick = { safLauncher.launch(null) }) {
                                        Text("Select Vault Folder")
                                    }
                                }
                            }
                        }

                        !isUnlocked -> {
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
                                    onValueChange = { viewModel.onEvent(VaultEvent.UpdatePinInput(it)) },
                                    label = { Text("Enter Previous PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.MD))

                                Button(
                                    onClick = { viewModel.onEvent(VaultEvent.RestoreVault) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Restore & Open Vault") }

                                Spacer(modifier = Modifier.height(Dimens.LG))
                                TextButton(
                                    onClick = { viewModel.onEvent(VaultEvent.ToggleDeleteDialog(true)) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("Forgot PIN? Factory Reset Vault") }

                            } else if (!state.isVaultSetup) {
                                Text("Create Master Vault PIN", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(Dimens.SM))

                                OutlinedTextField(
                                    value = state.pinInput,
                                    onValueChange = { viewModel.onEvent(VaultEvent.UpdatePinInput(it)) },
                                    label = { Text("Enter PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.SM))

                                OutlinedTextField(
                                    value = state.pinConfirmInput,
                                    onValueChange = { viewModel.onEvent(VaultEvent.UpdatePinConfirmInput(it)) },
                                    label = { Text("Confirm PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.MD))

                                Button(
                                    onClick = { viewModel.onEvent(VaultEvent.SetupVault) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Create Vault") }

                            } else {
                                Text("Enter Vault Master PIN", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(Dimens.SM))

                                OutlinedTextField(
                                    value = state.pinInput,
                                    onValueChange = { viewModel.onEvent(VaultEvent.UpdatePinInput(it)) },
                                    label = { Text("Master PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.MD))

                                Button(
                                    onClick = { viewModel.onEvent(VaultEvent.UnlockVault) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Unlock Vault") }

                                if (state.isBiometricEnabled) {
                                    Spacer(modifier = Modifier.height(Dimens.SM))
                                    Button(
                                        onClick = {
                                            BiometricAuthenticator.authenticate(
                                                activity = context as FragmentActivity,
                                                onSuccess = { viewModel.onEvent(VaultEvent.ProcessBiometricUnlock) },
                                                onError = { viewModel.onEvent(VaultEvent.SetError(it)) }
                                            )
                                        },
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

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Your Vault Files", style = MaterialTheme.typography.titleMedium)

                                if (!state.isBiometricEnabled) {
                                    TextButton(
                                        onClick = {
                                            BiometricAuthenticator.authenticate(
                                                activity = context as FragmentActivity,
                                                onSuccess = { viewModel.onEvent(VaultEvent.EnableBiometrics) },
                                                onError = { viewModel.onEvent(VaultEvent.SetError(it)) }
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(Dimens.IconSM))
                                        Spacer(Modifier.width(Dimens.XS))
                                        Text("Enable Biometrics")
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.onEvent(VaultEvent.DisableBiometrics) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Disable Biometrics") }
                                }
                            }

                            if (state.isRotatingKey) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(Dimens.MD)) {
                                        Text("Rotate Master Key & PIN", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "This will generate a new cryptographic Master Key and update all file headers. The actual file contents will not need to be re-encrypted.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = Dimens.SM)
                                        )
                                        OutlinedTextField(
                                            value = state.newPinInput,
                                            onValueChange = { viewModel.onEvent(VaultEvent.UpdateNewPinInput(it)) },
                                            label = { Text("Enter New PIN") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(Dimens.SM))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { viewModel.onEvent(VaultEvent.ToggleKeyRotation(false)) }) { Text("Cancel") }
                                            Spacer(modifier = Modifier.width(Dimens.SM))
                                            Button(onClick = { viewModel.onEvent(VaultEvent.RotateMasterKey) }) { Text("Confirm Rotation") }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = { viewModel.onEvent(VaultEvent.ToggleDeleteDialog(true)) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete Vault") }

                                    TextButton(onClick = { viewModel.onEvent(VaultEvent.ToggleKeyRotation(true)) }) { Text("Change PIN") }
                                }
                            }

                            state.errorMessage?.let {
                                Text(it, color = if(it.contains("success", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(Dimens.SM))
                            }

                            if (state.databaseFiles.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No secure items inside your vault catalog.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.SM)) {
                                    items(items = state.databaseFiles, key = { it.id }) { file ->
                                        val displayFilename = remember(file.encryptedName) {
                                            try { VaultNameEncryptor.decryptName(context, file.encryptedName) }
                                            catch (_: Exception) { "Encrypted_File_${file.id}" }
                                        }

                                        val readableSize = remember(file.fileSize) {
                                            Formatter.formatShortFileSize(context, file.fileSize)
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
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
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationNone)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(Dimens.MD),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.IconLG))
                                                Spacer(modifier = Modifier.width(Dimens.MD))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = displayFilename, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(Dimens.XXS))
                                                    Text(text = readableSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.onEvent(VaultEvent.ToggleFileDeleteDialog(file)) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}