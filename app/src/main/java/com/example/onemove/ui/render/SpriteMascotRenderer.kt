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
    private const val STATIC_CELL = 192
    private const val STATIC_FRAMING = 3.6f
    private const val PIP_CELL = 128
    private const val PIP_COLUMNS = 5
    private const val PIP_ROWS = 4

    @Volatile private var staticAtlas: ImageBitmap? = null
    @Volatile private var pipAnimatedAtlas: ImageBitmap? = null
    @Volatile private var pipFraming: Float = STATIC_FRAMING

    @Synchronized
    fun prepare(context: Context) {
        if (staticAtlas != null && pipAnimatedAtlas != null) return

        val staticBytes = context.assets.open("mascots/atlas.webp").use { it.readBytes() }
        val staticManifest = context.assets.open("mascots/manifest.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        check(sha256(staticBytes) == staticManifest.getString("atlas_sha256")) {
            "Mascot atlas integrity failure"
        }
        val staticBitmap = checkNotNull(BitmapFactory.decodeByteArray(staticBytes, 0, staticBytes.size)) {
            "Cannot decode mascot atlas"
        }
        check(staticBitmap.width == STATIC_CELL * 4 && staticBitmap.height == STATIC_CELL * 3) {
            "Incorrect static mascot atlas geometry"
        }
        check(staticBitmap.hasAlpha()) { "Mascot atlas must retain transparency" }
        staticAtlas = staticBitmap.asImageBitmap()

        val pipBytes = context.assets.open("mascots/pip_animated.webp").use { it.readBytes() }
        val pipManifest = context.assets.open("mascots/pip_animated.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        check(sha256(pipBytes) == pipManifest.getString("atlas_sha256")) {
            "Animated Pip atlas integrity failure"
        }
        val pipBitmap = checkNotNull(BitmapFactory.decodeByteArray(pipBytes, 0, pipBytes.size)) {
            "Cannot decode animated Pip atlas"
        }
        check(pipBitmap.width == PIP_CELL * PIP_COLUMNS && pipBitmap.height == PIP_CELL * PIP_ROWS) {
            "Incorrect animated Pip atlas geometry"
        }
        check(pipBitmap.hasAlpha()) { "Animated Pip atlas must retain transparency" }
        pipFraming = pipManifest.optDouble("framing", STATIC_FRAMING.toDouble()).toFloat()
        pipAnimatedAtlas = pipBitmap.asImageBitmap()

        Log.i(
            "OneMoveAssets",
            "Loaded static mascot atlas " + staticBitmap.width + "x" + staticBitmap.height +
                " and animated Pip " + pipBitmap.width + "x" + pipBitmap.height + "; " +
                (staticBytes.size + pipBytes.size) + " compressed bytes"
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private data class PipClip(val row: Int, val frameSeconds: Float, val fixedFrame: Int? = null)

    private fun selectPipClip(creature: Creature): PipClip = when {
        creature.expression == CreatureExpression.HAPPY || creature.isInsideGoal ->
            PipClip(row = 3, frameSeconds = 0.18f)
        creature.expression == CreatureExpression.DIZZY ||
            creature.expression == CreatureExpression.DISAPPOINTED ->
            PipClip(row = 2, frameSeconds = 0.11f, fixedFrame = 4)
        creature.velocity.y < -120f ->
            PipClip(row = 1, frameSeconds = 0.11f)
        creature.velocity.y > 140f ||
            creature.expression == CreatureExpression.SURPRISED ||
            creature.expression == CreatureExpression.PANIC ||
            creature.expression == CreatureExpression.SCARED ->
            PipClip(row = 2, frameSeconds = 0.11f)
        else -> PipClip(row = 0, frameSeconds = 0.40f)
    }

    /** False only for isolated vector preview tests which did not create MainActivity. */
    fun draw(scope: DrawScope, creature: Creature, center: Offset, visualTime: Float = 0f): Boolean {
        val animated = pipAnimatedAtlas
        if (creature.id == CreatureId.PIP && animated != null) {
            drawShadow(scope, creature, center)
            val clip = selectPipClip(creature)
            val safeTime = if (visualTime.isFinite() && visualTime >= 0f) visualTime else 0f
            val frame = clip.fixedFrame ?: ((safeTime / clip.frameSeconds).toInt() % PIP_COLUMNS)
            val side = (creature.radius * pipFraming).roundToInt().coerceAtLeast(1)
            with(scope) {
                drawImage(
                    image = animated,
                    srcOffset = IntOffset(frame * PIP_CELL, clip.row * PIP_CELL),
                    srcSize = IntSize(PIP_CELL, PIP_CELL),
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

        val image = staticAtlas ?: return false
        drawShadow(scope, creature, center)
        val column = when (creature.expression) {
            CreatureExpression.HAPPY -> 1
            CreatureExpression.SCARED,
            CreatureExpression.PANIC,
            CreatureExpression.SURPRISED -> 2
            CreatureExpression.DIZZY,
            CreatureExpression.DISAPPOINTED -> 3
            else -> 0
        }
        val side = (creature.radius * STATIC_FRAMING).roundToInt().coerceAtLeast(1)
        with(scope) {
            drawImage(
                image = image,
                srcOffset = IntOffset(column * STATIC_CELL, creature.id.ordinal * STATIC_CELL),
                srcSize = IntSize(STATIC_CELL, STATIC_CELL),
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

    private fun drawShadow(scope: DrawScope, creature: Creature, center: Offset) = with(scope) {
        drawOval(
            Color(0xFF20382C).copy(alpha = 0.15f),
            center + Offset(-creature.radius * 0.85f, creature.radius * 0.72f),
            Size(creature.radius * 1.7f, creature.radius * 0.34f)
        )
    }
}
