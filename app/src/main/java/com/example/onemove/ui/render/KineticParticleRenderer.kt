package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.Particle
import com.example.onemove.model.ParticleType

object KineticParticleRenderer {

    fun drawParticles(drawScope: DrawScope, particles: List<Particle>) {
        with(drawScope) {
            for (p in particles) {
                if (!p.isAlive) continue
                val alpha = p.alpha.coerceIn(0f, 1f)
                val c = p.color.copy(alpha = alpha)
                val pos = Offset(p.position.x, p.position.y)

                when (p.type) {
                    ParticleType.CONFETTI -> {
                        withTransform({
                            translate(pos.x, pos.y)
                            rotate(p.rotation)
                        }) {
                            drawRect(
                                color = c,
                                topLeft = Offset(-p.size * 0.5f, -p.size * 0.25f),
                                size = Size(p.size, p.size * 0.5f)
                            )
                        }
                    }
                    ParticleType.SPARK -> {
                        drawCircle(color = c, radius = p.size * 0.5f, center = pos)
                        drawCircle(color = Color.White.copy(alpha = alpha), radius = p.size * 0.25f, center = pos)
                    }
                    ParticleType.DUST, ParticleType.SMOKE -> {
                        drawCircle(color = c, radius = p.size * (1.5f - p.progress * 0.5f), center = pos)
                    }
                    ParticleType.GOAL_SPARKLE, ParticleType.HEART, ParticleType.POP -> {
                        drawCircle(color = c, radius = p.size * 0.5f, center = pos)
                    }
                }
            }
        }
    }
}
