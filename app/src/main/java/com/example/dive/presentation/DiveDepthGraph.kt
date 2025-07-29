package com.example.dive.presentation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import kotlin.math.roundToInt

@Composable
fun DiveDepthGraph(depthHistory: List<Pair<Float, Long>>) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)) {
        val points = depthHistory.takeLast(60)
        val maxDepth = points.maxOfOrNull { it.first } ?: 10f
        val spacing = size.width / (points.size + 1)

        points.forEachIndexed { i, (depth, _) ->
            val x = spacing * i
            val y = size.height - (depth / maxDepth * size.height)
            drawCircle(Color.Cyan, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}
