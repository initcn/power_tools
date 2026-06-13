package com.initcn.powertools.feature.callblocker.presentation

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.initcn.powertools.core.permissions.PermissionChecker
import com.initcn.powertools.feature.callblocker.data.CallBlockerPrefs
import com.initcn.powertools.feature.callblocker.data.CallRuleDao
import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.CallLogEntry
import com.initcn.powertools.feature.callblocker.domain.RuleType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CallBlockerViewModel @Inject constructor(
    private val dao: CallRuleDao,
    private val callBlockerPrefs: CallBlockerPrefs,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CallBlockerUiState(
            blockHiddenNumbers = callBlockerPrefs.isBlockHiddenEnabled(),
            blockUnsavedContacts = callBlockerPrefs.isBlockUnsavedEnabled(),
            blockAllCalls = callBlockerPrefs.isBlockAllEnabled(),
            disallowCall = callBlockerPrefs.isDisallowEnabled(),
            rejectCall = callBlockerPrefs.isRejectEnabled(),
            skipNotif = callBlockerPrefs.isSkipNotifEnabled(),
            silenceCall = callBlockerPrefs.isSilenceEnabled()
        )
    )
    val uiState: StateFlow<CallBlockerUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.getRulesByTypeFlow(RuleType.WHITELIST),
                dao.getRulesByTypeFlow(RuleType.BLOCKLIST_EXACT),
                dao.getRulesByTypeFlow(RuleType.BLOCKLIST_REGEX)
            ) { whitelist, exact, regex ->
                _uiState.update { state ->
                    state.copy(
                        whitelist = whitelist,
                        exactBlocklist = exact,
                        regexBlocklist = regex
                    )
                }
            }.collect {}
        }
    }

    fun onEvent(event: CallBlockerEvent) {
        when (event) {
            is CallBlockerEvent.ToggleBlockAll -> {
                callBlockerPrefs.setBlockAllEnabled(event.enabled)
                _uiState.update { it.copy(blockAllCalls = event.enabled) }
            }
            is CallBlockerEvent.ToggleBlockHidden -> {
                callBlockerPrefs.setBlockHiddenEnabled(event.enabled)
                _uiState.update { it.copy(blockHiddenNumbers = event.enabled) }
            }
            is CallBlockerEvent.ToggleBlockUnsaved -> {
                callBlockerPrefs.setBlockUnsavedEnabled(event.enabled)
                _uiState.update { it.copy(blockUnsavedContacts = event.enabled) }
            }
            is CallBlockerEvent.ToggleDisallow -> {
                callBlockerPrefs.setDisallowEnabled(event.enabled)
                _uiState.update { it.copy(disallowCall = event.enabled) }
            }
            is CallBlockerEvent.ToggleReject -> {
                callBlockerPrefs.setRejectEnabled(event.enabled)
                _uiState.update { it.copy(rejectCall = event.enabled) }
            }
            is CallBlockerEvent.ToggleSkipNotif -> {
                callBlockerPrefs.setSkipNotifEnabled(event.enabled)
                _uiState.update { it.copy(skipNotif = event.enabled) }
            }
            is CallBlockerEvent.ToggleSilence -> {
                callBlockerPrefs.setSilenceEnabled(event.enabled)
                _uiState.update { it.copy(silenceCall = event.enabled) }
            }
            is CallBlockerEvent.AddRule -> {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertRule(CallRuleEntity(pattern = event.pattern, ruleType = event.ruleType, label = event.label))
                }
            }
            is CallBlockerEvent.RemoveRule -> {
                viewModelScope.launch(Dispatchers.IO) { dao.deleteRule(event.rule) }
            }
            is CallBlockerEvent.ExportRules -> exportRules()
            is CallBlockerEvent.ImportRules -> importRules(event.uri)
            is CallBlockerEvent.FetchRecentCalls -> fetchRecentCalls()
            is CallBlockerEvent.ClearAllRules -> clearAllRules()
        }
    }

    private fun exportRules() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allRules = dao.getWhitelistSync() + dao.getBlocklistSync()
                val json = Gson().toJson(allRules)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "powertools_call_rules.json")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                    _uiEvent.emit("Exported successfully to Downloads folder.")
                } else {
                    _uiEvent.emit("Failed to create file in Downloads.")
                }
            } catch (e: Exception) {
                _uiEvent.emit("Failed to export rules.")
            }
        }
    }

    private fun importRules(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@launch

                val type = object : TypeToken<List<CallRuleEntity>>() {}.type
                val rules: List<CallRuleEntity> = Gson().fromJson(json, type)

                if (rules.isNotEmpty()) {
                    dao.insertRules(rules)
                    _uiEvent.emit("Successfully imported ${rules.size} rules.")
                } else {
                    _uiEvent.emit("No rules found in backup file.")
                }
            } catch (e: Exception) {
                _uiEvent.emit("Failed to import. Invalid file format.")
            }
        }
    }

    private fun fetchRecentCalls() {
        if (!PermissionChecker.hasCallLogAccess(context)) {
            _uiState.update { it.copy(recentCalls = emptyList()) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val logs = mutableListOf<CallLogEntry>()
            val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.TYPE)

            // Check if we have contacts permission to do a manual lookup
            val hasContactsPermission = PermissionChecker.hasContactsAccess(context)

            try {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->
                    val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                    while (cursor.moveToNext() && logs.size < 50) {
                        val number = cursor.getString(numberIndex) ?: "Unknown"
                        var name = cursor.getString(nameIndex)

                        // Forcefully check contacts if Android failed to cache the name
                        if (name.isNullOrBlank() && hasContactsPermission && number != "Unknown") {
                            name = getContactName(number)
                        }

                        logs.add(
                            CallLogEntry(
                                number = number,
                                name = name?.takeIf { it.isNotBlank() },
                                date = cursor.getLong(dateIndex),
                                type = cursor.getInt(typeIndex)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(recentCalls = logs) }
            }
        }
    }

    // Helper function to actively query the contacts database by phone number
    private fun getContactName(phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun clearAllRules() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteAllRules()
                _uiEvent.emit("All rules have been cleared.")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to clear rules.")
            }
        }
    }
}