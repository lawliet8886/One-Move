package com.example.onemove.ui.render

import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.onemove.model.DangerPit
import com.example.onemove.model.LevelDefinition
import com.example.onemove.physics.PhysicsWorld

/**
 * A single bounded raster of static paper, foliage and hazard decoration.
 * Dynamic rails, pins, nest indicators and all actors remain live. This neither
 * changes collision geometry nor freezes a gameplay state into a screenshot.
 */
object WorkshopBackdropCache {
    private data class Key(val number:Int, val pits:List<DangerPit>,
        val density:Float, val fontScale:Float, val direction:LayoutDirection)
    private var key:Key?=null
    private var bitmap:ImageBitmap?=null
    private var generation=0

    fun draw(scope:DrawScope,world:PhysicsWorld) {
        val wanted=Key(world.currentLevel.number,world.dangerPits.toList(),
            scope.density,scope.fontScale,scope.layoutDirection)
        var image=bitmap
        if(image==null || wanted!=key) {
            val start=System.nanoTime()
            val width=LevelDefinition.WORLD_WIDTH.toInt()
            val height=LevelDefinition.WORLD_HEIGHT.toInt()
            val next=ImageBitmap(width,height)
            CanvasDrawScope().draw(
                density=Density(scope.density,scope.fontScale),
                layoutDirection=scope.layoutDirection,canvas=Canvas(next),
                size=Size(width.toFloat(),height.toFloat())) {
                WorkshopRenderer.background(this,world.currentLevel.number,
                    LevelDefinition.WORLD_WIDTH,LevelDefinition.WORLD_HEIGHT)
                for(pit in world.dangerPits) WorkshopRenderer.danger(this,pit)
            }
            // Keep one raster, not one bitmap per phase. Do not recycle the old bitmap
            // manually: a render thread may still be consuming the previous frame.
            bitmap=next;key=wanted;image=next;generation++
            Log.i("OneMoveRender","Static backdrop generation=$generation level=${wanted.number} bytes=${width*height*4} buildMs=${(System.nanoTime()-start)/1_000_000.0}")
        }
        with(scope){drawImage(image)}
    }
}
