package com.example.dive.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.wearable.Wearable
import java.io.File

/**
 * 컴패니언 화면.
 * - 위: 받은 리포트(WebView, 인앱 표시)
 * - 아래: '워치에서 리포트 가져오기' → 'Google 로그인 / 계정변경(로그인된 계정 표시)'
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var loginBtn: Button
    private lateinit var signInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true // 리포트 탭 전환용
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url
                    if (url.scheme == "http" || url.scheme == "https") {
                        try {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, url)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            // 열 수 있는 앱 없음
                        }
                        return true
                    }
                    return false
                }
            }
        }
        val fetchBtn = Button(this).apply {
            text = "⬇ 워치에서 리포트 가져오기"
            setOnClickListener { requestReportFromWatch() }
        }
        loginBtn = Button(this).apply {
            setOnClickListener { signIn() }
        }

        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(fetchBtn, wrapParams())
        root.addView(loginBtn, wrapParams())
        setContentView(root)

        // 상태바/내비게이션바(엣지투엣지)에 가리지 않도록 인셋만큼 여백
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(16 + bars.left, 8 + bars.top, 16 + bars.right, 12 + bars.bottom)
            insets
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_FILE_SCOPE))
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)

        updateLoginButton()
        loadReport()
        requestNotificationPermissionIfNeeded()
    }

    private fun wrapParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun signIn() = startActivityForResult(signInClient.signInIntent, RC_SIGN_IN)

    private fun updateLoginButton() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        loginBtn.text = if (account != null) {
            "계정: ${account.email}  (변경)"
        } else {
            "Google 로그인 (Drive)"
        }
    }

    private fun loadReport() {
        val file = File(File(cacheDir, "reports"), "received_report.html")
        val html = if (file.exists()) file.readText() else placeholderHtml()
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun requestReportFromWatch() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Toast.makeText(this, "연결된 워치가 없습니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val mc = Wearable.getMessageClient(this)
                nodes.forEach { mc.sendMessage(it.id, REQUEST_PATH, ByteArray(0)) }
                Toast.makeText(this, "워치에 리포트를 요청했습니다…", Toast.LENGTH_SHORT).show()
                webView.postDelayed({ loadReport() }, 3000)
                webView.postDelayed({ loadReport() }, 6000)
            }
            .addOnFailureListener {
                Toast.makeText(this, "요청 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        updateLoginButton()
        loadReport()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            } catch (e: ApiException) {
                // 로그인 실패(예: OAuth 클라이언트 미설정)
            }
            updateLoginButton()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun placeholderHtml(): String = """
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>body{font-family:sans-serif;background:#0b1f2a;color:#9bbccb;
        display:flex;align-items:center;justify-content:center;min-height:80vh;
        text-align:center;padding:24px;line-height:1.6;}</style></head>
        <body><div>🤿 아직 받은 리포트가 없습니다.<br><br>
        아래 <b>'워치에서 리포트 가져오기'</b>를 누르거나<br>
        워치에서 <b>'폰으로 로그 공유'</b>를 누르세요.</div></body></html>
    """.trimIndent()

    private companion object {
        const val RC_SIGN_IN = 1001
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val REQUEST_PATH = "/request-report"
    }
}
