package com.example.dive.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dive.model.DiveSample

/**
 * 수심 프로파일 그래프. 수면이 위, 깊을수록 아래로 그려진다.
 */
@Composable
fun DiveDepthGraph(
    samples: List<DiveSample>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        if (samples.isEmpty()) return@Canvas

        val points = samples.takeLast(120)
        val maxDepth = (points.maxOf { it.depth }).coerceAtLeast(1f)
        val dx = size.width / points.size.coerceAtLeast(1)

        var prev: Offset? = null
        points.forEachIndexed { i, sample ->
            val x = dx * i
            val y = (sample.depth / maxDepth) * size.height // 깊을수록 아래(y 증가)
            val current = Offset(x, y)
            prev?.let { drawLine(Color.Cyan, it, current, strokeWidth = 3f) }
            prev = current
        }
    }
}
