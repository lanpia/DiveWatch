package com.example.dive.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.dive.model.DiveSession
import java.io.File

/**
 * HTML 리포트를 캐시에 생성하고 공유 시트(ACTION_SEND)로 내보낸다.
 * 사용자는 Google Drive·메일 등 설치된 대상으로 보낼 수 있다.
 */
object ReportExporter {

    fun exportAndShare(context: Context, sessions: List<DiveSession>, nowMs: Long) {
        try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(dir, "dive_report.html")
            file.writeText(HtmlReport.build(sessions, nowMs))

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "다이브 리포트 공유")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "리포트 공유 실패", Toast.LENGTH_SHORT).show()
        }
    }
}
