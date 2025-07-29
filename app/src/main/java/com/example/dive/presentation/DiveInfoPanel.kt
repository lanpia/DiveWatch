package com.example.dive.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*

@Composable
fun DiveInfoPanel(depth: Float, time: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "현재 수심: %.2f m".format(depth), fontSize = 18.sp)
        Text(text = "다이브 시간: ${time}초", fontSize = 16.sp)
    }
}
