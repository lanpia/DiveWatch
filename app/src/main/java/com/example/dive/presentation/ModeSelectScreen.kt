package com.example.dive.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.dive.model.DiveMode
import com.example.dive.model.WaterType

/** 앱 진입 화면: 모드 선택 + 로그 조회 + 수질 설정 */
@Composable
fun ModeSelectScreen(
    waterType: WaterType,
    onToggleWater: () -> Unit,
    onSelectMode: (DiveMode) -> Unit,
    onViewLogs: () -> Unit
) {
    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(text = "모드 선택", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
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
                    onClick = onToggleWater,
                    label = { Text("수질: ${waterType.label}") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
