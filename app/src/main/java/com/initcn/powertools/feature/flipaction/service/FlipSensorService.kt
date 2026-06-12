package com.initcn.powertools.feature.flipaction.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.initcn.powertools.feature.flipaction.domain.FlipActionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class FlipSensorService : Service() {

    @Inject lateinit var manager: FlipActionManager
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
        // Dynamically change text based on state
        val title = if (isActive) "Device Silenced" else "Flip to Action"
        val text = if (isActive) "Face-down mode is active." else "Monitoring for flip..."

        // Dynamically change icon.
        // NOTE: For production, you may want to replace these with your own custom R.drawable icons!
        val iconRes = if (isActive) {
            android.R.drawable.ic_lock_silent_mode_off // System Mute Icon
        } else {
            android.R.drawable.ic_dialog_info // System Info Icon
        }

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