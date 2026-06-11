package com.initcn.powertools.ui.screens.doze

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.initcn.powertools.R
import com.initcn.powertools.features.doze.DozeManager
import com.initcn.powertools.ui.components.PowerToolScaffold
import com.initcn.powertools.ui.components.StatusMessage
import com.initcn.powertools.ui.theme.Dimens

@Composable
fun DozeScreen() {
    val context = LocalContext.current
    var selectedLabel by rememberSaveable { mutableStateOf(DozeManager.getCurrentLabel(context) ?: "1 min") }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    PowerToolScaffold(title = stringResource(R.string.screen_doze)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
        ) {
            Text(
                text = stringResource(R.string.screen_timeout_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = Dimens.MD)
            )

            // FIX: Set containerColor to transparent or background to remove the grey
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // Or Color.Transparent
                )
            ) {
                LazyColumn {
                    items(DozeManager.supportedTimeouts) { option ->
                        ListItem(
                            headlineContent = { Text(option.label) },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedLabel == option.label,
                                    onClick = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLabel = option.label }
                        )
                    }
                }
            }

            StatusMessage(
                message = statusMessage,
                modifier = Modifier.padding(vertical = Dimens.SM)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!Settings.System.canWrite(context)) {
                        statusMessage = context.getString(R.string.grant_modify_settings)
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
                        return@Button
                    }
                    statusMessage = if (DozeManager.applyTimeout(context, selectedLabel))
                        context.getString(R.string.timeout_success) else context.getString(R.string.timeout_failed)
                }
            ) {
                Text(text = stringResource(R.string.apply_timeout))
            }
        }
    }
}