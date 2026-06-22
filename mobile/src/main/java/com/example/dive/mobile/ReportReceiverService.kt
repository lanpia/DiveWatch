package com.example.dive.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.dive.model.DiveSession
import com.example.dive.report.HtmlReport
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 워치가 보낸 원본 JSON(세션 목록)을 수신해 **폰에서 리포트를 생성**한다.
 * - Google 로그인 시: JSON·HTML 둘 다 Drive 폴더에 업로드(중복 삭제 후).
 * - 미로그인 시: 보기/공유 알림.
 * 받은 리포트는 항상 로컬(received_report.html)에 보관 → 폰 앱에서 표시.
 */
class ReportReceiverService : WearableListenerService() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        if (channel.path != PATH) return
        val client = Wearable.getChannelClient(this)
        try {
            val input = Tasks.await(client.getInputStream(channel))
            val jsonBytes = input.use { it.readBytes() }

            val sessions = try {
                json.decodeFromString<List<DiveSession>>(String(jsonBytes, Charsets.UTF_8))
            } catch (e: Exception) {
                emptyList()
            }
            val htmlBytes = HtmlReport.build(sessions, System.currentTimeMillis())
                .toByteArray(Charsets.UTF_8)

            val dir = File(cacheDir, "reports").apply { mkdirs() }
            val reportFile = File(dir, "received_report.html")
            reportFile.writeBytes(htmlBytes)
            File(dir, "received_data.json").writeBytes(jsonBytes)

            val account = GoogleSignIn.getLastSignedInAccount(this)?.account
            if (account != null) {
                try {
                    val link = DriveUploader.upload(
                        this, account, htmlBytes, "dive_report.html", "text/html", DRIVE_FOLDER
                    )
                    DriveUploader.upload(
                        this, account, jsonBytes, "dive_data.json", "application/json", DRIVE_FOLDER
                    )
                    notifyUploaded(link)
                    return
                } catch (e: Exception) {
                    // 업로드 실패 → 보기/공유 알림으로 폴백
                }
            }
            notifyReceived(reportFile)
        } catch (e: Exception) {
            // 수신/생성 실패
        } finally {
            client.close(channel)
        }
    }

    private fun notifyUploaded(link: String) {
        val open = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            this, 2, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notify(
            title = "Drive 업로드 완료",
            text = "리포트·원본 데이터 업로드됨 · 탭하여 Drive 열기",
            contentIntent = pending,
            action = null
        )
    }

    private fun notifyReceived(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val viewPending = PendingIntent.getActivity(
            this, 0, viewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "리포트 공유 (Drive 등)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val sharePending = PendingIntent.getActivity(
            this, 1, chooser,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        notify(
            title = "다이브 리포트 수신",
            text = "탭하여 보기 · 공유로 Drive 업로드",
            contentIntent = viewPending,
            action = Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_menu_share),
                "공유",
                sharePending
            ).build()
        )
    }

    private fun notify(
        title: String,
        text: String,
        contentIntent: PendingIntent,
        action: Notification.Action?
    ) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "다이브 리포트", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        if (action != null) builder.addAction(action)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        const val PATH = "/dive-report"
        private const val DRIVE_FOLDER = "Dive 컴패니언 로그"
        private const val CHANNEL_ID = "dive_reports"
        private const val NOTIFICATION_ID = 1001
    }
}
