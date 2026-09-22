package com.example.onemove.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class ParticleType {
    DUST,
    SMOKE,
    SPARK,
    CONFETTI,
    GOAL_SPARKLE,
    HEART,
    POP
}

data class Particle(
    var position: Vector2D,
    var velocity: Vector2D,
    var color: Color,
    val size: Float,
    val maxLife: Float,
    var life: Float = maxLife,
    val type: ParticleType = ParticleType.DUST,
    var rotation: Float = Random.nextFloat() * 360f,
    var rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 360f
) {
    val isAlive: Boolean
        get() = life > 0f

    val progress: Float
        get() = (life / maxLife).coerceIn(0f, 1f)

    val alpha: Float
        get() = when (type) {
            ParticleType.CONFETTI -> if (progress < 0.2f) progress / 0.2f else 1f
            else -> progress
        }

    fun update(dt: Float) {
        life -= dt
        position += velocity * dt
        rotation += rotationSpeed * dt

        when (type) {
            ParticleType.CONFETTI -> {
                velocity += Vector2D(0f, 300f) * dt // gentle gravity
                velocity *= 0.98f // drag
            }
            ParticleType.DUST, ParticleType.SMOKE -> {
                velocity *= 0.90f // quick drag
            }
            ParticleType.SPARK -> {
                velocity += Vector2D(0f, 600f) * dt
            }
            ParticleType.GOAL_SPARKLE -> {
                velocity *= 0.95f
            }
            ParticleType.HEART -> {
                velocity = Vector2D(velocity.x * 0.95f, velocity.y - 40f * dt) // float up
            }
            ParticleType.POP -> {
                velocity *= 0.85f
            }
        }
    }
}
