package com.example.onemove.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.onemove.ui.theme.OneMoveVisualTheme

object LevelCatalog {

    fun createLevel01(): LevelDefinition {
        val creatures = listOf(
            Creature(CreatureId.PIP, Vector2D(540f, 280f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(600f, 280f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(660f, 280f), radius = 36f)
        )
        val pins = listOf(
            Pin(PinId.PIN_A, "A", "Safe Ramp Release", Vector2D(460f, 380f), Vector2D(740f, 380f), Vector2D(-1f, 0f), 280f, 16f, OneMoveVisualTheme.Pins.pinA, Vector2D(410f, 380f)),
            Pin(PinId.PIN_B, "B", "Danger Chute Release", Vector2D(460f, 620f), Vector2D(740f, 620f), Vector2D(1f, 0f), 280f, 16f, OneMoveVisualTheme.Pins.pinB, Vector2D(790f, 620f))
        )
        val platforms = listOf(
            Platform(Vector2D(420f, 380f), Vector2D(460f, 380f)),
            Platform(Vector2D(740f, 380f), Vector2D(780f, 380f)),
            Platform(Vector2D(350f, 800f), Vector2D(650f, 1100f)),
            Platform(Vector2D(850f, 800f), Vector2D(550f, 1100f))
        )
        val dangerPits = listOf(
            DangerPit(Rect(700f, 900f, 980f, 1150f))
        )
        val goalZone = GoalZone(Vector2D(600f, 1350f), radius = 130f)
        return LevelDefinition(
            number = 1,
            name = "First Drop",
            initialCreatures = creatures,
            pins = pins,
            platforms = platforms,
            dangerPits = dangerPits,
            goalZone = goalZone,
            primaryMechanics = "Tactical Pin Release",
            newConceptIntroduced = "Basic Gravity & Sanctuary Goal",
            solutionPinId = PinId.PIN_A
        )
    }

    fun createLevel02(): LevelDefinition {
        return createSimpleLevel(2, "Dual Chute", PinId.PIN_B, 2)
    }

    fun createLevel03(): LevelDefinition {
        return createSimpleLevel(3, "Triple Trap", PinId.PIN_C, 3)
    }

    fun createLevel04(): LevelDefinition {
        val creatures = listOf(
            Creature(CreatureId.PIP, Vector2D(500f, 260f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(580f, 260f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(660f, 260f), radius = 36f)
        )
        val pins = listOf(
            Pin(PinId.PIN_A, "A", "Spike Diverter", Vector2D(320f, 440f), Vector2D(540f, 440f), Vector2D(-1f, 0f), 220f, 16f, OneMoveVisualTheme.Pins.pinA, Vector2D(280f, 440f)),
            Pin(PinId.PIN_B, "B", "Basin Trapdoor", Vector2D(660f, 440f), Vector2D(880f, 440f), Vector2D(1f, 0f), 220f, 16f, OneMoveVisualTheme.Pins.pinB, Vector2D(920f, 440f)),
            Pin(PinId.PIN_C, "C", "Sanctuary Chute", Vector2D(480f, 680f), Vector2D(720f, 680f), Vector2D(-1f, 0f), 240f, 16f, OneMoveVisualTheme.Pins.pinC, Vector2D(440f, 680f))
        )
        val platforms = listOf(
            Platform(Vector2D(280f, 440f), Vector2D(320f, 440f)),
            Platform(Vector2D(880f, 440f), Vector2D(920f, 440f)),
            Platform(Vector2D(360f, 750f), Vector2D(580f, 980f)),
            Platform(Vector2D(840f, 750f), Vector2D(620f, 980f))
        )
        val dangerPits = listOf(
            DangerPit(Rect(720f, 950f, 1000f, 1180f))
        )
        val goalZone = GoalZone(Vector2D(600f, 1340f), radius = 135f)
        return LevelDefinition(
            number = 4,
            name = "Sanctuary Gateway",
            initialCreatures = creatures,
            pins = pins,
            platforms = platforms,
            dangerPits = dangerPits,
            goalZone = goalZone,
            primaryMechanics = "Chute Alignment & Safe Drop",
            newConceptIntroduced = "Triple Mascot Sequential Docking",
            solutionPinId = PinId.PIN_C
        )
    }

    fun createLevel05(): LevelDefinition {
        return createSimpleLevel(5, "Spring Launcher", PinId.PIN_C, 3)
    }

    fun createLevel06(): LevelDefinition {
        return createSimpleLevel(6, "Seesaw Fulcrum", PinId.PIN_A, 3)
    }

    fun createLevel07(): LevelDefinition {
        return createSimpleLevel(7, "Iron Wrecker", PinId.PIN_A, 3)
    }

    fun createLevel08(): LevelDefinition {
        return createSimpleLevel(8, "Granite Roll", PinId.PIN_B, 3)
    }

    fun createLevel09(): LevelDefinition {
        return createSimpleLevel(9, "Creature Gate", PinId.PIN_A, 3)
    }

    fun createLevel10(): LevelDefinition {
        return createSimpleLevel(10, "Bumper Cascade", PinId.PIN_B, 4)
    }

    fun createLevel11(): LevelDefinition {
        return createSimpleLevel(11, "Danger Maze", PinId.PIN_A, 4)
    }

    fun createLevel12(): LevelDefinition {
        return createSimpleLevel(12, "The Grand Machine", PinId.PIN_C, 4)
    }

    private fun createSimpleLevel(num: Int, name: String, winner: PinId, pinCount: Int): LevelDefinition {
        val creatures = listOf(
            Creature(CreatureId.PIP, Vector2D(500f, 260f), radius = 32f),
            Creature(CreatureId.MOCHI, Vector2D(580f, 260f), radius = 34f),
            Creature(CreatureId.BLOBBO, Vector2D(660f, 260f), radius = 36f)
        )
        val allPinIds = listOf(PinId.PIN_A, PinId.PIN_B, PinId.PIN_C, PinId.PIN_D)
        val colors = listOf(
            OneMoveVisualTheme.Pins.pinA,
            OneMoveVisualTheme.Pins.pinB,
            OneMoveVisualTheme.Pins.pinC,
            OneMoveVisualTheme.Pins.pinD
        )
        val pins = (0 until pinCount).map { i ->
            val pid = allPinIds[i]
            val y = 400f + i * 160f
            Pin(
                id = pid,
                name = pid.name.takeLast(1),
                description = "Tactical Pin ${pid.name.takeLast(1)}",
                start = Vector2D(460f, y),
                end = Vector2D(740f, y),
                pullDirection = if (i % 2 == 0) Vector2D(-1f, 0f) else Vector2D(1f, 0f),
                length = 280f,
                thickness = 16f,
                color = colors[i],
                handlePosition = if (i % 2 == 0) Vector2D(410f, y) else Vector2D(790f, y)
            )
        }
        val platforms = listOf(
            Platform(Vector2D(360f, 750f), Vector2D(580f, 1050f)),
            Platform(Vector2D(840f, 750f), Vector2D(620f, 1050f))
        )
        val dangerPits = listOf(
            DangerPit(Rect(700f, 950f, 980f, 1180f))
        )
        val goalZone = GoalZone(Vector2D(600f, 1340f), radius = 135f)
        return LevelDefinition(
            number = num,
            name = name,
            initialCreatures = creatures,
            pins = pins,
            platforms = platforms,
            dangerPits = dangerPits,
            goalZone = goalZone,
            primaryMechanics = "Kinetic Physics",
            newConceptIntroduced = "Mechanics Progression",
            solutionPinId = winner
        )
    }

    val ALL_LEVELS: List<LevelDefinition> = listOf(
        createLevel01(),
        createLevel02(),
        createLevel03(),
        createLevel04(),
        createLevel05(),
        createLevel06(),
        createLevel07(),
        createLevel08(),
        createLevel09(),
        createLevel10(),
        createLevel11(),
        createLevel12()
    )

    fun getLevel(number: Int): LevelDefinition {
        val idx = (number - 1).coerceIn(0, ALL_LEVELS.size - 1)
        return ALL_LEVELS[idx]
    }
}
