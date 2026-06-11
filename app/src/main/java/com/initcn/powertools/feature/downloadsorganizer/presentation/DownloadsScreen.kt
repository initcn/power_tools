package com.initcn.powertools.feature.downloadsorganizer.presentation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.downloadsorganizer.domain.DownloadCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. Check for All Files Access (Android 11+)
    var hasAllFilesAccess by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
        )
    }

    // 2. Re-evaluate permission when returning to the screen from Android Settings
    LifecycleResumeEffect(Unit) {
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
        onPauseOrDispose { }
    }

    val downloadsOrganizerTitle = stringResource(R.string.downloads_organizer)
    val downloadsDescription = stringResource(R.string.downloads_description)
    val downloadsNoFilesText = stringResource(R.string.downloads_no_files)
    val downloadsFilesReadyTemplate = stringResource(R.string.downloads_files_ready)
    val filesMovedTemplate = stringResource(R.string.files_moved)
    val previewChangesText = stringResource(R.string.preview_changes)
    val organizeDownloadsText = stringResource(R.string.organize_downloads)
    val allRemainingFileTypesText = stringResource(R.string.all_remaining_file_types)

    PowerToolScaffold(title = downloadsOrganizerTitle) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.MD)
        ) {

            // 3. Just-In-Time (JIT) Permission UI for All Files Access
            if (!hasAllFilesAccess) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.LG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Dimens.SM))
                        Text(
                            text = "All Files Access Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "To automatically organize your global Downloads folder, PowerTools requires 'All Files Access'. Please grant this permission on the next screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Dimens.SM)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SM))

                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback if the specific app intent fails
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            }
                        }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                // 4. THE ACTUAL TOOL UI (Only shows if permission is granted)
                Text(
                    text = downloadsDescription,
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(modifier = Modifier.weight(1f)) {
                    LazyColumn {
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

                StatusMessage(message = state.statusMessage)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.onEvent(DownloadsEvent.PreviewChanges(downloadsNoFilesText, downloadsFilesReadyTemplate))
                    }
                ) {
                    Text(text = previewChangesText)
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.onEvent(DownloadsEvent.OrganizeDownloads(filesMovedTemplate))
                    }
                ) {
                    Text(text = organizeDownloadsText)
                }
            }
        }
    }
}

private fun DownloadCategory.icon(): ImageVector {
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