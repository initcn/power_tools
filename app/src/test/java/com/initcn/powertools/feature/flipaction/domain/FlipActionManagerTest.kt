package com.initcn.powertools.feature.flipaction.domain

import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import com.initcn.powertools.feature.flipaction.data.FlipActionPrefs
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.clearInvocations // Add this to your imports at the top!

class FlipActionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: FlipActionPrefs
    private lateinit var mockSensorManager: SensorManager
    private lateinit var mockAudioManager: AudioManager
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockSensor: Sensor

    private lateinit var manager: FlipActionManager

    @Before
    fun setup() {
        // Using mockito-kotlin's elegant mock() syntax
        mockContext = mock()
        mockPrefs = mock()
        mockSensorManager = mock()
        mockAudioManager = mock()
        mockNotificationManager = mock()
        mockSensor = mock()

        // Wire up the system services using 'whenever' instead of 'when'
        whenever(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager)
        whenever(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager)

        // Mock the wake-up gravity sensor
        whenever(mockSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY, true)).thenReturn(mockSensor)

        // Initialize the manager
        manager = FlipActionManager(mockContext, mockPrefs)
    }

    /**
     * Helper function to forcefully create a fake Android SensorEvent using Reflection.
     * Z-Axis is values[2].
     */
    private fun createFakeGravityEvent(zAxis: Float): SensorEvent {
        val event = mock<SensorEvent>()
        val valuesField = SensorEvent::class.java.getField("values")
        valuesField.isAccessible = true
        valuesField.set(event, floatArrayOf(0f, 0f, zAxis))
        return event
    }

    @Test
    fun sensor_is_NOT_registered_when_feature_is_disabled() {
        whenever(mockPrefs.isFeatureEnabled()).thenReturn(false)

        manager.startListening()

        // FIX: Explicitly tell Kotlin what types any() is matching
        verify(mockSensorManager, never()).registerListener(
            any<SensorEventListener>(),
            any<Sensor>(),
            any<Int>(),
            any<Int>()
        )
    }

    @Test
    fun face_down_applies_Silence_when_configured() {
        // Arrange
        whenever(mockPrefs.isFeatureEnabled()).thenReturn(true)
        whenever(mockPrefs.getSelectedMode()).thenReturn(FlipMode.SILENCE)
        whenever(mockAudioManager.ringerMode).thenReturn(AudioManager.RINGER_MODE_NORMAL)

        // Act: Simulate phone being placed face down (Gravity Z is roughly -9.8)
        val faceDownEvent = createFakeGravityEvent(-9.8f)
        manager.onSensorChanged(faceDownEvent)

        // Assert
        verify(mockPrefs).savePreviousRingerMode(AudioManager.RINGER_MODE_NORMAL)
        verify(mockPrefs).setDidAppMakeChange(true)
        verify(mockAudioManager).ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    @Test
    fun face_up_restores_previous_ringer_mode_if_app_made_the_change() {
        // Arrange
        whenever(mockPrefs.isFeatureEnabled()).thenReturn(true)
        whenever(mockPrefs.didAppMakeChange()).thenReturn(true)
        whenever(mockPrefs.getPreviousRingerMode()).thenReturn(AudioManager.RINGER_MODE_NORMAL)

        // FIX: Add this line so Mockito doesn't return null when applying the face-down action!
        whenever(mockPrefs.getSelectedMode()).thenReturn(FlipMode.SILENCE)

        // First, trick the manager into thinking it's already face down
        val faceDownEvent = createFakeGravityEvent(-9.8f)
        manager.onSensorChanged(faceDownEvent)

        // Act: Simulate picking the phone back up (Gravity Z is roughly 9.8)
        val faceUpEvent = createFakeGravityEvent(9.8f)
        manager.onSensorChanged(faceUpEvent)

        // Assert
        verify(mockAudioManager).ringerMode = AudioManager.RINGER_MODE_NORMAL
        verify(mockPrefs).setDidAppMakeChange(false)
    }



    @Test
    fun face_up_does_nothing_if_the_app_did_NOT_make_the_change() {
        // Arrange
        whenever(mockPrefs.isFeatureEnabled()).thenReturn(true)

        // The user manually silenced their phone, not the app
        whenever(mockPrefs.didAppMakeChange()).thenReturn(false)
        whenever(mockPrefs.getSelectedMode()).thenReturn(FlipMode.SILENCE)

        // 1. Simulate phone face down (This triggers applyAction, which touches the audio manager)
        val faceDownEvent = createFakeGravityEvent(-9.8f)
        manager.onSensorChanged(faceDownEvent)

        // ---> FIX: Wipe Mockito's memory of the face-down action so we only test the undo action! <---
        clearInvocations(mockAudioManager)

        // Act: Pick phone up (This triggers undoAction)
        val faceUpEvent = createFakeGravityEvent(9.8f)
        manager.onSensorChanged(faceUpEvent)

        // Assert: Check that the audio manager was NOT touched during the undo process
        verify(mockAudioManager, never()).ringerMode = any()
    }
}