package com.example.onemove.model

/** Handles win over rods; ties use distance then stable ID, never list order. */
object PinHitTester {
    fun find(pins: List<Pin>, x: Float, y: Float, handleRadius: Float = 65f, rodRadius: Float = 40f): PinId? {
        if (!x.isFinite() || !y.isFinite() || !handleRadius.isFinite() || !rodRadius.isFinite()) return null
        if (handleRadius <= 0f || rodRadius <= 0f) return null
        val point = Vector2D(x, y)
        val available = pins.filterNot { it.isRemoved }
        val handle = available.map { it to point.distanceTo(it.handlePosition) }
            .filter { it.second <= handleRadius }
            .minWithOrNull(compareBy<Pair<Pin, Float>> { it.second }.thenBy { it.first.id.ordinal })
        if (handle != null) return handle.first.id
        return available.map { pin ->
            val segment = pin.end - pin.start
            val fraction = if (segment.lengthSquared() > 0f)
                ((point - pin.start).dot(segment) / segment.lengthSquared()).coerceIn(0f, 1f) else 0f
            pin to point.distanceTo(pin.start + segment * fraction)
        }.filter { it.second <= rodRadius }
            .minWithOrNull(compareBy<Pair<Pin, Float>> { it.second }.thenBy { it.first.id.ordinal })?.first?.id
    }
}
