package com.example.onemove.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Tactile miniature-machine chassis. The level geometry remains the gameplay truth;
 * this layer only gives each chapter a stronger physical cabinet and lighting mood.
 */
object ToyBoxBackgroundRenderer {
    private data class Mood(
        val accent: Color,
        val secondary: Color,
        val surfaceTop: Color,
        val surfaceBottom: Color
    )

    private fun moodFor(levelNumber: Int) = when (levelNumber) {
        in 1..3 -> Mood(Color(0xFFF59E0B), Color(0xFF22D3EE), Color(0xFF173047), Color(0xFF08131F))
        in 4..6 -> Mood(Color(0xFF22D3EE), Color(0xFF34D399), Color(0xFF12384A), Color(0xFF071720))
        in 7..9 -> Mood(Color(0xFFFB7185), Color(0xFFFBBF24), Color(0xFF3A2438), Color(0xFF160B17))
        else -> Mood(Color(0xFFA78BFA), Color(0xFFF472B6), Color(0xFF312750), Color(0xFF0E0A1D))
    }

    fun drawBackingChassis(drawScope: DrawScope, width: Float, height: Float, levelNumber: Int = 1) {
        with(drawScope) {
            val mood = moodFor(levelNumber)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF030712), Color(0xFF07111E), Color(0xFF02050B)),
                    startY = 0f, endY = height
                ),
                size = Size(width, height)
            )

            val bayTopLeft = Offset(22f, 22f)
            val baySize = Size(width - 44f, height - 44f)
            val bayRadius = CornerRadius(44f, 44f)

            // Deep cabinet shadow and the raised enamel play surface.
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.62f),
                topLeft = bayTopLeft + Offset(0f, 18f), size = baySize,
                cornerRadius = bayRadius
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(mood.surfaceTop, mood.surfaceBottom),
                    startY = bayTopLeft.y, endY = height - 22f
                ),
                topLeft = bayTopLeft, size = baySize, cornerRadius = bayRadius
            )

            // Two soft work-lamp pools make the board feel dimensional without
            // pretending to be collision geometry.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(mood.accent.copy(alpha = 0.23f), Color.Transparent),
                    center = Offset(width * 0.23f, height * 0.20f), radius = width * 0.82f
                ),
                radius = width * 0.82f,
                center = Offset(width * 0.23f, height * 0.20f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(mood.secondary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(width * 0.82f, height * 0.70f), radius = width * 0.68f
                ),
                radius = width * 0.68f,
                center = Offset(width * 0.82f, height * 0.70f)
            )

            // Lower service deck. It creates a foreground plane instead of one flat void.
            val deckTop = height * 0.68f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, mood.secondary.copy(alpha = 0.09f)),
                    startY = deckTop, endY = height - 48f
                ),
                topLeft = Offset(54f, deckTop),
                size = Size(width - 108f, height - deckTop - 54f),
                cornerRadius = CornerRadius(30f, 30f)
            )
            drawLine(
                color = mood.accent.copy(alpha = 0.22f),
                start = Offset(82f, deckTop + 2f), end = Offset(width - 82f, deckTop + 2f),
                strokeWidth = 2.5f, cap = StrokeCap.Round
            )

            // Recessed panel seams and peg points. The low contrast keeps them from
            // competing with actual rails or pins.
            val seam = mood.secondary.copy(alpha = 0.08f)
            var py = 170f
            while (py < height - 140f) {
                drawLine(seam, Offset(54f, py), Offset(width - 54f, py), 1.2f)
                py += 150f
            }
            var px = 76f
            while (px < width - 60f) {
                var dotY = 92f
                while (dotY < height - 70f) {
                    drawCircle(Color.White.copy(alpha = 0.045f), 2.4f, Offset(px, dotY))
                    dotY += 112f
                }
                px += 112f
            }

            // Side conduits sell the miniature-machine silhouette while staying
            // outside the active puzzle space.
            for (x in listOf(43f, width - 43f)) {
                drawLine(Color(0xFF020617).copy(alpha = 0.72f), Offset(x, 132f), Offset(x, height - 132f), 14f, StrokeCap.Round)
                drawLine(mood.accent.copy(alpha = 0.48f), Offset(x, 180f), Offset(x, height * 0.39f), 3.5f, StrokeCap.Round)
                drawLine(mood.secondary.copy(alpha = 0.34f), Offset(x, height * 0.60f), Offset(x, height - 180f), 3.5f, StrokeCap.Round)
            }

            // Layered cabinet rim: dark extrusion, satin metal, chapter accent.
            drawRoundRect(
                color = Color(0xFF020617), topLeft = bayTopLeft, size = baySize,
                cornerRadius = bayRadius, style = Stroke(width = 12f)
            )
            drawRoundRect(
                color = Color(0xFF6B4A2A), topLeft = bayTopLeft + Offset(7f, 7f),
                size = Size(baySize.width - 14f, baySize.height - 14f),
                cornerRadius = CornerRadius(37f, 37f), style = Stroke(width = 3f)
            )
            drawRoundRect(
                color = mood.accent.copy(alpha = 0.72f), topLeft = bayTopLeft + Offset(11f, 11f),
                size = Size(baySize.width - 22f, baySize.height - 22f),
                cornerRadius = CornerRadius(33f, 33f), style = Stroke(width = 2.2f)
            )

            val screwPositions = listOf(
                Offset(52f, 52f), Offset(width - 52f, 52f),
                Offset(52f, height - 52f), Offset(width - 52f, height - 52f)
            )
            for (pos in screwPositions) {
                drawCircle(Color.Black.copy(alpha = 0.45f), 11f, pos + Offset(0f, 3f))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFE3A3), Color(0xFFC47723), Color(0xFF5F3419)),
                        center = pos - Offset(3f, 4f), radius = 15f
                    ),
                    radius = 10f, center = pos
                )
                drawLine(Color(0xFF4A2B17), pos - Offset(5f, 0f), pos + Offset(5f, 0f), 1.8f, StrokeCap.Round)
            }
        }
    }
}
