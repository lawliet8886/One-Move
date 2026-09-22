package com.example.onemove.model

/** A visible, solid weight switch. It latches only after sustained top contact. */
data class PressurePlate(
    val id: String,
    val center: Vector2D,
    val width: Float = 150f,
    val thickness: Float = 18f,
    val minimumMass: Float = 4f,
    val holdSeconds: Float = 0.08f,
    var contactTime: Float = 0f,
    var isLatched: Boolean = false
) {
    init {
        require(id.isNotBlank())
        require(center.x.isFinite() && center.y.isFinite())
        require(width.isFinite() && width > 0f)
        require(thickness.isFinite() && thickness > 0f)
        require(minimumMass.isFinite() && minimumMass > 0f)
        require(holdSeconds.isFinite() && holdSeconds > 0f)
    }
}
