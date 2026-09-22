package com.example.onemove.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.DangerPit
import com.example.onemove.ui.theme.OneMoveVisualTheme

/**
 * Official Danger Basin (Dead-End Containment Trap) Renderer for ONE MOVE: Midnight Kinetic Workshop.
 *
 * Visually communicates "WRONG PATH / TRAPPED CONTAINMENT" without violence:
 * - Recessed dark basin compartment with manufactured titanium depth bevels
 * - Sunken coral/red magma floor glow and containment grid
 * - Warning hazard diagonal hazard stripes at upper lip
 * - Playful muted hazard triangle warning emblem
 */
object DangerBasinRenderer {

    // Reusable Path caches for zero per-frame allocations
    private val reusableWarnPath = Path()

    fun drawDangerPit(drawScope: DrawScope, dangerPit: DangerPit) {
        with(drawScope) {
            val pitLeft = dangerPit.bounds.left
            val pitTop = dangerPit.bounds.top
            val pitWidth = dangerPit.bounds.width
            val pitHeight = dangerPit.bounds.height

            // 1. Recessed Dark Basin Floor (Depth gradient into dark void)
            val pitGradient = Brush.verticalGradient(
                colors = listOf(
                    OneMoveVisualTheme.Hazard.darkBasinTop,
                    Color(0xFF140608),
                    OneMoveVisualTheme.Hazard.darkBasinFloor
                ),
                startY = pitTop,
                endY = pitTop + pitHeight
            )
            drawRoundRect(
                brush = pitGradient,
                topLeft = Offset(pitLeft, pitTop),
                size = Size(pitWidth, pitHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 2. Soft Coral/Red Bottom Glow (Active trap containment)
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    OneMoveVisualTheme.Hazard.coralMagmaGlow,
                    Color(0x00000000)
                ),
                center = Offset(pitLeft + pitWidth * 0.5f, pitTop + pitHeight * 0.82f),
                radius = pitWidth * 0.70f
            )
            drawRoundRect(
                brush = glowBrush,
                topLeft = Offset(pitLeft, pitTop),
                size = Size(pitWidth, pitHeight),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 3. Inner Mesh Lattice Lines
            val gridStep = 24f
            var gy = pitTop + 24f
            while (gy < pitTop + pitHeight - 12f) {
                drawLine(
                    color = OneMoveVisualTheme.Hazard.coralRim.copy(alpha = 0.15f),
                    start = Offset(pitLeft + 8f, gy),
                    end = Offset(pitLeft + pitWidth - 8f, gy),
                    strokeWidth = 1.2f
                )
                gy += gridStep
            }

            // 4. Danger Hazard Striping at top entrance
            val stripeH = 14f
            val stripeCount = 6
            val segW = pitWidth / stripeCount
            for (i in 0 until stripeCount) {
                val stripeColor = if (i % 2 == 0) {
                    OneMoveVisualTheme.Hazard.coralRim.copy(alpha = 0.65f)
                } else {
                    OneMoveVisualTheme.Hazard.amberStripe.copy(alpha = 0.85f)
                }
                drawRect(
                    color = stripeColor,
                    topLeft = Offset(pitLeft + i * segW, pitTop),
                    size = Size(segW, stripeH)
                )
            }

            // 5. Beveled Containment Rim Border
            drawRoundRect(
                color = OneMoveVisualTheme.ToyShell.enclosureFrame,
                topLeft = Offset(pitLeft - 2f, pitTop - 2f),
                size = Size(pitWidth + 4f, pitHeight + 4f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3.0f)
            )
            drawRoundRect(
                color = OneMoveVisualTheme.Hazard.coralRim.copy(alpha = 0.75f),
                topLeft = Offset(pitLeft, pitTop),
                size = Size(pitWidth, pitHeight),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2.5f)
            )

            // 6. Playful Warning Triangle Emblem in Basin
            val centerPit = Offset(pitLeft + pitWidth * 0.5f, pitTop + pitHeight * 0.55f)
            reusableWarnPath.reset()
            reusableWarnPath.moveTo(centerPit.x, centerPit.y - 22f)
            reusableWarnPath.lineTo(centerPit.x + 20f, centerPit.y + 16f)
            reusableWarnPath.lineTo(centerPit.x - 20f, centerPit.y + 16f)
            reusableWarnPath.close()

            drawPath(reusableWarnPath, OneMoveVisualTheme.Hazard.warningIcon)
            drawPath(reusableWarnPath, OneMoveVisualTheme.Hazard.warningIconStroke, style = Stroke(width = 2.5f))

            // Warning exclamation mark
            drawLine(
                color = OneMoveVisualTheme.Hazard.warningIconStroke,
                start = Offset(centerPit.x, centerPit.y - 9f),
                end = Offset(centerPit.x, centerPit.y + 3f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = OneMoveVisualTheme.Hazard.warningIconStroke,
                radius = 2.0f,
                center = Offset(centerPit.x, centerPit.y + 9.5f)
            )
        }
    }
}
