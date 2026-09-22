package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** The weight must physically press the switch; no pin-to-answer shortcut. */
object WeightSwitchLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)
    fun create() = LevelDefinition(
        number = 9, name = "A Weighty Key",
        initialCreatures = listOf(
            Creature(CreatureId.PIP, Vector2D(630f, 350f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(710f, 350f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(790f, 350f), radius = 36f)),
        pins = listOf(
            Pin(PinId.PIN_A, "A", "Weight restraint", Vector2D(70f, 400f), Vector2D(290f, 400f),
                length = 220f, color = OneMoveVisualTheme.Pins.pinA, handlePosition = Vector2D(330f, 400f)),
            Pin(PinId.PIN_B, "B", "Bridge to the locked gate", Vector2D(520f, 480f), Vector2D(950f, 770f),
                length = 519f, color = OneMoveVisualTheme.Pins.pinB, handlePosition = Vector2D(470f, 455f)),
            Pin(PinId.PIN_C, "C", "Sanctuary floor", Vector2D(755f, 1465f), Vector2D(1045f, 1465f),
                length = 290f, color = OneMoveVisualTheme.Pins.pinC, handlePosition = Vector2D(705f, 1465f))),
        platforms = listOf(rail(70f, 460f, 70f, 980f), rail(290f, 460f, 290f, 980f),
            rail(600f, 1000f, 755f, 1260f), rail(1100f, 800f, 1045f, 1270f),
            rail(755f, 1220f, 755f, 1465f), rail(1045f, 1220f, 1045f, 1465f)),
        heavyBalls = listOf(HeavyBall(Vector2D(180f, 330f))),
        pressurePlates = listOf(PressurePlate("brass-key", Vector2D(180f, 900f), width = 180f)),
        creatureGates = listOf(CreatureGate(Vector2D(900f, 500f), Vector2D(900f, 1040f),
            requiredPlateIds = listOf("brass-key"))),
        dangerPits = listOf(DangerPit(Rect2D(40f, 1100f, 680f, 1540f))),
        goalZone = GoalZone(Vector2D(900f, 1360f), 170f),
        primaryMechanics = "Weight, pressure switch and a visibly linked gate",
        newConceptIntroduced = "Follow the cable: the weight is the key.",
        solutionPinId = PinId.PIN_A)
}
