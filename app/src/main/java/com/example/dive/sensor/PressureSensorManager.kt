package com.example.dive.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class PressureSensorManager(
    context: Context,
    private val onDepthChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private var seaLevelPressure: Float = 1013.25f
    var isCalibrated = false

    fun start() {
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun calibrate(currentPressure: Float) {
        seaLevelPressure = currentPressure
        isCalibrated = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.values?.firstOrNull()?.let { pressure ->
            if (!isCalibrated) return
            val depth = ((pressure - seaLevelPressure) * 1.0197f).coerceAtLeast(0f)
            onDepthChanged(depth)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister() {
        sensorManager.unregisterListener(this)
    }
}
