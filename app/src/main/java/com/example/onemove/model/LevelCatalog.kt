package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Physical recovery campaign: each board has a continuous, collision-tested route.
 * solutionPinId is documentation for QA only; PhysicsWorld never reads it.
 * Distinct bridge, spring, counterweight and shield puzzles replace recovery boards.
 * The remaining recovery funnels are not a completed difficulty curve.
 */
object LevelCatalog {
    private val colors = listOf(OneMoveVisualTheme.Pins.pinA, OneMoveVisualTheme.Pins.pinB,
        OneMoveVisualTheme.Pins.pinC, OneMoveVisualTheme.Pins.pinD)

    private fun board(number: Int, name: String, release: PinId, pinCount: Int,
                      startX: Float, goalX: Float, supportY: Float = 420f,
                      gate: Boolean = false, heavy: Boolean = false,
                      stone: Boolean = false, seesaw: Boolean = false): LevelDefinition {
        val c = listOf(
            Creature(CreatureId.PIP, Vector2D(startX - 84f, supportY - 66f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(startX, supportY - 68f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(startX + 84f, supportY - 70f), radius = 36f)
        )
        val ids = PinId.values().take(pinCount)
        var decoy = 0
        val pins = ids.map { id ->
            if (id == release) Pin(id, id.name.takeLast(1), "Release the cradle",
                Vector2D(startX - 165f, supportY), Vector2D(startX + 165f, supportY),
                pullDirection = Vector2D(-1f, 0f), length = 330f, color = colors[id.ordinal],
                handlePosition = Vector2D(startX - 200f, supportY))
            else {
                val n = decoy++
                val left = if (startX >= 600f) 80f else 925f
                val y = 360f + n * 200f
                Pin(id, id.name.takeLast(1), "Side mechanism support",
                    Vector2D(left, y), Vector2D(left + 190f, y), length = 190f,
                    color = colors[id.ordinal], handlePosition = Vector2D(if (left < 600f) left - 30f else left + 220f, y))
            }
        }
        val rails = listOf(
            Platform(Vector2D(startX - 205f, supportY + 70f), Vector2D(goalX - 102f, 1110f), thickness = 20f),
            Platform(Vector2D(startX + 205f, supportY + 70f), Vector2D(goalX + 102f, 1110f), thickness = 20f),
            Platform(Vector2D(goalX - 145f, 1220f), Vector2D(goalX - 145f, 1465f), thickness = 20f),
            Platform(Vector2D(goalX + 145f, 1220f), Vector2D(goalX + 145f, 1465f), thickness = 20f),
            Platform(Vector2D(goalX - 145f, 1465f), Vector2D(goalX + 145f, 1465f), thickness = 24f)
        )
        val sidePin = pins.first { it.id != release }
        val sideX = (sidePin.start.x + sidePin.end.x) * 0.5f
        val pitLeft = if (goalX > 600f) 45f else 920f
        return LevelDefinition(number = number, name = name, initialCreatures = c,
            pins = pins, platforms = rails,
            dangerPits = listOf(DangerPit(Rect2D(pitLeft, 1180f, pitLeft + 225f, 1480f))),
            goalZone = GoalZone(Vector2D(goalX, 1360f), radius = 170f),
            heavyBalls = if (heavy) listOf(HeavyBall(Vector2D(sideX, sidePin.start.y - 54f))) else emptyList(),
            rollingStones = if (stone) listOf(RollingStone(Vector2D(sideX, sidePin.start.y - 46f))) else emptyList(),
            seesaws = if (seesaw) listOf(SeesawLever(Vector2D(sideX, 990f), halfLength = 96f, angle = 0.18f)) else emptyList(),
            creatureGates = if (gate) listOf(CreatureGate(Vector2D(goalX - 115f, 1170f), Vector2D(goalX + 115f, 1170f), releasePinId = release)) else emptyList(),
            primaryMechanics = if (gate) "Gravity, rails and a linked gate" else "Gravity, solid supports and guide rails",
            newConceptIntroduced = when {
                heavy -> "Heavy-body hazard"
                stone -> "Rolling-body hazard"
                seesaw -> "Weighted pivot"
                gate -> "Mechanical gate linkage"
                number == 1 -> "One move. Rescue all three."
                else -> "Trace the route before pulling."
            }, solutionPinId = release)
    }

    fun createLevel01() = board(1, "First Drop", PinId.PIN_A, 2, 600f, 600f)
    fun createLevel02() = PuzzlePrototypes.bridge()
    fun createLevel03() = SwitchbackLevel.create()
    fun createLevel04() = board(4, "Sanctuary Gateway", PinId.PIN_C, 3, 600f, 600f, 660f, gate = true)
    fun createLevel05() = PuzzlePrototypes.springGap()
    fun createLevel06() = CounterweightLevel.create()
    fun createLevel07() = board(7, "Iron Wrecker", PinId.PIN_A, 3, 650f, 480f, heavy = true)
    fun createLevel08() = PuzzlePrototypes.shield()
    fun createLevel09() = WeightSwitchLevel.create()
    fun createLevel10() = ReturnFlightLevel.create()
    fun createLevel11() = FlyingKeyLevel.create()
    fun createLevel12() = GrandMachineLevel.create()

    val ALL_LEVELS: List<LevelDefinition> = listOf(createLevel01(), createLevel02(), createLevel03(), createLevel04(),
        createLevel05(), createLevel06(), createLevel07(), createLevel08(), createLevel09(), createLevel10(), createLevel11(), createLevel12())

    fun getLevel(number: Int) = ALL_LEVELS[(number - 1).coerceIn(0, ALL_LEVELS.lastIndex)]
}
