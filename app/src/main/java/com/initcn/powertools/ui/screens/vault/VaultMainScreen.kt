package com.initcn.powertools.ui.screens.vault

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.util.Base64
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.crypto.VaultNameEncryptor
import com.initcn.powertools.data.vault.VaultDatabase
import com.initcn.powertools.data.vault.VaultFileEntity
import com.initcn.powertools.data.vault.VaultStorageManager
import com.initcn.powertools.features.vault.VaultManager
import com.initcn.powertools.features.vault.saf.VaultSessionManager
import com.initcn.powertools.features.vault.saf.auth.BiometricAuthenticator
import com.initcn.powertools.features.vault.saf.auth.BiometricKeyManager
import com.initcn.powertools.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val vaultDir = VaultStorageManager.getPublicVaultDirectory()
    val dao = VaultDatabase.getDatabase(context).vaultDao()

    LaunchedEffect(Unit) {
        VaultSessionManager.initialize(context)
    }

    val isVaultSetup by VaultSessionManager.isVaultSetup.collectAsStateWithLifecycle()
    val isBiometricEnabled by VaultSessionManager.isBiometricEnabled.collectAsStateWithLifecycle()
    var isUnlocked by remember { mutableStateOf(VaultSessionManager.isUnlocked()) }

    // FIX: Add state keys so the UI re-checks the disk after a deletion or unlock event
    val hasEscrowBackup = remember(isVaultSetup, isUnlocked) { File(vaultDir, "master.key.blob").exists() }

    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Key Rotation State
    var isRotatingKey by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    // Vault Deletion State
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Single File Deletion State
    var fileToDelete by remember { mutableStateOf<VaultFileEntity?>(null) }

    val databaseFiles by dao.getFilesByParentPathFlow("/").collectAsStateWithLifecycle(initialValue = emptyList())

    // --- DELETION CONFIRMATION DIALOG ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Entire Vault?") },
            text = { Text("This action is permanent and cannot be undone. All encrypted files, your Master Key, and your PIN will be permanently destroyed.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = VaultManager.deleteEntireVault(context, vaultDir, dao)
                            if (success) {
                                isUnlocked = false
                                pinInput = ""
                                pinConfirmInput = ""
                            } else {
                                errorMessage = "Failed to fully delete vault."
                            }
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- SINGLE FILE DELETION DIALOG ---
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete File?") },
            text = { Text("Are you sure you want to permanently delete this file? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // 1. Remove from database
                            dao.deleteFile(file)
                            // 2. Destroy physical file
                            File(vaultDir, "${file.id}.enc").delete()

                            fileToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
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
                    if (isUnlocked) {
                        IconButton(
                            onClick = {
                                VaultSessionManager.lock()
                                isUnlocked = false
                                pinInput = ""
                                pinConfirmInput = ""
                                isRotatingKey = false
                            }
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(Dimens.MD)
                .fillMaxSize(),
            verticalArrangement = if (isUnlocked) Arrangement.Top else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isUnlocked) {
                if (!isVaultSetup && hasEscrowBackup) {
                    // AUTO-RECOVERY FLOW
                    Text("Reinstall Detected", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Enter your previous Vault PIN to restore your keys and access your encrypted files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Dimens.SM)
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; errorMessage = null },
                        label = { Text("Enter Previous PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.MD))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = VaultManager.initializeVault(context, vaultDir, dao, pinInput)
                                if (success) {
                                    VaultSessionManager.setupVault(context, pinInput)
                                    isUnlocked = true
                                } else {
                                    errorMessage = "Invalid PIN. Unable to restore encryption keys."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore & Open Vault")
                    }

                    // Failsafe for stuck recovery
                    Spacer(modifier = Modifier.height(Dimens.LG))
                    TextButton(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Forgot PIN? Factory Reset Vault")
                    }

                } else if (!isVaultSetup) {
                    // FIRST-TIME SETUP FLOW
                    Text("Create Master Vault PIN", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(Dimens.SM))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; errorMessage = null },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.SM))
                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { pinConfirmInput = it; errorMessage = null },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.MD))

                    Button(
                        onClick = {
                            if (pinInput.isBlank() || pinInput.length < 4) {
                                errorMessage = "PIN must be at least 4 digits."
                                return@Button
                            }
                            if (pinInput != pinConfirmInput) {
                                errorMessage = "PINs do not match."
                                return@Button
                            }

                            VaultSessionManager.setupVault(context, pinInput)
                            VaultManager.createEscrowBackup(vaultDir, pinInput)
                            isUnlocked = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Vault")
                    }
                } else {
                    // REGULAR UNLOCK FLOW
                    Text("Enter Vault Master PIN", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(Dimens.SM))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; errorMessage = null },
                        label = { Text("Master PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.MD))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = VaultManager.initializeVault(context, vaultDir, dao, pinInput)
                                if (success && VaultSessionManager.verifyAndUnlock(context, pinInput)) {
                                    isUnlocked = true
                                } else {
                                    errorMessage = "Incorrect master credentials."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock Vault")
                    }

                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(Dimens.SM))
                        Button(
                            onClick = {
                                val activity = context as FragmentActivity
                                BiometricAuthenticator.authenticate(
                                    activity = activity,
                                    onSuccess = {
                                        coroutineScope.launch {
                                            try {
                                                val (encryptedPinB64, ivB64) = VaultSessionManager.getBiometricData(context)
                                                if (encryptedPinB64 != null && ivB64 != null) {
                                                    val encryptedBytes = Base64.decode(encryptedPinB64, Base64.DEFAULT)
                                                    val iv = Base64.decode(ivB64, Base64.DEFAULT)

                                                    val cipher = BiometricKeyManager.getDecryptionCipher(iv)
                                                    val decryptedPin = String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)

                                                    val success = VaultManager.initializeVault(context, vaultDir, dao, decryptedPin)
                                                    if (success && VaultSessionManager.verifyAndUnlock(context, decryptedPin)) {
                                                        pinInput = decryptedPin
                                                        isUnlocked = true
                                                    } else {
                                                        errorMessage = "Vault rejected the biometric PIN recovery."
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                errorMessage = "Biometrics invalidated. Please use PIN."
                                                VaultSessionManager.disableBiometrics(context)
                                            }
                                        }
                                    },
                                    onError = { errorMessage = it }
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

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(Dimens.SM))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // SECURE CONTENT DASHBOARD
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Vault Files", style = MaterialTheme.typography.titleMedium)

                    if (!isBiometricEnabled) {
                        TextButton(
                            onClick = {
                                val activity = context as FragmentActivity
                                BiometricAuthenticator.authenticate(
                                    activity = activity,
                                    onSuccess = {
                                        try {
                                            val cipher = BiometricKeyManager.getEncryptionCipher()
                                            val encryptedBytes = cipher.doFinal(pinInput.toByteArray(Charsets.UTF_8))
                                            VaultSessionManager.enableBiometrics(
                                                context,
                                                Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
                                                Base64.encodeToString(cipher.iv, Base64.DEFAULT)
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            errorMessage = "Failed to securely enable biometrics."
                                        }
                                    },
                                    onError = { errorMessage = it }
                                )
                            }
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(Dimens.IconSM))
                            Spacer(Modifier.width(Dimens.XS))
                            Text("Enable Biometrics")
                        }
                    } else {
                        TextButton(
                            onClick = { VaultSessionManager.disableBiometrics(context) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Disable Biometrics")
                        }
                    }
                }

                // Security Actions Row
                if (isRotatingKey) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(Dimens.MD)) {
                            Text("Rotate Master Key & PIN", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "This will generate a new cryptographic Master Key and update all file headers. The actual file contents will not need to be re-encrypted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Dimens.SM)
                            )
                            Spacer(modifier = Modifier.height(Dimens.XS))
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it },
                                label = { Text("Enter New PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.SM))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { isRotatingKey = false }) { Text("Cancel") }
                                Spacer(modifier = Modifier.width(Dimens.SM))
                                Button(
                                    onClick = {
                                        if (newPinInput.length >= 4) {
                                            coroutineScope.launch {
                                                val success = VaultManager.rotateMasterVaultKey(context, vaultDir, newPinInput)
                                                if (success) {
                                                    isRotatingKey = false
                                                    newPinInput = ""
                                                    errorMessage = "Master Key successfully rotated!"
                                                } else {
                                                    errorMessage = "Failed to rotate key."
                                                }
                                            }
                                        } else {
                                            errorMessage = "PIN must be at least 4 digits."
                                        }
                                    }
                                ) {
                                    Text("Confirm Rotation")
                                }
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.MD), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text("Delete Vault")
                        }
                        TextButton(onClick = { isRotatingKey = true }) {
                            Text("Change PIN")
                        }
                    }
                }

                if (databaseFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No secure items inside your vault catalog.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SM)
                    ) {
                        items(
                            items = databaseFiles,
                            key = { it.id }
                        ) { file ->
                            val displayFilename = remember(file.encryptedName) {
                                try {
                                    VaultNameEncryptor.decryptName(context, file.encryptedName)
                                } catch (_: Exception) {
                                    "Encrypted_File_${file.id}"
                                }
                            }

                            val readableSize = remember(file.fileSize) {
                                Formatter.formatShortFileSize(context, file.fileSize)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val uri = DocumentsContract.buildDocumentUri(
                                                "com.initcn.powertools.vault.documents",
                                                file.id
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, file.mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            errorMessage = "No app installed to view this file type."
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            errorMessage = "Failed to open file."
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.MD),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(Dimens.IconLG)
                                    )

                                    Spacer(modifier = Modifier.width(Dimens.MD))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayFilename,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = readableSize,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { fileToDelete = file }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete File",
                                            tint = MaterialTheme.colorScheme.error
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
}