package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.SpringBumper

object SpringBumperRenderer {
    fun drawSpringBumper(scope:DrawScope,bumper:SpringBumper) {
        with(scope) {
            val pos=Offset(bumper.position.x,bumper.position.y)
            val normal=bumper.direction.normalized();val dir=Offset(normal.x,normal.y)
            val perp=Offset(-dir.y,dir.x);val w=bumper.width
            // Resting strike surface agrees with the solver's +18 world-unit normal offset.
            val h=18f*(1f-bumper.compression.coerceIn(0f,.9f))
            drawLine(Color(0xFF1E293B),pos-perp*(w*.5f),pos+perp*(w*.5f),10f,StrokeCap.Round)
            drawLine(Color(0xFF475569),pos-perp*(w*.5f),pos+perp*(w*.5f),6f,StrokeCap.Round)
            val coil=Path().apply {
                moveTo(pos.x,pos.y)
                repeat(4){i->
                    val sign=if(i%2==0) 1f else -1f
                    val mid=pos+dir*((i+.5f)*h/4f)+perp*(sign*w*.25f)
                    val end=pos+dir*((i+1)*h/4f)
                    lineTo(mid.x,mid.y);lineTo(end.x,end.y)
                }
            }
            drawPath(coil,Color(0xFF94A3B8),style=Stroke(3.5f))
            val head=pos+dir*h
            drawLine(if(bumper.flashTimer>0f) Color(0xFFA5F3FC) else Color(0xFF0EA5E9),head-perp*(w*.5f),head+perp*(w*.5f),12f,StrokeCap.Round)
            drawLine(Color(0xFFE0F2FE),head-perp*(w*.5f),head+perp*(w*.5f),4f,StrokeCap.Round)
        }
    }
}
