package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Early causal gate: release the rolling key while preserving its ramp and the sanctuary floor. */
object GatewayKeyLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)

    fun create() = LevelDefinition(
        number = 4,
        name = "Sanctuary Gateway",
        initialCreatures = listOf(
            Creature(CreatureId.PIP, Vector2D(760f, 350f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(840f, 350f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(920f, 350f), radius = 36f)
        ),
        pins = listOf(
            Pin(
                PinId.PIN_A, "A", "Sanctuary floor",
                Vector2D(755f, 1465f), Vector2D(1045f, 1465f), length = 290f,
                color = OneMoveVisualTheme.Pins.pinA, handlePosition = Vector2D(705f, 1465f)
            ),
            Pin(
                PinId.PIN_B, "B", "Key catcher",
                Vector2D(680f, 820f), Vector2D(680f, 1020f), length = 200f,
                color = OneMoveVisualTheme.Pins.pinB, handlePosition = Vector2D(715f, 860f)
            ),
            Pin(
                PinId.PIN_C, "C", "Rolling key restraint",
                Vector2D(100f, 410f), Vector2D(330f, 410f), length = 230f,
                color = OneMoveVisualTheme.Pins.pinC, handlePosition = Vector2D(370f, 410f)
            )
        ),
        platforms = listOf(
            rail(80f, 620f, 350f, 760f),
            rail(70f, 520f, 70f, 1040f),
            rail(350f, 760f, 610f, 900f),
            rail(640f, 500f, 980f, 660f),
            rail(760f, 720f, 760f, 1120f),
            rail(700f, 1080f, 755f, 1260f),
            rail(1170f, 1080f, 1045f, 1260f),
            rail(755f, 1220f, 755f, 1465f),
            rail(1045f, 1220f, 1045f, 1465f)
        ),
        rollingStones = listOf(RollingStone(Vector2D(215f, 330f))),
        pressurePlates = listOf(
            PressurePlate("gateway-key", Vector2D(570f, 908f), width = 190f, minimumMass = 3f, holdSeconds = 0.25f)
        ),
        creatureGates = listOf(
            CreatureGate(
                Vector2D(700f, 1000f), Vector2D(1170f, 1000f),
                requiredPlateIds = listOf("gateway-key")
            )
        ),
        goalZone = GoalZone(Vector2D(900f, 1360f), radius = 170f),
        primaryMechanics = "Rolling key, pressure lock and sanctuary gate",
        newConceptIntroduced = "Release the key. Keep its bridge and the sanctuary floor.",
        solutionPinId = PinId.PIN_C
    )
}

