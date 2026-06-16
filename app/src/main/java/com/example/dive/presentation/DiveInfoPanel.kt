package com.example.dive.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

/** 다이브 진행 중 표시 패널 */
@Composable
fun DiveInfoPanel(depth: Float, maxDepth: Float, elapsedSec: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "현재 %.1f m".format(depth), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "최대 %.1f m".format(maxDepth), fontSize = 14.sp)
        Text(text = "시간 ${formatClock(elapsedSec)}", fontSize = 14.sp)
    }
}
