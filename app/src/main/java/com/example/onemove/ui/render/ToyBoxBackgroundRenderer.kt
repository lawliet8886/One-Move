package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.ui.theme.OneMoveVisualTheme

object ToyBoxBackgroundRenderer {
    fun drawBackingChassis(drawScope: DrawScope, width: Float, height: Float) {
        with(drawScope) {
            // 1. Radial gradient vignette
            val bgGradient = Brush.radialGradient(
                colors = listOf(
                    OneMoveVisualTheme.ToyShell.backgroundSlate,
                    OneMoveVisualTheme.ToyShell.deepMidnight,
                    Color(0xFF020617)
                ),
                center = Offset(width * 0.5f, height * 0.45f),
                radius = maxOf(width, height) * 0.85f
            )
            drawRect(brush = bgGradient, size = Size(width, height))

            // 2. Chassis frame border
            drawRect(
                color = OneMoveVisualTheme.ToyShell.enclosureFrame,
                size = Size(width, height),
                style = Stroke(width = 8f)
            )

            // 3. Satin brass inset rim
            drawRect(
                color = OneMoveVisualTheme.ToyShell.chassisBrass,
                topLeft = Offset(10f, 10f),
                size = Size(width - 20f, height - 20f),
                style = Stroke(width = 2.5f)
            )

            // 4. Modular peg dot grid
            val gridSpacing = 80f
            var px = 40f
            while (px < width) {
                var py = 40f
                while (py < height) {
                    drawCircle(
                        color = Color(0x22475569),
                        radius = 2.5f,
                        center = Offset(px, py)
                    )
                    py += gridSpacing
                }
                px += gridSpacing
            }

            // 5. Corner assembly screws
            val screwPad = 24f
            val screwPositions = listOf(
                Offset(screwPad, screwPad),
                Offset(width - screwPad, screwPad),
                Offset(screwPad, height - screwPad),
                Offset(width - screwPad, height - screwPad)
            )
            for (pos in screwPositions) {
                drawCircle(
                    color = OneMoveVisualTheme.ToyShell.brassScrew,
                    radius = 7f,
                    center = pos
                )
                drawCircle(
                    color = Color(0xFF78350F),
                    radius = 7f,
                    center = pos,
                    style = Stroke(width = 1.5f)
                )
                // Cross slot
                drawLine(
                    color = Color(0xFF451A03),
                    start = Offset(pos.x - 4f, pos.y),
                    end = Offset(pos.x + 4f, pos.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF451A03),
                    start = Offset(pos.x, pos.y - 4f),
                    end = Offset(pos.x, pos.y + 4f),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}
