package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.Pin
import com.example.onemove.model.PinId
import com.example.onemove.ui.theme.OneMoveVisualTheme

object PinRenderer {

    fun drawPin(drawScope: DrawScope, pin: Pin, isReady: Boolean = true) {
        if (pin.isRemoved) return
        with(drawScope) {
            val start = Offset(pin.start.x, pin.start.y)
            val end = Offset(pin.end.x, pin.end.y)
            val handle = Offset(pin.handlePosition.x, pin.handlePosition.y)

            // 1. Drop shadow for pin shaft
            drawLine(
                color = Color(0x55000000),
                start = start + Offset(0f, 6f),
                end = end + Offset(0f, 6f),
                strokeWidth = pin.thickness,
                cap = StrokeCap.Round
            )

            // 2. Pin shaft body
            drawLine(
                color = OneMoveVisualTheme.Pins.pinShaft,
                start = start,
                end = end,
                strokeWidth = pin.thickness,
                cap = StrokeCap.Round
            )

            // Pin shaft colored accent stripe
            drawLine(
                color = pin.color,
                start = start,
                end = end,
                strokeWidth = pin.thickness * 0.45f,
                cap = StrokeCap.Round
            )

            // Specular edge
            drawLine(
                color = Color(0xFFE2E8F0),
                start = start - Offset(0f, pin.thickness * 0.2f),
                end = end - Offset(0f, pin.thickness * 0.2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // 3. Tactile Pull Ring Handle
            drawCircle(
                color = Color(0x66000000),
                radius = 26f,
                center = handle + Offset(0f, 6f)
            )
            drawCircle(
                color = OneMoveVisualTheme.Pins.satinBrassRing,
                radius = 26f,
                center = handle
            )
            drawCircle(
                color = Color(0xFF78350F),
                radius = 26f,
                center = handle,
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 16f,
                center = handle
            )

            // 4. Distinct Tactical Geometric Emblems:
            // PIN A: Diamond
            // PIN B: Square
            // PIN C: Circle
            // PIN D: Triangle
            val emblemColor = pin.color
            when (pin.id) {
                PinId.PIN_A -> {
                    // Diamond
                    val path = Path().apply {
                        moveTo(handle.x, handle.y - 9f)
                        lineTo(handle.x + 9f, handle.y)
                        lineTo(handle.x, handle.y + 9f)
                        lineTo(handle.x - 9f, handle.y)
                        close()
                    }
                    drawPath(path, emblemColor)
                }
                PinId.PIN_B -> {
                    // Square
                    drawRect(
                        color = emblemColor,
                        topLeft = Offset(handle.x - 7f, handle.y - 7f),
                        size = androidx.compose.ui.geometry.Size(14f, 14f)
                    )
                }
                PinId.PIN_C -> {
                    // Circle
                    drawCircle(color = emblemColor, radius = 7.5f, center = handle)
                }
                PinId.PIN_D -> {
                    // Triangle
                    val path = Path().apply {
                        moveTo(handle.x, handle.y - 9f)
                        lineTo(handle.x + 8f, handle.y + 7f)
                        lineTo(handle.x - 8f, handle.y + 7f)
                        close()
                    }
                    drawPath(path, emblemColor)
                }
            }
        }
    }
}
