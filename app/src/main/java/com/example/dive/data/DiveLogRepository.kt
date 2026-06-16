package com.example.dive.data

import android.content.Context
import com.example.dive.model.DiveSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 다이브 세션을 기기 내부 저장소의 JSON 파일에 영구 저장한다.
 * (프로토타입 단계용 단순 저장소 — 추후 Room 등으로 교체 가능)
 */
class DiveLogRepository(context: Context) {

    private val file = File(context.filesDir, "dive_sessions.json")
    private val json = Json { ignoreUnknownKeys = true }

    /** 최신순으로 정렬된 전체 세션 */
    fun loadAll(): List<DiveSession> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<DiveSession>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 새 세션을 추가(최신이 앞) */
    fun save(session: DiveSession) {
        val all = loadAll().toMutableList()
        all.add(0, session)
        write(all)
    }

    fun delete(id: Long) = write(loadAll().filterNot { it.id == id })

    fun deleteAll() = write(emptyList())

    private fun write(sessions: List<DiveSession>) {
        try {
            file.writeText(json.encodeToString(sessions))
        } catch (e: Exception) {
            // 저장 실패는 일단 무시(추후 사용자 알림 추가)
        }
    }
}
