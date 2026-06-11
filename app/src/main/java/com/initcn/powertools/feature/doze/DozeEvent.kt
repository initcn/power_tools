package com.initcn.powertools.feature.doze

sealed interface DozeEvent {
    data class SelectLabel(val label: String) : DozeEvent
    data class ApplyTimeout(val successMessage: String, val failureMessage: String) : DozeEvent
    data class SetStatusMessage(val message: String?) : DozeEvent
}