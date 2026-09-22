package com.example.onemove.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.physics.PhysicsWorld

/** The cable explains the causal link. The solid gate matches the collision rail. */
object WeightSwitchRenderer {
    private val brass = Color(0xFFB87C32)
    private val edge = Color(0xFFFFE4AA)
    private val dark = Color(0xFF34413B)
    private val live = Color(0xFF168063)
    fun draw(scope: DrawScope, world: PhysicsWorld) = with(scope) {
        for (plate in world.pressurePlates) {
            val c = plate.center.toOffset()
            val accent = if (plate.isLatched) live else brass
            for (gate in world.creatureGates.filter { plate.id in it.requiredPlateIds }) {
                val end = gate.pivot.toOffset()
                val bendX = c.x + plate.width * 0.5f + 74f
                val cable = Path().apply {
                    moveTo(c.x, c.y + 28f); lineTo(bendX, c.y + 28f)
                    lineTo(bendX, end.y - 54f); lineTo(end.x, end.y - 54f); lineTo(end.x, end.y)
                }
                drawPath(cable, Color(0xFFE9D9B8), style = Stroke(11f, cap = StrokeCap.Round))
                drawPath(cable, accent.copy(alpha = 0.72f), style = Stroke(4f, cap = StrokeCap.Round))
                drawCircle(dark, 10f, c + Offset(0f, 28f))
                drawCircle(accent, 6f, c + Offset(0f, 28f))
            }
            // Foot and rivets are ornamental; the contact slab uses exact physical size.
            drawRoundRect(Color(0xFF664E36), c + Offset(-plate.width/2f-8f, 8f),
                Size(plate.width+16f, 26f), CornerRadius(8f))
            drawRoundRect(brass, c - Offset(plate.width/2f, plate.thickness/2f),
                Size(plate.width, plate.thickness), CornerRadius(5f))
            drawLine(edge, c + Offset(-plate.width/2f+5f, -plate.thickness/2f+2f),
                c + Offset(plate.width/2f-5f, -plate.thickness/2f+2f), 3f)
            for (side in listOf(-1f, 1f)) {
                val screw = c + Offset(side*(plate.width/2f-14f), 19f)
                drawCircle(edge, 4.5f, screw)
                drawLine(dark, screw-Offset(2f,0f), screw+Offset(2f,0f), 1.5f)
            }
            val indicator = c + Offset(0f, 22f)
            drawCircle(dark, 9f, indicator); drawCircle(accent, 6f, indicator)
        }
        for (gate in world.creatureGates.filter { it.requiredPlateIds.isNotEmpty() }) {
            val a = gate.pivot.toOffset()
            val b = (gate.pivot + (gate.closedEnd-gate.pivot)*(1f-gate.openProgress)).toOffset()
            if (gate.openProgress < 0.95f) {
                drawLine(dark, a+Offset(4f,4f), b+Offset(4f,4f), gate.thickness+5f, StrokeCap.Round)
                drawLine(brass, a, b, gate.thickness, StrokeCap.Round)
                drawLine(edge, a-Offset(3f,0f), b-Offset(3f,0f), 3f, StrokeCap.Round)
            }
            // A lock-shaped housing gives the cable a clear destination.
            drawRoundRect(dark, a-Offset(24f,20f), Size(48f,44f), CornerRadius(9f))
            drawRoundRect(brass, a-Offset(19f,17f), Size(38f,34f), CornerRadius(7f))
            val accent = if (gate.isOpen) live else dark
            drawCircle(accent, 7f, a-Offset(0f,3f))
            drawLine(accent, a, a+Offset(0f,10f), 5f, StrokeCap.Round)
            if (gate.isOpen) {
                drawLine(edge, a+Offset(-9f,-4f), a+Offset(-2f,3f), 3f, StrokeCap.Round)
                drawLine(edge, a+Offset(-2f,3f), a+Offset(10f,-8f), 3f, StrokeCap.Round)
            }
        }
    }
}
