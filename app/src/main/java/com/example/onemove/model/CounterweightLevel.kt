package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** The falling weight, lever, landing floor and guard are all causally necessary. */
object CounterweightLevel {
    private fun rail(ax:Float, ay:Float, bx:Float, by:Float) =
        Platform(Vector2D(ax,ay),Vector2D(bx,by),thickness=20f)
    fun create() = LevelDefinition(
        number=6, name="The Counterweight",
        initialCreatures=listOf(
            Creature(CreatureId.PIP,Vector2D(445f,620f),radius=32f),
            Creature(CreatureId.MOCHI,Vector2D(510f,620f),radius=34f),
            Creature(CreatureId.BLOBBO,Vector2D(575f,620f),radius=36f)),
        pins=listOf(
            Pin(PinId.PIN_A,"A","Counterweight restraint",Vector2D(665f,450f),Vector2D(855f,450f),
                length=190f,color=OneMoveVisualTheme.Pins.pinA,handlePosition=Vector2D(895f,450f)),
            Pin(PinId.PIN_B,"B","Landing floor",Vector2D(755f,1465f),Vector2D(1045f,1465f),
                length=290f,color=OneMoveVisualTheme.Pins.pinB,handlePosition=Vector2D(705f,1465f)),
            Pin(PinId.PIN_C,"C","Landing guard",Vector2D(1045f,1180f),Vector2D(1045f,1465f),
                length=285f,color=OneMoveVisualTheme.Pins.pinC,handlePosition=Vector2D(1045f,1120f))),
        platforms=listOf(rail(755f,1210f,755f,1465f)),
        seesaws=listOf(SeesawLever(Vector2D(600f,900f),halfLength=290f,angle=-0.1f)),
        heavyBalls=listOf(HeavyBall(Vector2D(760f,380f))),
        dangerPits=listOf(DangerPit(Rect2D(40f,1110f,680f,1550f))),
        goalZone=GoalZone(Vector2D(900f,1360f),170f),
        primaryMechanics="Release the counterweight to tilt the bridge",
        newConceptIntroduced="A weight can help when it lands on the other end.",
        solutionPinId=PinId.PIN_A)
}
