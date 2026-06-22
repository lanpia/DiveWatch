package com.example.dive.wear

import com.example.dive.data.DiveLogRepository
import com.example.dive.export.WearReportSender
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * 폰의 '워치에서 리포트 가져오기' 요청을 받으면, 저장된 세션으로 리포트를 만들어 폰에 푸시한다.
 */
class ReportRequestService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PATH) return
        try {
            val sessions = DiveLogRepository(this).loadAll()
            runBlocking {
                WearReportSender.sendReport(this@ReportRequestService, sessions)
            }
        } catch (e: Exception) {
            // 요청 처리 실패(연결 끊김 등)
        }
    }

    companion object {
        const val PATH = "/request-report"
    }
}
