package com.example.dive.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.data.DiveLogRepository
import com.example.dive.export.WearReportSender
import com.example.dive.model.DiveMode
import com.example.dive.model.WaterType
import kotlinx.coroutines.launch

/** 앱 진입 화면: 모드 선택 + 로그 조회 + 폰으로 로그 공유 + 수질 설정 */
@Composable
fun ModeSelectScreen(
    repository: DiveLogRepository,
    waterType: WaterType,
    onToggleWater: () -> Unit,
    onSelectMode: (DiveMode) -> Unit,
    onViewLogs: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Text(text = "모드 선택", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectMode(DiveMode.FREEDIVING) },
                    label = { Text("프리다이빙") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectMode(DiveMode.SCUBA) },
                    label = { Text("스쿠버 다이빙") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onViewLogs,
                    label = { Text("이전 로그 조회") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            try {
                                val sessions = repository.loadAll()
                                if (sessions.isEmpty()) {
                                    Toast.makeText(context, "보낼 로그가 없습니다", Toast.LENGTH_SHORT).show()
                                } else {
                                    val name = WearReportSender.sendReport(context, sessions)
                                    Toast.makeText(context, "$name(으)로 전송됨", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "전송 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    label = { Text("폰으로 로그 공유") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleWater,
                    label = { Text("수질: ${waterType.label}") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
