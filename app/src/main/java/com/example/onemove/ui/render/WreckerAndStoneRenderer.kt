package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.HeavyBall
import com.example.onemove.model.RollingStone
import com.example.onemove.ui.theme.OneMoveVisualTheme

/**
 * Official Heavy Wrecker Ball and Granite Stone Renderer for ONE MOVE: Midnight Kinetic Workshop.
 *
 * Implements high-inertia physical obstacle weights:
 * - Wrecker Ball: Heavy cast iron sphere with satin brass equator belt, suspension shackle & mascot crest
 * - Rolling Stone: Chiseled granite boulder with crater facets and rotational textures
 */
object WreckerAndStoneRenderer {

    fun drawHeavyWreckerBall(drawScope: DrawScope, ball: HeavyBall) {
        with(drawScope) {
            val center = Offset(ball.position.x, ball.position.y)
            val rad = ball.radius
            val angleDeg = Math.toDegrees(ball.rotation.toDouble()).toFloat()

            // 1. Soft Heavy Drop Shadow
            drawOval(
                color = Color(0x66000000),
                topLeft = Offset(center.x - rad * 0.95f, center.y + rad * 0.70f),
                size = Size(rad * 1.9f, rad * 0.45f)
            )

            // 2. Cast Iron Sphere Body with Spherical Specular Depth
            val ironGradient = Brush.radialGradient(
                colors = OneMoveVisualTheme.HeavyObjects.wreckerGradient,
                center = center - Offset(rad * 0.35f, rad * 0.35f),
                radius = rad * 1.35f
            )
            drawCircle(brush = ironGradient, radius = rad, center = center)
            drawCircle(
                color = OneMoveVisualTheme.HeavyObjects.wreckerIronCore,
                radius = rad,
                center = center,
                style = Stroke(width = 3.0f)
            )

            // 3. Rotating Satin Brass Equator Ring & Emblem
            withTransform({
                translate(center.x, center.y)
                rotate(angleDeg)
            }) {
                // Brass equator belt
                drawLine(
                    color = OneMoveVisualTheme.HeavyObjects.wreckerGoldBelt,
                    start = Offset(-rad * 0.88f, 0f),
                    end = Offset(rad * 0.88f, 0f),
                    strokeWidth = 9.0f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = OneMoveVisualTheme.HeavyObjects.wreckerBeltHighlight,
                    start = Offset(-rad * 0.88f, -1.8f),
                    end = Offset(rad * 0.88f, -1.8f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Brass rivets on belt
                drawCircle(color = OneMoveVisualTheme.Brass.highlight, radius = 3.0f, center = Offset(-rad * 0.55f, 0f))
                drawCircle(color = OneMoveVisualTheme.Brass.highlight, radius = 3.0f, center = Offset(rad * 0.55f, 0f))

                // Center mascot crest on wrecker
                drawCircle(color = OneMoveVisualTheme.HeavyObjects.wreckerIronCore, radius = 11f, center = Offset.Zero)
                drawCircle(color = OneMoveVisualTheme.Brass.light, radius = 8f, center = Offset.Zero)
                drawCircle(color = OneMoveVisualTheme.Brass.dark, radius = 4f, center = Offset.Zero)

                // Top suspension shackle ring mount
                drawCircle(
                    color = OneMoveVisualTheme.Brass.mid,
                    radius = 9f,
                    center = Offset(0f, -rad * 0.95f),
                    style = Stroke(width = 3.5f)
                )
            }

            // 4. Glossy Specular Light Reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = rad * 0.22f,
                center = center - Offset(rad * 0.38f, rad * 0.38f)
            )
        }
    }

    fun drawRollingStone(drawScope: DrawScope, stone: RollingStone) {
        with(drawScope) {
            val center = Offset(stone.position.x, stone.position.y)
            val rad = stone.radius
            val angleDeg = Math.toDegrees(stone.rotation.toDouble()).toFloat()

            // 1. Soft Shadow
            drawOval(
                color = Color(0x66000000),
                topLeft = Offset(center.x - rad * 0.90f, center.y + rad * 0.70f),
                size = Size(rad * 1.8f, rad * 0.42f)
            )

            // 2. Chiseled Granite Boulder Body
            val stoneGradient = Brush.radialGradient(
                colors = OneMoveVisualTheme.HeavyObjects.stoneGradient,
                center = center - Offset(rad * 0.30f, rad * 0.30f),
                radius = rad * 1.30f
            )
            drawCircle(brush = stoneGradient, radius = rad, center = center)
            drawCircle(
                color = OneMoveVisualTheme.HeavyObjects.stoneDarkRim,
                radius = rad,
                center = center,
                style = Stroke(width = 3.0f)
            )

            // 3. Rotating Surface Crater Facets
            withTransform({
                translate(center.x, center.y)
                rotate(angleDeg)
            }) {
                val markColor = OneMoveVisualTheme.HeavyObjects.stoneCraterMark
                drawCircle(color = markColor, radius = rad * 0.24f, center = Offset(-rad * 0.35f, -rad * 0.20f))
                drawCircle(color = markColor, radius = rad * 0.18f, center = Offset(rad * 0.35f, -rad * 0.10f))
                drawCircle(color = markColor, radius = rad * 0.22f, center = Offset(0f, rad * 0.40f))

                // Highlight flecks
                drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 2.5f, center = Offset(-rad * 0.35f, -rad * 0.25f))
                drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 2.5f, center = Offset(rad * 0.35f, -rad * 0.15f))
            }
        }
    }
}
