package com.example.dive.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

/**
 * 컴패니언 런처 화면.
 * - 최초 1회 실행해야 워치 데이터 레이어 수신이 활성화된다.
 * - Google 로그인 시 수신한 리포트를 자동으로 Drive에 업로드한다.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var signInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(56, 120, 56, 56)
        }
        val title = TextView(this).apply {
            text = "🤿 Dive 컴패니언"
            textSize = 20f
        }
        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 40, 0, 40)
            setLineSpacing(10f, 1f)
        }
        val signInBtn = Button(this).apply {
            text = "Google 로그인 (Drive)"
            setOnClickListener { startActivityForResult(signInClient.signInIntent, RC_SIGN_IN) }
        }
        root.addView(title)
        root.addView(status)
        root.addView(signInBtn)
        setContentView(root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_FILE_SCOPE))
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)

        updateStatus()
        requestNotificationPermissionIfNeeded()
    }

    private fun updateStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        status.text = if (account == null) {
            "워치에서 '폰으로 보내기'를 누르면 리포트가 이 폰으로 전송됩니다.\n\n" +
                "Google 로그인하면 수신 시 자동으로 Drive에 업로드됩니다.\n" +
                "(미로그인 시: 알림에서 보기/공유 선택)"
        } else {
            "로그인됨: ${account.email}\n\n" +
                "워치에서 '폰으로 보내기'를 누르면\n자동으로 Drive에 업로드됩니다."
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            } catch (e: ApiException) {
                // 로그인 실패(예: OAuth 클라이언트 미설정) — 상태만 갱신
            }
            updateStatus()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    companion object {
        private const val RC_SIGN_IN = 1001
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
