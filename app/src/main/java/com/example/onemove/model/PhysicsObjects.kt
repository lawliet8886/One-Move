package com.example.onemove.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

enum class PinId {
    PIN_A,
    PIN_B,
    PIN_C,
    PIN_D
}

enum class CreatureId {
    PIP,
    MOCHI,
    BLOBBO
}

enum class CreatureExpression {
    NORMAL,
    HAPPY,
    SCARED,
    DIZZY,
    SURPRISED,
    CURIOUS,
    PANIC,
    DISAPPOINTED
}

data class Creature(
    val id: CreatureId,
    var position: Vector2D,
    var velocity: Vector2D = Vector2D.Zero,
    val radius: Float = 32f,
    var expression: CreatureExpression = CreatureExpression.NORMAL,
    var isInsideGoal: Boolean = false,
    var inGoalTime: Float = 0f,
    var isGrounded: Boolean = false,
    var isTrapped: Boolean = false
)

data class Pin(
    val id: PinId,
    val name: String,
    val description: String = "",
    var start: Vector2D,
    var end: Vector2D,
    val pullDirection: Vector2D = Vector2D(-1f, 0f),
    val length: Float = 140f,
    val thickness: Float = 16f,
    val color: Color = Color(0xFFF59E0B),
    var handlePosition: Vector2D = start,
    var isRemoved: Boolean = false,
    var isSelected: Boolean = false,
    var pullProgress: Float = 0f
)

data class Platform(
    val start: Vector2D,
    val end: Vector2D,
    val thickness: Float = 18f,
    val color: Color = Color(0xFF334155),
    val isBouncy: Boolean = false
)

data class GoalZone(
    val center: Vector2D,
    val radius: Float = 120f,
    val bounds: Rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
)

data class Rect2D(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class DangerPit(
    val bounds: Rect2D
) {
    constructor(rect: Rect) : this(
        Rect2D(rect.left, rect.top, rect.right, rect.bottom)
    )
}

data class SpringBumper(
    var position: Vector2D,
    val direction: Vector2D = Vector2D(0f, -1f),
    val width: Float = 90f,
    val restHeight: Float = 35f,
    var compression: Float = 0f,
    var flashTimer: Float = 0f
)

data class SeesawLever(
    val pivot: Vector2D,
    val halfLength: Float = 170f,
    val thickness: Float = 18f,
    var angle: Float = 0f,
    var angularVelocity: Float = 0f
) {
    val fulcrum: Vector2D get() = pivot
}

data class HeavyBall(
    var position: Vector2D,
    var velocity: Vector2D = Vector2D.Zero,
    val radius: Float = 44f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
)

data class RollingStone(
    var position: Vector2D,
    var velocity: Vector2D = Vector2D.Zero,
    val radius: Float = 36f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
)

data class CreatureGate(
    val pivot: Vector2D,
    val closedEnd: Vector2D,
    val thickness: Float = 16f,
    var openProgress: Float = 0f,
    var isOpen: Boolean = false,
    val releasePinId: PinId? = null,
    val requiredPlateIds: List<String> = emptyList()
)
