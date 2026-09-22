package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.CreatureGate
import com.example.onemove.model.SeesawLever

object MechanicalJointRenderer {
    fun drawSeesawAssembly(scope:DrawScope,seesaw:SeesawLever) {
        with(scope) {
            val pivot=Offset(seesaw.pivot.x,seesaw.pivot.y)
            val stand=Path().apply{moveTo(pivot.x,pivot.y);lineTo(pivot.x-30f,pivot.y+40f);lineTo(pivot.x+30f,pivot.y+40f);close()}
            drawPath(stand,Color(0xFF263A52));drawPath(stand,Color(0xFF67829D),style=Stroke(2.5f))
            withTransform({translate(pivot.x,pivot.y);rotate(Math.toDegrees(seesaw.angle.toDouble()).toFloat(),pivot=Offset.Zero)}) {
                drawLine(Color(0x66000000),Offset(-seesaw.halfLength,5f),Offset(seesaw.halfLength,5f),seesaw.thickness+4f,StrokeCap.Round)
                drawLine(Color(0xFF536A83),Offset(-seesaw.halfLength,0f),Offset(seesaw.halfLength,0f),seesaw.thickness,StrokeCap.Round)
                drawLine(Color(0xFFE1CA96),Offset(-seesaw.halfLength+6f,-seesaw.thickness*.25f),Offset(seesaw.halfLength-6f,-seesaw.thickness*.25f),3f,StrokeCap.Round)
            }
            drawCircle(Color(0xFFB45309),16f,pivot);drawCircle(Color(0xFFF59E0B),12f,pivot);drawCircle(Color(0xFFFEF3C7),6f,pivot)
        }
    }
    fun drawCreatureGate(scope:DrawScope,gate:CreatureGate) {
        with(scope) {
            val pivot=Offset(gate.pivot.x,gate.pivot.y)
            val tip=gate.currentEnd();val end=Offset(tip.x,tip.y)
            drawLine(Color(0xFF475569),pivot,end,gate.thickness,StrokeCap.Round)
            drawLine(Color(0xFF38BDF8),pivot,end,gate.thickness*.5f,StrokeCap.Round)
            drawCircle(Color(0xFFF59E0B),gate.thickness*.75f,pivot)
            drawCircle(Color(0xFF78350F),gate.thickness*.45f,pivot)
        }
    }
}
