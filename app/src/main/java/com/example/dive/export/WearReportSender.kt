package com.example.dive.export

import android.content.Context
import com.example.dive.model.DiveSession
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 워치 → 폰(컴패니언 앱) 으로 HTML 리포트를 데이터 레이어(ChannelClient)로 전송한다.
 * 성공 시 수신 노드(폰) 이름을 반환, 실패 시 예외를 던진다.
 */
object WearReportSender {
    private const val CAPABILITY = "dive_report_receiver"
    private const val PATH = "/dive-report"

    suspend fun sendReport(context: Context, sessions: List<DiveSession>, nowMs: Long): String =
        withContext(Dispatchers.IO) {
            val html = HtmlReport.build(sessions, nowMs)

            val capability = Tasks.await(
                Wearable.getCapabilityClient(context)
                    .getCapability(CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            )
            val node = capability.nodes.firstOrNull { it.isNearby }
                ?: capability.nodes.firstOrNull()
                ?: throw IllegalStateException("연결된 폰(컴패니언 앱)이 없습니다")

            val channelClient = Wearable.getChannelClient(context)
            val channel = Tasks.await(channelClient.openChannel(node.id, PATH))
            val output = Tasks.await(channelClient.getOutputStream(channel))
            output.use { it.write(html.toByteArray(Charsets.UTF_8)) }

            node.displayName
        }
}
