package com.samplocal.manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.ui.theme.Divider
import com.samplocal.manager.ui.theme.TextDim

@Composable
fun LineChart(
    values: List<Float>,
    maxValue: Float,
    formatY: (Float) -> String,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    gridLines: Int = 4
) {
    val safeMax = (if (maxValue <= 0f) 1f else maxValue)
    val gridColor = Divider
    val labels = remember(safeMax, gridLines) {
        (0..gridLines).map { i -> safeMax - (safeMax / gridLines) * i }
    }
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        Column(
            modifier = Modifier.fillMaxHeight().width(56.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { v ->
                Text(formatY(v), color = TextDim, fontSize = 11.sp, maxLines = 1)
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height

            for (i in 0..gridLines) {
                val y = h - (h / gridLines) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            if (values.isEmpty()) {
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 2.dp.toPx()
                )
                return@Canvas
            }
            fun xAt(i: Int): Float =
                if (values.size == 1) 0f else w * i / (values.size - 1)
            fun yAt(v: Float): Float =
                h - (h * (v.coerceIn(0f, safeMax) / safeMax))
            if (values.size == 1) {
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(0f, yAt(values[0])))
                return@Canvas
            }
            val line = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until values.size) lineTo(xAt(i), yAt(values[i]))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(xAt(values.size - 1), h)
                lineTo(xAt(0), h)
                close()
            }
            drawPath(fill, color = lineColor.copy(alpha = 0.22f))
            drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
