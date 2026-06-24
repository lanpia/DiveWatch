package com.example.dive.model

import kotlinx.serialization.Serializable

/** 다이빙 모드 */
@Serializable
enum class DiveMode {
    FREEDIVING,
    SCUBA
}

/** 수심 프로파일 1포인트 */
@Serializable
data class DiveSample(
    val timeSec: Long,
    val depth: Float
)

/** 체온(피부온도) 측정 1포인트. 세션 시작 후 경과 timeSec(초), skin/ambient(℃). */
@Serializable
data class TempReading(
    val timeSec: Long,
    val skin: Float,
    val ambient: Float
)

/** 완료된 다이브 1회 기록 */
@Serializable
data class DiveLog(
    val id: Long,
    val mode: DiveMode,
    val startTime: Long,
    val durationSec: Long,
    val maxDepth: Float,
    val avgDepth: Float,
    val samples: List<DiveSample> = emptyList()
)
