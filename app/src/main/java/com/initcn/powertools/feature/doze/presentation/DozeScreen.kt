package com.initcn.powertools.feature.doze.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.R
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.core.ui.components.StatusMessage
import com.initcn.powertools.feature.doze.domain.DozeManager

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun DozeRoute(
    onNavigateBack: () -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DozeScreen(
    state: DozeUiState,
    supportedTimeouts: List<DozeManager.TimeoutOption>,
    onEvent: (DozeEvent) -> Unit
) {
    PowerToolScaffold(title = stringResource(R.string.screen_doze)) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_timeout_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(supportedTimeouts) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Clickable goes BEFORE padding so the ripple effect looks clean
                                .clickable { onEvent(DozeEvent.SelectLabel(option.label)) }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.selectedLabel == option.label),
                                onClick = { onEvent(DozeEvent.SelectLabel(option.label)) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Safely unwrap the UiText here in the Compose UI
            StatusMessage(
                message = state.statusMessage?.asString()
            )
        }
    }
}