package com.example.onemove.ui.render

import android.graphics.Picture
import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.Platform
import com.example.onemove.physics.PhysicsWorld

/**
 * Records the immutable platform artwork once as native vector commands.
 * Replay stays at the exact old z-order and does not allocate a raster layer.
 */
object WorkshopRailPictureCache {
    private data class Key(
        val levelNumber: Int,
        val platforms: List<Platform>,
        val density: Float,
        val fontScale: Float
    )

    private var key: Key? = null
    private var picture: Picture? = null
    private var generation = 0

    internal val generationCount: Int get() = generation
    internal val rasterBytes: Int get() = 0

    fun draw(scope: DrawScope, world: PhysicsWorld) {
        val wanted = Key(
            levelNumber = world.currentLevel.number,
            platforms = world.platforms.toList(),
            density = scope.density,
            fontScale = scope.fontScale
        )
        var recorded = picture
        if (recorded == null || wanted != key) {
            val start = System.nanoTime()
            val next = Picture()
            val native = next.beginRecording(
                LevelDefinition.WORLD_WIDTH.toInt(),
                LevelDefinition.WORLD_HEIGHT.toInt()
            )
            CanvasDrawScope().draw(
                density = Density(scope.density, scope.fontScale),
                layoutDirection = scope.layoutDirection,
                canvas = Canvas(native),
                size = Size(LevelDefinition.WORLD_WIDTH, LevelDefinition.WORLD_HEIGHT)
            ) {
                for (platform in world.platforms) WorkshopRenderer.platform(this, platform)
            }
            next.endRecording()
            picture = next
            key = wanted
            recorded = next
            generation++
            Log.i(
                "OneMoveRender",
                "Rail picture generation=$generation level=${wanted.levelNumber} " +
                    "rails=${wanted.platforms.size} buildMs=${(System.nanoTime() - start) / 1_000_000.0}"
            )
        }
        scope.drawIntoCanvas { it.nativeCanvas.drawPicture(recorded) }
    }
}
