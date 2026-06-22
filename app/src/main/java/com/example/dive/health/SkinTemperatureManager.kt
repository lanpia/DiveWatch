package com.example.dive.health

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey

/**
 * Samsung Health Sensor SDK 기반 피부온도(체온) 측정 매니저.
 *
 * 흐름: connect() → 연결 성공 시 capability 확인 → on-demand 트래커 준비 →
 *       measureOnce()로 1회 측정(약 30초, 손목 정지 필요) → onMeasurement 콜백.
 *
 * ⚠ 동작 조건: 갤럭시워치 + Sensor SDK + (개인용은) 삼성헬스 개발자 모드 ON.
 *   미지원/미승인 시 isAvailable=false 로 떨어진다(앱은 영향 없이 계속 동작).
 */
class SkinTemperatureManager(private val context: Context) {

    private var service: HealthTrackingService? = null
    private var tracker: HealthTracker? = null
    private val handler = Handler(Looper.getMainLooper())

    /** 측정 가능 여부(연결 후 갱신) */
    var isAvailable: Boolean = false
        private set

    /** 성공 측정 콜백: (피부온도℃, 주변온도℃) */
    var onMeasurement: (Float, Float) -> Unit = { _, _ -> }

    /** 상태/오류 메시지 콜백 */
    var onStatus: (String) -> Unit = {}

    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            val svc = service ?: return
            isAvailable = isSkinTemperatureAvailable(svc)
            if (isAvailable) {
                tracker = svc.getHealthTracker(HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND)
                onStatus("체온 센서 준비됨")
            } else {
                onStatus("피부온도 미지원 (또는 개발자 모드 OFF)")
            }
        }

        override fun onConnectionEnded() {
            Log.i(TAG, "Health service disconnected")
        }

        override fun onConnectionFailed(e: HealthTrackerException) {
            isAvailable = false
            onStatus("헬스 서비스 연결 실패: ${e.message ?: ""}")
        }
    }

    private val trackerListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(list: List<DataPoint>) {
            // on-demand: 한 번 받으면 리스너 해제
            handler.post { tracker?.unsetEventListener() }
            for (data in list) {
                val status: Int = data.getValue(ValueKey.SkinTemperatureSet.STATUS)
                if (status == STATUS_SUCCESS) {
                    val skin: Float = data.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE)
                    val ambient: Float = data.getValue(ValueKey.SkinTemperatureSet.AMBIENT_TEMPERATURE)
                    onMeasurement(skin, ambient)
                } else {
                    onStatus("측정 실패(status=$status) — 손목 정지 후 재시도")
                }
            }
        }

        override fun onFlushCompleted() {}

        override fun onError(error: HealthTracker.TrackerError) {
            onStatus("측정 오류: $error")
        }
    }

    fun connect() {
        service = HealthTrackingService(connectionListener, context)
        service?.connectService()
    }

    fun disconnect() {
        try {
            tracker?.unsetEventListener()
        } catch (e: Exception) {
            // ignore
        }
        service?.disconnectService()
        service = null
        tracker = null
    }

    /** 1회 on-demand 측정 시작 */
    fun measureOnce() {
        val t = tracker
        if (t == null) {
            onStatus("체온 트래커 미준비")
            return
        }
        handler.post { t.setEventListener(trackerListener) }
    }

    private fun isSkinTemperatureAvailable(svc: HealthTrackingService): Boolean {
        return try {
            val caps = svc.trackingCapability ?: return false
            caps.supportHealthTrackerTypes?.contains(HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND) == true
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val TAG = "SkinTemperatureManager"
        const val STATUS_SUCCESS = 0 // SkinTemperatureStatus.SUCCESSFUL_MEASUREMENT
    }
}
