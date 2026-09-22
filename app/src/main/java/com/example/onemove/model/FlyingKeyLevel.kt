package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Launch the heavy key into its switch pocket while preserving the rescue bridge. */
object FlyingKeyLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax,ay),Vector2D(bx,by),thickness=20f)
    fun create() = LevelDefinition(number=11,name="The Flying Key",
        initialCreatures=listOf(
            Creature(CreatureId.PIP,Vector2D(690f,340f),radius=32f),
            Creature(CreatureId.MOCHI,Vector2D(770f,340f),radius=34f),
            Creature(CreatureId.BLOBBO,Vector2D(850f,340f),radius=36f)),
        pins=listOf(
            Pin(PinId.PIN_A,"A","Flying key restraint",Vector2D(80f,400f),Vector2D(280f,400f),
                length=200f,color=OneMoveVisualTheme.Pins.pinA,handlePosition=Vector2D(330f,400f)),
            Pin(PinId.PIN_B,"B","Rescue bridge",Vector2D(620f,460f),Vector2D(980f,700f),
                length=420f,color=OneMoveVisualTheme.Pins.pinB,handlePosition=Vector2D(570f,430f)),
            Pin(PinId.PIN_C,"C","Sanctuary floor",Vector2D(755f,1465f),Vector2D(1045f,1465f),
                length=290f,color=OneMoveVisualTheme.Pins.pinC,handlePosition=Vector2D(705f,1465f)),
            Pin(PinId.PIN_D,"D","Key catcher",Vector2D(730f,610f),Vector2D(730f,1010f),
                length=400f,color=OneMoveVisualTheme.Pins.pinD,handlePosition=Vector2D(730f,555f))),
        platforms=listOf(rail(80f,1070f,290f,1070f),rail(450f,1040f,730f,1040f),
            rail(450f,840f,450f,1020f),rail(670f,1120f,755f,1260f),
            rail(1100f,500f,1045f,1270f),rail(755f,1220f,755f,1465f),rail(1045f,1220f,1045f,1465f)),
        heavyBalls=listOf(HeavyBall(Vector2D(180f,330f))),
        springBumpers=listOf(SpringBumper(Vector2D(180f,780f),direction=Vector2D(0.85f,-0.55f),width=210f)),
        pressurePlates=listOf(PressurePlate("flying-key",Vector2D(585f,980f),width=270f)),
        creatureGates=listOf(CreatureGate(Vector2D(750f,920f),Vector2D(1080f,920f),
            requiredPlateIds=listOf("flying-key"))),
        dangerPits=listOf(DangerPit(Rect2D(40f,1120f,670f,1540f))),
        goalZone=GoalZone(Vector2D(900f,1360f),170f),
        primaryMechanics="Spring-launched weight, catcher, pressure lock and rescue bridge",
        newConceptIntroduced="The spring delivers a key, not a friend. Keep its catcher.",
        solutionPinId=PinId.PIN_A)
}
