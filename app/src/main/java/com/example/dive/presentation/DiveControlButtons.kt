package com.example.dive.presentation

import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier

@Composable
fun DiveControlButtons(
    isDiving: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (!isDiving) {
            Button(onClick = onStart) {
                Text("시작")
            }
        } else {
            Button(onClick = onEnd) {
                Text("종료")
            }
        }
    }
}
