package com.initcn.powertools.feature.doze.presentation

import com.initcn.powertools.core.utils.UiText

data class DozeUiState(
    val selectedLabel: String = "1 min",
    val statusMessage: UiText? = null
)