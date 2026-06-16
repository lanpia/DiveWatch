package com.example.dive.deco

import kotlin.math.ln
import kotlin.math.pow

/**
 * Bühlmann ZHL-16C 기반 질소 무감압 한계(NDL) 추정 모델.
 *
 * ⚠ 참고용 근사치입니다. 실제 다이브 플래닝/감압 계산에 사용하지 마세요.
 * 공기(N2 79%) 기준, 단일 불활성 기체(질소)만 고려합니다.
 */
class DecoModel(private val surfacePressure: Float = 1.0f) {

    // ZHL-16C N2 반감기(분) / a, b 계수
    private val halfTimes = floatArrayOf(
        4.0f, 8.0f, 12.5f, 18.5f, 27.0f, 38.3f, 54.3f, 77.0f,
        109.0f, 146.0f, 187.0f, 239.0f, 305.0f, 390.0f, 498.0f, 635.0f
    )
    private val a = floatArrayOf(
        1.2599f, 1.0000f, 0.8618f, 0.7562f, 0.6667f, 0.5600f, 0.4947f, 0.4500f,
        0.4187f, 0.3798f, 0.3497f, 0.3223f, 0.2850f, 0.2737f, 0.2523f, 0.2327f
    )
    private val b = floatArrayOf(
        0.5050f, 0.6514f, 0.7222f, 0.7825f, 0.8126f, 0.8434f, 0.8693f, 0.8910f,
        0.9092f, 0.9222f, 0.9319f, 0.9403f, 0.9477f, 0.9544f, 0.9602f, 0.9653f
    )

    private val pH2O = 0.0627f   // 폐포 수증기압(bar)
    private val fN2 = 0.79f      // 공기 중 질소 분율
    private val ln2 = ln(2.0)

    private val tissues = FloatArray(16) { initialLoading() }

    private fun initialLoading() = (surfacePressure - pH2O) * fN2
    private fun ambient(depthM: Float) = surfacePressure + depthM / 10f
    private fun inspiredN2(depthM: Float) = (ambient(depthM) - pH2O) * fN2

    /** 수면 포화 상태로 초기화 */
    fun reset() {
        val p = initialLoading()
        for (i in tissues.indices) tissues[i] = p
    }

    /** depthM에서 dtMinutes 동안의 질소 부하 갱신(Haldane) */
    fun update(depthM: Float, dtMinutes: Float) {
        if (dtMinutes <= 0f) return
        val pInspired = inspiredN2(depthM)
        for (i in tissues.indices) {
            val k = (1.0 - 2.0.pow((-dtMinutes / halfTimes[i]).toDouble())).toFloat()
            tissues[i] += (pInspired - tissues[i]) * k
        }
    }

    /**
     * 현재 조직 부하 기준, depthM에 머무를 때의 무감압 잔여 시간(분).
     * 제한이 없으면 999를 반환.
     */
    fun ndlMinutes(depthM: Float): Float {
        val pGas = inspiredN2(depthM)
        var ndl = Float.MAX_VALUE
        for (i in tissues.indices) {
            val pLimit = surfacePressure / b[i] + a[i]   // 직상승 가능 한계 부하
            if (pGas <= pLimit) continue                  // 이 조직은 한계에 도달하지 않음
            val p0 = tissues[i]
            if (p0 >= pLimit) return 0f                   // 이미 무감압 초과
            val ratio = (pGas - pLimit) / (pGas - p0)
            if (ratio <= 0f) return 0f
            val t = (-halfTimes[i] * (ln(ratio.toDouble()) / ln2)).toFloat()
            if (t < ndl) ndl = t
        }
        return if (ndl == Float.MAX_VALUE) 999f else ndl.coerceAtLeast(0f)
    }
}
