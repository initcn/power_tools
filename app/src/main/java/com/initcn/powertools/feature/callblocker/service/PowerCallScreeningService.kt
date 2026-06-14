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

    private val evaluator = CallEvaluator()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
            allowCall(callDetails)
            return
        }

        if (callDetails.hasProperty(Call.Details.PROPERTY_SELF_MANAGED) || callDetails.handle?.scheme != "tel") {
            allowCall(callDetails)
            return
        }
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val isHidden = phoneNumber.isBlank()

        val hasContactsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        serviceScope.launch {
            // Fetch the actual contact name instead of just a true/false boolean
            val contactName = if (!isHidden && hasContactsPermission) getContactName(phoneNumber) else null
            val isSaved = contactName != null

            val result = evaluator.evaluateCall(
                incomingNumber = phoneNumber,
                isSavedContact = isSaved,
                isHiddenNumber = isHidden,
                whitelist = dao.getWhitelistSync(),
                exactBlocklist = dao.getExactBlocklistSync(),
                regexBlocklist = dao.getRegexBlocklistSync(),
                blockHiddenPref = prefs.isBlockHiddenEnabled(),
                blockUnsavedPref = prefs.isBlockUnsavedEnabled() && hasContactsPermission,
                blockAllPref = prefs.isBlockAllEnabled()
            )

            when (result) {
                // Pass the contactName down to the blockCall method
                is CallResult.Block -> blockCall(callDetails, result.reason, contactName)
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

    private fun blockCall(callDetails: Call.Details, reason: String, contactName: String?) {
        val disallow = prefs.isDisallowEnabled()
        val reject = prefs.isRejectEnabled()
        val skipNotif = prefs.isSkipNotifEnabled()
        val silence = prefs.isSilenceEnabled()

        val responseBuilder = CallResponse.Builder()
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
            // Use the contact name if available, otherwise fallback to the raw phone number
            val displayName = contactName ?: phoneNumber
            showBlockedCallNotification(phoneNumber, displayName, reason, actionTaken)
        }
    }

    private fun showBlockedCallNotification(
        phoneNumber: String,
        displayName: String,
        reason: String,
        actionTaken: String
    ) {
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
            .setSmallIcon(com.initcn.powertools.R.drawable.ic_notification)
            .setContentTitle("Call $actionTaken")
            // Display the resolved name in the notification body
            .setContentText("$actionTaken $displayName ($reason)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // We still use the raw phoneNumber for the ID so multiple calls from the same number stack cleanly
        notificationManager.notify(phoneNumber.hashCode(), notification)
    }

    // Swapped isContactSaved for getContactName to retrieve the actual string
    private fun getContactName(phoneNumber: String): String? {
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
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }
}