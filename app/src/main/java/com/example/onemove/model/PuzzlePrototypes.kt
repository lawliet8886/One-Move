package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Three candidate causal puzzles. No trajectory/answer-key logic lives here. */
object PuzzlePrototypes {
    private val palette = listOf(OneMoveVisualTheme.Pins.pinA, OneMoveVisualTheme.Pins.pinB,
        OneMoveVisualTheme.Pins.pinC, OneMoveVisualTheme.Pins.pinD)
    private fun pin(id: PinId, ax: Float, ay: Float, bx: Float, by: Float,
                    hx: Float, hy: Float, description: String) = Pin(id, id.name.takeLast(1), description,
        Vector2D(ax, ay), Vector2D(bx, by), color = palette[id.ordinal],
        length = Vector2D(bx - ax, by - ay).length(), handlePosition = Vector2D(hx, hy))
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness = 20f)
    private fun friends(x: Float, y: Float) = listOf(
        Creature(CreatureId.PIP, Vector2D(x-80f,y), radius = 32f),
        Creature(CreatureId.MOCHI, Vector2D(x,y), radius = 34f),
        Creature(CreatureId.BLOBBO, Vector2D(x+80f,y), radius = 36f))
    private fun nest(x: Float) = listOf(rail(x-145f,1200f,x-145f,1465f),
        rail(x+145f,1200f,x+145f,1465f),rail(x-145f,1465f,x+145f,1465f))

    fun bridge() = LevelDefinition(number = 2, name = "Keep the Bridge", initialCreatures = friends(300f, 290f),
        pins = listOf(
            pin(PinId.PIN_A,120f,420f,840f,900f,95f,400f,"Bridge across the danger basin"),
            pin(PinId.PIN_B,700f,420f,700f,920f,700f,360f,"Barrier across the bridge")),
        platforms = listOf(rail(1080f,850f,925f,1250f),rail(590f,1060f,655f,1250f)) + nest(800f),
        dangerPits = listOf(DangerPit(Rect2D(60f,700f,520f,1490f))),
        goalZone = GoalZone(Vector2D(800f,1360f),170f),
        primaryMechanics = "Keep the bridge; remove the obstruction",
        newConceptIntroduced = "Not every pin is meant to be removed.", solutionPinId = PinId.PIN_B)

    fun springGap() = LevelDefinition(number = 5, name = "Mind the Gap", initialCreatures = listOf(
        Creature(CreatureId.PIP,Vector2D(330f,300f),radius=32f),
        Creature(CreatureId.MOCHI,Vector2D(330f,460f),radius=34f),
        Creature(CreatureId.BLOBBO,Vector2D(330f,620f),radius=36f)),
        pins = listOf(
            pin(PinId.PIN_A,755f,1465f,1045f,1465f,705f,1465f,"Landing floor above the basin"),
            pin(PinId.PIN_B,1045f,900f,1045f,1465f,1045f,840f,"Outer landing guard"),
            pin(PinId.PIN_C,160f,730f,500f,730f,105f,730f,"Cover above the spring")),
        platforms = listOf(rail(755f,1280f,755f,1465f)),
        springBumpers = listOf(SpringBumper(Vector2D(330f,880f),direction=Vector2D(0.85f,-0.40f),width=360f)),
        dangerPits = listOf(DangerPit(Rect2D(60f,1070f,710f,1550f))),
        goalZone = GoalZone(Vector2D(900f,1360f),170f),
        primaryMechanics = "A spring must carry the friends across a real gap",
        newConceptIntroduced = "Trace the bounce, not just the fall.", solutionPinId = PinId.PIN_C)

    fun shield() = LevelDefinition(number = 8, name = "Weight of a Choice", initialCreatures = friends(720f,680f),
        pins = listOf(
            pin(PinId.PIN_A,530f,570f,900f,570f,480f,570f,"Shield holding the falling weight"),
            pin(PinId.PIN_B,580f,620f,580f,1100f,580f,1160f,"Exit barrier"),
            pin(PinId.PIN_C,520f,800f,940f,660f,990f,645f,"Bridge under the friends")),
        platforms = listOf(rail(280f,1070f,390f,1290f),rail(590f,1210f,680f,1450f)) + nest(500f),
        heavyBalls = listOf(HeavyBall(Vector2D(720f,270f))),
        dangerPits = listOf(DangerPit(Rect2D(800f,940f,1130f,1490f))),
        goalZone = GoalZone(Vector2D(500f,1360f),170f),
        primaryMechanics = "Keep the shield and bridge while opening the exit",
        newConceptIntroduced = "The heavy object is part of the decision.", solutionPinId = PinId.PIN_B)
}
