package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.SpringBumper
import kotlin.math.atan2
import kotlin.math.sin

/** A compact three-coil cartridge; the arrow follows the actual launch normal. */
object SpringBumperRenderer {
    private val ink=Color(0xFF273C37)
    private val brass=Color(0xFFB3844F)
    private val brassLight=Color(0xFFF9D393)
    private val steel=Color(0xFFACC2BF)
    private val enamel=Color(0xFF258C91)
    fun drawSpringBumper(drawScope: DrawScope, bumper: SpringBumper) = with(drawScope) {
        val n=bumper.direction.normalized()
        val angle=Math.toDegrees(atan2(n.y.toDouble(),n.x.toDouble())).toFloat()+90f
        val w=bumper.width
        val h=bumper.restHeight*(1f-0.70f*bumper.compression.coerceIn(0f,1f))
        val half=w*0.5f
        withTransform({ translate(bumper.position.x,bumper.position.y); rotate(angle,pivot=Offset.Zero) }) {
            // Backplate stays behind the actual spring band, not an invented support.
            drawLine(Color(0x33203931),Offset(-half,7f),Offset(half,7f),22f,StrokeCap.Round)
            drawLine(ink,Offset(-half,0f),Offset(half,0f),18f,StrokeCap.Round)
            drawLine(brass,Offset(-half,0f),Offset(half,0f),12f,StrokeCap.Round)
            drawLine(brassLight,Offset(-half+4f,-4f),Offset(half-4f,-4f),2f,StrokeCap.Round)
            for(x in listOf(-w*0.43f,w*0.43f)) {
                drawCircle(ink,5f,Offset(x,0f)); drawCircle(brassLight,3f,Offset(x,-1f))
            }
            val coil=Path()
            val amplitude=minOf(12f,w*0.09f)
            // Three narrow real-looking coils replace the old wide flat zigzag.
            for(x in listOf(-w*0.28f,0f,w*0.28f)) {
                drawLine(ink.copy(alpha=0.28f),Offset(x,0f),Offset(x,-h),4f,StrokeCap.Round)
                coil.moveTo(x,-2f)
                for(i in 1..32) {
                    val t=i/32f
                    coil.lineTo(x+sin(t*Math.PI.toFloat()*8f)*amplitude,-2f-t*(h-4f))
                }
            }
            drawPath(coil,ink,style=Stroke(width=5.4f,cap=StrokeCap.Round))
            drawPath(coil,steel,style=Stroke(width=3.2f,cap=StrokeCap.Round))
            drawPath(coil,Color(0xFFEDF6DD),style=Stroke(width=1.0f,cap=StrokeCap.Round))
            val headColor=if(bumper.flashTimer>0f) Color(0xFF6BE1C6) else enamel
            drawLine(ink,Offset(-half,-h),Offset(half,-h),18f,StrokeCap.Round)
            drawLine(headColor,Offset(-half,-h),Offset(half,-h),12f,StrokeCap.Round)
            drawLine(Color(0xFFD8F4DD),Offset(-half+4f,-h-4f),Offset(half-4f,-h-4f),2.6f,StrokeCap.Round)
            for(x in listOf(-half+4f,half-4f)) {
                drawCircle(brass,6.5f,Offset(x,-h)); drawCircle(brassLight,3f,Offset(x-1f,-h-1f))
            }
            // The direction is a property of this machine, not an answer-key hint.
            val tip=Offset(0f,-h-78f); val tail=Offset(0f,-h-30f)
            val arrow=Color(0xFF367C75)
            drawLine(arrow,tail,tip,5f,StrokeCap.Round)
            drawLine(arrow,tip,tip+Offset(-13f,18f),5f,StrokeCap.Round)
            drawLine(arrow,tip,tip+Offset(13f,18f),5f,StrokeCap.Round)
            if(bumper.flashTimer>0f) {
                drawCircle(Color(0xAAABFFE0),10f,Offset(0f,-h),style=Stroke(3f))
            }
        }
    }
}
