package com.example.dive.mobile

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Drive 업로드(REST 멀티파트). 무거운 Drive Java 클라이언트 대신
 * OAuth 액세스 토큰 + HttpURLConnection 으로 직접 업로드한다.
 *
 * ⚠ 사전 설정 필요: Google Cloud OAuth 클라이언트 ID(패키지 com.example.dive + SHA-1) 등록,
 * Drive API 활성화, OAuth 동의 화면에 drive.file 범위/테스트 사용자 추가.
 */
object DriveUploader {

    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
    private const val UPLOAD_URL =
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id"

    /** 업로드 후 공유 보기 링크를 반환한다. */
    fun upload(context: Context, account: Account, html: String, fileName: String): String {
        val token = GoogleAuthUtil.getToken(context, account, SCOPE)
        val boundary = "diveBoundary${System.nanoTime()}"
        val metadata = """{"name":"$fileName","mimeType":"text/html"}"""

        val conn = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { w ->
            w.append("--$boundary\r\n")
            w.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            w.append(metadata).append("\r\n")
            w.append("--$boundary\r\n")
            w.append("Content-Type: text/html; charset=UTF-8\r\n\r\n")
            w.append(html).append("\r\n")
            w.append("--$boundary--\r\n")
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("Drive 업로드 실패($code): $body")

        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: throw IOException("업로드 응답에서 파일 ID를 찾지 못함")
        return "https://drive.google.com/file/d/$id/view"
    }
}
