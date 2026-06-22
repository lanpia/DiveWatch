package com.example.dive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dive.presentation.DiveApp
import com.example.dive.sensor.PressureSensorManager
import com.example.dive.ui.theme.DiveTheme

class MainActivity : ComponentActivity() {
    private lateinit var pressureSensorManager: PressureSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱이 보이는 동안 화면이 꺼지지 않도록 유지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestLocationPermissionIfNeeded()

        pressureSensorManager = PressureSensorManager(this)

        setContent {
            DiveTheme {
                DiveApp(pressureSensorManager)
            }
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        if (checkSelfPermission(fine) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(fine, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pressureSensorManager.unregister()
    }

    private companion object {
        const val REQ_LOCATION = 100
    }
}
