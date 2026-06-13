package com.initcn.powertools.feature.downloadsorganizer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.downloadsorganizer.domain.DownloadCategory

@Composable
fun DownloadsRoute(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeaturePermissionGuard(
        requiredPermissions = listOf(RequiredPermission.ALL_FILES_ACCESS),
        onNavigateBack = onNavigateBack
    ) {
        DownloadsScreen(
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    onEvent: (DownloadsEvent) -> Unit
) {
    PowerToolScaffold(title = stringResource(R.string.downloads_organizer)) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DownloadsContent(state, onEvent)
        }
    }
}

@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onEvent: (DownloadsEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.MD)
    ) {
        // Evaluate the UiText to a String here in the View
        StatusMessage(message = state.statusMessage?.asString())

        if (state.isOrganizing || state.moveLogs.isNotEmpty()) {
            Text(stringResource(R.string.move_logs), style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.moveLogs) { log ->
                    ListItem(
                        headlineContent = { Text(log) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        } else {
            Text(stringResource(R.string.pending_files), style = MaterialTheme.typography.titleMedium)

            if (!state.hasScanned) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.downloads_click_preview),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.liveFiles.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.downloads_no_files),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.liveFiles) { file ->
                        ListItem(
                            headlineContent = { Text(file.fileName) },
                            supportingContent = { Text(file.category.folderName) },
                            leadingContent = {
                                Icon(imageVector = file.category.icon(), contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        // BUTTONS AREA
        if (!state.isOrganizing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SM)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    // Much cleaner event dispatching!
                    onClick = { onEvent(DownloadsEvent.PreviewChanges) }
                ) {
                    Text(stringResource(R.string.preview_changes))
                }

                Button(
                    modifier = Modifier.weight(1f),
                    // Much cleaner event dispatching!
                    onClick = { onEvent(DownloadsEvent.OrganizeDownloads) },
                    enabled = state.hasScanned && state.liveFiles.isNotEmpty()
                ) {
                    Text(stringResource(R.string.organize_downloads))
                }
            }
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { },
                enabled = false
            ) {
                Text(stringResource(R.string.organizing))
            }
        }
    }
}

fun DownloadCategory.icon(): ImageVector {
    return when (this) {
        DownloadCategory.PHOTOS -> Icons.Outlined.Image
        DownloadCategory.VIDEOS -> Icons.Outlined.Movie
        DownloadCategory.AUDIO -> Icons.Outlined.AudioFile
        DownloadCategory.APPLICATIONS -> Icons.Outlined.Android
        DownloadCategory.PDF -> Icons.Outlined.PictureAsPdf
        DownloadCategory.DOCUMENTS -> Icons.Outlined.Description
        DownloadCategory.SPREADSHEETS -> Icons.Outlined.TableChart
        DownloadCategory.PRESENTATIONS -> Icons.Outlined.Slideshow
        DownloadCategory.ARCHIVES -> Icons.Outlined.FolderZip
        DownloadCategory.OTHERS -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}