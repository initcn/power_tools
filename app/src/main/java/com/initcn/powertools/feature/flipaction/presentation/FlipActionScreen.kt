package com.initcn.powertools.feature.flipaction.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.feature.flipaction.domain.FlipActionManager
import com.initcn.powertools.feature.flipaction.service.FlipSensorService
import java.util.Locale

@Composable
fun FlipActionRoute(
    onNavigateBack: () -> Unit,
    viewModel: FlipActionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeaturePermissionGuard(
        requiredPermissions = listOf(RequiredPermission.ACCESS_NOTIFICATION_POLICY),
        onNavigateBack = onNavigateBack
    ) {
        // Starts/Stops the robust Foreground Service when the toggle changes
        LaunchedEffect(state.isEnabled) {
            if (state.isEnabled) {
                FlipSensorService.start(context)
            } else {
                FlipSensorService.stop(context)
            }
        }

        FlipActionScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onRequestBatteryExemption = { requestBatteryExemption(context) },
            isBatteryOptimized = !isIgnoringBatteryOptimizations(context)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipActionScreen(
    state: FlipActionUiState,
    onEvent: (FlipActionEvent) -> Unit,
    onRequestBatteryExemption: () -> Unit,
    isBatteryOptimized: Boolean
) {
    PowerToolScaffold(title = "Flip to Action") { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MAIN TOGGLE CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.MD),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.LG),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Flip Sensor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Place phone face down to trigger action.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { onEvent(FlipActionEvent.ToggleFeature(it)) }
                    )
                }
            }

            // BATTERY WARNING
            if (state.isEnabled && isBatteryOptimized) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.MD, vertical = Dimens.SM),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(Dimens.MD)) {
                        Text(
                            text = "Background Execution Restricted",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Android will kill the flip sensor to save battery. Please exempt PowerTools from battery optimizations.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = Dimens.XS, bottom = Dimens.SM)
                        )
                        Button(
                            onClick = onRequestBatteryExemption,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Exempt App")
                        }
                    }
                }
            }

            // LIVE SENSOR MONITOR
            if (state.isEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.MD, vertical = Dimens.SM),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(Dimens.MD)) {
                        Text(
                            text = "Live Gravity Sensor Data",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(Dimens.SM))

                        // FIX: Explicitly passing Locale.US to prevent comma/period formatting bugs
                        Text("X-Axis (Left/Right Tilt): ${String.format(Locale.US, "%.2f", state.sensorData.x)}")
                        Text("Y-Axis (Up/Down Tilt): ${String.format(Locale.US, "%.2f", state.sensorData.y)}")

                        Text(
                            text = "Z-Axis (Face Up/Down): ${String.format(Locale.US, "%.2f", state.sensorData.z)}",
                            fontWeight = FontWeight.Bold,
                            color = if (state.sensorData.z < -6.0f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text(
                text = "ACTION ON FLIP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Dimens.LG, top = Dimens.MD, bottom = Dimens.XS)
            )

            // ACTION SELECTION LIST
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Dimens.MD)
            ) {
                items(FlipActionManager.FlipMode.entries) { mode ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.XS)
                            .clickable { onEvent(FlipActionEvent.SelectMode(mode)) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (state.selectedMode == mode) 2.dp else 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.selectedMode == mode,
                                onClick = { onEvent(FlipActionEvent.SelectMode(mode)) }
                            )
                            Spacer(modifier = Modifier.width(Dimens.MD))
                            Column {
                                Text(
                                    text = mode.title,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers for checking battery optimization status
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
fun requestBatteryExemption(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}