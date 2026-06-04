package com.initcn.powertools.ui.screens.doze

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.initcn.powertools.R
import com.initcn.powertools.features.doze.DozeManager
import com.initcn.powertools.ui.components.PowerToolScaffold
import com.initcn.powertools.ui.components.StatusMessage
import com.initcn.powertools.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DozeScreen() {

    val context = LocalContext.current

    val screenDozeTitle =
        stringResource(
            R.string.screen_doze
        )

    val screenTimeoutDescription =
        stringResource(
            R.string.screen_timeout_description
        )

    val applyTimeoutText =
        stringResource(
            R.string.apply_timeout
        )

    val grantModifySettingsText =
        stringResource(
            R.string.grant_modify_settings
        )

    val timeoutSuccessText =
        stringResource(
            R.string.timeout_success
        )

    val timeoutFailedText =
        stringResource(
            R.string.timeout_failed
        )

    var selectedLabel by rememberSaveable {
        mutableStateOf(
            DozeManager.getCurrentLabel(
                context
            ) ?: "1 min"
        )
    }

    var statusMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    PowerToolScaffold(
        title = screenDozeTitle
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
                text = screenTimeoutDescription,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            Card(
                modifier = Modifier.weight(1f)
            ) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1)
                ) {

                    items(
                        DozeManager.supportedTimeouts
                    ) { option ->

                        ListItem(
                            headlineContent = {

                                Text(
                                    text =
                                        option.label
                                )
                            },

                            leadingContent = {

                                RadioButton(
                                    selected =
                                        selectedLabel ==
                                                option.label,

                                    onClick = {

                                        selectedLabel =
                                            option.label
                                    }
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth()
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
                        !Settings.System.canWrite(
                            context
                        )
                    ) {

                        statusMessage =
                            grantModifySettingsText

                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                "package:${context.packageName}"
                                    .toUri()
                            )
                        )

                        return@Button
                    }

                    val success =
                        DozeManager.applyTimeout(
                            context,
                            selectedLabel
                        )

                    statusMessage =
                        if (success) {

                            timeoutSuccessText

                        } else {

                            timeoutFailedText
                        }
                }
            ) {

                Text(
                    text =
                        applyTimeoutText
                )
            }
        }
    }
}