package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PriorityPattern(
    priority: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height

        when (priority) {
            3 -> {
                // Priority 3 (Alta): Elegant Abstract Diamond (Rombo)
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Fill
                )
            }
            2 -> {
                // Priority 2 (Media): Elegant Abstract Hexagon (Hexágono)
                val path = Path().apply {
                    moveTo(w / 2f, 0f)                     // Top
                    lineTo(w, h * 0.25f)                   // Top-Right
                    lineTo(w, h * 0.75f)                   // Bottom-Right
                    lineTo(w / 2f, h)                     // Bottom
                    lineTo(0f, h * 0.75f)                  // Bottom-Left
                    lineTo(0f, h * 0.25f)                  // Top-Left
                    close()
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Fill
                )
            }
            else -> {
                // Priority 1 (Baja): Concetric Dot-in-Ring (Círculo con anillo concéntrico)
                drawCircle(
                    color = color,
                    radius = w / 2f,
                    style = Stroke(width = w * 0.18f)
                )
                drawCircle(
                    color = color,
                    radius = w * 0.22f,
                    style = Fill
                )
            }
        }
    }
}
