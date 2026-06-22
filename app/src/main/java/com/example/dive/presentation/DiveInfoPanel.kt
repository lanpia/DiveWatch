package com.example.dive.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

/** 다이브 진행 중 핵심 지표 패널 (현재/최대/평균 수심 + 시간) */
@Composable
fun DiveInfoPanel(depth: Float, maxDepth: Float, avgDepth: Float, elapsedSec: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "현재 %.1f m".format(depth), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "최대 %.1f m · 평균 %.1f m".format(maxDepth, avgDepth), fontSize = 13.sp)
        Text(text = "시간 ${formatClock(elapsedSec)}", fontSize = 14.sp)
    }
}
