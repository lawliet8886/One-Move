package com.example.onemove.ui.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.onemove.model.GoalZone
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Premium rescue dock. The GoalZone remains the only gameplay authority. */
object RescueSanctuaryRenderer {
    private const val CELL = 256
    const val DECODED_BYTES = CELL * CELL * 4

    @Volatile private var atlas: ImageBitmap? = null
    @Volatile private var framing = 2.18f
    @Volatile private var lampOffsets = floatArrayOf(-0.23f, 0f, 0.23f)
    @Volatile private var lampY = -0.32f

    val isPrepared: Boolean get() = atlas != null

    @Synchronized
    fun prepare(context: Context) {
        if (atlas != null) return
        val bytes = context.assets.open("sanctuary/rescue_halo.webp").use { it.readBytes() }
        val manifest = context.assets.open("sanctuary/rescue_halo.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        check(manifest.getBoolean("approved_for_runtime")) { "Sanctuary atlas is not approved" }
        check(sha256(bytes) == manifest.getString("atlas_sha256")) {
            "Sanctuary atlas hash mismatch"
        }

        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Cannot decode sanctuary atlas"
        }
        check(bitmap.width == CELL && bitmap.height == CELL && bitmap.hasAlpha()) {
            "Sanctuary atlas geometry/alpha mismatch"
        }
        check(bitmap.allocationByteCount == DECODED_BYTES) {
            "Unexpected sanctuary decoded size"
        }

        val bounds = manifest.getJSONArray("alpha_bounds")
        check(
            bounds.getInt(0) >= 2 && bounds.getInt(1) >= 2 &&
                bounds.getInt(2) <= CELL - 2 && bounds.getInt(3) <= CELL - 2
        ) { "Sanctuary art touches the atlas edge" }

        framing = manifest.getDouble("framing").toFloat()
        val offsets = manifest.getJSONArray("lamp_offsets_radius")
        lampOffsets = FloatArray(offsets.length()) { offsets.getDouble(it).toFloat() }
        lampY = manifest.getDouble("lamp_y_radius").toFloat()
        atlas = bitmap.asImageBitmap()

        Log.i(
            "OneMoveAssets",
            "Sanctuary atlas ready: " + bitmap.width + "x" + bitmap.height +
                "; " + bytes.size + " compressed bytes; " +
                DECODED_BYTES + " decoded bytes"
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun draw(scope: DrawScope, goal: GoalZone, rescued: Int) {
        val image = atlas
        if (image == null) {
            drawVectorFallback(scope, goal, rescued)
            return
        }

        with(scope) {
            val c = Offset(goal.center.x, goal.center.y)
            val r = goal.radius
            val glow = Brush.radialGradient(
                listOf(Color(0xFF8CFFD0).copy(alpha = 0.16f), Color.Transparent),
                c + Offset(0f, r * 0.18f),
                r * 1.28f
            )
            drawCircle(glow, r * 1.28f, c)
            drawCircle(
                Color(0xFF65A986).copy(alpha = 0.32f),
                r,
                c,
                style = Stroke(2f)
            )
            drawOval(
                Color(0xFF142D27).copy(alpha = 0.20f),
                c + Offset(-r * 1.08f, r * 0.78f),
                Size(r * 2.16f, r * 0.18f)
            )

            val sideF = r * framing
            val side = sideF.roundToInt().coerceAtLeast(1)
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(CELL, CELL),
                dstOffset = IntOffset(
                    (c.x - sideF / 2f).roundToInt(),
                    (c.y - sideF / 2f).roundToInt()
                ),
                dstSize = IntSize(side, side),
                filterQuality = FilterQuality.Medium
            )
            drawStatusLamps(c, r, rescued)
        }
    }

    private fun DrawScope.drawStatusLamps(center: Offset, radius: Float, rescued: Int) {
        for (index in lampOffsets.indices) {
            val lamp = center + Offset(lampOffsets[index] * radius, lampY * radius)
            drawCircle(
                Color(0xFF102D28).copy(alpha = 0.92f),
                radius * 0.050f,
                lamp
            )
            val lit = index < rescued.coerceIn(0, 3)
            drawCircle(
                if (lit) Color(0xFFB9FFD2) else Color(0xFF657C70),
                radius * 0.031f,
                lamp
            )
            if (lit) {
                drawCircle(
                    Color(0xFFFFFFE8),
                    radius * 0.010f,
                    lamp - Offset(radius * 0.009f, radius * 0.010f)
                )
            }
        }
    }

    private class Shell(val x: Float, val y: Float, val r: Float) {
        val center = Offset(x, y)
        val outer = arch(x, y - r * 0.94f, r * 2.10f, y + r * 0.64f)
        val inner = arch(x, y - r * 0.62f, r * 1.68f, y + r * 0.54f)
        val brass = Brush.linearGradient(
            listOf(Color(0xFFFFE5AF), Color(0xFFDEAF68), Color(0xFF8B5938)),
            Offset(x-r, y-r), Offset(x+r, y+r)
        )
        val recess = Brush.radialGradient(
            listOf(Color(0xFF517C68), Color(0xFF173E35), Color(0xFF0B2927)),
            Offset(x, y+r*0.5f), r*1.3f
        )

        val glow = Brush.radialGradient(
            listOf(Color(0xFFFFDF94).copy(alpha=0.45f), Color.Transparent),
            center,
            r*1.4f
        )
        val sill = Brush.verticalGradient(
            listOf(Color(0xFFDAF0B8), Color(0xFF87B590), Color(0xFF456D55)),
            y+r*0.5f,
            y+r*0.74f
        )
        val home = Path().apply {
            moveTo(x-r*0.105f, y-r*0.69f)
            lineTo(x, y-r*0.79f)
            lineTo(x+r*0.105f, y-r*0.69f)
            moveTo(x-r*0.073f, y-r*0.69f)
            lineTo(x-r*0.073f, y-r*0.59f)
            lineTo(x+r*0.073f, y-r*0.59f)
            lineTo(x+r*0.073f, y-r*0.69f)
        }
    }

    private var cached: Shell? = null

    private fun shell(goal: GoalZone): Shell {
        val old = cached
        if (old != null && old.x == goal.center.x &&
            old.y == goal.center.y && old.r == goal.radius) return old
        return Shell(goal.center.x, goal.center.y, goal.radius)
            .also { cached = it }
    }

    private fun arch(cx: Float, top: Float, width: Float, bottom: Float) =
        Path().apply {
            val left = cx-width/2f
            val right = cx+width/2f
            val shoulder = top+width*0.43f
            moveTo(left,bottom)
            lineTo(left,shoulder)
            cubicTo(left,top,cx-width*0.22f,top,cx,top)
            cubicTo(cx+width*0.22f,top,right,top,right,shoulder)
            lineTo(right,bottom)
            close()
        }

    private fun drawVectorFallback(
        scope: DrawScope,
        goal: GoalZone,
        rescued: Int
    ) = with(scope) {
        val s = shell(goal)
        val c = s.center
        val r = s.r
        drawCircle(s.glow,r*1.4f,c)
        drawCircle(Color(0xFF528A68).copy(alpha=0.30f),r,c,style=Stroke(2f))
        drawOval(
            Color(0xFF213C32).copy(alpha=0.23f),
            c+Offset(-r*1.1f,r*0.61f),
            Size(r*2.3f,r*0.20f)
        )
        drawPath(s.outer,Color(0xFF4C3C2F),style=Stroke(12f))
        drawPath(s.outer,s.brass)
        drawPath(s.outer,Color(0xFFFFEBC1),style=Stroke(3f))
        drawPath(s.inner,Color(0xFF775734),style=Stroke(8f))
        drawPath(s.inner,s.recess)
        drawPath(s.inner,Color(0xFFEDD09A),style=Stroke(2f))
        for (side in listOf(-1f,1f)) {
            val x=c.x+side*r*0.94f
            drawLine(
                Color(0xFFFFE6B3).copy(alpha=0.65f),
                Offset(x,c.y-r*0.12f),
                Offset(x,c.y+r*0.48f),
                2.5f,
                StrokeCap.Round
            )
        }
        val badge=c+Offset(0f,-r*0.69f)
        drawCircle(Color(0xFF2D5144),r*0.15f,badge)
        drawCircle(Color(0xFFFFE8B7),r*0.15f,badge,style=Stroke(2f))
        drawPath(s.home,Color(0xFFFFEAC1),style=Stroke(2.8f,cap=StrokeCap.Round))
        drawRoundRect(
            s.sill,
            c+Offset(-r*1.03f,r*0.52f),
            Size(r*2.06f,r*0.20f),
            CornerRadius(10f)
        )
        drawStatusLamps(c,r,rescued)
    }
}
