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

object WreckerAndStoneRenderer {
    fun drawHeavyWreckerBall(scope:DrawScope,ball:HeavyBall) {
        with(scope) {
            val c=Offset(ball.position.x,ball.position.y);val r=ball.radius
            drawOval(Color(0x66000000),c+Offset(-r*.95f,r*.7f),Size(r*1.9f,r*.45f))
            drawCircle(Brush.radialGradient(OneMoveVisualTheme.HeavyObjects.wreckerGradient,c-Offset(r*.35f,r*.35f),r*1.35f),r,c)
            drawCircle(Color(0xFF1E293B),r,c,style=Stroke(3f))
            withTransform({translate(c.x,c.y);rotate(Math.toDegrees(ball.rotation.toDouble()).toFloat(),pivot=Offset.Zero)}) {
                drawLine(Color(0xFFF59E0B),Offset(-r*.88f,0f),Offset(r*.88f,0f),9f,StrokeCap.Round)
                drawLine(Color(0xFFFEF3C7),Offset(-r*.88f,-1.8f),Offset(r*.88f,-1.8f),2.5f,StrokeCap.Round)
                drawCircle(Color(0xFFFEF3C7),3f,Offset(-r*.55f,0f));drawCircle(Color(0xFFFEF3C7),3f,Offset(r*.55f,0f))
                drawCircle(Color(0xFF1E293B),11f,Offset.Zero);drawCircle(Color(0xFFFDE68A),8f,Offset.Zero);drawCircle(Color(0xFFB45309),4f,Offset.Zero)
                drawCircle(Color(0xFFF59E0B),9f,Offset(0f,-r*.95f),style=Stroke(3.5f))
            }
            drawCircle(Color.White.copy(alpha=.55f),r*.22f,c-Offset(r*.38f,r*.38f))
        }
    }
    fun drawRollingStone(scope:DrawScope,stone:RollingStone) {
        with(scope) {
            val c=Offset(stone.position.x,stone.position.y);val r=stone.radius
            drawOval(Color(0x66000000),c+Offset(-r*.9f,r*.7f),Size(r*1.8f,r*.42f))
            drawCircle(Brush.radialGradient(OneMoveVisualTheme.HeavyObjects.stoneGradient,c-Offset(r*.3f,r*.3f),r*1.3f),r,c)
            drawCircle(Color(0xFF1E293B),r,c,style=Stroke(3f))
            withTransform({translate(c.x,c.y);rotate(Math.toDegrees(stone.rotation.toDouble()).toFloat(),pivot=Offset.Zero)}) {
                val mark=OneMoveVisualTheme.HeavyObjects.stoneCraterMark
                drawCircle(mark,r*.24f,Offset(-r*.35f,-r*.2f));drawCircle(mark,r*.18f,Offset(r*.35f,-r*.1f));drawCircle(mark,r*.22f,Offset(0f,r*.4f))
                drawCircle(Color.White.copy(alpha=.35f),2.5f,Offset(-r*.35f,-r*.25f));drawCircle(Color.White.copy(alpha=.35f),2.5f,Offset(r*.35f,-r*.15f))
            }
        }
    }
}
