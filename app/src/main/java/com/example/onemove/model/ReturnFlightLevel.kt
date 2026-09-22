package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Bounce across the gap, keep the return bridge, and open its obstruction. */
object ReturnFlightLevel {
    private fun rail(ax: Float, ay: Float, bx: Float, by: Float) =
        Platform(Vector2D(ax, ay), Vector2D(bx, by), thickness=20f)
    fun create() = LevelDefinition(number=10, name="The Return Flight",
        initialCreatures=listOf(
            Creature(CreatureId.PIP,Vector2D(330f,300f),radius=32f),
            Creature(CreatureId.MOCHI,Vector2D(330f,460f),radius=34f),
            Creature(CreatureId.BLOBBO,Vector2D(330f,620f),radius=36f)),
        pins=listOf(
            Pin(PinId.PIN_A,"A","Return bridge",Vector2D(410f,1250f),Vector2D(1030f,980f),
                length=676f,color=OneMoveVisualTheme.Pins.pinA,handlePosition=Vector2D(1090f,980f)),
            Pin(PinId.PIN_B,"B","Obstruction on the return bridge",Vector2D(650f,950f),Vector2D(650f,1300f),
                length=350f,color=OneMoveVisualTheme.Pins.pinB,handlePosition=Vector2D(650f,890f)),
            Pin(PinId.PIN_C,"C","Sanctuary floor",Vector2D(155f,1465f),Vector2D(445f,1465f),
                length=290f,color=OneMoveVisualTheme.Pins.pinC,handlePosition=Vector2D(100f,1465f)),
            Pin(PinId.PIN_D,"D","Far landing guard",Vector2D(1045f,750f),Vector2D(1045f,1410f),
                length=660f,color=OneMoveVisualTheme.Pins.pinD,handlePosition=Vector2D(1045f,690f))),
        platforms=listOf(rail(155f,1210f,155f,1465f),rail(445f,1340f,445f,1465f)),
        springBumpers=listOf(SpringBumper(Vector2D(330f,880f),direction=Vector2D(0.85f,-0.40f),width=360f)),
        dangerPits=listOf(DangerPit(Rect2D(190f,990f,475f,1110f)),
            DangerPit(Rect2D(510f,1240f,1135f,1540f))),
        goalZone=GoalZone(Vector2D(300f,1360f),170f),
        primaryMechanics="Spring flight, a far guard and a return bridge",
        newConceptIntroduced="Trace the return trip: the landing is not the destination.",
        solutionPinId=PinId.PIN_B)
}
