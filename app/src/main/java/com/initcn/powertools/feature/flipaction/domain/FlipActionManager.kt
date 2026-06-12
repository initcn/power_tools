package com.initcn.powertools.feature.flipaction.domain

import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import com.initcn.powertools.feature.flipaction.data.FlipActionPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlipActionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: FlipActionPrefs
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Explicitly request the wake-up version of the gravity sensor to bypass Doze mode safely
    private val gravitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private var isFaceDown = false
    private var lastUiUpdateTime = 0L

    // Coroutine scope dedicated strictly to background OS calls
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mutex to prevent race conditions if the phone is flipped rapidly
    private val actionMutex = Mutex()

    // State flow to tell the Foreground Service when to update the status bar icon
    private val _isActionActive = MutableStateFlow(false)
    val isActionActive: StateFlow<Boolean> = _isActionActive.asStateFlow()

    // Data class to prevent array allocation overhead
    data class SensorData(val x: Float, val y: Float, val z: Float)
    private val _sensorValues = MutableStateFlow(SensorData(0f, 0f, 0f))
    val sensorValues: StateFlow<SensorData> = _sensorValues.asStateFlow()

    fun startListening() {
        if (!prefs.isFeatureEnabled() || gravitySensor == null) return

        // 300ms polling interval | 3-second hardware FIFO batching
        // This is the absolute most battery-efficient registration possible.
        sensorManager.registerListener(
            this,
            gravitySensor,
            300_000,
            3_000_000
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        if (isFaceDown) {
            ioScope.launch {
                actionMutex.withLock { undoAction() }
            }
            isFaceDown = false
        }
        _sensorValues.value = SensorData(0f, 0f, 0f)
        _isActionActive.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!prefs.isFeatureEnabled() || event == null) return

        val zAxis = event.values[2]

        // UI Update Throttling: Only allocate a new state object twice a second
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUiUpdateTime > 500) {
            _sensorValues.value = SensorData(event.values[0], event.values[1], zAxis)
            lastUiUpdateTime = currentTime
        }

        // --- THE DEADZONE LOGIC ---
        if (!isFaceDown && zAxis < -6.0f) {
            isFaceDown = true
            // Offload the heavy OS modifications to the IO thread with a Mutex lock
            ioScope.launch {
                actionMutex.withLock { applyAction() }
            }
        } else if (isFaceDown && zAxis > 2.0f) {
            isFaceDown = false
            ioScope.launch {
                actionMutex.withLock { undoAction() }
            }
        }
    }

    private fun applyAction() {
        val currentMode = prefs.getSelectedMode()

        // Clean, standard state saving
        if (currentMode == FlipMode.SILENCE || currentMode == FlipMode.VIBRATE) {
            prefs.savePreviousRingerMode(audioManager.ringerMode)
        } else if (currentMode == FlipMode.DND) {
            prefs.savePreviousDndState(notificationManager.currentInterruptionFilter)
        }

        prefs.setDidAppMakeChange(true)
        _isActionActive.value = true

        // Standard Android APIs
        when (currentMode) {
            FlipMode.SILENCE -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            FlipMode.VIBRATE -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            FlipMode.DND -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
            }
        }
    }

    private fun undoAction() {
        if (!prefs.didAppMakeChange()) return

        val currentMode = prefs.getSelectedMode()

        _isActionActive.value = false

        // Standard Android state restoration
        when (currentMode) {
            FlipMode.SILENCE, FlipMode.VIBRATE -> {
                val previousRinger = prefs.getPreviousRingerMode()
                if (previousRinger != -1) audioManager.ringerMode = previousRinger
            }
            FlipMode.DND -> {
                val previousDnd = prefs.getPreviousDndState()
                if (previousDnd != -1 && notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(previousDnd)
                }
            }
        }

        prefs.setDidAppMakeChange(false)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Unused but required by interface
    }

    // --- MERGED DOMAIN MODEL ---

    enum class FlipMode(val title: String, val description: String) {
        SILENCE("Silence Ringer", "Mutes incoming calls and notifications."),
        VIBRATE("Vibrate Only", "Sets the phone to vibrate for calls and alerts."),
        DND("Do Not Disturb", "Blocks all visual and audio interruptions.")
    }
}