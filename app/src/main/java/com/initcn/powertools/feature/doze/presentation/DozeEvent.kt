package com.initcn.powertools.feature.doze.presentation

import com.initcn.powertools.core.utils.UiText

sealed interface DozeEvent {
    data class SelectLabel(val label: String) : DozeEvent
    data class SetStatusMessage(val message: UiText?) : DozeEvent
    data object ApplyTimeout : DozeEvent
}