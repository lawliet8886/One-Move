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

    private data class AnimatedAsset(
        val baseRow: Int,
        val framing: Float
    )

    @Volatile private var trioAtlas: ImageBitmap? = null
    @Volatile private var trioDecodedBytes: Int = 0
    @Volatile private var trioCompressedBytes: Int = 0
    @Volatile private var animatedAssets: Map<CreatureId, AnimatedAsset> = emptyMap()

    @Synchronized
    fun prepare(context: Context) {
        if (trioAtlas != null && animatedAssets.size == 3) return

        val bytes = context.assets.open("mascots/trio_runtime.webp").use { it.readBytes() }
        val manifest = context.assets.open("mascots/trio_runtime.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        check(manifest.getBoolean("approved_for_runtime")) {
            "Trio runtime atlas is not approved"
        }
        check(sha256(bytes) == manifest.getString("atlas_sha256")) {
            "Trio runtime atlas integrity failure"
        }
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Cannot decode trio runtime atlas"
        }
        check(bitmap.width == ANIMATED_CELL * ANIMATED_COLUMNS &&
            bitmap.height == ANIMATED_CELL * ANIMATED_ROWS * 3) {
            "Incorrect trio runtime atlas geometry"
        }
        check(bitmap.hasAlpha()) { "Trio runtime atlas must retain transparency" }

        val characters = manifest.getJSONObject("characters")
        val loaded = linkedMapOf<CreatureId, AnimatedAsset>()
        for (id in listOf(CreatureId.PIP, CreatureId.MOCHI, CreatureId.BLOBBO)) {
            val spec = characters.getJSONObject(id.name)
            check(spec.getBoolean("approved_for_runtime")) {
                "Animated mascot is not approved for runtime: $id"
            }
            loaded[id] = AnimatedAsset(
                baseRow = spec.getInt("base_row"),
                framing = spec.getDouble("framing").toFloat()
            )
        }
        trioAtlas = bitmap.asImageBitmap()
        trioDecodedBytes = bitmap.allocationByteCount
        trioCompressedBytes = bytes.size
        animatedAssets = loaded

        Log.i(
            "OneMoveAssets",
            "Loaded shared trio atlas; textureCount=1; compressedBytes=" + trioCompressedBytes +
                "; decodedBytes=" + trioDecodedBytes + "; legacyStaticDecodedBytes=0"
        )
    }

    internal fun residentDecodedBytesForTest(): Int = trioDecodedBytes
    internal fun runtimeTextureCountForTest(): Int = if (trioAtlas == null) 0 else 1

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
        val shared = trioAtlas
        if (animated != null && shared != null) {
            drawShadow(scope, creature, center)
            val clip = selectClip(creature)
            val safeTime = if (visualTime.isFinite() && visualTime >= 0f) visualTime else 0f
            val frame = clip.fixedFrame ?: ((safeTime / clip.frameSeconds).toInt() % ANIMATED_COLUMNS)
            val side = (creature.radius * animated.framing).roundToInt().coerceAtLeast(1)
            with(scope) {
                drawImage(
                    image = shared,
                    srcOffset = IntOffset(frame * ANIMATED_CELL, (animated.baseRow + clip.row) * ANIMATED_CELL),
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
        val shared = trioAtlas
        if (animated != null && shared != null) {
            drawShadow(scope, creature, center)
            val clip = selectClip(creature)
            val frame = clip.fixedFrame ?: 0
            val side = (creature.radius * animated.framing).roundToInt().coerceAtLeast(1)
            with(scope) {
                drawImage(
                    image = shared,
                    srcOffset = IntOffset(frame * ANIMATED_CELL, (animated.baseRow + clip.row) * ANIMATED_CELL),
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
