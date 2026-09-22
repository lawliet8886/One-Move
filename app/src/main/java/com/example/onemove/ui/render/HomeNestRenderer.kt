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
import com.example.onemove.model.*
import com.example.onemove.physics.SimulationState

object HomeNestRenderer {
    fun reset() = Unit

    object MascotVisualMetrics {
        fun getVisualBounds(id: CreatureId, radius: Float, center: Offset, scale: Float): RectF {
            val r=radius*scale
            val extra=when(id) { CreatureId.PIP->0.12f; CreatureId.MOCHI->0.43f; CreatureId.BLOBBO->0.38f }
            return RectF(center.x-r,center.y-r*(1f+extra),center.x+r,center.y+r)
        }
    }

    fun getDockGroupInwardBias(goalZone: GoalZone) = Vector2D.Zero
    fun getCreaturePresentationScale(creature: Creature, goalZone: GoalZone, state: SimulationState) = 1f
    @Deprecated("Elapsed time cannot establish a rescue; read Creature.isInsideGoal")
    fun getDockProgress(creatureId: CreatureId, simTime: Float) = 0f
    fun getSanctuaryClosureProgress(state: SimulationState, simTime: Float) = if(state==SimulationState.SUCCESS) 1f else 0f
    fun getCreatureRenderPosition(creature: Creature, goalZone: GoalZone, creatures: List<Creature>,
        simulationTime: Float, state: SimulationState) = Offset(creature.position.x,creature.position.y)

    fun drawBackLayer(scope: DrawScope, goalZone: GoalZone, creatures: List<Creature>, simulationTime: Float, state: SimulationState) = with(scope) {
        val c=Offset(goalZone.center.x,goalZone.center.y)
        val r=goalZone.radius
        val safe=creatures.count{it.isInsideGoal}
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(alpha=0.15f+safe*0.055f),Color.Transparent),c,r*1.5f),r*1.5f,c)
        val w=r*2.2f; val h=r*1.4f; val p=c-Offset(w/2,h/2)
        val canopy=Path().apply {
            moveTo(p.x,p.y+h);lineTo(p.x,p.y+h*0.35f)
            cubicTo(p.x,p.y,p.x+w,p.y,p.x+w,p.y+h*0.35f)
            lineTo(p.x+w,p.y+h);close()
        }
        drawPath(canopy,Brush.verticalGradient(listOf(Color(0xFF915C36),Color(0xFF3F261E)),p.y,p.y+h))
        drawPath(canopy,Color(0xFFD3A66D),style=Stroke(4f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF253A45),Color(0xFF101E2B))),p+Offset(14f,h*0.35f),Size(w-28f,h*0.60f),CornerRadius(24f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF628A7D),Color(0xFF264F47))),p+Offset(15f,h*0.70f),Size(w-30f,h*0.29f),CornerRadius(15f))
        for(i in 1..2) drawLine(Color(0xFF92B4A3).copy(alpha=0.4f),p+Offset(w*i/3,h*0.73f),p+Offset(w*i/3,h*0.97f),2f)
    }

    fun drawForegroundLayer(scope: DrawScope, goalZone: GoalZone, creatures: List<Creature>, simulationTime: Float, state: SimulationState) = with(scope) {
        val c=Offset(goalZone.center.x,goalZone.center.y);val r=goalZone.radius
        val safe=creatures.count{it.isInsideGoal}
        drawLine(Color(0xFFE2B981),c+Offset(-r*1.04f,r*0.69f),c+Offset(r*1.04f,r*0.69f),4f)
        for(i in 0..2) {
            val p=c+Offset((i-1)*34f,-r*0.35f)
            drawCircle(Color(0xFF111D2D),9f,p)
            drawCircle(if(i<safe) Color(0xFF6EE7B7) else Color(0xFF475569),6f,p)
        }
    }

    fun drawHomeNest(scope: DrawScope, goalZone: GoalZone, rescuedCount: Int = 0) {
        // Only status lamps use the count. No creature position is changed by rendering.
        val status=List(rescuedCount.coerceIn(0,3)){Creature(CreatureId.values()[it],goalZone.center,isInsideGoal=true)}
        drawBackLayer(scope,goalZone,status,0f,SimulationState.READY)
        drawForegroundLayer(scope,goalZone,status,0f,SimulationState.READY)
    }
}
