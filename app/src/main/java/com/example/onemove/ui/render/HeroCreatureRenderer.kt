package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureExpression
import com.example.onemove.model.CreatureId
import com.example.onemove.model.Vector2D
import com.example.onemove.ui.theme.OneMoveVisualTheme

object HeroCreatureRenderer {

    fun drawCreature(drawScope: DrawScope, creature: Creature) {
        drawCreature(drawScope, creature, Offset(creature.position.x, creature.position.y))
    }

    fun drawCreature(drawScope: DrawScope, creature: Creature, renderPos: Vector2D) {
        drawCreature(drawScope, creature, Offset(renderPos.x, renderPos.y))
    }

    fun drawCreature(drawScope: DrawScope, creature: Creature, center: Offset) {
        with(drawScope) {
            val rad = creature.radius * 1.15f

            // 1. Ground Drop Shadow
            drawOval(
                color = Color(0x55000000),
                topLeft = Offset(center.x - rad * 0.9f, center.y + rad * 0.65f),
                size = Size(rad * 1.8f, rad * 0.45f)
            )

            // 2. Creature Body by Mascot ID
            when (creature.id) {
                CreatureId.PIP -> {
                    // Amber cat with pointed ears
                    // Ears
                    val earLeft = Path().apply {
                        moveTo(center.x - rad * 0.7f, center.y - rad * 0.4f)
                        lineTo(center.x - rad * 0.85f, center.y - rad * 1.1f)
                        lineTo(center.x - rad * 0.25f, center.y - rad * 0.75f)
                        close()
                    }
                    val earRight = Path().apply {
                        moveTo(center.x + rad * 0.7f, center.y - rad * 0.4f)
                        lineTo(center.x + rad * 0.85f, center.y - rad * 1.1f)
                        lineTo(center.x + rad * 0.25f, center.y - rad * 0.75f)
                        close()
                    }
                    drawPath(earLeft, OneMoveVisualTheme.Creatures.pipBody)
                    drawPath(earRight, OneMoveVisualTheme.Creatures.pipBody)

                    // Inner pink ear flaps
                    val innerEarLeft = Path().apply {
                        moveTo(center.x - rad * 0.65f, center.y - rad * 0.5f)
                        lineTo(center.x - rad * 0.78f, center.y - rad * 0.95f)
                        lineTo(center.x - rad * 0.35f, center.y - rad * 0.75f)
                        close()
                    }
                    drawPath(innerEarLeft, Color(0xFFF472B6))

                    // Round Body
                    drawCircle(OneMoveVisualTheme.Creatures.pipBody, radius = rad, center = center)
                    drawCircle(Color(0xFFB45309), radius = rad, center = center, style = Stroke(width = 2.5f))

                    // Cream belly
                    drawOval(
                        color = Color(0xFFFEF3C7),
                        topLeft = Offset(center.x - rad * 0.55f, center.y),
                        size = Size(rad * 1.1f, rad * 0.85f)
                    )
                }

                CreatureId.MOCHI -> {
                    // Mint jelly with bobbly antennae
                    val antPath = Path().apply {
                        moveTo(center.x, center.y - rad * 0.85f)
                        lineTo(center.x, center.y - rad * 1.25f)
                    }
                    drawPath(antPath, Color(0xFF047857), style = Stroke(width = 3.5f))
                    drawCircle(Color(0xFF34D399), radius = 6.5f, center = Offset(center.x, center.y - rad * 1.25f))

                    // Body
                    drawCircle(OneMoveVisualTheme.Creatures.mochiBody, radius = rad, center = center)
                    drawCircle(Color(0xFF065F46), radius = rad, center = center, style = Stroke(width = 2.5f))

                    // Freckles
                    drawCircle(Color(0xFF047857), radius = 2f, center = Offset(center.x - rad * 0.45f, center.y + rad * 0.1f))
                    drawCircle(Color(0xFF047857), radius = 2f, center = Offset(center.x + rad * 0.45f, center.y + rad * 0.1f))
                }

                CreatureId.BLOBBO -> {
                    // Coral bean with head sprout
                    val sproutPath = Path().apply {
                        moveTo(center.x, center.y - rad * 0.9f)
                        cubicTo(center.x - 12f, center.y - rad * 1.2f, center.x - 8f, center.y - rad * 1.35f, center.x - 16f, center.y - rad * 1.3f)
                    }
                    drawPath(sproutPath, Color(0xFF10B981), style = Stroke(width = 3f))

                    drawCircle(OneMoveVisualTheme.Creatures.blobboBody, radius = rad, center = center)
                    drawCircle(Color(0xFFBE123C), radius = rad, center = center, style = Stroke(width = 2.5f))

                    // Golden cheek blush
                    drawCircle(Color(0xFFFBBF24), radius = 4f, center = Offset(center.x - rad * 0.55f, center.y + rad * 0.15f))
                    drawCircle(Color(0xFFFBBF24), radius = 4f, center = Offset(center.x + rad * 0.55f, center.y + rad * 0.15f))
                }
            }

            // 3. Facial Expression
            drawFace(this, center, rad, creature.expression)
        }
    }

    private fun drawFace(drawScope: DrawScope, center: Offset, rad: Float, expression: CreatureExpression) {
        with(drawScope) {
            val eyeOffsetY = -rad * 0.15f
            val eyeSpacing = rad * 0.35f

            when (expression) {
                CreatureExpression.HAPPY -> {
                    // Arched happy eyes
                    drawArc(
                        color = Color(0xFF0F172A),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - eyeSpacing - 6f, center.y + eyeOffsetY - 4f),
                        size = Size(12f, 8f),
                        style = Stroke(width = 2.5f)
                    )
                    drawArc(
                        color = Color(0xFF0F172A),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x + eyeSpacing - 6f, center.y + eyeOffsetY - 4f),
                        size = Size(12f, 8f),
                        style = Stroke(width = 2.5f)
                    )
                    // Smile
                    drawArc(
                        color = Color(0xFF0F172A),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - 7f, center.y + rad * 0.15f),
                        size = Size(14f, 10f),
                        style = Stroke(width = 2.5f)
                    )
                }

                CreatureExpression.SCARED, CreatureExpression.PANIC -> {
                    // Wide open eyes
                    drawCircle(Color.White, radius = 7f, center = Offset(center.x - eyeSpacing, center.y + eyeOffsetY))
                    drawCircle(Color.White, radius = 7f, center = Offset(center.x + eyeSpacing, center.y + eyeOffsetY))
                    drawCircle(Color(0xFF0F172A), radius = 3.5f, center = Offset(center.x - eyeSpacing, center.y + eyeOffsetY))
                    drawCircle(Color(0xFF0F172A), radius = 3.5f, center = Offset(center.x + eyeSpacing, center.y + eyeOffsetY))
                    // O-mouth
                    drawOval(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(center.x - 5f, center.y + rad * 0.15f),
                        size = Size(10f, 14f)
                    )
                }

                CreatureExpression.DIZZY -> {
                    // Spiral or X eyes
                    val ex1 = center.x - eyeSpacing
                    val ex2 = center.x + eyeSpacing
                    val ey = center.y + eyeOffsetY
                    drawLine(Color(0xFF0F172A), Offset(ex1 - 4f, ey - 4f), Offset(ex1 + 4f, ey + 4f), strokeWidth = 2.5f)
                    drawLine(Color(0xFF0F172A), Offset(ex1 - 4f, ey + 4f), Offset(ex1 + 4f, ey - 4f), strokeWidth = 2.5f)
                    drawLine(Color(0xFF0F172A), Offset(ex2 - 4f, ey - 4f), Offset(ex2 + 4f, ey + 4f), strokeWidth = 2.5f)
                    drawLine(Color(0xFF0F172A), Offset(ex2 - 4f, ey + 4f), Offset(ex2 + 4f, ey - 4f), strokeWidth = 2.5f)
                }

                else -> {
                    // Normal eyes
                    drawCircle(Color(0xFF0F172A), radius = 4f, center = Offset(center.x - eyeSpacing, center.y + eyeOffsetY))
                    drawCircle(Color(0xFF0F172A), radius = 4f, center = Offset(center.x + eyeSpacing, center.y + eyeOffsetY))
                    // Eye highlights
                    drawCircle(Color.White, radius = 1.5f, center = Offset(center.x - eyeSpacing - 1.5f, center.y + eyeOffsetY - 1.5f))
                    drawCircle(Color.White, radius = 1.5f, center = Offset(center.x + eyeSpacing - 1.5f, center.y + eyeOffsetY - 1.5f))
                    // Small smile
                    drawArc(
                        color = Color(0xFF0F172A),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - 5f, center.y + rad * 0.12f),
                        size = Size(10f, 6f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
