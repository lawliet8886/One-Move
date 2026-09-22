package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.onemove.model.*
import com.example.onemove.ui.theme.OneMoveVisualTheme

object HeroCreatureRenderer {
    fun drawCreature(scope: DrawScope, creature: Creature) = drawCreature(scope, creature, Offset(creature.position.x, creature.position.y))
    fun drawCreature(scope: DrawScope, creature: Creature, renderPos: Vector2D) = drawCreature(scope, creature, Offset(renderPos.x, renderPos.y))

    fun drawCreature(scope: DrawScope, creature: Creature, center: Offset) {
        with(scope) {
            // Body silhouette equals the physical radius. Ears/sprouts are cosmetic only.
            val r = creature.radius
            val color = when (creature.id) {
                CreatureId.PIP -> OneMoveVisualTheme.Creatures.pipBody
                CreatureId.MOCHI -> OneMoveVisualTheme.Creatures.mochiBody
                CreatureId.BLOBBO -> OneMoveVisualTheme.Creatures.blobboBody
            }
            drawOval(Color.Black.copy(alpha = 0.28f), center + Offset(-r*0.82f,r*0.70f), Size(r*1.64f,r*0.4f))
            when (creature.id) {
                CreatureId.PIP -> for (side in listOf(-1f,1f)) {
                    val ear=Path().apply {
                        moveTo(center.x+side*r*0.24f,center.y-r*0.64f)
                        lineTo(center.x+side*r*0.80f,center.y-r*1.12f)
                        lineTo(center.x+side*r*0.75f,center.y-r*0.25f);close()
                    }
                    drawPath(ear,lerp(color,Color.Black,0.13f))
                    drawLine(Color(0xFFF9A8D4),center+Offset(side*r*0.57f,-r*0.65f),center+Offset(side*r*0.74f,-r*0.95f),4f)
                }
                CreatureId.MOCHI -> {
                    drawLine(Color(0xFF047857),center+Offset(0f,-r*0.8f),center+Offset(0f,-r*1.24f),3.5f)
                    drawCircle(Color(0xFF6EE7B7),6f,center+Offset(0f,-r*1.24f))
                }
                CreatureId.BLOBBO -> {
                    val sprout=Path().apply {
                        moveTo(center.x,center.y-r*0.85f)
                        cubicTo(center.x-12f,center.y-r*1.2f,center.x-8f,center.y-r*1.32f,center.x-16f,center.y-r*1.28f)
                    }
                    drawPath(sprout,Color(0xFF34D399),style=Stroke(3.5f))
                }
            }
            drawCircle(Brush.radialGradient(listOf(lerp(color,Color.White,0.42f),color,lerp(color,Color.Black,0.28f)),
                center+Offset(-r*0.32f,-r*0.38f),r*1.65f),r,center)
            drawCircle(lerp(color,Color.Black,0.40f),r,center,style=Stroke(2f))
            if(creature.id==CreatureId.PIP) drawOval(Color(0xFFFFEAC0),center+Offset(-r*0.50f,r*0.02f),Size(r,r*0.77f))
            drawOval(Color.White.copy(alpha=0.27f),center+Offset(-r*0.55f,-r*0.70f),Size(r*0.52f,r*0.22f))
            for(side in listOf(-1f,1f)) drawOval(Color(0xFFFB7185).copy(alpha=0.40f),center+Offset(side*r*0.58f-4f,r*0.05f),Size(8f,5f))
            face(this,center,r,creature.expression)
        }
    }

    private fun face(scope: DrawScope, c: Offset, r: Float, expression: CreatureExpression) = with(scope) {
        val ink=Color(0xFF132037)
        val happy=expression==CreatureExpression.HAPPY
        val surprised=expression in listOf(CreatureExpression.SCARED,CreatureExpression.PANIC,CreatureExpression.SURPRISED)
        for(side in listOf(-1f,1f)) {
            val eye=c+Offset(side*r*0.34f,-r*0.14f)
            when {
                expression==CreatureExpression.DIZZY -> {
                    drawLine(ink,eye+Offset(-4f,-4f),eye+Offset(4f,4f),2.5f)
                    drawLine(ink,eye+Offset(-4f,4f),eye+Offset(4f,-4f),2.5f)
                }
                happy -> drawArc(ink,180f,180f,false,eye+Offset(-6f,-4f),Size(12f,8f),style=Stroke(2.5f))
                else -> {
                    if(surprised) drawCircle(Color.White,7f,eye)
                    drawCircle(ink,if(surprised)3.5f else 4.5f,eye)
                    drawCircle(Color.White,1.5f,eye+Offset(-1.5f,-1.5f))
                }
            }
        }
        if(surprised) drawOval(ink,c+Offset(-4f,r*0.18f),Size(8f,12f))
        else drawArc(ink,0f,180f,false,c+Offset(-6f,r*0.14f),Size(12f,8f),style=Stroke(2f))
    }
}
