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
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.onemove.model.DangerPit
import com.example.onemove.model.GoalZone
import com.example.onemove.model.Platform
import kotlin.math.cos
import kotlin.math.sin

/** Original, code-drawn miniature workshop art. Never changes gameplay geometry or state. */
object WorkshopRenderer {
    data class Chapter(val name: String, val paper: Color, val shade: Color, val foliage: Color, val accent: Color)
    private val chapters = listOf(
        Chapter("THE MOSS WORKSHOP", Color(0xFFFFF1D9), Color(0xFFBCD2BA), Color(0xFF325F50), Color(0xFFF1BC62)),
        Chapter("THE COPPER GARDEN", Color(0xFFFFE8D1), Color(0xFFD6BCAB), Color(0xFF785146), Color(0xFFE8A17C)),
        Chapter("THE MOON ATELIER", Color(0xFFE7EDF7), Color(0xFFABC5D1), Color(0xFF455D7B), Color(0xFFB4C6F0))
    )
    fun chapter(level: Int): Chapter = chapters[((level - 1) / 4).coerceIn(0, 2)]
    private val walnut = Color(0xFF47372E)
    private val ink = Color(0xFF263F3A)
    private val brass = Brush.linearGradient(listOf(Color(0xFFFFE1A0), Color(0xFFA46737)), Offset.Zero, Offset(1200f, 1600f))
    private val paperBrushes = chapters.map { Brush.verticalGradient(listOf(it.paper, it.shade), 0f, 1600f) }
    private val glows = chapters.map { Brush.radialGradient(listOf(Color.White.copy(alpha=0.72f), Color.Transparent), Offset(325f,220f),950f) }

    fun background(scope: DrawScope, level: Int, width: Float, height: Float) = with(scope) {
        val index=((level-1)/4).coerceIn(0,2)
        val theme=chapters[index]
        drawRect(walnut, size=Size(width,height))
        drawRoundRect(brass, Offset(5f,5f), Size(width-10f,height-10f), CornerRadius(44f))
        drawRoundRect(Color(0xFF3F5348), Offset(14f,14f), Size(width-28f,height-28f), CornerRadius(36f))
        drawRoundRect(paperBrushes[index], Offset(23f,23f), Size(width-46f,height-46f), CornerRadius(30f))
        drawRoundRect(glows[index], Offset(23f,23f), Size(width-46f,height-46f), CornerRadius(30f))
        clipRect(26f,26f,width-26f,height-26f) {
            // Fine paper fibres stay low-contrast and behind every interactive object.
            for(i in 0..45) {
                val y=55f+i*34f
                drawLine(theme.foliage.copy(alpha=0.045f),Offset(24f,y),Offset(width-24f,y+18f),1.2f)
            }
            for(i in 0..14) for(j in 0..19) {
                val x=55f+i*79f; val y=65f+j*79f
                drawCircle(theme.foliage.copy(alpha=0.10f),1.7f,Offset(x,y))
            }
            // Embossed botanical margins, not fake platforms in the playfield.
            for(side in listOf(-1f,1f)) {
                val x=if(side<0f) 35f else width-35f
                for(i in 0..5) {
                    val y=110f+i*270f
                    leaf(this,Offset(x,y),Offset(-side*36f,-56f),theme.foliage.copy(alpha=0.17f))
                    leaf(this,Offset(x,y+20f),Offset(-side*48f,35f),theme.foliage.copy(alpha=0.13f))
                }
            }
        }
        drawRoundRect(Color.White.copy(alpha=0.52f),Offset(24f,24f),Size(width-48f,height-48f),CornerRadius(30f),style=Stroke(2f))
        for(x in listOf(13f,width-13f)) for(y in listOf(48f,height-48f)) bolt(this,Offset(x,y),6f)
    }

    fun platform(scope: DrawScope, platform: Platform) = with(scope) {
        val a=Offset(platform.start.x,platform.start.y); val b=Offset(platform.end.x,platform.end.y)
        val t=platform.thickness
        val d=b-a; val length=d.getDistance().coerceAtLeast(1f)
        val normal=Offset(-d.y/length,d.x/length)
        drawLine(Color(0xFF243A32).copy(alpha=0.19f),a+Offset(5f,8f),b+Offset(5f,8f),t+4f,StrokeCap.Round)
        drawLine(Color(0xFF493A31),a,b,t,StrokeCap.Round)
        drawLine(if(platform.isBouncy) Color(0xFFE99959) else Color(0xFFB88453),a-normal*(t*0.07f),b-normal*(t*0.07f),t*0.74f,StrokeCap.Round)
        drawLine(Color(0xFFFAD6A0),a-normal*(t*0.29f),b-normal*(t*0.29f),2.2f,StrokeCap.Round)
        drawLine(Color(0xFF80522F),a+normal*(t*0.18f),b+normal*(t*0.18f),1.4f,StrokeCap.Round)
        bolt(this,a,t*0.25f);bolt(this,b,t*0.25f)
        // Grain follows the actual collision segment; it never depicts extra support.
        if(length>180f) for(i in 1..3) {
            val c=a+d*(i/4f)
            drawLine(Color(0xFF7D522F).copy(alpha=0.6f),c-d/length*16f,c+d/length*16f,1.2f)
        }
    }

    fun danger(scope: DrawScope, pit: DangerPit) = with(scope) {
        val bounds=pit.bounds
        val a=Offset(bounds.left,bounds.top); val size=Size(bounds.width,bounds.height)
        drawRoundRect(Color(0xFF473133).copy(alpha=0.25f),a+Offset(5f,8f),size,CornerRadius(16f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF653E50),Color(0xFF281E32)),bounds.top,bounds.bottom),a,size,CornerRadius(16f))
        clipRect(bounds.left,bounds.top,bounds.right,bounds.bottom) {
            for(row in 0..8) {
                val y=bounds.top+30f+row*54f
                for(col in 0..((bounds.width/62f).toInt()+1)) {
                    val x=bounds.left+col*62f+(row%2)*31f
                    val thorn=Path().apply {
                        moveTo(x-24f,y+32f);quadraticBezierTo(x-4f,y+22f,x,y)
                        quadraticBezierTo(x+5f,y+24f,x+27f,y+32f);close()
                    }
                    drawPath(thorn,if(row%2==0) Color(0xFF9D6370) else Color(0xFF79546D))
                    drawLine(Color(0xFFBD8E8C).copy(alpha=0.35f),Offset(x,y+5f),Offset(x+6f,y+23f),1.6f)
                }
            }
            // A visible danger edge is aligned exactly with the actual hazard boundary.
            for(i in 0..((bounds.width/38f).toInt())) {
                val x=bounds.left+i*38f
                drawLine(Color(0xFFF0B079),Offset(x,bounds.top+3f),Offset(x+19f,bounds.top+3f),6f)
            }
        }
        drawRoundRect(Color(0xFFE4A18B),a,size,CornerRadius(16f),style=Stroke(3f))
        val c=Offset((bounds.left+bounds.right)/2f,(bounds.top+bounds.bottom)/2f)
        drawCircle(Color(0xFF372734),29f,c)
        val warning=Path().apply { moveTo(c.x,c.y-19f);lineTo(c.x+21f,c.y+16f);lineTo(c.x-21f,c.y+16f);close() }
        drawPath(warning,Color(0xFFFFD5AA),style=Stroke(3f))
        drawLine(Color(0xFFFFD5AA),c+Offset(0f,-8f),c+Offset(0f,4f),3.5f,StrokeCap.Round)
        drawCircle(Color(0xFFFFD5AA),2f,c+Offset(0f,10f))
    }

    fun nest(scope: DrawScope, goal: GoalZone, rescued: Int) = with(scope) {
        val c=Offset(goal.center.x,goal.center.y); val r=goal.radius
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE5AA).copy(alpha=0.70f),Color.Transparent),c,r*1.6f),r*1.6f,c)
        // The stitched halo shows the real circular goal trigger, not a decorative destination.
        drawCircle(Color(0xFF628B65).copy(alpha=0.26f),r,c,style=Stroke(2f))
        val p=c-Offset(r*1.10f,r*0.69f);val w=r*2.2f;val h=r*1.39f
        drawRoundRect(Color(0xFF294439).copy(alpha=0.20f),p+Offset(7f,10f),Size(w,h),CornerRadius(34f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFDAAF71),Color(0xFF735133)),p.y,p.y+h),p,Size(w,h),CornerRadius(34f))
        drawRoundRect(Color(0xFFFFDB9C),p,Size(w,h),CornerRadius(34f),style=Stroke(3f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF233D38),Color(0xFF526B52)),p.y,p.y+h),p+Offset(15f,h*0.29f),Size(w-30f,h*0.61f),CornerRadius(21f))
        drawRoundRect(Color(0xFF7EAD7C),p+Offset(16f,h*0.78f),Size(w-32f,h*0.19f),CornerRadius(14f))
        for(i in 0..13) {
            val x=p.x+23f+i*(w-46f)/13f
            drawLine(Color(0xFFC7D995),Offset(x,p.y+h*0.84f),Offset(x+3f,p.y+h*0.94f),2f,StrokeCap.Round)
        }
        for(i in 0..2) {
            val lamp=c+Offset((i-1)*34f,-r*0.40f)
            drawCircle(Color(0xFF624C35),8.5f,lamp)
            drawCircle(if(i<rescued) Color(0xFFB8F39C) else Color(0xFFA49574),5.5f,lamp)
        }
        for(side in listOf(-1f,1f)) {
            val stem=c+Offset(side*r*0.98f,r*0.5f)
            leaf(this,stem,Offset(side*26f,-35f),Color(0xFF50794F))
            leaf(this,stem-Offset(0f,19f),Offset(-side*23f,-35f),Color(0xFF709859))
        }
    }

    private fun bolt(scope: DrawScope, p: Offset, r: Float) = with(scope) {
        drawCircle(Color(0xFF61482F),r+1f,p+Offset(0f,1f))
        drawCircle(Color(0xFFE8C894),r,p)
        drawLine(Color(0xFF806440),p-Offset(r*0.55f,0f),p+Offset(r*0.55f,0f),1.3f)
    }
    private fun leaf(scope: DrawScope, base: Offset, direction: Offset, color: Color) = with(scope) {
        val side=Offset(-direction.y,direction.x)*0.24f
        val path=Path().apply {
            moveTo(base.x,base.y)
            val m=base+direction*0.45f
            quadraticBezierTo(m.x+side.x,m.y+side.y,base.x+direction.x,base.y+direction.y)
            quadraticBezierTo(m.x-side.x,m.y-side.y,base.x,base.y);close()
        }
        drawPath(path,color)
        drawLine(color.copy(alpha=color.alpha*0.45f),base,base+direction,1.5f)
    }
}
