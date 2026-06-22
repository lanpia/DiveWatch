package com.example.dive.export

import android.content.Context
import com.example.dive.model.DiveSession
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 워치 → 폰(컴패니언) 으로 **원본 세션 JSON만** 전송한다.
 * 리포트(HTML) 생성은 폰이 담당한다. 성공 시 수신 노드(폰) 이름을 반환.
 */
object WearReportSender {
    private const val CAPABILITY = "dive_report_receiver"
    private const val PATH = "/dive-report"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendReport(context: Context, sessions: List<DiveSession>): String =
        withContext(Dispatchers.IO) {
            val jsonBytes = json.encodeToString(sessions).toByteArray(Charsets.UTF_8)

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
            output.use { it.write(jsonBytes) }
            node.displayName
        }
}
