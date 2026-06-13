package com.initcn.powertools.feature.callblocker.presentation

import android.net.Uri
import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.RuleType

sealed interface CallBlockerEvent {

    data class ToggleBlockAll(val enabled: Boolean) : CallBlockerEvent
    data class ToggleBlockHidden(val enabled: Boolean) : CallBlockerEvent
    data class ToggleBlockUnsaved(val enabled: Boolean) : CallBlockerEvent
    data class ToggleDisallow(val enabled: Boolean) : CallBlockerEvent
    data class ToggleReject(val enabled: Boolean) : CallBlockerEvent
    data class ToggleSkipNotif(val enabled: Boolean) : CallBlockerEvent
    data class ToggleSilence(val enabled: Boolean) : CallBlockerEvent

    data class AddRule(val pattern: String, val ruleType: RuleType, val label: String?) : CallBlockerEvent
    data class RemoveRule(val rule: CallRuleEntity) : CallBlockerEvent

    data object ExportRules : CallBlockerEvent
    data class ImportRules(val uri: Uri) : CallBlockerEvent
    data object FetchRecentCalls : CallBlockerEvent
    data object ClearAllRules : CallBlockerEvent
}