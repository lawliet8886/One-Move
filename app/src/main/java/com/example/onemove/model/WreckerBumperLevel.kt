package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/**
 * Heavy-mass lesson: the iron wrecker alone can latch the industrial switch.
 * The switch opens a staging gate; the trio then follows the preserved diversion ramp.
 */
object WreckerBumperLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)

    fun create() = LevelDefinition(
        number = 7,
        name = "Iron Wrecker",
        initialCreatures = listOf(
            Creature(CreatureId.PIP, Vector2D(820f, 330f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(900f, 330f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(980f, 330f), radius = 36f)
        ),
        pins = listOf(
            Pin(
                PinId.PIN_A, "A", "Wrecker restraint",
                Vector2D(90f, 430f), Vector2D(290f, 430f), length = 200f,
                color = OneMoveVisualTheme.Pins.pinA, handlePosition = Vector2D(335f, 430f)
            ),
            Pin(
                PinId.PIN_B, "B", "Safe diversion ramp",
                Vector2D(840f, 820f), Vector2D(610f, 1120f), length = 380f,
                color = OneMoveVisualTheme.Pins.pinB, handlePosition = Vector2D(885f, 800f)
            ),
            Pin(
                PinId.PIN_C, "C", "Sanctuary floor",
                Vector2D(355f, 1465f), Vector2D(645f, 1465f), length = 290f,
                color = OneMoveVisualTheme.Pins.pinC, handlePosition = Vector2D(690f, 1465f)
            )
        ),
        platforms = listOf(
            // Isolated wrecker shaft: friends never share the industrial switch lane.
            rail(80f, 500f, 80f, 1000f),
            rail(300f, 500f, 300f, 1000f),
            // Backup shaft floor is below the plate. With the switch removed, the wrecker
            // settles here so the blocked machine fails causally instead of timing out.
            rail(100f, 970f, 280f, 970f),
            // Friends wait behind the gate, then slide onto the removable B ramp.
            rail(1080f, 520f, 840f, 820f),
            rail(355f, 1120f, 355f, 1465f),
            rail(645f, 1120f, 645f, 1465f)
        ),
        heavyBalls = listOf(HeavyBall(Vector2D(190f, 350f))),
        pressurePlates = listOf(
            // Mass threshold deliberately excludes creatures and rolling stones.
            PressurePlate("wrecker-set", Vector2D(190f, 950f), width = 180f, minimumMass = 4f, holdSeconds = 0.12f)
        ),
        creatureGates = listOf(
            CreatureGate(
                Vector2D(760f, 500f), Vector2D(1120f, 500f),
                requiredPlateIds = listOf("wrecker-set")
            )
        ),
        dangerPits = listOf(
            // With B intact the trio is already left of x=650 before reaching this depth.
            // Removing B drops them directly into this basin.
            DangerPit(Rect2D(650f, 1120f, 1180f, 1540f))
        ),
        goalZone = GoalZone(Vector2D(500f, 1360f), radius = 170f),
        primaryMechanics = "Heavy-duty switch, linked gate and diversion ramp",
        newConceptIntroduced = "Only the iron wrecker is heavy enough to unlock this gate.",
        solutionPinId = PinId.PIN_A
    )
}
