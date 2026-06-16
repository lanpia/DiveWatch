package com.example.dive.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.data.DiveLogRepository
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession

/** 단일 다이브 세션 상세 */
@Composable
fun LogDetailScreen(
    session: DiveSession,
    repository: DiveLogRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var confirmDelete by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(4.dp))

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
                        text = if (confirmDelete) "한 번 더 누르면 삭제" else "삭제",
                        color = Color.Red
                    )
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
