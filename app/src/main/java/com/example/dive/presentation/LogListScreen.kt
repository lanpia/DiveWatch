package com.example.dive.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.data.DiveLogRepository
import com.example.dive.export.ReportExporter
import com.example.dive.export.WearReportSender
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession
import kotlinx.coroutines.launch

/** 저장된 다이브 세션 목록 */
@Composable
fun LogListScreen(
    repository: DiveLogRepository,
    onBack: () -> Unit,
    onOpen: (DiveSession) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf(repository.loadAll()) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(text = "다이브 로그", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (sessions.isEmpty()) {
                item { Text(text = "저장된 로그 없음", fontSize = 13.sp) }
            } else {
                items(sessions) { session ->
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpen(session) },
                        label = { Text(formatDate(session.startTime)) },
                        secondaryLabel = { Text(sessionSubtitle(session)) }
                    )
                }
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                try {
                                    val name = WearReportSender.sendReport(
                                        context, sessions, System.currentTimeMillis()
                                    )
                                    Toast.makeText(context, "$name(으)로 전송됨", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        e.message ?: "전송 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        label = { Text("폰으로 보내기") },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { ReportExporter.exportAndShare(context, sessions, System.currentTimeMillis()) },
                        label = { Text("리포트 공유") },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (confirmClear) {
                                repository.deleteAll()
                                sessions = emptyList()
                                confirmClear = false
                            } else {
                                confirmClear = true
                            }
                        },
                        label = {
                            Text(
                                text = if (confirmClear) "한 번 더 누르면 전체 삭제" else "전체 삭제",
                                color = Color.Red
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }
    }
}

private fun sessionSubtitle(session: DiveSession): String =
    if (session.mode == DiveMode.FREEDIVING) {
        "%s · %d회 · 최대 %.1fm".format(modeLabel(session.mode), session.diveCount, session.maxDepth)
    } else {
        "%s · 최대 %.1fm · %s".format(
            modeLabel(session.mode),
            session.maxDepth,
            formatClock(session.totalDiveSec)
        )
    }
