package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.*

/** Procedural mascots: body radii match collision radii; ears and highlights are decorative. */
object HeroCreatureRenderer {
    fun drawCreature(scope:DrawScope,c:Creature)=drawCreature(scope,c,Offset(c.position.x,c.position.y))
    fun drawCreature(scope:DrawScope,c:Creature,p:Vector2D)=drawCreature(scope,c,Offset(p.x,p.y))
    fun drawCreature(scope:DrawScope,c:Creature,center:Offset) {
        with(scope) {
            val r=c.radius
            val colors=when(c.id) {
                CreatureId.PIP->listOf(Color(0xFFFFEBAD),Color(0xFFFBBF24),Color(0xFFD97706))
                CreatureId.MOCHI->listOf(Color(0xFFC9FFE8),Color(0xFF52DDA8),Color(0xFF059669))
                CreatureId.BLOBBO->listOf(Color(0xFFFFD4E0),Color(0xFFFB7185),Color(0xFFDB2777))
            }
            drawOval(Color(0x66000000),center+Offset(-r*.88f,r*.7f),Size(r*1.76f,r*.38f))
            if(c.id==CreatureId.PIP) for(side in listOf(-1f,1f)) {
                val ear=Path().apply{moveTo(center.x+side*r*.18f,center.y-r*.65f);lineTo(center.x+side*r*.76f,center.y-r*1.17f);lineTo(center.x+side*r*.8f,center.y-r*.3f);close()}
                drawPath(ear,colors[1]);drawPath(ear,colors[2],style=Stroke(2f))
                drawLine(Color(0xFFFDA4AF),center+Offset(side*r*.63f,-r*.91f),center+Offset(side*r*.66f,-r*.58f),4f,StrokeCap.Round)
            }
            if(c.id==CreatureId.MOCHI) {
                drawLine(colors[2],center+Offset(0f,-r*.8f),center+Offset(r*.1f,-r*1.25f),4f,StrokeCap.Round)
                drawCircle(colors[0],r*.13f,center+Offset(r*.1f,-r*1.25f))
            }
            if(c.id==CreatureId.BLOBBO) {
                drawLine(Color(0xFF059669),center+Offset(0f,-r*.8f),center+Offset(0f,-r*1.2f),3f,StrokeCap.Round)
                drawOval(Color(0xFF6EE7B7),center+Offset(0f,-r*1.32f),Size(r*.42f,r*.2f))
            }
            drawCircle(Brush.radialGradient(colors,center-Offset(r*.35f,r*.45f),r*1.6f),r,center)
            drawCircle(colors[2],r,center,style=Stroke(2f))
            drawOval(colors[0].copy(alpha=.55f),center+Offset(-r*.54f,r*.12f),Size(r*1.08f,r*.65f))
            drawOval(Color.White.copy(alpha=.28f),center+Offset(-r*.56f,-r*.72f),Size(r*.64f,r*.2f))
            val eyes=Color(0xFF14243B)
            val happy=c.expression==CreatureExpression.HAPPY
            val dizzy=c.expression==CreatureExpression.DIZZY
            val startled=c.expression in listOf(CreatureExpression.PANIC,CreatureExpression.SCARED,CreatureExpression.SURPRISED)
            for(side in listOf(-1f,1f)) {
                val e=center+Offset(side*r*.34f,-r*.15f)
                if(happy) drawArc(eyes,190f,160f,false,e-Offset(r*.13f,r*.06f),Size(r*.26f,r*.2f),style=Stroke(3f,cap=StrokeCap.Round))
                else if(dizzy) {
                    drawLine(eyes,e-Offset(4f,4f),e+Offset(4f,4f),2.5f,StrokeCap.Round)
                    drawLine(eyes,e-Offset(4f,-4f),e+Offset(4f,-4f),2.5f,StrokeCap.Round)
                } else {
                    drawOval(eyes,e-Offset(r*.11f,r*.15f),Size(r*.22f,r*.3f))
                    drawCircle(Color.White,r*.045f,e-Offset(r*.03f,r*.06f))
                }
                drawOval(Color(0xFFFDA4AF).copy(alpha=.65f),center+Offset(side*r*.55f-r*.09f,r*.1f),Size(r*.18f,r*.1f))
            }
            if(startled) drawOval(eyes,center+Offset(-r*.09f,r*.13f),Size(r*.18f,r*.24f))
            else drawArc(eyes,0f,180f,false,center+Offset(-r*.13f,r*.13f),Size(r*.26f,r*.16f),style=Stroke(2.5f,cap=StrokeCap.Round))
        }
    }
}
