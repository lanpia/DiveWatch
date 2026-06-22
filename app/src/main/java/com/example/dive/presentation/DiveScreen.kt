package com.example.dive.presentation

import android.content.Context
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.data.DiveLogRepository
import com.example.dive.deco.DecoModel
import com.example.dive.location.LocationHelper
import com.example.dive.model.DiveLog
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSample
import com.example.dive.model.DiveSession
import com.example.dive.sensor.PressureSensorManager
import com.example.dive.util.vibrateWarning
import kotlinx.coroutines.delay

private const val START_DEPTH = 1.0f          // m: 이 수심 이상 내려가면 기록 시작
private const val SURFACE_DEPTH = 0.5f        // m: 수면 복귀 판정 수심
private const val SURFACE_CONFIRM_MS = 3000L  // 프리다이빙 1회 종료 확정 대기
private const val MAX_ASCENT_RATE = 10f       // m/min: 스쿠버 안전 상승 속도 한계
private const val ASCENT_WINDOW_MS = 3000L    // 상승 속도 계산 창
private const val NDL_WARN_MIN = 5f           // 무감압 잔여 경고 임계(분)
private const val SAFETY_STOP_DEPTH_MIN = 3f
private const val SAFETY_STOP_DEPTH_MAX = 6f
private const val SAFETY_STOP_SECONDS = 180   // 안전정지 3분
private const val SAFETY_STOP_TRIGGER_DEPTH = 10f // 이 수심보다 깊었던 경우만 권고
private const val BACK_PRESSES_TO_EXIT = 3
private const val BACK_RESET_MS = 2000L

/**
 * 모드별 다이브 화면. 화면 1회 방문 = 1 세션.
 * - 공통: 수심이 START_DEPTH(1m) 이상 내려가면 자동으로 기록 시작.
 * - 프리다이빙: 수면 복귀가 확정되면 1회 다이브를 세션에 추가하고 다음 다이브 대기(반복).
 * - 스쿠버: 연속 기록 + 상승 속도 경고 + 무감압 한계(NDL) + 안전정지 타이머.
 * - 종료: 뒤로가기 3회 → 세션(누적된 다이브들)을 저장하고 종료.
 */
@Composable
fun DiveScreen(
    mode: DiveMode,
    sensorManager: PressureSensorManager,
    repository: DiveLogRepository,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    var currentDepth by remember { mutableStateOf(0f) }
    var diving by remember { mutableStateOf(false) }
    var diveStartMs by remember { mutableStateOf(0L) }
    var elapsedSec by remember { mutableStateOf(0L) }
    var maxDepth by remember { mutableStateOf(0f) }
    var avgDepth by remember { mutableStateOf(0f) }
    var depthSum by remember { mutableStateOf(0.0) }
    val samples = remember { mutableStateListOf<DiveSample>() }
    var lastSampleSec by remember { mutableStateOf(-1L) }
    var surfaceSinceMs by remember { mutableStateOf(0L) }

    // 세션(이번 화면 방문 동안의 다이브들)
    val sessionStartMs = remember { System.currentTimeMillis() }
    val sessionDives = remember { mutableStateListOf<DiveLog>() }
    var diveLat by remember { mutableStateOf<Double?>(null) }
    var diveLng by remember { mutableStateOf<Double?>(null) }
    var divePlace by remember { mutableStateOf<String?>(null) }

    // 상승 속도(스쿠버)
    val rateWindow = remember { ArrayDeque<Pair<Long, Float>>() }
    var ascentRate by remember { mutableStateOf(0f) }   // m/min, 양수 = 상승
    var ascentWarning by remember { mutableStateOf(false) }

    // 무감압 한계(스쿠버)
    val decoModel = remember { DecoModel() }
    var ndlMin by remember { mutableStateOf(999f) }
    var lastDecoMs by remember { mutableStateOf(0L) }

    // 안전정지(스쿠버)
    var safetyStopRemaining by remember { mutableStateOf(SAFETY_STOP_SECONDS) }
    var safetyStopActive by remember { mutableStateOf(false) }
    var safetyStopDone by remember { mutableStateOf(false) }

    // 프리다이빙 수면 휴식
    var lastDiveEndMs by remember { mutableStateOf(0L) }
    var surfaceIntervalSec by remember { mutableStateOf(0L) }

    var backCount by remember { mutableStateOf(0) }

    fun buildLog(): DiveLog {
        val now = System.currentTimeMillis()
        val durSec = ((now - diveStartMs) / 1000).coerceAtLeast(0)
        val avg = if (samples.isEmpty()) 0f else samples.map { it.depth }.average().toFloat()
        return DiveLog(
            id = diveStartMs,
            mode = mode,
            startTime = diveStartMs,
            durationSec = durSec,
            maxDepth = maxDepth,
            avgDepth = avg,
            samples = samples.toList()
        )
    }

    fun finalizeCurrentDive() {
        if (!diving) return
        sessionDives.add(buildLog())
        lastDiveEndMs = System.currentTimeMillis()
        diving = false
        surfaceSinceMs = 0L
        ascentRate = 0f
        ascentWarning = false
        rateWindow.clear()
    }

    fun startDive(now: Long, depth: Float) {
        diving = true
        diveStartMs = now
        elapsedSec = 0
        maxDepth = depth
        avgDepth = depth
        depthSum = 0.0
        samples.clear()
        lastSampleSec = -1L
        surfaceSinceMs = 0L
        ascentRate = 0f
        ascentWarning = false
        rateWindow.clear()
        // 스쿠버 안전 지표 초기화
        decoModel.reset()
        ndlMin = 999f
        lastDecoMs = now
        safetyStopRemaining = SAFETY_STOP_SECONDS
        safetyStopActive = false
        safetyStopDone = false
    }

    fun endSessionAndExit() {
        finalizeCurrentDive() // 진행 중이면 세션에 추가
        if (sessionDives.isNotEmpty()) {
            repository.save(
                DiveSession(
                    id = sessionStartMs,
                    mode = mode,
                    startTime = sessionStartMs,
                    endTime = System.currentTimeMillis(),
                    dives = sessionDives.toList(),
                    latitude = diveLat,
                    longitude = diveLng,
                    placeName = divePlace
                )
            )
        }
        onExit()
    }

    fun handleDepth(depth: Float) {
        val now = System.currentTimeMillis()
        currentDepth = depth

        if (!diving) {
            if (depth >= START_DEPTH) startDive(now, depth)
            return
        }

        if (depth > maxDepth) maxDepth = depth
        val sec = (now - diveStartMs) / 1000
        if (sec != lastSampleSec) {
            samples.add(DiveSample(timeSec = sec, depth = depth))
            lastSampleSec = sec
            depthSum += depth
            avgDepth = (depthSum / samples.size).toFloat()

            // 스쿠버: 1초 단위로 질소 부하/NDL 갱신
            if (mode == DiveMode.SCUBA) {
                val dtMin = (now - lastDecoMs) / 60000f
                decoModel.update(depth, dtMin)
                lastDecoMs = now
                ndlMin = decoModel.ndlMinutes(depth)
            }
        }

        // 상승 속도 계산 (최근 ASCENT_WINDOW_MS 창)
        rateWindow.addLast(now to depth)
        while (rateWindow.size > 1 && now - rateWindow.first().first > ASCENT_WINDOW_MS) {
            rateWindow.removeFirst()
        }
        val oldest = rateWindow.first()
        val dtSec = (now - oldest.first) / 1000f
        ascentRate = if (dtSec >= 0.5f) ((oldest.second - depth) / dtSec) * 60f else 0f
        ascentWarning = mode == DiveMode.SCUBA && depth > START_DEPTH && ascentRate > MAX_ASCENT_RATE

        // 프리다이빙만 수면 복귀 시 자동으로 1회 다이브 종료/저장
        if (mode == DiveMode.FREEDIVING) {
            if (depth < SURFACE_DEPTH) {
                if (surfaceSinceMs == 0L) {
                    surfaceSinceMs = now
                } else if (now - surfaceSinceMs >= SURFACE_CONFIRM_MS) {
                    finalizeCurrentDive()
                }
            } else {
                surfaceSinceMs = 0L
            }
        }
    }

    val ndlLow = diving && mode == DiveMode.SCUBA && ndlMin < NDL_WARN_MIN
    val diveCount = sessionDives.size
    val lastDive = sessionDives.lastOrNull()

    // 센서 콜백 연결 + 진입 시 수면 기준 보정 + 다이브 동안 CPU 깨움 유지
    DisposableEffect(Unit) {
        sensorManager.requestCalibration()
        sensorManager.onDepthChanged = { d -> handleDepth(d) }

        // 화면이 잠시 꺼져도 기록(센서 샘플링)이 끊기지 않도록 CPU 깨움 유지
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dive:recording")
        @Suppress("WakelockTimeout")
        wakeLock.acquire(3 * 60 * 60 * 1000L) // 최대 3시간 안전 타임아웃

        onDispose {
            sensorManager.onDepthChanged = {}
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    // 진입(수면) 시 GPS 위치 1회 기록 + 장소명 역지오코딩
    LaunchedEffect(Unit) {
        if (LocationHelper.hasPermission(context)) {
            val loc = LocationHelper.currentLocation(context)
            if (loc != null) {
                diveLat = loc.first
                diveLng = loc.second
                divePlace = LocationHelper.placeName(context, loc.first, loc.second)
            }
        }
    }

    // 다이브 경과 시간
    LaunchedEffect(diving) {
        while (diving) {
            elapsedSec = (System.currentTimeMillis() - diveStartMs) / 1000
            delay(1000)
        }
    }

    // 안전정지 타이머(스쿠버)
    LaunchedEffect(diving) {
        if (diving && mode == DiveMode.SCUBA) {
            while (diving) {
                if (maxDepth >= SAFETY_STOP_TRIGGER_DEPTH && !safetyStopDone &&
                    currentDepth in SAFETY_STOP_DEPTH_MIN..SAFETY_STOP_DEPTH_MAX
                ) {
                    safetyStopActive = true
                    safetyStopRemaining -= 1
                    if (safetyStopRemaining <= 0) {
                        safetyStopRemaining = 0
                        safetyStopActive = false
                        safetyStopDone = true
                        vibrateWarning(context, 500)
                    }
                } else {
                    safetyStopActive = false
                }
                delay(1000)
            }
        }
    }

    // 수면 휴식 시간(프리다이빙: 다이브 사이 간격)
    LaunchedEffect(diving, lastDiveEndMs) {
        if (!diving && lastDiveEndMs > 0L) {
            while (!diving) {
                surfaceIntervalSec = (System.currentTimeMillis() - lastDiveEndMs) / 1000
                delay(1000)
            }
        }
    }

    // 경고 진동(상승 속도 / 무감압 임박)
    LaunchedEffect(ascentWarning, ndlLow) {
        while (ascentWarning || ndlLow) {
            vibrateWarning(context)
            delay(1500)
        }
    }

    // 뒤로가기 카운트는 일정 시간 지나면 리셋
    LaunchedEffect(backCount) {
        if (backCount in 1 until BACK_PRESSES_TO_EXIT) {
            delay(BACK_RESET_MS)
            backCount = 0
        }
    }

    BackHandler {
        backCount += 1
        if (backCount >= BACK_PRESSES_TO_EXIT) {
            endSessionAndExit()
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = modeLabel(mode), fontSize = 14.sp, fontWeight = FontWeight.Bold)

            if (diving) {
                DiveInfoPanel(depth = currentDepth, maxDepth = maxDepth, avgDepth = avgDepth, elapsedSec = elapsedSec)

                if (mode == DiveMode.SCUBA) {
                    Text(
                        text = if (ndlMin >= 99f) "무감압 99+분" else "무감압 ${ndlMin.toInt()}분",
                        fontSize = 13.sp,
                        color = if (ndlLow) Color.Red else Color.Unspecified,
                        fontWeight = if (ndlLow) FontWeight.Bold else FontWeight.Normal
                    )
                    if (safetyStopActive) {
                        Text(
                            text = "안전정지 ${formatClock(safetyStopRemaining.toLong())}",
                            fontSize = 13.sp,
                            color = Color(0xFF4FC3F7),
                            fontWeight = FontWeight.Bold
                        )
                    } else if (safetyStopDone) {
                        Text(text = "안전정지 완료", fontSize = 12.sp, color = Color(0xFF81C784))
                    }
                    if (ascentRate > 0.5f) {
                        Text(
                            text = "상승 %.0f m/min".format(ascentRate),
                            fontSize = 13.sp,
                            color = if (ascentWarning) Color.Red else Color.Unspecified,
                            fontWeight = if (ascentWarning) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (ascentWarning) {
                        Text(
                            text = "⚠ 상승 속도가 너무 빠릅니다",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (ndlLow) {
                        Text(
                            text = "⚠ 무감압 한계 임박",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                DiveDepthGraph(samples = samples)
            } else {
                Text(text = "현재 %.1f m".format(currentDepth), fontSize = 22.sp)
                Text(
                    text = "수심 %.1f m에서 자동 기록".format(START_DEPTH),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "💧 워터락을 켜두면 오작동·화면꺼짐 방지",
                    fontSize = 10.sp,
                    color = Color(0xFF4FC3F7),
                    textAlign = TextAlign.Center
                )
                val place = divePlace
                if (place != null) {
                    Text(text = "📍 $place", fontSize = 10.sp, textAlign = TextAlign.Center)
                } else if (diveLat != null) {
                    Text(text = "📍 위치 기록됨", fontSize = 10.sp, textAlign = TextAlign.Center)
                }
                if (mode == DiveMode.FREEDIVING && diveCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = "다이브 ${diveCount}회", fontSize = 13.sp)
                    Text(text = "휴식 ${formatClock(surfaceIntervalSec)}", fontSize = 13.sp)
                    lastDive?.let {
                        Text(
                            text = "직전 최대 %.1f m · %s".format(it.maxDepth, formatClock(it.durationSec)),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (backCount == 0) "종료: 뒤로 ${BACK_PRESSES_TO_EXIT}회"
                else "종료하려면 ${BACK_PRESSES_TO_EXIT - backCount}번 더",
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
