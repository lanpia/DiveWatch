package com.example.dive.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.data.DiveLogRepository
import com.example.dive.model.DiveMode
import com.example.dive.model.DiveSession

/** 저장된 다이브 세션 목록 (모드 탭으로 필터). 폰 공유는 모드 선택 화면에서. */
@Composable
fun LogListScreen(
    repository: DiveLogRepository,
    onBack: () -> Unit,
    onOpen: (DiveSession) -> Unit
) {
    BackHandler { onBack() }

    var allSessions by remember { mutableStateOf(repository.loadAll()) }
    var filter by remember { mutableStateOf<DiveMode?>(null) } // null = 전체
    var confirmClear by remember { mutableStateOf(false) }

    val shown = remember(allSessions, filter) {
        if (filter == null) allSessions else allSessions.filter { it.mode == filter }
    }

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Text(text = "다이브 로그", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            // 모드 탭(필터)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterTab("전체", filter == null, Modifier.weight(1f)) { filter = null }
                    FilterTab("프리", filter == DiveMode.FREEDIVING, Modifier.weight(1f)) {
                        filter = DiveMode.FREEDIVING
                    }
                    FilterTab("스쿠버", filter == DiveMode.SCUBA, Modifier.weight(1f)) {
                        filter = DiveMode.SCUBA
                    }
                }
            }

            when {
                allSessions.isEmpty() -> item { Text(text = "저장된 로그 없음", fontSize = 13.sp) }
                shown.isEmpty() -> item { Text(text = "이 모드 로그 없음", fontSize = 13.sp) }
                else -> items(shown) { session ->
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpen(session) },
                        label = { Text(formatDate(session.startTime)) },
                        secondaryLabel = { Text(sessionSubtitle(session)) }
                    )
                }
            }

            if (allSessions.isNotEmpty()) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (confirmClear) {
                                repository.deleteAll()
                                allSessions = emptyList()
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

@Composable
private fun FilterTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Chip(
        modifier = modifier,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors()
    )
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
