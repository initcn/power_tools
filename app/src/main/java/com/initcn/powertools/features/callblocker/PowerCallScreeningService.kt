package com.initcn.powertools.features.callblocker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import com.initcn.powertools.data.callblocker.CallBlockerDatabase
import com.initcn.powertools.data.callblocker.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PowerCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart
        val response = CallResponse.Builder()

        if (incomingNumber.isNullOrBlank()) {
            if (CallBlockerPrefs.isBlockHiddenEnabled(applicationContext)) {
                executeInterceptBehavior(callDetails, response)
            } else {
                allowCall(callDetails, response)
            }
            return
        }

        serviceScope.launch {
            try {
                val dao = CallBlockerDatabase.getDatabase(applicationContext).callRuleDao()
                val cleanIncoming = cleanNumber(incomingNumber)

                // 1. Whitelist checking (Absolute Priority)
                val whitelisted = dao.getWhitelistSync().any { rule ->
                    cleanNumber(rule.pattern) == cleanIncoming
                }
                if (whitelisted) {
                    allowCall(callDetails, response)
                    return@launch
                }

                // 2. Block Unsaved Contacts Check
                if (CallBlockerPrefs.isBlockUnsavedEnabled(applicationContext)) {
                    val isSavedContact = isNumberInContacts(incomingNumber)
                    if (!isSavedContact) {
                        executeInterceptBehavior(callDetails, response)
                        return@launch
                    }
                }

                // 3. Exact and Regex Blocklist check
                val blocklist = dao.getBlocklistSync()
                val shouldBlock = blocklist.any { rule ->
                    when (rule.ruleType) {
                        RuleType.BLOCKLIST_EXACT -> cleanNumber(rule.pattern) == cleanIncoming
                        RuleType.BLOCKLIST_REGEX -> {
                            try {
                                Regex(rule.pattern).containsMatchIn(incomingNumber)
                            } catch (e: Exception) {
                                false
                            }
                        }
                        else -> false
                    }
                }

                if (shouldBlock) {
                    executeInterceptBehavior(callDetails, response)
                } else {
                    allowCall(callDetails, response)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                allowCall(callDetails, response)
            }
        }
    }

    /**
     * Queries Android's contacts directory to check if the number belongs to a saved contact.
     */
    private fun isNumberInContacts(number: String): Boolean {
        var isSaved = false
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.count > 0) {
                    isSaved = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isSaved
    }

    private fun executeInterceptBehavior(callDetails: Call.Details, response: CallResponse.Builder) {
        val isDisallow = CallBlockerPrefs.isDisallowEnabled(applicationContext)

        if (isDisallow) {
            response.setDisallowCall(true)
            response.setRejectCall(CallBlockerPrefs.isRejectEnabled(applicationContext))

            val skipNotif = CallBlockerPrefs.isSkipNotifEnabled(applicationContext)
            response.setSkipNotification(skipNotif)
            response.setSkipCallLog(skipNotif)

            // Natively, Android does not show "Missed Call" notifications for actively rejected calls.
            // If the user wants to be notified (!skipNotif), we must generate our own local notification.
            if (!skipNotif) {
                showBlockedCallNotification(callDetails.handle?.schemeSpecificPart)
            }

        } else {
            response.setDisallowCall(false)
            response.setSilenceCall(CallBlockerPrefs.isSilenceEnabled(applicationContext))
        }

        respondToCall(callDetails, response.build())
    }

    private fun showBlockedCallNotification(number: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "power_tools_blocked_calls"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Blocked Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for calls intercepted by PowerTools"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_missed) // Standard Android system icon
            .setContentTitle("Call Intercepted")
            .setContentText("PowerTools blocked a call from ${number ?: "Unknown"}.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Use the hash of the number as the ID so repeated blocks update the same notification
        val notificationId = number?.hashCode() ?: System.currentTimeMillis().toInt()

        // Safely check for Android 13+ Notification permissions before posting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, notification)
            }
        } else {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun allowCall(callDetails: Call.Details, response: CallResponse.Builder) {
        respondToCall(callDetails, response.build())
    }

    private fun cleanNumber(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }
}