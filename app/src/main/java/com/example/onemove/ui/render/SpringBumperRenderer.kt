package com.example.onemove.ui.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.onemove.model.SpringBumper
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin

/** Premium 2.5D cartridge. Physics position/direction/compression remain authoritative. */
object SpringBumperRenderer {
    private const val CELL = 128
    private const val FRAME_COUNT = 4
    const val DECODED_BYTES = 512 * 128 * 4

    private val ink = Color(0xFF273C37)
    private val brass = Color(0xFFB3844F)
    private val brassLight = Color(0xFFF9D393)
    private val steel = Color(0xFFACC2BF)
    private val enamel = Color(0xFF258C91)

    @Volatile private var atlas: ImageBitmap? = null
    @Volatile private var pivotX = 64f
    @Volatile private var pivotY = 89f
    @Volatile private var physicalWidthPixels = 94f
    @Volatile private var frameTops = intArrayOf(21, 32, 44, 56)

    val isPrepared: Boolean get() = atlas != null
    @Synchronized
    fun prepare(context: Context) {
        if (atlas != null) return
        val bytes = context.assets.open("hardware/spring_atlas.webp").use { it.readBytes() }
        val manifest = context.assets.open("hardware/spring_manifest.json")
            .bufferedReader().use { JSONObject(it.readText()) }

        check(manifest.getBoolean("approved_for_runtime")) { "Spring atlas is not runtime-approved" }
        check(sha256(bytes) == manifest.getString("atlas_sha256")) { "Spring atlas hash mismatch" }
        check(manifest.getInt("cell_size") == CELL && manifest.getInt("columns") == FRAME_COUNT) {
            "Spring manifest geometry mismatch"
        }

        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Cannot decode spring atlas"
        }
        check(bitmap.width == CELL * FRAME_COUNT && bitmap.height == CELL && bitmap.hasAlpha()) {
            "Spring decode geometry/alpha mismatch: " + bitmap.width + "x" + bitmap.height +
                "; alpha=" + bitmap.hasAlpha()
        }

        val pivot = manifest.getJSONArray("pivot_cell")
        pivotX = pivot.getInt(0).toFloat()
        pivotY = pivot.getInt(1).toFloat()
        physicalWidthPixels = manifest.getInt("physical_width_pixels").toFloat()
        check(physicalWidthPixels > 0f && pivotX in 0f..CELL.toFloat() && pivotY in 0f..CELL.toFloat())

        val frames = manifest.getJSONArray("frames")
        check(frames.length() == FRAME_COUNT)
        frameTops = IntArray(FRAME_COUNT) { index ->
            frames.getJSONObject(index).getJSONArray("alpha_bounds").getInt(1)
        }
        atlas = bitmap.asImageBitmap()
        Log.i(
            "OneMoveAssets",
            "Spring atlas ready: " + bitmap.width + "x" + bitmap.height + "; " + bytes.size +
                " compressed bytes; " + DECODED_BYTES + " decoded bytes; pivot=" +
                pivotX.toInt() + "," + pivotY.toInt()
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    internal data class VisualMetrics(
        val frame: Int,
        val destinationWidth: Int,
        val destinationHeight: Int,
        val offsetX: Int,
        val offsetY: Int,
        val machineTopY: Float,
        val visibleSpringHeight: Float
    )

    internal fun visualMetricsForTest(width: Float, restHeight: Float, compression: Float): VisualMetrics {
        val c = compression.coerceIn(0f, 1f)
        val frame = (c * (FRAME_COUNT - 1)).roundToInt().coerceIn(0, FRAME_COUNT - 1)
        val xScale = width / physicalWidthPixels
        val targetSpringHeight = restHeight * (1f - 0.70f * c)
        val sourceSpringHeight = (pivotY - frameTops[frame]).coerceAtLeast(1f)
        val yScale = targetSpringHeight / sourceSpringHeight
        return VisualMetrics(
            frame = frame,
            destinationWidth = (CELL * xScale).roundToInt().coerceAtLeast(1),
            destinationHeight = (CELL * yScale).roundToInt().coerceAtLeast(1),
            offsetX = (-pivotX * xScale).roundToInt(),
            offsetY = (-pivotY * yScale).roundToInt(),
            machineTopY = (frameTops[frame] - pivotY) * yScale,
            visibleSpringHeight = targetSpringHeight
        )
    }

    fun drawSpringBumper(drawScope: DrawScope, bumper: SpringBumper) = with(drawScope) {
        val n = bumper.direction.normalized()
        val angle = Math.toDegrees(atan2(n.y.toDouble(), n.x.toDouble())).toFloat() + 90f
        val compression = bumper.compression.coerceIn(0f, 1f)
        val image = atlas

        withTransform({
            translate(bumper.position.x, bumper.position.y)
            rotate(angle, pivot = Offset.Zero)
        }) {
            if (image != null) {
                val metrics = visualMetricsForTest(bumper.width, bumper.restHeight, compression)
                drawImage(
                    image = image,
                    srcOffset = IntOffset(metrics.frame * CELL, 0),
                    srcSize = IntSize(CELL, CELL),
                    dstOffset = IntOffset(metrics.offsetX, metrics.offsetY),
                    dstSize = IntSize(metrics.destinationWidth, metrics.destinationHeight),
                    filterQuality = FilterQuality.Medium
                )
                drawDirectionOverlay(bumper, metrics.machineTopY)
            } else {
                drawVectorFallback(bumper, compression)
            }
        }
    }

    private fun DrawScope.drawDirectionOverlay(bumper: SpringBumper, machineTopY: Float) {
        val tail = Offset(0f, machineTopY - 14f)
        val tip = Offset(0f, machineTopY - 54f)
        val arrow = Color(0xFF367C75)
        drawLine(arrow, tail, tip, 5f, StrokeCap.Round)
        drawLine(arrow, tip, tip + Offset(-12f, 17f), 5f, StrokeCap.Round)
        drawLine(arrow, tip, tip + Offset(12f, 17f), 5f, StrokeCap.Round)
        if (bumper.flashTimer > 0f) {
            drawCircle(Color(0xAAABFFE0), 11f, Offset(0f, machineTopY + 10f), style = Stroke(3f))
        }
    }
    private fun DrawScope.drawVectorFallback(bumper: SpringBumper, compression: Float) {
        val w = bumper.width
        val h = bumper.restHeight * (1f - 0.70f * compression)
        val half = w * 0.5f

        drawLine(Color(0x33203931), Offset(-half, 7f), Offset(half, 7f), 22f, StrokeCap.Round)
        drawLine(ink, Offset(-half, 0f), Offset(half, 0f), 18f, StrokeCap.Round)
        drawLine(brass, Offset(-half, 0f), Offset(half, 0f), 12f, StrokeCap.Round)
        drawLine(brassLight, Offset(-half + 4f, -4f), Offset(half - 4f, -4f), 2f, StrokeCap.Round)

        val coil = Path()
        val amplitude = minOf(12f, w * 0.09f)
        for (x in listOf(-w * 0.28f, 0f, w * 0.28f)) {
            drawLine(ink.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, -h), 4f, StrokeCap.Round)
            coil.moveTo(x, -2f)
            for (i in 1..32) {
                val t = i / 32f
                coil.lineTo(x + sin(t * Math.PI.toFloat() * 8f) * amplitude, -2f - t * (h - 4f))
            }
        }
        drawPath(coil, ink, style = Stroke(width = 5.4f, cap = StrokeCap.Round))
        drawPath(coil, steel, style = Stroke(width = 3.2f, cap = StrokeCap.Round))
        drawPath(coil, Color(0xFFEDF6DD), style = Stroke(width = 1.0f, cap = StrokeCap.Round))

        val headColor = if (bumper.flashTimer > 0f) Color(0xFF6BE1C6) else enamel
        drawLine(ink, Offset(-half, -h), Offset(half, -h), 18f, StrokeCap.Round)
        drawLine(headColor, Offset(-half, -h), Offset(half, -h), 12f, StrokeCap.Round)
        drawLine(Color(0xFFD8F4DD), Offset(-half + 4f, -h - 4f), Offset(half - 4f, -h - 4f), 2.6f, StrokeCap.Round)
        drawDirectionOverlay(bumper, -h - 6f)
    }
}
