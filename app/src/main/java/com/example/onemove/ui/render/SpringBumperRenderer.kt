package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.SpringBumper

/** The coil sits behind the plate; its uncompressed front edge remains at the solver's +18 offset. */
object SpringBumperRenderer {
    fun drawSpringBumper(scope: DrawScope, bumper: SpringBumper) {
        with(scope) {
            val position = Offset(bumper.position.x, bumper.position.y)
            val normal = bumper.direction.normalized()
            val direction = Offset(normal.x, normal.y)
            val tangent = Offset(-direction.y, direction.x)
            val width = bumper.width
            val base = position - direction * 60f
            val front = position + direction * (18f - bumper.compression.coerceIn(0f, 1f) * 14f)
            val plate = front - direction * 6f
            val coilStart = base + direction * 8f
            val coilLength = (plate - base).getDistance() - 16f
            val amplitude = minOf(width * .18f, 28f)
            val coil = Path().apply {
                moveTo(coilStart.x, coilStart.y)
                repeat(7) { index ->
                    val sign = if (index % 2 == 0) 1f else -1f
                    val point = coilStart + direction * ((index + 1f) * coilLength / 8f) + tangent * (amplitude * sign)
                    lineTo(point.x, point.y)
                }
                val end = plate - direction * 8f
                lineTo(end.x, end.y)
            }
            drawLine(Color(0xFF15263B), base - tangent * (width * .38f), base + tangent * (width * .38f), 14f, StrokeCap.Round)
            drawLine(Color(0xFF7890AA), base - tangent * (width * .38f), base + tangent * (width * .38f), 5f, StrokeCap.Round)
            drawPath(coil, Color(0xFF142134), style = Stroke(8f, cap = StrokeCap.Round))
            drawPath(coil, Color(0xFFB8CCDC), style = Stroke(4.5f, cap = StrokeCap.Round))
            for (side in listOf(-1f, 1f)) {
                val screw = base + tangent * (width * .32f * side)
                drawCircle(Color(0xFFF59E0B), 5f, screw)
                drawCircle(Color(0xFF7C4A20), 2f, screw)
            }
            val face = if (bumper.flashTimer > 0f) Color(0xFFCCFBF1) else Color(0xFF0EA5E9)
            drawLine(face, plate - tangent * (width * .5f), plate + tangent * (width * .5f), 12f, StrokeCap.Round)
            drawLine(Color(0xFFE0F2FE), plate - tangent * (width * .48f) + direction * 2f, plate + tangent * (width * .48f) + direction * 2f, 3f, StrokeCap.Round)
        }
    }
}
