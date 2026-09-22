package com.example.onemove.physics

import com.example.onemove.model.Vector2D

enum class TelemetryEventType {
    WRECKER_SEESAW_IMPACT,
    WRECKER_ENTER_CATCH,
    WRECKER_SETTLED_IN_CATCH,
    CREATURE_GATE_RELEASE,
    FIRST_CREATURE_SPRING_CONTACT
}

data class TelemetryEvent(
    val type: TelemetryEventType,
    val time: Float,
    val position: Vector2D,
    val velocity: Vector2D,
    val details: String = ""
)
