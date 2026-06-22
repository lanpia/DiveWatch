package com.example.dive.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.dive.data.DiveLogRepository
import com.example.dive.data.SettingsStore
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession
import com.example.dive.model.WaterType
import com.example.dive.sensor.PressureSensorManager

/** 화면 상태 */
private sealed interface Screen {
    data object ModeSelect : Screen
    data class Diving(val mode: DiveMode) : Screen
    data object LogList : Screen
    data class LogDetail(val session: DiveSession) : Screen
}

/**
 * 앱 진입점 컴포저블. 모드 선택 → 다이브/로그 화면 간 단순 상태 기반 내비게이션.
 */
@Composable
fun DiveApp(sensorManager: PressureSensorManager) {
    val context = LocalContext.current
    val repository = remember { DiveLogRepository(context) }
    val settings = remember { SettingsStore(context) }

    var screen by remember { mutableStateOf<Screen>(Screen.ModeSelect) }
    var waterType by remember { mutableStateOf(settings.waterType) }

    LaunchedEffect(Unit) { sensorManager.start() }
    LaunchedEffect(waterType) { sensorManager.waterType = waterType }

    when (val current = screen) {
        is Screen.ModeSelect -> ModeSelectScreen(
            repository = repository,
            waterType = waterType,
            onToggleWater = {
                val next = if (waterType == WaterType.SEA) WaterType.FRESH else WaterType.SEA
                waterType = next
                settings.waterType = next
            },
            onSelectMode = { mode -> screen = Screen.Diving(mode) },
            onViewLogs = { screen = Screen.LogList }
        )

        is Screen.Diving -> DiveScreen(
            mode = current.mode,
            sensorManager = sensorManager,
            repository = repository,
            onExit = { screen = Screen.ModeSelect }
        )

        is Screen.LogList -> LogListScreen(
            repository = repository,
            onBack = { screen = Screen.ModeSelect },
            onOpen = { session -> screen = Screen.LogDetail(session) }
        )

        is Screen.LogDetail -> LogDetailScreen(
            session = current.session,
            repository = repository,
            onBack = { screen = Screen.LogList }
        )
    }
}
