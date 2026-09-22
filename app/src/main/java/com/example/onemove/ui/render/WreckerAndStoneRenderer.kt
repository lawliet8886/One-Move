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

/** Lit materials anchored to the exact circle used by the physics solver. */
object WreckerAndStoneRenderer {
    fun drawHeavyWreckerBall(drawScope: DrawScope, ball: HeavyBall) = with(drawScope) {
        val c = Offset(ball.position.x, ball.position.y)
        val r = ball.radius
        drawOval(Color(0x66000000), c + Offset(-r * 0.95f, r * 0.70f), Size(r * 1.9f, r * 0.45f))
        if (HardwareSpriteRenderer.draw(this,HardwareSpriteRenderer.Kind.WRECKER,c,r,ball.rotation)) return@with
        drawCircle(Brush.radialGradient(OneMoveVisualTheme.HeavyObjects.wreckerGradient,
            c - Offset(r * 0.35f, r * 0.35f), r * 1.35f), r, c)
        drawCircle(OneMoveVisualTheme.HeavyObjects.wreckerIronCore, r, c, style = Stroke(3f))
        withTransform({
            translate(c.x, c.y)
            // PhysicsWorld stores ball/stone rotation in degrees, unlike seesaw angles.
            rotate(ball.rotation, pivot = Offset.Zero)
        }) {
            drawLine(OneMoveVisualTheme.HeavyObjects.wreckerGoldBelt, Offset(-r * 0.88f, 0f), Offset(r * 0.88f, 0f), 9f, StrokeCap.Round)
            drawLine(OneMoveVisualTheme.HeavyObjects.wreckerBeltHighlight, Offset(-r * 0.88f, -1.8f), Offset(r * 0.88f, -1.8f), 2.5f, StrokeCap.Round)
            drawCircle(OneMoveVisualTheme.Brass.highlight, 3f, Offset(-r * 0.55f, 0f))
            drawCircle(OneMoveVisualTheme.Brass.highlight, 3f, Offset(r * 0.55f, 0f))
            drawCircle(OneMoveVisualTheme.HeavyObjects.wreckerIronCore, 11f, Offset.Zero)
            drawCircle(OneMoveVisualTheme.Brass.light, 8f, Offset.Zero)
            drawCircle(OneMoveVisualTheme.Brass.dark, 4f, Offset.Zero)
            drawCircle(OneMoveVisualTheme.Brass.mid, 9f, Offset(0f, -r * 0.95f), style = Stroke(3.5f))
        }
        drawCircle(Color.White.copy(alpha = 0.55f), r * 0.22f, c - Offset(r * 0.38f, r * 0.38f))
    }

    fun drawRollingStone(drawScope: DrawScope, stone: RollingStone) = with(drawScope) {
        val c = Offset(stone.position.x, stone.position.y)
        val r = stone.radius
        drawOval(Color(0x66000000), c + Offset(-r * 0.90f, r * 0.70f), Size(r * 1.8f, r * 0.42f))
        if (HardwareSpriteRenderer.draw(this,HardwareSpriteRenderer.Kind.STONE,c,r,stone.rotation)) return@with
        drawCircle(Brush.radialGradient(OneMoveVisualTheme.HeavyObjects.stoneGradient,
            c - Offset(r * 0.30f, r * 0.30f), r * 1.30f), r, c)
        drawCircle(OneMoveVisualTheme.HeavyObjects.stoneDarkRim, r, c, style = Stroke(3f))
        withTransform({
            translate(c.x, c.y)
            rotate(stone.rotation, pivot = Offset.Zero)
        }) {
            val mark = OneMoveVisualTheme.HeavyObjects.stoneCraterMark
            drawCircle(mark, r * 0.24f, Offset(-r * 0.35f, -r * 0.20f))
            drawCircle(mark, r * 0.18f, Offset(r * 0.35f, -r * 0.10f))
            drawCircle(mark, r * 0.22f, Offset(0f, r * 0.40f))
            drawCircle(Color.White.copy(alpha = 0.35f), 2.5f, Offset(-r * 0.35f, -r * 0.25f))
            drawCircle(Color.White.copy(alpha = 0.35f), 2.5f, Offset(r * 0.35f, -r * 0.15f))
        }
    }
}
