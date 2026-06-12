package com.initcn.powertools.feature.doze.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.doze.domain.DozeManager

// ROUTE
// Handles Hilt, Context, Permissions, Intents, and Lifecycle.

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun DozeRoute(
    onNavigateBack: () -> Unit, // ADD THIS
    viewModel: DozeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeaturePermissionGuard(
        requiredPermissions = listOf(RequiredPermission.WRITE_SYSTEM_SETTINGS),
        onNavigateBack = onNavigateBack
    ) {
        DozeScreen(
            state = state,
            supportedTimeouts = viewModel.supportedTimeouts,
            onEvent = viewModel::onEvent
        )
    }
}

// SCREEN (Dumb Composable)
// Pure UI representation. Fully previewable. No framework dependencies.

@Composable
fun DozeScreen(
    state: DozeUiState,
    supportedTimeouts: List<DozeManager.TimeoutOption>,
    onEvent: (DozeEvent) -> Unit
) {
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
                    items(supportedTimeouts) { option ->
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
                                    onEvent(DozeEvent.SelectLabel(option.label))
                                }
                        )
                    }
                }
            }

            StatusMessage(
                message = state.statusMessage
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onEvent(DozeEvent.ApplyTimeout(successMsg, failureMsg))
                }
            ) {
                Text(text = stringResource(R.string.apply_timeout))
            }
        }
    }
}