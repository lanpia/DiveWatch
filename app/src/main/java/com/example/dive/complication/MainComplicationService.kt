package com.example.dive.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.dive.MainActivity
import com.example.dive.data.DiveLogRepository

/**
 * 최근 다이브 최대 수심을 보여주는 SHORT_TEXT 컴플리케이션. 탭하면 앱을 연다.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return createComplicationData("12.4m", "최근 최대 수심")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val last = DiveLogRepository(this).loadAll().firstOrNull()
        val text = if (last == null) "--" else "%.1fm".format(last.maxDepth)
        val desc = if (last == null) "다이브 기록 없음" else "최근 최대 수심"
        return createComplicationData(text, desc)
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setTapAction(launchIntent())
            .build()

    private fun launchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
