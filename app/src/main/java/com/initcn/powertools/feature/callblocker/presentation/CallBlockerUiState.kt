package com.initcn.powertools.feature.callblocker.presentation

import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.CallLogEntry

data class CallBlockerUiState(
    val whitelist: List<CallRuleEntity> = emptyList(),
    val exactBlocklist: List<CallRuleEntity> = emptyList(),
    val regexBlocklist: List<CallRuleEntity> = emptyList(),
    val blockHiddenNumbers: Boolean = false,
    val blockUnsavedContacts: Boolean = false,
    val disallowCall: Boolean = true,
    val rejectCall: Boolean = true,
    val skipNotif: Boolean = true,
    val silenceCall: Boolean = false,
    val recentCalls: List<CallLogEntry> = emptyList()
)