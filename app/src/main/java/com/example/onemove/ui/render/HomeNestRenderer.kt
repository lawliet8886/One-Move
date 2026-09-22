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

        // Warm beacon and brass portal ring make the destination legible at gameplay scale.
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(alpha=0.23f+safe*0.06f),Color.Transparent),c,r*1.62f),r*1.62f,c)
        drawOval(Color.Black.copy(alpha=0.42f),c+Offset(-r*0.95f,r*0.63f),Size(r*1.9f,r*0.42f))
        val portalCenter=c+Offset(0f,-r*0.08f)
        drawCircle(Color(0xFF121B28),r*0.90f,portalCenter)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFDE68A).copy(alpha=0.18f),Color(0xFF102430),Color(0xFF07111B)),portalCenter,r*0.82f),r*0.82f,portalCenter)
        drawCircle(Color(0xFFD7A45F),r*0.90f,portalCenter,style=Stroke(9f))
        drawCircle(Color(0xFFFFE4A3).copy(alpha=0.72f),r*0.84f,portalCenter,style=Stroke(2.5f))

        val w=r*2.2f; val h=r*1.4f; val p=c-Offset(w/2,h/2)
        val canopy=Path().apply {
            moveTo(p.x,p.y+h);lineTo(p.x,p.y+h*0.35f)
            cubicTo(p.x,p.y,p.x+w,p.y,p.x+w,p.y+h*0.35f)
            lineTo(p.x+w,p.y+h);close()
        }
        drawPath(canopy,Brush.verticalGradient(listOf(Color(0xFFA96E3E),Color(0xFF40231A)),p.y,p.y+h))
        drawPath(canopy,Color(0xFFE8BD7D),style=Stroke(4.5f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF294854),Color(0xFF10202C))),p+Offset(14f,h*0.35f),Size(w-28f,h*0.60f),CornerRadius(24f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF69A28F),Color(0xFF245649))),p+Offset(15f,h*0.70f),Size(w-30f,h*0.29f),CornerRadius(15f))
        for(i in 1..2) drawLine(Color(0xFFB6DAC8).copy(alpha=0.44f),p+Offset(w*i/3,h*0.73f),p+Offset(w*i/3,h*0.97f),2f)
    }

    fun drawForegroundLayer(scope: DrawScope, goalZone: GoalZone, creatures: List<Creature>, simulationTime: Float, state: SimulationState) = with(scope) {
        val c=Offset(goalZone.center.x,goalZone.center.y);val r=goalZone.radius
        val safe=creatures.count{it.isInsideGoal}
        drawLine(Color(0xFFFFD99B),c+Offset(-r*1.04f,r*0.69f),c+Offset(r*1.04f,r*0.69f),4.5f)
        for(i in 0..2) {
            val p=c+Offset((i-1)*34f,-r*0.35f)
            drawCircle(Color.Black.copy(alpha=0.35f),11f,p+Offset(0f,2f))
            drawCircle(Color(0xFF111D2D),9f,p)
            drawCircle(if(i<safe) Color(0xFF6EE7B7) else Color(0xFF475569),6f,p)
            if(i<safe) drawCircle(Color(0xFFD1FAE5),2.2f,p-Offset(1.5f,1.5f))
        }
    }

    fun drawHomeNest(scope: DrawScope, goalZone: GoalZone, rescuedCount: Int = 0) {
        // Only status lamps use the count. No creature position is changed by rendering.
        val status=List(rescuedCount.coerceIn(0,3)){Creature(CreatureId.values()[it],goalZone.center,isInsideGoal=true)}
        drawBackLayer(scope,goalZone,status,0f,SimulationState.READY)
        drawForegroundLayer(scope,goalZone,status,0f,SimulationState.READY)
    }
}
