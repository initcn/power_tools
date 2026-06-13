package com.initcn.powertools.feature.vault.presentation.components

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import androidx.compose.ui.text.style.TextOverflow
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.feature.vault.data.VaultFileEntity
import com.initcn.powertools.feature.vault.domain.VaultNameEncryptor
import com.initcn.powertools.feature.vault.presentation.VaultEvent
import com.initcn.powertools.feature.vault.presentation.VaultUiState

@Composable
fun VaultHomeTab(
    state: VaultUiState,
    onViewFile: (VaultFileEntity) -> Unit,
    onEvent: (VaultEvent) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.databaseFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.vault_empty_catalog), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.MD,
                    end = Dimens.MD,
                    top = Dimens.MD,
                    bottom = Dimens.ListBottomFabClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SM)
            ) {
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
                            .clickable { onViewFile(file) },
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
                            IconButton(onClick = { onEvent(VaultEvent.ToggleFileDeleteDialog(file)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}