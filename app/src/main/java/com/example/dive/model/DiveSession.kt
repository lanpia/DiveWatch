package com.example.dive.model

import kotlinx.serialization.Serializable

/**
 * 다이브 화면 1회 방문 = 1 세션.
 * - 프리다이빙: 여러 번의 다이브(dives)를 담는다.
 * - 스쿠버: 보통 다이브 1개.
 */
@Serializable
data class DiveSession(
    val id: Long,                 // 세션 시작 시각(epoch millis)
    val mode: DiveMode,
    val startTime: Long,
    val endTime: Long,
    val dives: List<DiveLog>
) {
    val diveCount: Int get() = dives.size
    val maxDepth: Float get() = dives.maxOfOrNull { it.maxDepth } ?: 0f
    val totalDiveSec: Long get() = dives.sumOf { it.durationSec }
}
