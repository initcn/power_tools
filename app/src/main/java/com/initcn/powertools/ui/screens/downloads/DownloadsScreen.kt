package com.initcn.powertools.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import com.initcn.powertools.features.download.DownloadsOrganizer
import com.initcn.powertools.model.DownloadCategory
import com.initcn.powertools.permissions.AllFilesAccessManager
import com.initcn.powertools.ui.components.PowerToolScaffold
import com.initcn.powertools.ui.components.StatusMessage
import com.initcn.powertools.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {

    val context = LocalContext.current

    val downloadsOrganizerTitle =
        stringResource(
            R.string.downloads_organizer
        )

    val downloadsDescription =
        stringResource(
            R.string.downloads_description
        )

    val downloadsNoFilesText =
        stringResource(
            R.string.downloads_no_files
        )

    val downloadsFilesReadyTemplate =
        stringResource(
            R.string.downloads_files_ready
        )

    val filesMovedTemplate =
        stringResource(
            R.string.files_moved
        )

    val previewChangesText =
        stringResource(
            R.string.preview_changes
        )

    val organizeDownloadsText =
        stringResource(
            R.string.organize_downloads
        )

    val grantAllFilesAccessText =
        stringResource(
            R.string.grant_files_access
        )

    val allRemainingFileTypesText =
        stringResource(
            R.string.all_remaining_file_types
        )

    var statusMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    PowerToolScaffold(
        title = downloadsOrganizerTitle
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    Dimens.ScreenPadding
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    Dimens.MD
                )
        ) {

            Text(
                text = downloadsDescription,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            Card(
                modifier = Modifier.weight(1f)
            ) {

                LazyColumn {

                    items(
                        items = DownloadCategory.entries,
                        key = { it.name }
                    ) { category ->

                        ListItem(
                            headlineContent = {

                                Text(
                                    text =
                                        category.folderName
                                )
                            },

                            supportingContent = {

                                Text(
                                    text =
                                        category.extensions
                                            .takeIf {
                                                it.isNotEmpty()
                                            }
                                            ?.joinToString(
                                                ", "
                                            )
                                            ?: allRemainingFileTypesText
                                )
                            },

                            leadingContent = {

                                Icon(
                                    imageVector =
                                        category.icon(),
                                    contentDescription =
                                        null
                                )
                            }
                        )
                    }
                }
            }

            StatusMessage(
                message = statusMessage
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    if (
                        !AllFilesAccessManager
                            .hasAccess()
                    ) {

                        statusMessage =
                            grantAllFilesAccessText

                        AllFilesAccessManager
                            .requestAccess(
                                context
                            )

                        return@Button
                    }

                    val operations =
                        DownloadsOrganizer
                            .preview()

                    statusMessage =
                        if (
                            operations.isEmpty()
                        ) {

                            downloadsNoFilesText

                        } else {

                            downloadsFilesReadyTemplate
                                .format(
                                    operations.size
                                )
                        }
                }
            ) {

                Text(
                    text =
                        previewChangesText
                )
            }

            Button(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick = {

                    if (
                        !AllFilesAccessManager
                            .hasAccess()
                    ) {

                        statusMessage =
                            grantAllFilesAccessText

                        AllFilesAccessManager
                            .requestAccess(
                                context
                            )

                        return@Button
                    }

                    val moved =
                        DownloadsOrganizer
                            .organize()

                    statusMessage =
                        filesMovedTemplate
                            .format(
                                moved
                            )
                }
            ) {

                Text(
                    text =
                        organizeDownloadsText
                )
            }
        }
    }
}

private fun DownloadCategory.icon():
        ImageVector {

    return when (this) {

        DownloadCategory.PHOTOS ->
            Icons.Outlined.Image

        DownloadCategory.VIDEOS ->
            Icons.Outlined.Movie

        DownloadCategory.AUDIO ->
            Icons.Outlined.AudioFile

        DownloadCategory.APPLICATIONS ->
            Icons.Outlined.Android

        DownloadCategory.PDF ->
            Icons.Outlined.PictureAsPdf

        DownloadCategory.DOCUMENTS ->
            Icons.Outlined.Description

        DownloadCategory.SPREADSHEETS ->
            Icons.Outlined.TableChart

        DownloadCategory.PRESENTATIONS ->
            Icons.Outlined.Slideshow

        DownloadCategory.ARCHIVES ->
            Icons.Outlined.FolderZip

        DownloadCategory.OTHERS ->
            Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}