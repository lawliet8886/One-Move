package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/**
 * The iron wrecker is useful only after it drops into its open-left pocket and becomes a bumper.
 * Friends reach the junction later, collide with the settled weight and are diverted left.
 */
object WreckerBumperLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)

    fun create() = LevelDefinition(
        number = 7,
        name = "Iron Wrecker",
        initialCreatures = listOf(
            Creature(CreatureId.PIP, Vector2D(870f, 330f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(950f, 330f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(1030f, 330f), radius = 36f)
        ),
        pins = listOf(
            Pin(
                PinId.PIN_A, "A", "Wrecker restraint",
                Vector2D(680f, 652f), Vector2D(840f, 652f), length = 160f,
                color = OneMoveVisualTheme.Pins.pinA, handlePosition = Vector2D(640f, 652f)
            ),
            Pin(
                PinId.PIN_B, "B", "Safe diversion ramp",
                Vector2D(670f, 950f), Vector2D(520f, 1150f), length = 250f,
                color = OneMoveVisualTheme.Pins.pinB, handlePosition = Vector2D(710f, 930f)
            ),
            Pin(
                PinId.PIN_C, "C", "Sanctuary floor",
                Vector2D(355f, 1465f), Vector2D(645f, 1465f), length = 290f,
                color = OneMoveVisualTheme.Pins.pinC, handlePosition = Vector2D(690f, 1465f)
            )
        ),
        platforms = listOf(
            // Long approach delays the friends so the wrecker can settle first.
            // One clean chute: gravity carries the trio down-left toward the settled bumper.
            rail(840f, 820f, 1080f, 520f),
            // Open-left pocket: the ball settles against the right stop while friends
            // can still be redirected out of the pocket toward the removable safe ramp.
            rail(680f, 920f, 850f, 950f),
            rail(850f, 820f, 850f, 950f),
            // Straight sanctuary corridor: no elbow that can park a rescued friend.
            rail(355f, 1120f, 355f, 1465f),
            rail(645f, 1120f, 645f, 1465f)
        ),
        heavyBalls = listOf(
            HeavyBall(Vector2D(760f, 600f))
        ),
        dangerPits = listOf(
            // B spans this narrow gap. Without the removable safe ramp the diverted
            // friends drop here instead of magically falling into the sanctuary lane.
            DangerPit(Rect2D(600f, 1030f, 700f, 1220f)),
            DangerPit(Rect2D(720f, 1030f, 1180f, 1540f))
        ),
        goalZone = GoalZone(Vector2D(500f, 1360f), radius = 170f),
        primaryMechanics = "Drop the wrecker into its pocket so the weight becomes the safety bumper",
        newConceptIntroduced = "A heavy object can redirect the route instead of merely threatening it.",
        solutionPinId = PinId.PIN_A
    )
}
