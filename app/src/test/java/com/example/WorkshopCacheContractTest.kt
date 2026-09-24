package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.ui.render.WorkshopBackdropCache
import com.example.onemove.ui.render.WorkshopRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkshopCacheContractTest {
    private fun render(width:Int,world:PhysicsWorld,cached:Boolean):ImageBitmap {
        val height=ceil(width*4.0/3.0).toInt()
        val image=ImageBitmap(width,height)
        CanvasDrawScope().draw(Density(2f),LayoutDirection.Ltr,Canvas(image),Size(width.toFloat(),height.toFloat())) {
            scale(width/LevelDefinition.WORLD_WIDTH,pivot=Offset.Zero) {
                if(cached) WorkshopBackdropCache.draw(this,world)
                else {
                    WorkshopRenderer.background(this,world.currentLevel.number,LevelDefinition.WORLD_WIDTH,LevelDefinition.WORLD_HEIGHT)
                    world.dangerPits.forEach { WorkshopRenderer.danger(this,it) }
                }
            }
        }
        return image
    }
    @Test fun rasterTracksVisibleSizeReusesCacheAndPreservesThePaperAndHazards() {
        val out=File("build/reports/backdrop-contract").apply{mkdirs()}
        val rows=mutableListOf("display_width,display_height,raster_width,raster_height,cached_bytes,legacy_bytes,mean_channel_error,generation")
        for(width in listOf(250,578,1056)) {
            val world=PhysicsWorld(LevelCatalog.getLevel(11))
            val expected=render(width,world,false)
            val actual=render(width,world,true)
            val height=ceil(width*4.0/3.0).toInt()
            val rasterWidth=ceil(width*WorkshopBackdropCache.RASTER_SCALE).toInt().coerceAtLeast(1)
            val rasterHeight=ceil(rasterWidth*4.0/3.0).toInt()
            assertEquals(rasterWidth*rasterHeight*4,WorkshopBackdropCache.cachedPixelBytes)
            val generation=WorkshopBackdropCache.generationCount
            render(width,world,true)
            assertEquals("Same scene rerasterized",generation,WorkshopBackdropCache.generationCount)
            val count=width*height;val a=IntArray(count);val b=IntArray(count)
            expected.asAndroidBitmap().getPixels(a,0,width,0,0,width,height)
            actual.asAndroidBitmap().getPixels(b,0,width,0,0,width,height)
            var error=0L
            for(i in 0 until count)for(shift in listOf(0,8,16,24))error+=abs(((a[i] ushr shift)and 255)-((b[i] ushr shift)and 255))
            val mean=error.toDouble()/(count*4.0)
            assertTrue("Raster changed paper/hazard pixels too much: $mean",mean<2.0)
            rows+="$width,$height,$rasterWidth,$rasterHeight,${WorkshopBackdropCache.cachedPixelBytes},7680000,$mean,$generation"
            File(out,"native-render-$width.png").outputStream().use {
                actual.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
            world.loadLevel(LevelCatalog.getLevel(5));render(width,world,true)
            assertTrue("Level change kept stale hazards",WorkshopBackdropCache.generationCount>generation)
            val afterLevel=WorkshopBackdropCache.generationCount
            world.dangerPits.clear();render(width,world,true)
            assertTrue("Changed danger geometry was not invalidated",WorkshopBackdropCache.generationCount>afterLevel)
        }
        File(out,"raster-memory-and-pixels.csv").writeText(rows.joinToString("\n")+"\n")
        File(out,"scope.txt").writeText("Native Android graphics under Robolectric. Pixel/memory contracts, NOT interactive Android gameplay or physical-phone FPS.\n")
    }
}
