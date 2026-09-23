package com.example.onemove.ui.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureExpression
import com.example.onemove.model.CreatureId
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Reviewed sprite art. Animation is visual-only and never feeds positions back into physics. */
object SpriteMascotRenderer {
    private const val DEFAULT_FRAMING = 3.6f
    private const val ANIMATED_CELL = 128
    private const val ANIMATED_COLUMNS = 5
    private const val ANIMATED_ROWS = 4

    private val animatedSpecs = linkedMapOf(
        CreatureId.PIP to "pip_animated",
        CreatureId.MOCHI to "mochi_animated",
        CreatureId.BLOBBO to "blobbo_animated"
    )

    private data class AnimatedAsset(
        val image: ImageBitmap,
        val framing: Float,
        val compressedBytes: Int,
        val decodedBytes: Int
    )

    @Volatile private var animatedAssets: Map<CreatureId, AnimatedAsset> = emptyMap()

    @Synchronized
    fun prepare(context: Context) {
        if (animatedAssets.keys.containsAll(animatedSpecs.keys)) return

        val loaded = linkedMapOf<CreatureId, AnimatedAsset>()
        for ((id, stem) in animatedSpecs) loaded[id] = loadAnimated(context, stem)
        animatedAssets = loaded

        Log.i(
            "OneMoveAssets",
            "Loaded animated " + loaded.keys.joinToString() +
                "; compressedBytes=" + loaded.values.sumOf { it.compressedBytes } +
                "; decodedBytes=" + loaded.values.sumOf { it.decodedBytes } +
                "; legacyStaticDecodedBytes=0"
        )
    }

    internal fun residentDecodedBytesForTest(): Int = animatedAssets.values.sumOf { it.decodedBytes }

    private fun loadAnimated(context: Context, stem: String): AnimatedAsset {
        val bytes = context.assets.open("mascots/" + stem + ".webp").use { it.readBytes() }
        val manifest = context.assets.open("mascots/" + stem + ".json")
            .bufferedReader().use { JSONObject(it.readText()) }
        check(manifest.getBoolean("approved_for_runtime")) {
            "Animated mascot is not approved for runtime: $stem"
        }
        check(sha256(bytes) == manifest.getString("atlas_sha256")) {
            "Animated mascot atlas integrity failure: $stem"
        }
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Cannot decode animated mascot atlas: $stem"
        }
        check(bitmap.width == ANIMATED_CELL * ANIMATED_COLUMNS &&
            bitmap.height == ANIMATED_CELL * ANIMATED_ROWS) {
            "Incorrect animated mascot atlas geometry: $stem"
        }
        check(bitmap.hasAlpha()) { "Animated mascot atlas must retain transparency: $stem" }
        return AnimatedAsset(
            image = bitmap.asImageBitmap(),
            framing = manifest.optDouble("framing", DEFAULT_FRAMING.toDouble()).toFloat(),
            compressedBytes = bytes.size,
            decodedBytes = bitmap.allocationByteCount
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private data class Clip(val row: Int, val frameSeconds: Float, val fixedFrame: Int? = null)

    private fun selectClip(creature: Creature): Clip = when {
        creature.expression == CreatureExpression.HAPPY || creature.isInsideGoal ->
            Clip(row = 3, frameSeconds = 0.18f)
        creature.expression == CreatureExpression.DIZZY ||
            creature.expression == CreatureExpression.DISAPPOINTED ->
            Clip(row = 2, frameSeconds = 0.11f, fixedFrame = 4)
        creature.velocity.y < -120f ->
            Clip(row = 1, frameSeconds = 0.11f)
        creature.velocity.y > 140f ||
            creature.expression == CreatureExpression.SURPRISED ||
            creature.expression == CreatureExpression.PANIC ||
            creature.expression == CreatureExpression.SCARED ->
            Clip(row = 2, frameSeconds = 0.11f)
        else -> Clip(row = 0, frameSeconds = 0.40f)
    }

    /** False only for isolated vector preview tests which did not create MainActivity. */
    fun draw(scope: DrawScope, creature: Creature, center: Offset, visualTime: Float = 0f): Boolean {
        val animated = animatedAssets[creature.id]
        if (animated != null) {
            drawShadow(scope, creature, center)
            val clip = selectClip(creature)
            val safeTime = if (visualTime.isFinite() && visualTime >= 0f) visualTime else 0f
            val frame = clip.fixedFrame ?: ((safeTime / clip.frameSeconds).toInt() % ANIMATED_COLUMNS)
            val side = (creature.radius * animated.framing).roundToInt().coerceAtLeast(1)
            with(scope) {
                drawImage(
                    image = animated.image,
                    srcOffset = IntOffset(frame * ANIMATED_CELL, clip.row * ANIMATED_CELL),
                    srcSize = IntSize(ANIMATED_CELL, ANIMATED_CELL),
                    dstOffset = IntOffset(
                        (center.x - side / 2f).roundToInt(),
                        (center.y - side / 2f).roundToInt()
                    ),
                    dstSize = IntSize(side, side),
                    filterQuality = FilterQuality.Medium
                )
            }
            return true
        }
        return drawPortrait(scope, creature, center)
    }

    /** Portraits use the reviewed new designs but freeze motion to avoid a twitchy HUD. */
    fun drawPortrait(scope: DrawScope, creature: Creature, center: Offset): Boolean {
        val animated = animatedAssets[creature.id]
        if (animated != null) {
            drawShadow(scope, creature, center)
            val clip = selectClip(creature)
            val frame = clip.fixedFrame ?: 0
            val side = (creature.radius * animated.framing).roundToInt().coerceAtLeast(1)
            with(scope) {
                drawImage(
                    image = animated.image,
                    srcOffset = IntOffset(frame * ANIMATED_CELL, clip.row * ANIMATED_CELL),
                    srcSize = IntSize(ANIMATED_CELL, ANIMATED_CELL),
                    dstOffset = IntOffset(
                        (center.x - side / 2f).roundToInt(),
                        (center.y - side / 2f).roundToInt()
                    ),
                    dstSize = IntSize(side, side),
                    filterQuality = FilterQuality.Medium
                )
            }
            return true
        }

        return false
    }

    private fun drawShadow(scope: DrawScope, creature: Creature, center: Offset) = with(scope) {
        drawOval(
            Color(0xFF20382C).copy(alpha = 0.15f),
            center + Offset(-creature.radius * 0.85f, creature.radius * 0.72f),
            Size(creature.radius * 1.7f, creature.radius * 0.34f)
        )
    }
}
