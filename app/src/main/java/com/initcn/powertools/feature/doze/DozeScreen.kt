package com.initcn.powertools.feature.doze

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage

@Composable
fun DozeScreen(
    viewModel: DozeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Single source of truth from MVI state
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. Track permission state
    var hasWriteSettingsPermission by remember {
        mutableStateOf(Settings.System.canWrite(context))
    }

    // 2. Re-evaluate permission whenever the screen resumes (e.g., coming back from Settings)
    LifecycleResumeEffect(Unit) {
        hasWriteSettingsPermission = Settings.System.canWrite(context)
        onPauseOrDispose { }
    }

    // Strings for messages
    val successMsg = stringResource(R.string.timeout_success)
    val failureMsg = stringResource(R.string.timeout_failed)

    PowerToolScaffold(title = stringResource(R.string.screen_doze)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.MD)
        ) {

            // 3. Just-In-Time (JIT) Permission UI
            if (!hasWriteSettingsPermission) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.LG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Dimens.SM))
                        Text(
                            text = "Modify System Settings Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "To alter the screen timeout, PowerTools needs permission to modify system settings. Please grant this permission on the next screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Dimens.SM)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SM))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        "package:${context.packageName}".toUri()
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                // 4. THE ACTUAL TOOL UI (Only shows if permission is granted)
                Text(
                    text = stringResource(R.string.screen_timeout_description),
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn {
                        items(viewModel.supportedTimeouts) { option ->
                            ListItem(
                                headlineContent = { Text(option.label) },
                                leadingContent = {
                                    RadioButton(
                                        selected = state.selectedLabel == option.label,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onEvent(DozeEvent.SelectLabel(option.label))
                                    }
                            )
                        }
                    }
                }

                StatusMessage(
                    message = state.statusMessage,
                    modifier = Modifier.padding(vertical = Dimens.SM)
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.onEvent(DozeEvent.ApplyTimeout(successMsg, failureMsg))
                    }
                ) {
                    Text(text = stringResource(R.string.apply_timeout))
                }
            }
        }
    }
}