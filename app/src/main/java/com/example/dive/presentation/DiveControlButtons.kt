package com.example.dive.presentation

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*

@Composable
fun DiveControlButtons(
    isDiving: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (!isDiving) {
            Button(onClick = onStart) {
                Text("다이브 시작")
            }
        } else {
            Button(onClick = onEnd) {
                Text("종료")
            }
        }
    }
}
