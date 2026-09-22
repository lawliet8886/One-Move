package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Two bridges form a return route. Removing a bridge is not the solution. */
object SwitchbackLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)
    fun create() = LevelDefinition(
        number = 3, name = "The Long Way Round",
        initialCreatures = listOf(
            Creature(CreatureId.PIP, Vector2D(340f, 330f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(420f, 330f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(500f, 330f), radius = 36f)),
        pins = listOf(
            Pin(PinId.PIN_A, "A", "Outbound bridge", Vector2D(160f, 500f), Vector2D(870f, 810f),
                length = 775f, color = OneMoveVisualTheme.Pins.pinA, handlePosition = Vector2D(110f, 478f)),
            Pin(PinId.PIN_B, "B", "Return bridge", Vector2D(1110f, 880f), Vector2D(500f, 1220f),
                length = 698f, color = OneMoveVisualTheme.Pins.pinB, handlePosition = Vector2D(1150f, 852f)),
            Pin(PinId.PIN_C, "C", "Outbound barrier", Vector2D(740f, 560f), Vector2D(740f, 890f),
                length = 330f, color = OneMoveVisualTheme.Pins.pinC, handlePosition = Vector2D(740f, 500f))),
        platforms = listOf(rail(1130f, 660f, 1100f, 1010f),
            rail(305f, 1130f, 305f, 1470f), rail(595f, 1280f, 595f, 1470f), rail(305f, 1470f, 595f, 1470f)),
        dangerPits = listOf(DangerPit(Rect2D(40f, 800f, 410f, 1080f)),
            DangerPit(Rect2D(700f, 1270f, 1150f, 1550f))),
        goalZone = GoalZone(Vector2D(450f, 1365f), 170f),
        primaryMechanics = "Preserve both bridges and follow the return route",
        newConceptIntroduced = "The safe path can first lead away from home.",
        solutionPinId = PinId.PIN_C)
}
