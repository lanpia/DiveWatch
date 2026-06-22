package com.example.dive.mobile

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google Drive 업로드. 전용 폴더(folderName) 안에서 관리하며,
 * 같은 이름의 기존 파일이 있으면 삭제 후 새로 올린다(중복 방지).
 *
 * 매 업로드가 전체 스냅샷이므로 폴더엔 항상 최신 1벌(report+data)만 유지된다.
 */
object DriveUploader {

    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
    private const val FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"

    fun upload(
        context: Context,
        account: Account,
        content: ByteArray,
        fileName: String,
        mimeType: String,
        folderName: String
    ): String {
        val token = GoogleAuthUtil.getToken(context, account, SCOPE)
        val folderId = ensureFolder(token, folderName)
        findFileId(token, folderId, fileName)?.let { deleteFile(token, it) }
        return createInFolder(token, folderId, fileName, content, mimeType)
    }

    private fun ensureFolder(token: String, name: String): String {
        val q = "mimeType='$FOLDER_MIME' and name='${escapeQ(name)}' and trashed=false"
        queryFirstId(token, q)?.let { return it }

        val conn = open("$FILES?fields=id", "POST", token)
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true
        conn.outputStream.use {
            it.write("{\"name\":\"$name\",\"mimeType\":\"$FOLDER_MIME\"}".toByteArray(Charsets.UTF_8))
        }
        return extractId(readResponse(conn)) ?: throw IOException("폴더 생성 실패")
    }

    private fun findFileId(token: String, folderId: String, fileName: String): String? {
        val q = "name='${escapeQ(fileName)}' and '$folderId' in parents and trashed=false"
        return queryFirstId(token, q)
    }

    private fun queryFirstId(token: String, q: String): String? {
        val url = "$FILES?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id)&spaces=drive&pageSize=1"
        return extractId(readResponse(open(url, "GET", token)))
    }

    private fun deleteFile(token: String, id: String) {
        val conn = open("$FILES/$id", "DELETE", token)
        try {
            conn.responseCode
        } catch (e: Exception) {
            // 삭제 실패는 무시(다음 업로드가 새 파일을 만듦)
        } finally {
            conn.disconnect()
        }
    }

    private fun createInFolder(
        token: String,
        folderId: String,
        fileName: String,
        content: ByteArray,
        mimeType: String
    ): String {
        val boundary = "diveB${System.nanoTime()}"
        val conn = open(UPLOAD, "POST", token)
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.outputStream.use { os ->
            val head = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append("{\"name\":\"$fileName\",\"mimeType\":\"$mimeType\",\"parents\":[\"$folderId\"]}\r\n")
                append("--$boundary\r\n")
                append("Content-Type: $mimeType\r\n\r\n")
            }
            os.write(head.toByteArray(Charsets.UTF_8))
            os.write(content)
            os.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }
        val id = extractId(readResponse(conn)) ?: throw IOException("업로드 실패")
        return "https://drive.google.com/file/d/$id/view"
    }

    private fun open(url: String, method: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("Drive 요청 실패($code): $body")
        return body
    }

    private fun extractId(json: String): String? =
        Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)

    private fun escapeQ(s: String): String = s.replace("\\", "\\\\").replace("'", "\\'")
}
