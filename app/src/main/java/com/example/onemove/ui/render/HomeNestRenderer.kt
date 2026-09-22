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

/** Decorative shelter only. Presentation never changes a creature's simulated position. */
object HomeNestRenderer {
    fun reset()=Unit
    object MascotVisualMetrics {
        fun getVisualBounds(id:CreatureId,radius:Float,center:Offset,scale:Float):RectF {
            val r=radius*scale
            return RectF(center.x-r,center.y-r*1.35f,center.x+r,center.y+r)
        }
    }
    fun getDockGroupInwardBias(goalZone:GoalZone)=Vector2D.Zero
    fun getCreaturePresentationScale(creature:Creature,goalZone:GoalZone,state:SimulationState)=1f
    /** Kept for historical art-export source compatibility; elapsed time cannot prove arrival. */
    fun getDockProgress(creatureId:CreatureId,simTime:Float)=0f
    fun getSanctuaryClosureProgress(state:SimulationState,simTime:Float)=if(state==SimulationState.SUCCESS) 1f else 0f
    fun getCreatureRenderPosition(creature:Creature,goalZone:GoalZone,creatures:List<Creature>,simulationTime:Float,state:SimulationState)=Offset(creature.position.x,creature.position.y)
    fun drawBackLayer(scope:DrawScope,goal:GoalZone,creatures:List<Creature>,simulationTime:Float,state:SimulationState) {
        with(scope) {
            val c=Offset(goal.center.x,goal.center.y);val r=goal.radius
            val count=creatures.count{it.isInsideGoal}
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(alpha=.18f+count*.07f),Color.Transparent),c,r*1.7f),r*1.7f,c)
            val arch=Path().apply{moveTo(c.x-r*1.1f,c.y+r*.65f);lineTo(c.x-r*1.1f,c.y-r*.2f);cubicTo(c.x-r*1.1f,c.y-r*1.15f,c.x+r*1.1f,c.y-r*1.15f,c.x+r*1.1f,c.y-r*.2f);lineTo(c.x+r*1.1f,c.y+r*.65f);close()}
            drawPath(arch,Brush.verticalGradient(listOf(Color(0xFF79543C),Color(0xFF2E2530)),c.y-r,c.y+r))
            drawPath(arch,Color(0xFFDAAF72),style=Stroke(5f))
            drawRoundRect(Color(0xFF503449),c+Offset(-r*.98f,r*.1f),Size(r*1.96f,r*.58f),CornerRadius(22f))
            drawRoundRect(Color(0xFFAA6071),c+Offset(-r*.96f,r*.47f),Size(r*1.92f,r*.17f),CornerRadius(12f))
            drawCircle(Color(0xFFFDE68A),14f,c-Offset(0f,r*.66f))
            drawCircle(Color(0xFF945B38),7f,c-Offset(0f,r*.66f))
        }
    }
    fun drawForegroundLayer(scope:DrawScope,goal:GoalZone,creatures:List<Creature>,simulationTime:Float,state:SimulationState) {
        with(scope) {
            val c=Offset(goal.center.x,goal.center.y);val r=goal.radius
            val count=creatures.count{it.isInsideGoal}
            repeat(3){i->
                val p=c+Offset((i-1)*44f,r*.83f)
                drawCircle(Color(0xFF0F2035),9f,p)
                drawCircle(if(i<count) Color(0xFF6EE7B7) else Color(0xFF425066),5f,p)
            }
        }
    }
    fun drawHomeNest(scope:DrawScope,goalZone:GoalZone,rescuedCount:Int=0) {
        val indicators=(0 until rescuedCount).map{Creature(CreatureId.PIP,goalZone.center,isInsideGoal=true)}
        drawBackLayer(scope,goalZone,indicators,0f,SimulationState.READY)
        drawForegroundLayer(scope,goalZone,indicators,0f,SimulationState.READY)
    }
}
