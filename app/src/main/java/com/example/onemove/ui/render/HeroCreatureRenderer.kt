package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.onemove.model.*

/** Bundled plush sprites in Android; vector fallback for isolated preview tests. */
object HeroCreatureRenderer {
    fun drawCreature(scope: DrawScope, creature: Creature) = drawCreature(scope,creature,Offset(creature.position.x,creature.position.y))
    fun drawCreature(scope: DrawScope, creature: Creature, renderPos: Vector2D) = drawCreature(scope,creature,Offset(renderPos.x,renderPos.y))
    fun drawCreature(scope: DrawScope, creature: Creature, center: Offset) = with(scope) {
        if(SpriteMascotRenderer.draw(scope,creature,center)) return@with
        val r=creature.radius
        val base=when(creature.id) {
            CreatureId.PIP -> Color(0xFFE9A242)
            CreatureId.MOCHI -> Color(0xFF65B899)
            CreatureId.BLOBBO -> Color(0xFFDF7E9C)
        }
        val dark=lerp(base,Color(0xFF24352F),0.48f)
        val cream=Color(0xFFFFECD1)
        drawOval(Color(0xFF21392E).copy(alpha=0.18f),center+Offset(-r*0.9f,r*0.67f),Size(r*1.8f,r*0.5f))
        when(creature.id) {
            CreatureId.PIP -> for(side in listOf(-1f,1f)) {
                val ear=Path().apply {
                    moveTo(center.x+side*r*0.17f,center.y-r*0.62f)
                    quadraticBezierTo(center.x+side*r*0.45f,center.y-r*1.20f,center.x+side*r*0.81f,center.y-r*1.18f)
                    quadraticBezierTo(center.x+side*r*1.04f,center.y-r*0.60f,center.x+side*r*0.70f,center.y-r*0.29f);close()
                }
                drawPath(ear,dark)
                drawLine(cream,center+Offset(side*r*0.57f,-r*0.64f),center+Offset(side*r*0.76f,-r*1.00f),r*0.15f,StrokeCap.Round)
            }
            CreatureId.MOCHI -> {
                val stem=center+Offset(0f,-r*0.91f)
                for(side in listOf(-1f,1f)) {
                    val leaf=Path().apply {
                        moveTo(stem.x,stem.y)
                        cubicTo(stem.x+side*r*0.50f,stem.y-r*0.70f,stem.x+side*r*0.65f,stem.y-r*0.17f,stem.x,stem.y);close()
                    }
                    drawPath(leaf,if(side<0f) Color(0xFF406E4D) else Color(0xFFA4C969))
                }
            }
            CreatureId.BLOBBO -> for(side in listOf(-1f,1f)) {
                drawCircle(dark,r*0.27f,center+Offset(side*r*0.52f,-r*0.91f))
                drawCircle(cream,r*0.14f,center+Offset(side*r*0.52f,-r*0.91f))
            }
        }
        drawCircle(Brush.radialGradient(listOf(lerp(base,cream,0.43f),base,dark),center+Offset(-r*0.37f,-r*0.45f),r*1.65f),r,center)
        drawCircle(dark,r,center,style=Stroke(r*0.045f))
        if(creature.id==CreatureId.PIP) for(side in listOf(-1f,1f)) {
            val mask=Path().apply {
                moveTo(center.x+side*r*0.83f,center.y-r*0.15f)
                quadraticBezierTo(center.x+side*r*0.84f,center.y+r*0.55f,center.x,center.y+r*0.68f)
                quadraticBezierTo(center.x-side*r*0.02f,center.y+r*0.28f,center.x+side*r*0.83f,center.y-r*0.15f);close()
            }
            drawPath(mask,cream)
        } else drawOval(cream.copy(alpha=0.26f),center+Offset(-r*0.60f,r*0.08f),Size(r*1.2f,r*0.74f))
        for(side in listOf(-1f,1f)) {
            drawOval(lerp(base,cream,0.32f),center+Offset(side*r*0.48f-r*0.16f,r*0.70f),Size(r*0.32f,r*0.20f))
            drawOval(Color(0xFFB34F69).copy(alpha=0.40f),center+Offset(side*r*0.63f-r*0.13f,r*0.02f),Size(r*0.26f,r*0.16f))
        }
        drawArc(cream.copy(alpha=0.5f),205f,74f,false,center-Offset(r*0.77f,r*0.77f),Size(r*1.54f,r*1.54f),style=Stroke(r*0.06f))
        val ink=Color(0xFF26332F)
        val surprised=creature.expression in listOf(CreatureExpression.SCARED,CreatureExpression.PANIC,CreatureExpression.SURPRISED)
        for(side in listOf(-1f,1f)) {
            val eye=center+Offset(side*r*0.33f,-r*0.18f)
            when(creature.expression) {
                CreatureExpression.HAPPY -> drawArc(ink,190f,160f,false,eye-Offset(r*0.14f,r*0.08f),Size(r*0.28f,r*0.20f),style=Stroke(r*0.065f,cap=StrokeCap.Round))
                CreatureExpression.DIZZY -> {
                    drawLine(ink,eye-Offset(r*0.12f,r*0.10f),eye+Offset(r*0.12f,r*0.10f),r*0.065f,StrokeCap.Round)
                    drawLine(ink,eye+Offset(-r*0.12f,r*0.10f),eye+Offset(r*0.12f,-r*0.10f),r*0.065f,StrokeCap.Round)
                }
                else -> {
                    drawOval(cream,eye-Offset(r*0.19f,r*0.22f),Size(r*0.38f,r*0.43f))
                    drawOval(ink,eye-Offset(r*0.12f,r*0.17f),Size(r*0.24f,r*(if(surprised)0.37f else 0.32f)))
                    drawCircle(Color.White,r*0.053f,eye+Offset(-r*0.045f,-r*0.09f))
                }
            }
        }
        if(creature.id==CreatureId.PIP) drawOval(ink,center+Offset(-r*0.09f,r*0.12f),Size(r*0.18f,r*0.12f))
        if(surprised) drawOval(ink,center+Offset(-r*0.11f,r*0.30f),Size(r*0.22f,r*0.26f))
        else drawArc(ink,0f,180f,false,center+Offset(-r*0.16f,r*0.24f),Size(r*0.32f,r*0.19f),style=Stroke(r*0.047f,cap=StrokeCap.Round))
    }
}
