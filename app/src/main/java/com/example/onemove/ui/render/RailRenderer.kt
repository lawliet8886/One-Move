package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.onemove.model.Platform
import com.example.onemove.ui.theme.OneMoveVisualTheme

object RailRenderer {
    fun drawPlatformRail(drawScope: DrawScope, platform: Platform) {
        with(drawScope) {
            val start = Offset(platform.start.x, platform.start.y)
            val end = Offset(platform.end.x, platform.end.y)
            val thickness = platform.thickness

            // 1. Drop Shadow
            drawLine(
                color = OneMoveVisualTheme.Rails.shadowColor,
                start = start + Offset(0f, 7f),
                end = end + Offset(0f, 7f),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )

            // 2. Dark Structural Underside
            drawLine(
                color = OneMoveVisualTheme.Rails.darkUnderside,
                start = start + Offset(0f, 1.5f),
                end = end + Offset(0f, 1.5f),
                strokeWidth = thickness + 2f,
                cap = StrokeCap.Round
            )

            // 3. Brushed Titanium Body
            drawLine(
                color = platform.color,
                start = start,
                end = end,
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )

            // 4. Specular Top Highlight Edge
            drawLine(
                color = OneMoveVisualTheme.Rails.specularEdge,
                start = start - Offset(0f, thickness * 0.25f),
                end = end - Offset(0f, thickness * 0.25f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // 5. Endpoint Rivet Caps
            for (pt in listOf(start, end)) {
                drawCircle(
                    color = OneMoveVisualTheme.Rails.rivetHousing,
                    radius = thickness * 0.55f,
                    center = pt
                )
                drawCircle(
                    color = OneMoveVisualTheme.Rails.rivetCore,
                    radius = thickness * 0.35f,
                    center = pt
                )
            }
        }
    }
}
