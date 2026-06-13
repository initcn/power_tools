package com.initcn.powertools.feature.flipaction.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.initcn.powertools.R
import com.initcn.powertools.feature.flipaction.data.FlipActionPrefs
import com.initcn.powertools.feature.flipaction.domain.FlipActionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class FlipSensorService : Service() {

    @Inject lateinit var manager: FlipActionManager
    @Inject lateinit var prefs: FlipActionPrefs // <-- Injected the preferences

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    // Create a scope tied to the Service lifecycle
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "flip_action_channel"
        const val SERVICE_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FlipSensorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FlipSensorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Start foreground with the default monitoring state
        startForeground(SERVICE_ID, buildNotification(isActive = false))

        manager.startListening()

        // OBSERVE MANAGER STATE: Swap the status bar icon instantly on flip
        serviceScope.launch {
            manager.isActionActive.collectLatest { isActive ->
                notificationManager.notify(SERVICE_ID, buildNotification(isActive))
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel() // Prevent memory leaks when the service is stopped
        manager.stopListening()
        super.onDestroy()
    }

    private fun buildNotification(isActive: Boolean): Notification {
        // Read the actual selected mode from preferences
        val currentMode = prefs.getSelectedMode()

        // Dynamically change text based on state and selected mode
        val title = if (isActive) {
            when (currentMode) {
                FlipActionManager.FlipMode.SILENCE -> getString(R.string.notif_title_silenced)
                FlipActionManager.FlipMode.VIBRATE -> getString(R.string.notif_title_vibrate)
                FlipActionManager.FlipMode.DND -> getString(R.string.notif_title_dnd)
            }
        } else {
            getString(R.string.notif_title_monitoring)
        }

        val text = if (isActive) {
            when (currentMode) {
                FlipActionManager.FlipMode.SILENCE -> getString(R.string.notif_desc_silenced)
                FlipActionManager.FlipMode.VIBRATE -> getString(R.string.notif_desc_vibrate)
                FlipActionManager.FlipMode.DND -> getString(R.string.notif_desc_dnd)
            }
        } else {
            getString(R.string.notif_desc_monitoring)
        }

        val iconRes = R.drawable.ic_notification

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(iconRes)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, this::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Flip Sensor", NotificationManager.IMPORTANCE_MIN
        )
        // Set sound to null so the user doesn't get an audio ping when we silently update the notification icon
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null
}