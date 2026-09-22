package com.example.onemove.model

data class LevelDefinition(
    val number: Int,
    val id: String = "LEVEL_${String.format("%02d", number)}",
    val name: String,
    val initialCreatures: List<Creature> = emptyList(),
    val pins: List<Pin> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val dangerPits: List<DangerPit> = emptyList(),
    val springBumpers: List<SpringBumper> = emptyList(),
    val seesaws: List<SeesawLever> = emptyList(),
    val heavyBalls: List<HeavyBall> = emptyList(),
    val rollingStones: List<RollingStone> = emptyList(),
    val creatureGates: List<CreatureGate> = emptyList(),
    val goalZone: GoalZone = GoalZone(Vector2D(600f, 1400f)),
    val primaryMechanics: String = "Tactical Pin Pull",
    val newConceptIntroduced: String = "Basic Mechanics",
    val solutionPinId: PinId = PinId.PIN_A
) {
    val levelNumber: Int get() = number
    val title: String get() = name
    val expectedWinningPin: PinId get() = solutionPinId
    val seesaw: SeesawLever? get() = seesaws.firstOrNull()
    val heavyBall: HeavyBall? get() = heavyBalls.firstOrNull()
    val rollingStone: RollingStone? get() = rollingStones.firstOrNull()

    companion object {
        const val WORLD_WIDTH = 1200f
        const val WORLD_HEIGHT = 1600f
    }
}
