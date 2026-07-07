package com.mornshield.wear.sleep

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

enum class SleepStage {
    DEEP, REM, AWAKE
}

/**
 * MornShield Sleep Monitor
 * Uses a combination of heart rate variability and motion sensing to estimate sleep stages.
 * Focuses on detecting the transition to REM for the "Gentle Wake" feature.
 */
class SleepMonitor(
    context: Context, 
    private val onStageChanged: (SleepStage) -> Unit
) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var lastHeartRate = 0f
    private var movementSum = 0f
    private var currentStage: SleepStage? = null

    fun start() {
        Log.d("SleepMonitor", "MornShield Sleep Tracking Started")
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_HEART_RATE -> {
                lastHeartRate = event.values[0]
                analyzeSleepStage()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                movementSum = kotlin.math.abs(x) + kotlin.math.abs(y) + kotlin.math.abs(z)
            }
        }
    }

    private fun analyzeSleepStage() {
        val nextStage = if (movementSum > 15f) {
            SleepStage.AWAKE
        } else if (lastHeartRate > 70 && movementSum < 10f) {
            SleepStage.REM
        } else {
            SleepStage.DEEP
        }

        if (nextStage != currentStage) {
            currentStage = nextStage
            onStageChanged(nextStage)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
