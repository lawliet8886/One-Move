package com.example.onemove.ui.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.onemove.model.Pin

object PinRenderer {
    private val lettering = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun drawPin(drawScope: DrawScope, pin: Pin, isReady: Boolean = true) {
        if (pin.isRemoved && pin.pullProgress >= 1f) return
        with(drawScope) {
            val progress = if (pin.isRemoved) pin.pullProgress.coerceIn(0f, 1f) else 0f
            val shift = pin.pullDirection * (pin.length * progress)
            val start = Offset(pin.start.x + shift.x, pin.start.y + shift.y)
            val end = Offset(pin.end.x + shift.x, pin.end.y + shift.y)
            val handle = Offset(pin.handlePosition.x + shift.x, pin.handlePosition.y + shift.y)
            val alpha = 1f - progress
            drawLine(Color.Black.copy(alpha = 0.35f * alpha), start + Offset(0f, 7f), end + Offset(0f, 7f), pin.thickness + 4f, StrokeCap.Round)
            drawLine(Color(0xFF64748B).copy(alpha = alpha), start, end, pin.thickness, StrokeCap.Round)
            drawLine(pin.color.copy(alpha = alpha), start, end, pin.thickness * 0.48f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.7f * alpha), start - Offset(0f, 4f), end - Offset(0f, 4f), 2f, StrokeCap.Round)
            if (isReady) drawCircle(Brush.radialGradient(listOf(pin.color.copy(alpha = 0.24f), Color.Transparent), handle, 56f), 56f, handle)
            drawCircle(Color.Black.copy(alpha = 0.38f * alpha), 36f, handle + Offset(0f, 6f))
            drawCircle(Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFB45309)), handle - Offset(30f, 30f), handle + Offset(30f, 30f)), 36f, handle, alpha = alpha)
            drawCircle(Color(0xFF0F172A).copy(alpha = alpha), 27f, handle)
            drawCircle(pin.color.copy(alpha = alpha), 27f, handle, style = Stroke(3f))
            lettering.alpha = (255 * alpha).toInt()
            drawIntoCanvas { it.nativeCanvas.drawText(pin.name, handle.x, handle.y - (lettering.ascent() + lettering.descent()) / 2f, lettering) }
        }
    }
}
