package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.CreatureGate
import com.example.onemove.model.SeesawLever
import kotlin.math.cos
import kotlin.math.sin

object MechanicalJointRenderer {

    fun drawSeesawAssembly(drawScope: DrawScope, seesaw: SeesawLever) {
        with(drawScope) {
            val pivot = Offset(seesaw.pivot.x, seesaw.pivot.y)

            // 1. Triangular Fulcrum Stand
            val fulcrumPath = Path().apply {
                moveTo(pivot.x, pivot.y)
                lineTo(pivot.x - 30f, pivot.y + 40f)
                lineTo(pivot.x + 30f, pivot.y + 40f)
                close()
            }
            drawPath(fulcrumPath, Color(0xFF1E293B))
            drawPath(fulcrumPath, Color(0xFF334155), style = Stroke(width = 2.5f))

            // 2. Rotating Beam
            val angleDeg = Math.toDegrees(seesaw.angle.toDouble()).toFloat()
            withTransform({
                translate(pivot.x, pivot.y)
                rotate(angleDeg)
            }) {
                // Drop shadow
                drawLine(
                    color = Color(0x66000000),
                    start = Offset(-seesaw.halfLength, 4f),
                    end = Offset(seesaw.halfLength, 4f),
                    strokeWidth = seesaw.thickness,
                    cap = StrokeCap.Round
                )
                // Lever bar
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(-seesaw.halfLength, 0f),
                    end = Offset(seesaw.halfLength, 0f),
                    strokeWidth = seesaw.thickness,
                    cap = StrokeCap.Round
                )
                // Top highlight
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(-seesaw.halfLength + 6f, -seesaw.thickness * 0.25f),
                    end = Offset(seesaw.halfLength - 6f, -seesaw.thickness * 0.25f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Central Axle Bearing
            drawCircle(Color(0xFFB45309), radius = 16f, center = pivot)
            drawCircle(Color(0xFFF59E0B), radius = 12f, center = pivot)
            drawCircle(Color(0xFFFEF3C7), radius = 6f, center = pivot)
        }
    }

    fun drawCreatureGate(drawScope: DrawScope, gate: CreatureGate) {
        with(drawScope) {
            val pivot = Offset(gate.pivot.x, gate.pivot.y)
            val closedEnd = Offset(gate.closedEnd.x, gate.closedEnd.y)

            val dir = closedEnd - pivot
            val currentEnd = pivot + dir * (1f - gate.openProgress.coerceIn(0f, 1f))

            // Gate beam
            drawLine(
                color = Color(0xFF475569),
                start = pivot,
                end = currentEnd,
                strokeWidth = gate.thickness,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF0EA5E9),
                start = pivot,
                end = currentEnd,
                strokeWidth = gate.thickness * 0.5f,
                cap = StrokeCap.Round
            )

            // Pivot hinge
            drawCircle(Color(0xFFF59E0B), radius = gate.thickness * 0.75f, center = pivot)
            drawCircle(Color(0xFF78350F), radius = gate.thickness * 0.45f, center = pivot)
        }
    }
}
