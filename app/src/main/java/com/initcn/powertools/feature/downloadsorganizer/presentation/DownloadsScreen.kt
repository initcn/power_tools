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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

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
    val downloadsOrganizerTitle = stringResource(R.string.downloads_organizer)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    PowerToolScaffold(title = downloadsOrganizerTitle) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                val tabs = listOf(stringResource(R.string.tab_home), stringResource(R.string.tab_about))
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
                    0 -> DownloadsHomeTab(state, onEvent)
                    1 -> DownloadsAboutTab()
                }
            }
        }
    }
}

@Composable
fun DownloadsHomeTab(
    state: DownloadsUiState,
    onEvent: (DownloadsEvent) -> Unit
) {
    val previewChangesText = stringResource(R.string.preview_changes)
    val organizeDownloadsText = stringResource(R.string.organize_downloads)
    val downloadsNoFilesText = stringResource(R.string.downloads_no_files)
    val downloadsFilesReadyTemplate = stringResource(R.string.downloads_files_ready)
    val filesMovedTemplate = stringResource(R.string.files_moved)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.MD)
    ) {
        StatusMessage(message = state.statusMessage)

        if (state.isOrganizing || state.moveLogs.isNotEmpty()) {
            Text(stringResource(R.string.move_logs), style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.moveLogs) { log ->
                        ListItem(headlineContent = { Text(log) })
                    }
                }
            }
        } else {
            Text(stringResource(R.string.pending_files), style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (!state.hasScanned) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Click Preview to scan your Downloads folder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (state.liveFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(downloadsNoFilesText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.liveFiles) { file ->
                            ListItem(
                                headlineContent = { Text(file.fileName) },
                                supportingContent = { Text("Will move to: ${file.category.folderName}") },
                                leadingContent = {
                                    Icon(imageVector = file.category.icon(), contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        }

        // BUTTONS AREA (Now side-by-side using Row and weight)
        if (!state.isOrganizing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SM)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(DownloadsEvent.PreviewChanges(downloadsNoFilesText, downloadsFilesReadyTemplate)) }
                ) {
                    Text(previewChangesText)
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(DownloadsEvent.OrganizeDownloads(filesMovedTemplate)) },
                    enabled = state.hasScanned && state.liveFiles.isNotEmpty()
                ) {
                    Text(organizeDownloadsText)
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

@Composable
fun DownloadsAboutTab() {
    val allRemainingFileTypesText = stringResource(R.string.all_remaining_file_types)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.MD)
    ) {
        Text(
            text = stringResource(R.string.downloads_description),
            style = MaterialTheme.typography.bodyLarge
        )

        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = DownloadCategory.entries, key = { it.name }) { category ->
                    ListItem(
                        headlineContent = { Text(text = category.folderName) },
                        supportingContent = {
                            Text(
                                text = category.extensions
                                    .takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ")
                                    ?: allRemainingFileTypesText
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = category.icon(),
                                contentDescription = null
                            )
                        }
                    )
                }
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