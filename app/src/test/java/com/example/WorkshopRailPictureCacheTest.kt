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
import com.example.onemove.ui.render.WorkshopRailPictureCache
import com.example.onemove.ui.render.WorkshopRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkshopRailPictureCacheTest {
    private fun render(width: Int, world: PhysicsWorld, cached: Boolean): ImageBitmap {
        val height = ceil(width * 4.0 / 3.0).toInt()
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            Density(2f), LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())
        ) {
            scale(width / LevelDefinition.WORLD_WIDTH, pivot = Offset.Zero) {
                if (cached) WorkshopRailPictureCache.draw(this, world)
                else world.platforms.forEach { WorkshopRenderer.platform(this, it) }
            }
        }
        return image
    }

    private fun meanChannelError(a: ImageBitmap, b: ImageBitmap): Double {
        val width = a.width
        val height = a.height
        val count = width * height
        val pa = IntArray(count)
        val pb = IntArray(count)
        a.asAndroidBitmap().getPixels(pa, 0, width, 0, 0, width, height)
        b.asAndroidBitmap().getPixels(pb, 0, width, 0, 0, width, height)
        var error = 0L
        for (i in 0 until count) for (shift in listOf(0, 8, 16, 24)) {
            error += abs(((pa[i] ushr shift) and 255) - ((pb[i] ushr shift) and 255))
        }
        return error.toDouble() / (count * 4.0)
    }

    @Test
    fun pictureReplayPreservesRailsReusesRecordingAndAddsNoRasterMemory() {
        val out = File("build/reports/rail-picture-contract").apply { mkdirs() }
        val rows = mutableListOf("level,width,rails,mean_channel_error,generation")
        for (levelNumber in listOf(4, 7, 12)) {
            for (width in listOf(250, 578, 1056)) {
                val world = PhysicsWorld(LevelCatalog.getLevel(levelNumber))
                val expected = render(width, world, false)
                val actual = render(width, world, true)
                val mean = meanChannelError(expected, actual)
                assertTrue("Rail picture changed level $levelNumber/$width pixels too much: $mean", mean < 0.75)
                assertEquals(0, WorkshopRailPictureCache.rasterBytes)
                val generation = WorkshopRailPictureCache.generationCount
                render(width, world, true)
                assertEquals("Same rails re-recorded", generation, WorkshopRailPictureCache.generationCount)
                rows += "$levelNumber,$width,${world.platforms.size},$mean,$generation"
            }
        }

        val world = PhysicsWorld(LevelCatalog.getLevel(12))
        render(578, world, true)
        val beforeLevel = WorkshopRailPictureCache.generationCount
        world.loadLevel(LevelCatalog.getLevel(4))
        render(578, world, true)
        assertTrue(WorkshopRailPictureCache.generationCount > beforeLevel)
        val beforeGeometry = WorkshopRailPictureCache.generationCount
        world.platforms.removeAt(world.platforms.lastIndex)
        render(578, world, true)
        assertTrue(WorkshopRailPictureCache.generationCount > beforeGeometry)

        File(out, "pixels.csv").writeText(rows.joinToString("\n") + "\n")
        File(out, "scope.txt").writeText(
            "Robolectric native graphics: vector-picture pixel/reuse contract; not physical-phone FPS.\n"
        )
    }
}
