package com.initcn.powertools.feature.colorfilter.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.initcn.powertools.feature.colorfilter.domain.ColorMode

// THE ROUTE:
@Composable
fun ColorFilterRoute(
    onBackClick: () -> Unit,
    viewModel: ColorFilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ColorFilterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

// THE SCREEN:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorFilterScreen(
    uiState: ColorFilterUiState,
    onEvent: (ColorFilterEvent) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Correction") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Adjust system display color spaces. Switching your phone to Grayscale can decrease overall phone interaction and screen fatigue.",
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
                    items(ColorMode.entries.toList()) { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (uiState.currentMode == mode),
                                onClick = { onEvent(ColorFilterEvent.ChangeMode(mode)) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = when (mode) {
                                    ColorMode.OFF -> "Disabled (Default System Colors)"
                                    ColorMode.GRAYSCALE -> "Grayscale (Black & White)"
                                    ColorMode.PROTANOMALY -> "Protanomaly (Red-Green)"
                                    ColorMode.DEUTERANOMALY -> "Deuteranomaly (Green-Red)"
                                    ColorMode.TRITANOMALY -> "Tritanomaly (Blue-Yellow)"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (!uiState.isPermissionGranted) {
                Text(
                    text = uiState.errorMessage ?: "Permission missing",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}