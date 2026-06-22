package com.example.dive.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.PlaceInputActivity
import com.example.dive.data.DiveLogRepository
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession

/** 단일 다이브 세션 상세 (+ 위치 수정 / 삭제) */
@Composable
fun LogDetailScreen(
    session: DiveSession,
    repository: DiveLogRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var place by remember { mutableStateOf(session.placeName) }
    var confirmDelete by remember { mutableStateOf(false) }

    val placeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringExtra(PlaceInputActivity.EXTRA_RESULT)?.trim()
            if (!text.isNullOrEmpty()) {
                place = text
                repository.update(session.copy(placeName = text))
            }
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = modeLabel(session.mode), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = formatDate(session.startTime), fontSize = 12.sp)

            val p = place
            val lat = session.latitude
            val lng = session.longitude
            when {
                p != null -> Text(text = "📍 $p", fontSize = 11.sp)
                lat != null && lng != null ->
                    Text(text = "📍 %.4f, %.4f".format(lat, lng), fontSize = 11.sp)
                else -> Text(text = "📍 위치 없음", fontSize = 11.sp, color = Color(0xFF9BBCCB))
            }
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(context, PlaceInputActivity::class.java)
                        .putExtra(PlaceInputActivity.EXTRA_INITIAL, place ?: "")
                    placeLauncher.launch(intent)
                },
                label = { Text("위치 입력/수정", fontSize = 12.sp) },
                colors = ChipDefaults.secondaryChipColors()
            )

            Spacer(Modifier.height(6.dp))

            if (session.mode == DiveMode.SCUBA) {
                val dive = session.dives.firstOrNull()
                if (dive != null) {
                    Text(text = "최대 %.1f m".format(dive.maxDepth), fontSize = 14.sp)
                    Text(text = "평균 %.1f m".format(dive.avgDepth), fontSize = 14.sp)
                    Text(text = "시간 ${formatClock(dive.durationSec)}", fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    DiveDepthGraph(samples = dive.samples)
                }
            } else {
                Text(text = "다이브 ${session.diveCount}회", fontSize = 14.sp)
                Text(text = "최대 %.1f m".format(session.maxDepth), fontSize = 14.sp)
                Text(text = "총 ${formatClock(session.totalDiveSec)}", fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                session.dives.forEachIndexed { i, dive ->
                    Text(
                        text = "#${i + 1}  최대 %.1f m · %s".format(dive.maxDepth, formatClock(dive.durationSec)),
                        fontSize = 12.sp
                    )
                }
                val deepest = session.dives.maxByOrNull { it.maxDepth }
                if (deepest != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = "최고 수심 다이브", fontSize = 11.sp)
                    DiveDepthGraph(samples = deepest.samples)
                }
            }

            Spacer(Modifier.height(8.dp))
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (confirmDelete) {
                        repository.delete(session.id)
                        onBack()
                    } else {
                        confirmDelete = true
                    }
                },
                label = {
                    Text(
                        text = if (confirmDelete) "한 번 더 누르면 삭제" else "이 로그 삭제",
                        color = Color.Red
                    )
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
