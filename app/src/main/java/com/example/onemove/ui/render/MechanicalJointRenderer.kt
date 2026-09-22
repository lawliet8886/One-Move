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

object MechanicalJointRenderer {
    fun drawSeesawAssembly(drawScope: DrawScope, seesaw: SeesawLever) = with(drawScope) {
        val pivot = Offset(seesaw.pivot.x, seesaw.pivot.y)
        val stand = Path().apply {
            moveTo(pivot.x, pivot.y)
            lineTo(pivot.x - 30f, pivot.y + 40f)
            lineTo(pivot.x + 30f, pivot.y + 40f)
            close()
        }
        drawPath(stand, Color(0xFF1E293B))
        drawPath(stand, Color(0xFF334155), style = Stroke(2.5f))
        // Translate to the physical axle, then rotate around the local origin.
        // The default Canvas centre would displace the beam away from its collider.
        withTransform({
            translate(pivot.x, pivot.y)
            rotate(Math.toDegrees(seesaw.angle.toDouble()).toFloat(), pivot = Offset.Zero)
        }) {
            drawLine(Color(0x66000000), Offset(-seesaw.halfLength, 4f), Offset(seesaw.halfLength, 4f), seesaw.thickness, StrokeCap.Round)
            drawLine(Color(0xFF334155), Offset(-seesaw.halfLength, 0f), Offset(seesaw.halfLength, 0f), seesaw.thickness, StrokeCap.Round)
            drawLine(Color(0xFF94A3B8), Offset(-seesaw.halfLength + 6f, -seesaw.thickness * 0.25f), Offset(seesaw.halfLength - 6f, -seesaw.thickness * 0.25f), 2.5f, StrokeCap.Round)
        }
        drawCircle(Color(0xFFB45309), 16f, pivot)
        drawCircle(Color(0xFFF59E0B), 12f, pivot)
        drawCircle(Color(0xFFFEF3C7), 6f, pivot)
    }

    fun drawCreatureGate(drawScope: DrawScope, gate: CreatureGate) = with(drawScope) {
        val pivot = Offset(gate.pivot.x, gate.pivot.y)
        val closedEnd = Offset(gate.closedEnd.x, gate.closedEnd.y)
        val end = pivot + (closedEnd - pivot) * (1f - gate.openProgress.coerceIn(0f, 1f))
        drawLine(Color(0xFF475569), pivot, end, gate.thickness, StrokeCap.Round)
        drawLine(Color(0xFF0EA5E9), pivot, end, gate.thickness * 0.5f, StrokeCap.Round)
        drawCircle(Color(0xFFF59E0B), gate.thickness * 0.75f, pivot)
        drawCircle(Color(0xFF78350F), gate.thickness * 0.45f, pivot)
    }
}
