package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.SpringBumper

object SpringBumperRenderer {

    fun drawSpringBumper(drawScope: DrawScope, bumper: SpringBumper) {
        with(drawScope) {
            val pos = Offset(bumper.position.x, bumper.position.y)
            val normal = bumper.direction.normalized()
            val dir = Offset(normal.x, normal.y)
            val perp = Offset(-dir.y, dir.x)
            val w = bumper.width
            val currentH = bumper.restHeight * (1f - bumper.compression.coerceIn(0f, 0.9f))

            // 1. Steel Mounting Base Plate
            val baseP1 = pos - perp * (w * 0.5f)
            val baseP2 = pos + perp * (w * 0.5f)
            drawLine(
                color = Color(0xFF1E293B),
                start = baseP1,
                end = baseP2,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF475569),
                start = baseP1,
                end = baseP2,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // 2. Coiled Spring Wire
            val coils = 4
            val coilStep = currentH / coils
            val springPath = Path()
            springPath.moveTo(pos.x, pos.y)

            for (i in 0 until coils) {
                val sign = if (i % 2 == 0) 1f else -1f
                val midX = pos.x + dir.x * (i + 0.5f) * coilStep + perp.x * sign * (w * 0.25f)
                val midY = pos.y + dir.y * (i + 0.5f) * coilStep + perp.y * sign * (w * 0.25f)
                val endX = pos.x + dir.x * (i + 1f) * coilStep
                val endY = pos.y + dir.y * (i + 1f) * coilStep
                springPath.lineTo(midX, midY)
                springPath.lineTo(endX, endY)
            }
            drawPath(springPath, Color(0xFF94A3B8), style = Stroke(width = 3.5f))

            // 3. Kinetic Cyan Strike Piston Head
            val headCenter = pos + dir * currentH
            val headP1 = headCenter - perp * (w * 0.45f)
            val headP2 = headCenter + perp * (w * 0.45f)
            val headColor = if (bumper.flashTimer > 0f) Color(0xFF38BDF8) else Color(0xFF0EA5E9)

            drawLine(
                color = headColor,
                start = headP1,
                end = headP2,
                strokeWidth = 12f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE0F2FE),
                start = headP1,
                end = headP2,
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // The launch direction must be legible before committing the one move.
            // Uses the same normalized direction as the physical impulse.
            val arrowBase = headCenter + dir * 32f
            val arrowTip = arrowBase + dir * 58f
            val arrowColor = Color(0xFFBAE6FD)
            drawLine(arrowColor, arrowBase, arrowTip, 5f, cap = StrokeCap.Round)
            drawLine(arrowColor, arrowTip, arrowTip - dir * 19f + perp * 14f, 5f, cap = StrokeCap.Round)
            drawLine(arrowColor, arrowTip, arrowTip - dir * 19f - perp * 14f, 5f, cap = StrokeCap.Round)
        }
    }
}
