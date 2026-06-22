package com.example.dive.model

import kotlinx.serialization.Serializable

/** 다이브 화면 1회 방문 = 1 세션 (워치 ↔ 폰 공통 데이터 모델) */
@Serializable
data class DiveSession(
    val id: Long,
    val mode: DiveMode,
    val startTime: Long,
    val endTime: Long,
    val dives: List<DiveLog>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null
) {
    val diveCount: Int get() = dives.size
    val maxDepth: Float get() = dives.maxOfOrNull { it.maxDepth } ?: 0f
    val totalDiveSec: Long get() = dives.sumOf { it.durationSec }
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
