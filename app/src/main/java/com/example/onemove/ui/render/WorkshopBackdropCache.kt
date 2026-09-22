package com.example.onemove.ui.render

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.example.onemove.model.DangerPit
import com.example.onemove.model.LevelDefinition
import com.example.onemove.physics.PhysicsWorld
import kotlin.math.ceil

/** One immutable background raster at displayed resolution, never the gameplay state. */
object WorkshopBackdropCache {
    private data class Key(val number:Int,val pits:List<DangerPit>,val width:Int,val height:Int,
        val density:Float,val fontScale:Float,val direction:LayoutDirection)
    private var key:Key?=null
    private var bitmap:ImageBitmap?=null
    private var generation=0
    val cachedPixelBytes:Int get()=(bitmap?.let { it.width*it.height*4 } ?: 0)
    val generationCount:Int get()=generation

    fun draw(scope:DrawScope,world:PhysicsWorld) {
        // DrawScope.size stays in display pixels when the outer world transform scales.
        val width=ceil(scope.size.width.toDouble()).toInt().coerceIn(1,LevelDefinition.WORLD_WIDTH.toInt())
        val height=ceil(width*LevelDefinition.WORLD_HEIGHT.toDouble()/LevelDefinition.WORLD_WIDTH).toInt()
        val wanted=Key(world.currentLevel.number,world.dangerPits.toList(),width,height,
            scope.density,scope.fontScale,scope.layoutDirection)
        var image=bitmap
        if(image==null || wanted!=key) {
            val start=System.nanoTime()
            val next=ImageBitmap(width,height)
            CanvasDrawScope().draw(density=Density(scope.density,scope.fontScale),
                layoutDirection=scope.layoutDirection,canvas=Canvas(next),size=Size(width.toFloat(),height.toFloat())) {
                scale(width/LevelDefinition.WORLD_WIDTH,height/LevelDefinition.WORLD_HEIGHT,pivot=Offset.Zero) {
                    WorkshopRenderer.background(this,world.currentLevel.number,LevelDefinition.WORLD_WIDTH,LevelDefinition.WORLD_HEIGHT)
                    for(pit in world.dangerPits) WorkshopRenderer.danger(this,pit)
                }
            }
            // Old rasters are released by GC after render-thread references finish.
            bitmap=next;key=wanted;image=next;generation++
            Log.i("OneMoveRender","Static backdrop generation=$generation level=${wanted.number} ${width}x$height bytes=${width*height*4} buildMs=${(System.nanoTime()-start)/1_000_000.0}")
        }
        with(scope) {
            drawImage(image,srcOffset=IntOffset.Zero,srcSize=IntSize(image.width,image.height),
                dstOffset=IntOffset.Zero,dstSize=IntSize(LevelDefinition.WORLD_WIDTH.toInt(),LevelDefinition.WORLD_HEIGHT.toInt()),
                filterQuality=FilterQuality.Low)
        }
    }
}
