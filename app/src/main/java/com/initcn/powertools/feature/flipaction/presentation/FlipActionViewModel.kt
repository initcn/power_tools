package com.initcn.powertools.feature.flipaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.initcn.powertools.feature.flipaction.data.FlipActionPrefs
import com.initcn.powertools.feature.flipaction.domain.FlipActionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlipActionViewModel @Inject constructor(
    private val prefs: FlipActionPrefs,
    private val flipActionManager: FlipActionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FlipActionUiState(
            isEnabled = prefs.isFeatureEnabled(),
            selectedMode = prefs.getSelectedMode()
        )
    )
    val uiState: StateFlow<FlipActionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            flipActionManager.sensorValues.collectLatest { data ->
                _uiState.update { it.copy(sensorData = data) }
            }
        }
    }

    fun onEvent(event: FlipActionEvent) {
        when (event) {
            is FlipActionEvent.ToggleFeature -> {
                prefs.setFeatureEnabled(event.enabled)
                _uiState.update { it.copy(isEnabled = event.enabled) }
            }
            is FlipActionEvent.SelectMode -> {
                prefs.setSelectedMode(event.mode)
                _uiState.update { it.copy(selectedMode = event.mode) }
            }
        }
    }
}

// --- MERGED PRESENTATION CONTRACT ---

data class FlipActionUiState(
    val isEnabled: Boolean = false,
    val selectedMode: FlipActionManager.FlipMode = FlipActionManager.FlipMode.SILENCE,
    val sensorData: FlipActionManager.SensorData = FlipActionManager.SensorData(0f, 0f, 0f)
)

sealed interface FlipActionEvent {
    data class ToggleFeature(val enabled: Boolean) : FlipActionEvent
    data class SelectMode(val mode: FlipActionManager.FlipMode) : FlipActionEvent
}