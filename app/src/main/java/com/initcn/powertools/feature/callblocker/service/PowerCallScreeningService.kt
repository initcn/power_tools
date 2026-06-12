package com.initcn.powertools.feature.callblocker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.initcn.powertools.feature.callblocker.data.CallBlockerPrefs
import com.initcn.powertools.feature.callblocker.data.CallRuleDao
import com.initcn.powertools.feature.callblocker.domain.CallEvaluator
import com.initcn.powertools.feature.callblocker.domain.CallResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PowerCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var dao: CallRuleDao

    @Inject
    lateinit var prefs: CallBlockerPrefs

    // Instantiate your pure, tested domain logic
    private val evaluator = CallEvaluator()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val isHidden = phoneNumber.isBlank()

        // ALWAYS verify the permission exists at the exact moment a call comes in
        val hasContactsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        serviceScope.launch {
            // Only try to check the contact database if the permission is currently granted
            val isSaved = if (!isHidden && hasContactsPermission) isContactSaved(phoneNumber) else false

            // Capture the specific CallResult object
            val result = evaluator.evaluateCall(
                incomingNumber = phoneNumber,
                isSavedContact = isSaved,
                isHiddenNumber = isHidden,
                whitelist = dao.getWhitelistSync(),
                exactBlocklist = dao.getExactBlocklistSync(),
                regexBlocklist = dao.getRegexBlocklistSync(),
                blockHiddenPref = prefs.isBlockHiddenEnabled(),
                // FAILSAFE: Force the setting to false if the permission was revoked
                blockUnsavedPref = prefs.isBlockUnsavedEnabled() && hasContactsPermission
            )

            // Use Kotlin's 'when' statement to cleanly handle the result
            when (result) {
                is CallResult.Block -> blockCall(callDetails, result.reason) // Passes the exact reason!
                is CallResult.Allow -> allowCall(callDetails)
            }
        }
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun blockCall(callDetails: Call.Details, reason: String) {
        val disallow = prefs.isDisallowEnabled()
        val reject = prefs.isRejectEnabled()
        val skipNotif = prefs.isSkipNotifEnabled()
        val silence = prefs.isSilenceEnabled()

        val responseBuilder = CallResponse.Builder()

        // Determine the dynamic action string for the notification
        var actionTaken = "Blocked"

        if (disallow) {
            responseBuilder.setDisallowCall(true)
            responseBuilder.setRejectCall(reject)
            responseBuilder.setSkipNotification(skipNotif)
            responseBuilder.setSkipCallLog(false)

            actionTaken = if (reject) "Rejected" else "Disallowed"
        } else if (silence) {
            responseBuilder.setDisallowCall(false)
            responseBuilder.setSilenceCall(true)

            actionTaken = "Silenced"
        }

        respondToCall(callDetails, responseBuilder.build())

        if (!skipNotif) {
            val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown Number"
            showBlockedCallNotification(phoneNumber, reason, actionTaken)
        }
    }

    private fun showBlockedCallNotification(phoneNumber: String, reason: String, actionTaken: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "powertools_call_blocker"

        val channel = NotificationChannel(
            channelId,
            "Call Interceptions",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for calls blocked or silenced by PowerTools"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            // Note: Change this icon to your app's actual drawable icon if necessary!
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Call $actionTaken") // e.g., "Call Silenced" or "Call Disallowed"
            .setContentText("$actionTaken $phoneNumber ($reason)") // e.g., "Silenced 1234567890 (Exact Match)"
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Use a unique ID based on the phone number so multiple blocked calls show up separately
        notificationManager.notify(phoneNumber.hashCode(), notification)
    }

    private fun isContactSaved(phoneNumber: String): Boolean {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                return cursor.count > 0
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }
}