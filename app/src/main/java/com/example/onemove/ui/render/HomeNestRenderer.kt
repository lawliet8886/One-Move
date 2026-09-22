package com.example.onemove.ui.render

import android.graphics.RectF
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureId
import com.example.onemove.model.GoalZone
import com.example.onemove.model.Vector2D
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.theme.OneMoveVisualTheme

object HomeNestRenderer {
    private var dockingAnimationProgress = 0f

    fun reset() {
        dockingAnimationProgress = 0f
    }

    object MascotVisualMetrics {
        fun getVisualBounds(id: CreatureId, radius: Float, center: Offset, scale: Float): RectF {
            val r = radius * scale
            val topExtra = when (id) {
                CreatureId.PIP -> r * 0.15f
                CreatureId.MOCHI -> r * 0.25f
                CreatureId.BLOBBO -> r * 0.20f
            }
            return RectF(
                center.x - r,
                center.y - r - topExtra,
                center.x + r,
                center.y + r
            )
        }
    }

    fun getDockGroupInwardBias(goalZone: GoalZone): Vector2D {
        return Vector2D(0f, 0f)
    }

    fun getCreaturePresentationScale(creature: Creature, goalZone: GoalZone, state: SimulationState): Float {
        return 1.0f
    }

    fun getDockProgress(creatureId: CreatureId, simTime: Float): Float {
        return when (creatureId) {
            CreatureId.PIP -> ((simTime - 0.20f) / 0.35f).coerceIn(0f, 1f)
            CreatureId.MOCHI -> ((simTime - 0.55f) / 0.35f).coerceIn(0f, 1f)
            CreatureId.BLOBBO -> ((simTime - 0.90f) / 0.35f).coerceIn(0f, 1f)
        }
    }

    fun getSanctuaryClosureProgress(state: SimulationState, simTime: Float): Float {
        return if (state == SimulationState.SUCCESS) 1.0f else 0.0f
    }

    fun getCreatureRenderPosition(
        creature: Creature,
        goalZone: GoalZone,
        creatures: List<Creature>,
        simulationTime: Float,
        state: SimulationState
    ): Offset {
        val center = goalZone.center
        val berthOffset = when (creature.id) {
            CreatureId.PIP -> -56f
            CreatureId.MOCHI -> 0f
            CreatureId.BLOBBO -> 56f
        }
        val targetPos = Offset(center.x + berthOffset, center.y + 12f)
        val progress = getDockProgress(creature.id, simulationTime)

        return if (creature.isInsideGoal || progress >= 1.0f) {
            targetPos
        } else if (progress > 0f) {
            val currentPhys = Offset(creature.position.x, creature.position.y)
            Offset(
                currentPhys.x + (targetPos.x - currentPhys.x) * progress,
                currentPhys.y + (targetPos.y - currentPhys.y) * progress
            )
        } else {
            Offset(creature.position.x, creature.position.y)
        }
    }

    fun drawBackLayer(
        drawScope: DrawScope,
        goalZone: GoalZone,
        creatures: List<Creature>,
        simulationTime: Float,
        state: SimulationState
    ) {
        with(drawScope) {
            val center = Offset(goalZone.center.x, goalZone.center.y)
            val rad = goalZone.radius
            val rescuedCount = creatures.count { it.isInsideGoal }

            // 1. Ambient Glow
            val glowRadius = rad * 1.5f
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    OneMoveVisualTheme.Sanctuary.glowAmber.copy(alpha = 0.35f + rescuedCount * 0.12f),
                    Color(0x00000000)
                ),
                center = center,
                radius = glowRadius
            )
            drawCircle(brush = glowBrush, radius = glowRadius, center = center)

            // 2. Shelter Canopy
            val nestW = rad * 2.2f
            val nestH = rad * 1.4f
            val nestTopLeft = Offset(center.x - nestW * 0.5f, center.y - nestH * 0.5f)

            val canopyPath = Path().apply {
                moveTo(nestTopLeft.x, nestTopLeft.y + nestH)
                lineTo(nestTopLeft.x, nestTopLeft.y + nestH * 0.35f)
                cubicTo(
                    nestTopLeft.x, nestTopLeft.y,
                    nestTopLeft.x + nestW, nestTopLeft.y,
                    nestTopLeft.x + nestW, nestTopLeft.y + nestH * 0.35f
                )
                lineTo(nestTopLeft.x + nestW, nestTopLeft.y + nestH)
                close()
            }
            drawPath(canopyPath, OneMoveVisualTheme.Sanctuary.woodCanopy)
            drawPath(canopyPath, OneMoveVisualTheme.Sanctuary.woodTrim, style = Stroke(width = 4f))

            // 3. Velvet Bed
            val sofaH = nestH * 0.45f
            val sofaY = nestTopLeft.y + nestH - sofaH
            drawRoundRect(
                color = OneMoveVisualTheme.Sanctuary.velvetCouch,
                topLeft = Offset(nestTopLeft.x + 12f, sofaY),
                size = Size(nestW - 24f, sofaH),
                cornerRadius = CornerRadius(16f, 16f)
            )
        }
    }

    fun drawForegroundLayer(
        drawScope: DrawScope,
        goalZone: GoalZone,
        creatures: List<Creature>,
        simulationTime: Float,
        state: SimulationState
    ) {
        with(drawScope) {
            val center = Offset(goalZone.center.x, goalZone.center.y)
            val rad = goalZone.radius
            val nestW = rad * 2.2f
            val nestH = rad * 1.4f
            val nestTopLeft = Offset(center.x - nestW * 0.5f, center.y - nestH * 0.5f)
            val rescuedCount = creatures.count { it.isInsideGoal }

            // Foreground Rim & Crest
            val sofaH = nestH * 0.45f
            val sofaY = nestTopLeft.y + nestH - sofaH
            drawRoundRect(
                color = OneMoveVisualTheme.Sanctuary.couchLight,
                topLeft = Offset(nestTopLeft.x + 16f, sofaY + 4f),
                size = Size(nestW - 32f, sofaH - 8f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2.5f)
            )

            // Mascot Victory Crest
            drawCircle(
                color = OneMoveVisualTheme.Sanctuary.mascotCrest,
                radius = 14f,
                center = Offset(center.x, nestTopLeft.y + nestH * 0.2f)
            )

            // LED Status Wells
            val dockSpacing = 56f
            for (i in 0 until 3) {
                val dx = center.x + (i - 1) * dockSpacing
                val dy = center.y + nestH * 0.15f
                val isDocked = i < rescuedCount
                val ledColor = if (isDocked) Color(0xFF34D399) else Color(0xFF334155)
                drawCircle(color = ledColor, radius = 7f, center = Offset(dx, dy))
                drawCircle(color = Color(0xFFFEF3C7), radius = 2.5f, center = Offset(dx, dy))
            }
        }
    }

    fun drawHomeNest(drawScope: DrawScope, goalZone: GoalZone, rescuedCount: Int = 0) {
        val dummyList = (0 until rescuedCount).map {
            Creature(CreatureId.PIP, goalZone.center, isInsideGoal = true)
        }
        drawBackLayer(drawScope, goalZone, dummyList, 0f, SimulationState.READY)
        drawForegroundLayer(drawScope, goalZone, dummyList, 0f, SimulationState.READY)
    }
}
