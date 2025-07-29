package com.example.dive.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import kotlinx.coroutines.delay
import com.example.dive.sensor.PressureSensorManager

@Composable
fun DiveScreen(sensorManager: PressureSensorManager) {
    var depth by remember { mutableStateOf(0f) }
    var isDiving by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }
    val depthHistory = remember { mutableStateListOf<Pair<Float, Long>>() }

    LaunchedEffect(Unit) {
        sensorManager.start()
    }

    sensorManager.onDepthChanged = { newDepth ->
        depth = newDepth
        if (isDiving) {
            depthHistory.add(Pair(newDepth, System.currentTimeMillis()))
        }
    }

    LaunchedEffect(isDiving) {
        while (isDiving) {
            elapsedTime = (System.currentTimeMillis() - startTime) / 1000
            delay(1000)
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            DiveInfoPanel(depth = depth, time = elapsedTime)
            Spacer(Modifier.height(8.dp))
            DiveDepthGraph(depthHistory = depthHistory)
            Spacer(Modifier.height(8.dp))
            DiveControlButtons(
                isDiving = isDiving,
                onStart = {
                    isDiving = true
                    startTime = System.currentTimeMillis()
                    elapsedTime = 0
                    sensorManager.calibrate(sensorManager.lastPressure)
                },
                onEnd = { isDiving = false }
            )
        }
    }
}