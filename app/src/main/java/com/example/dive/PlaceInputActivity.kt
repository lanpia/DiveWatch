package com.example.dive

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

/**
 * 다이브 위치(장소명)를 입력/수정하는 간단 화면.
 * EditText를 탭하면 Wear 입력기(키보드/음성/손글씨)가 뜬다.
 */
class PlaceInputActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val edit = EditText(this).apply {
            hint = "다이브 장소"
            setText(intent.getStringExtra(EXTRA_INITIAL) ?: "")
            setSingleLine()
        }
        val ok = Button(this).apply {
            text = "확인"
            setOnClickListener {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, edit.text.toString()))
                finish()
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 60, 40, 40)
            addView(
                edit,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                ok,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        setContentView(layout)
        edit.requestFocus()
    }

    companion object {
        const val EXTRA_INITIAL = "initial"
        const val EXTRA_RESULT = "result"
    }
}
