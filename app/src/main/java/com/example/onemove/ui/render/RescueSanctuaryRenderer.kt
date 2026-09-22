package com.example.onemove.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.onemove.model.GoalZone

/** Open rescue doorway, drawn BEHIND all real colliders and creatures. */
object RescueSanctuaryRenderer {
    private class Shell(val x: Float, val y: Float, val r: Float) {
        val center = Offset(x, y)
        val outer = arch(x, y - r * 0.94f, r * 2.10f, y + r * 0.64f)
        val inner = arch(x, y - r * 0.62f, r * 1.68f, y + r * 0.54f)
        val brass = Brush.linearGradient(listOf(Color(0xFFFFE5AF), Color(0xFFDEAF68), Color(0xFF8B5938)),
            Offset(x-r, y-r), Offset(x+r, y+r))
        val recess = Brush.radialGradient(listOf(Color(0xFF517C68), Color(0xFF173E35), Color(0xFF0B2927)),
            Offset(x, y+r*0.5f), r*1.3f)
        val glow = Brush.radialGradient(listOf(Color(0xFFFFDF94).copy(alpha=0.45f), Color.Transparent), center, r*1.4f)
        val sill = Brush.verticalGradient(listOf(Color(0xFFDAF0B8), Color(0xFF87B590), Color(0xFF456D55)), y+r*0.5f, y+r*0.74f)
        val home = Path().apply {
            moveTo(x-r*0.105f, y-r*0.69f); lineTo(x, y-r*0.79f); lineTo(x+r*0.105f, y-r*0.69f)
            moveTo(x-r*0.073f, y-r*0.69f); lineTo(x-r*0.073f, y-r*0.59f)
            lineTo(x+r*0.073f, y-r*0.59f); lineTo(x+r*0.073f, y-r*0.69f)
        }
    }
    // One geometry entry, not a growing level cache or animation clock.
    private var cached: Shell? = null
    private fun shell(goal: GoalZone): Shell {
        val old = cached
        if (old != null && old.x == goal.center.x && old.y == goal.center.y && old.r == goal.radius) return old
        return Shell(goal.center.x, goal.center.y, goal.radius).also { cached = it }
    }
    private fun arch(cx: Float, top: Float, width: Float, bottom: Float) = Path().apply {
        val left = cx-width/2f; val right = cx+width/2f; val shoulder = top+width*0.43f
        moveTo(left,bottom); lineTo(left,shoulder)
        cubicTo(left,top,cx-width*0.22f,top,cx,top)
        cubicTo(cx+width*0.22f,top,right,top,right,shoulder)
        lineTo(right,bottom); close()
    }
    fun draw(scope: DrawScope, goal: GoalZone, rescued: Int) = with(scope) {
        val s = shell(goal); val c = s.center; val r = s.r
        drawCircle(s.glow,r*1.4f,c)
        // Ring remains the real goal trigger. The doorway is presentation only.
        drawCircle(Color(0xFF528A68).copy(alpha=0.30f),r,c,style=Stroke(2f))
        drawOval(Color(0xFF213C32).copy(alpha=0.23f),c+Offset(-r*1.1f,r*0.61f),Size(r*2.3f,r*0.20f))
        drawPath(s.outer,Color(0xFF4C3C2F),style=Stroke(12f))
        drawPath(s.outer,s.brass)
        drawPath(s.outer,Color(0xFFFFEBC1),style=Stroke(3f))
        drawPath(s.inner,Color(0xFF775734),style=Stroke(8f))
        drawPath(s.inner,s.recess)
        drawPath(s.inner,Color(0xFFEDD09A),style=Stroke(2f))
        for (side in listOf(-1f,1f)) {
            val x=c.x+side*r*0.94f
            drawLine(Color(0xFFFFE6B3).copy(alpha=0.65f),Offset(x,c.y-r*0.12f),Offset(x,c.y+r*0.48f),2.5f,StrokeCap.Round)
            for (dy in listOf(0.10f,0.47f)) {
                val bolt=Offset(x,c.y+r*dy)
                drawCircle(Color(0xFF705237),5.5f,bolt)
                drawCircle(Color(0xFFFBE0A9),3.5f,bolt-Offset(0.6f,0.8f))
            }
        }
        val badge=c+Offset(0f,-r*0.69f)
        drawCircle(Color(0xFF2D5144),r*0.15f,badge)
        drawCircle(Color(0xFFFFE8B7),r*0.15f,badge,style=Stroke(2f))
        drawPath(s.home,Color(0xFFFFEAC1),style=Stroke(2.8f,cap=StrokeCap.Round))
        // Lit sill, not a cushion-like seat. Real floor pins draw over it.
        drawRoundRect(s.sill,c+Offset(-r*1.03f,r*0.52f),Size(r*2.06f,r*0.20f),CornerRadius(10f))
        drawLine(Color(0xFFFFE8B7),c+Offset(-r,r*0.52f),c+Offset(r,r*0.52f),3f,StrokeCap.Round)
        for (index in 0..2) {
            val lamp=c+Offset((index-1)*r*0.23f,-r*0.32f)
            drawCircle(Color(0xFF345245),7f,lamp)
            drawCircle(if(index<rescued.coerceIn(0,3)) Color(0xFFCDFFAD) else Color(0xFF728E71),4.6f,lamp)
            if(index<rescued) drawCircle(Color(0xFFFFFFDD),1.5f,lamp-Offset(1f,1f))
        }
    }
}
