package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.example.onemove.model.Platform
import com.example.onemove.ui.theme.OneMoveVisualTheme

object RailRenderer {
    fun drawPlatformRail(drawScope: DrawScope, platform: Platform) {
        with(drawScope) {
            val start = Offset(platform.start.x, platform.start.y)
            val end = Offset(platform.end.x, platform.end.y)
            val thickness = platform.thickness
            val body = lerp(platform.color, Color(0xFFB6C8D8), 0.34f)

            // Deep cast shadow gives every physical route a readable layer above the chassis.
            drawLine(OneMoveVisualTheme.Rails.shadowColor, start + Offset(0f, 9f), end + Offset(0f, 9f), thickness + 6f, StrokeCap.Round)
            drawLine(OneMoveVisualTheme.Rails.darkUnderside, start + Offset(0f, 3f), end + Offset(0f, 3f), thickness + 4f, StrokeCap.Round)

            // Brighter satin body. Geometry remains exactly the physical segment.
            drawLine(body, start, end, thickness, StrokeCap.Round)
            drawLine(Color(0xFFCBD5E1).copy(alpha = 0.72f), start - Offset(0f, thickness * 0.26f), end - Offset(0f, thickness * 0.26f), 3.2f, StrokeCap.Round)
            drawLine(Color(0xFF0F172A).copy(alpha = 0.72f), start + Offset(0f, thickness * 0.30f), end + Offset(0f, thickness * 0.30f), 2.2f, StrokeCap.Round)

            // Endpoint collars make joints look assembled rather than like loose lines.
            for (pt in listOf(start, end)) {
                drawCircle(Color.Black.copy(alpha = 0.35f), thickness * 0.68f, pt + Offset(0f, 3f))
                drawCircle(OneMoveVisualTheme.Rails.rivetHousing, thickness * 0.58f, pt)
                drawCircle(OneMoveVisualTheme.Rails.rivetCore, thickness * 0.36f, pt)
                drawCircle(Color(0xFFFFF1C7).copy(alpha = 0.65f), thickness * 0.12f, pt - Offset(2f, 2f))
            }
        }
    }
}
