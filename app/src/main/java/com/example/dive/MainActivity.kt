package com.example.dive

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dive.presentation.DiveScreen
import com.example.dive.sensor.PressureSensorManager
import com.example.dive.ui.theme.DiveTheme

class MainActivity : ComponentActivity() {
    private lateinit var pressureSensorManager: PressureSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pressureSensorManager = PressureSensorManager(this)

        setContent {
            DiveTheme {
//                DiveScreen(sensorManager)
                DiveScreen(pressureSensorManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pressureSensorManager.unregister()
    }
}
