package com.initcn.powertools.features.callblocker.ui

import android.app.Application
import android.net.Uri
import android.provider.CallLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.initcn.powertools.data.callblocker.CallBlockerDatabase
import com.initcn.powertools.data.callblocker.CallRuleEntity
import com.initcn.powertools.data.callblocker.RuleType
import com.initcn.powertools.features.callblocker.CallBlockerPrefs
import com.initcn.powertools.model.CallLogEntry
import com.initcn.powertools.permissions.PermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CallBlockerDatabase.getDatabase(application).callRuleDao()

    // Database Streams (Automatically update UI when rules change)
    val whitelist = dao.getRulesByTypeFlow(RuleType.WHITELIST)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val exactBlocklist = dao.getRulesByTypeFlow(RuleType.BLOCKLIST_EXACT)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val regexBlocklist = dao.getRulesByTypeFlow(RuleType.BLOCKLIST_REGEX)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Filter Preferences State ---
    private val _blockHiddenNumbers = MutableStateFlow(CallBlockerPrefs.isBlockHiddenEnabled(application))
    val blockHiddenNumbers: StateFlow<Boolean> = _blockHiddenNumbers.asStateFlow()

    private val _blockUnsavedContacts = MutableStateFlow(CallBlockerPrefs.isBlockUnsavedEnabled(application))
    val blockUnsavedContacts: StateFlow<Boolean> = _blockUnsavedContacts.asStateFlow()

    // --- Behavior Preferences State ---
    private val _disallowCall = MutableStateFlow(CallBlockerPrefs.isDisallowEnabled(application))
    val disallowCall: StateFlow<Boolean> = _disallowCall.asStateFlow()

    private val _rejectCall = MutableStateFlow(CallBlockerPrefs.isRejectEnabled(application))
    val rejectCall: StateFlow<Boolean> = _rejectCall.asStateFlow()

    private val _skipNotif = MutableStateFlow(CallBlockerPrefs.isSkipNotifEnabled(application))
    val skipNotif: StateFlow<Boolean> = _skipNotif.asStateFlow()

    private val _silenceCall = MutableStateFlow(CallBlockerPrefs.isSilenceEnabled(application))
    val silenceCall: StateFlow<Boolean> = _silenceCall.asStateFlow()

    // Call Log State
    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    // UI Events (For Toasts/Snackbars)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // --- Toggles ---
    fun toggleBlockHiddenNumbers(enabled: Boolean) {
        CallBlockerPrefs.setBlockHiddenEnabled(getApplication(), enabled)
        _blockHiddenNumbers.value = enabled
    }

    fun toggleBlockUnsavedContacts(enabled: Boolean) {
        CallBlockerPrefs.setBlockUnsavedEnabled(getApplication(), enabled)
        _blockUnsavedContacts.value = enabled
    }

    fun toggleDisallowCall(enabled: Boolean) {
        CallBlockerPrefs.setDisallowEnabled(getApplication(), enabled)
        _disallowCall.value = enabled
    }

    fun toggleRejectCall(enabled: Boolean) {
        CallBlockerPrefs.setRejectEnabled(getApplication(), enabled)
        _rejectCall.value = enabled
    }

    fun toggleSkipNotif(enabled: Boolean) {
        CallBlockerPrefs.setSkipNotifEnabled(getApplication(), enabled)
        _skipNotif.value = enabled
    }

    fun toggleSilenceCall(enabled: Boolean) {
        CallBlockerPrefs.setSilenceEnabled(getApplication(), enabled)
        _silenceCall.value = enabled
    }

    // --- Rules Management ---
    fun addRule(pattern: String, ruleType: RuleType, label: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val rule = CallRuleEntity(
                pattern = pattern,
                ruleType = ruleType,
                label = label
            )
            dao.insertRule(rule)
        }
    }

    fun removeRule(rule: CallRuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteRule(rule)
        }
    }

    // --- Import / Export Logic ---
    // --- Import / Export Logic ---
    fun exportRules() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val allRules = dao.getWhitelistSync() + dao.getBlocklistSync()
                val json = Gson().toJson(allRules)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "powertools_call_rules.json")
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        }
                        _uiEvent.emit("Exported successfully to Downloads folder.")
                    } else {
                        _uiEvent.emit("Failed to create file in Downloads.")
                    }
                } else {
                    // Pre-Android 10 fallback
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = java.io.File(downloadsDir, "powertools_call_rules_${System.currentTimeMillis()}.json")
                    file.writeText(json)
                    _uiEvent.emit("Exported successfully to Downloads folder.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.emit("Failed to export rules: ${e.localizedMessage}")
            }
        }
    }

    fun importRules(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
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
                e.printStackTrace()
                _uiEvent.emit("Failed to import. Invalid file format.")
            }
        }
    }

    // --- Call Log ---
    fun fetchRecentCalls() {
        val context = getApplication<Application>()

        if (!PermissionChecker.hasCallLogAccess(context)) {
            _recentCalls.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val logs = mutableListOf<CallLogEntry>()
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
            )

            try {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->
                    val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                    while (cursor.moveToNext() && logs.size < 50) {
                        logs.add(
                            CallLogEntry(
                                number = cursor.getString(numberIndex) ?: "Unknown",
                                name = cursor.getString(nameIndex),
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
                _recentCalls.value = logs
            }
        }
    }
}