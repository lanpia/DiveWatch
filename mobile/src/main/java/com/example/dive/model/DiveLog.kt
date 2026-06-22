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
