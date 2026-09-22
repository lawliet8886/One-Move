package com.example.onemove.ui.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.example.onemove.model.Pin

/** Large lettered handles supplement the accessible footer controls; color is never the only cue. */
object PinRenderer {
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign=Paint.Align.CENTER
        typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
        textSize=29f
    }
    fun drawPin(drawScope:DrawScope,pin:Pin,isReady:Boolean=true) {
        if(pin.isRemoved && pin.pullProgress>=1f) return
        val progress=if(pin.isRemoved) pin.pullProgress.coerceIn(0f,1f) else 0f
        val ease=1f-(1f-progress)*(1f-progress)
        val alpha=1f-progress
        with(drawScope) {
            withTransform({translate(pin.pullDirection.x*ease*(pin.length+90f),pin.pullDirection.y*ease*(pin.length+90f))}) {
                val start=Offset(pin.start.x,pin.start.y)
                val end=Offset(pin.end.x,pin.end.y)
                val handle=Offset(pin.handlePosition.x,pin.handlePosition.y)
                drawLine(Color(0x88000000).copy(alpha=.45f*alpha),start+Offset(0f,7f),end+Offset(0f,7f),pin.thickness+3f,StrokeCap.Round)
                drawLine(Color(0xFF94A3B8).copy(alpha=alpha),start,end,pin.thickness,StrokeCap.Round)
                drawLine(pin.color.copy(alpha=alpha),start,end,pin.thickness*.5f,StrokeCap.Round)
                drawLine(Color.White.copy(alpha=.75f*alpha),start-Offset(0f,pin.thickness*.22f),end-Offset(0f,pin.thickness*.22f),2f,StrokeCap.Round)
                if(isReady) drawCircle(pin.color.copy(alpha=.13f),radius=48f,center=handle)
                drawCircle(Color(0x88000000).copy(alpha=.45f*alpha),36f,handle+Offset(0f,6f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEDBB).copy(alpha=alpha),pin.color.copy(alpha=alpha),Color(0xFF925E27).copy(alpha=alpha)),handle-Offset(10f,12f),62f),36f,handle)
                drawCircle(Color(0xFFFDE9BD).copy(alpha=.8f*alpha),36f,handle,style=Stroke(2.5f))
                drawCircle(Color(0xFF0F2035).copy(alpha=alpha),24f,handle)
                label.color=android.graphics.Color.argb((alpha*255).toInt(),245,245,238)
                drawIntoCanvas{it.nativeCanvas.drawText(pin.name,handle.x,handle.y-(label.ascent()+label.descent())*.5f,label)}
            }
        }
    }
}
