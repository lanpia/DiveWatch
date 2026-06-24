package com.example.dive.model

import kotlinx.serialization.Serializable

/** 다이빙 모드 */
@Serializable
enum class DiveMode {
    FREEDIVING,   // 프리다이빙: 짧은 다이브 반복
    SCUBA         // 스쿠버: 연속 1회 다이브
}

/** 수심 프로파일 1포인트 */
@Serializable
data class DiveSample(
    val timeSec: Long,   // 다이브 시작 후 경과 시간(초)
    val depth: Float     // 수심(m)
)

/**
 * 체온(피부온도) 측정 1포인트. 세션 동안 1분마다 측정한다.
 * - timeSec: 세션 시작 후 경과 시간(초)
 * - skin: 손목 피부온도(℃), ambient: 주변온도(℃)
 */
@Serializable
data class TempReading(
    val timeSec: Long,
    val skin: Float,
    val ambient: Float
)

/** 완료된 다이브 1회 기록 */
@Serializable
data class DiveLog(
    val id: Long,                       // 시작 시각(epoch millis)을 식별자로 사용
    val mode: DiveMode,
    val startTime: Long,                // epoch millis
    val durationSec: Long,
    val maxDepth: Float,                // m
    val avgDepth: Float,                // m
    val samples: List<DiveSample> = emptyList()
)
