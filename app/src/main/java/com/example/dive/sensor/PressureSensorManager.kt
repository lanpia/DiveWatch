package com.example.dive.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.dive.model.WaterType

class PressureSensorManager(
    context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private var seaLevelPressure: Float = 1013.25f
    private var pendingCalibration = false

    var isCalibrated = false
        private set

    var lastPressure: Float = 0f
        private set

    /** 수질(담수/해수) — 수심 환산 계수에 사용 */
    var waterType: WaterType = WaterType.SEA

    /** 수심(m)이 갱신될 때마다 호출 */
    var onDepthChanged: (Float) -> Unit = {}

    fun start() {
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /** 다음에 들어오는 센서 값을 수면(0 m) 기준으로 보정한다. */
    fun requestCalibration() {
        pendingCalibration = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val pressure = event?.values?.firstOrNull() ?: return
        lastPressure = pressure

        if (pendingCalibration) {
            seaLevelPressure = pressure
            isCalibrated = true
            pendingCalibration = false
        }

        if (isCalibrated) {
            val depth = ((pressure - seaLevelPressure) * waterType.metersPerHpa).coerceAtLeast(0f)
            onDepthChanged(depth)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister() {
        sensorManager.unregisterListener(this)
    }
}
