package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/**
 * Campaign finale: one pull releases TWO physical keys. Both switches must latch
 * before the creature gate opens. The other three pins are real structural parts.
 * No answer-key data is read by PhysicsWorld.
 */
object GrandMachineLevel {
    private fun rail(ax:Float, ay:Float, bx:Float, by:Float, thickness:Float=20f) =
        Platform(Vector2D(ax,ay),Vector2D(bx,by),thickness=thickness)

    fun create() = LevelDefinition(
        number=12,
        name="The Grand Machine",
        initialCreatures=listOf(
            Creature(CreatureId.PIP,Vector2D(760f,300f),radius=32f),
            Creature(CreatureId.MOCHI,Vector2D(840f,300f),radius=34f),
            Creature(CreatureId.BLOBBO,Vector2D(920f,300f),radius=36f)
        ),
        pins=listOf(
            Pin(PinId.PIN_A,"A","Rescue bridge",Vector2D(620f,500f),Vector2D(1030f,780f),
                length=497f,color=OneMoveVisualTheme.Pins.pinA,handlePosition=Vector2D(570f,470f)),
            Pin(PinId.PIN_B,"B","Sanctuary floor",Vector2D(755f,1465f),Vector2D(1045f,1465f),
                length=290f,color=OneMoveVisualTheme.Pins.pinB,handlePosition=Vector2D(705f,1465f)),
            Pin(PinId.PIN_C,"C","Twin-key restraint",Vector2D(80f,420f),Vector2D(520f,420f),
                length=440f,color=OneMoveVisualTheme.Pins.pinC,handlePosition=Vector2D(565f,420f)),
            Pin(PinId.PIN_D,"D","Rolling-key catcher",Vector2D(790f,760f),Vector2D(790f,1050f),
                length=290f,color=OneMoveVisualTheme.Pins.pinD,handlePosition=Vector2D(790f,700f))
        ),
        platforms=listOf(
            // Heavy-key shaft.
            rail(80f,470f,80f,965f), rail(290f,470f,290f,965f),
            // Rolling-key ramp and a harmless parking floor when its catcher is removed.
            rail(330f,570f,680f,820f), rail(790f,1050f,1080f,1050f),
            // Creature approach and final chute.
            rail(1080f,770f,1080f,1120f),
            rail(1080f,1120f,1045f,1260f),
            rail(755f,1220f,755f,1465f), rail(1045f,1220f,1045f,1465f)
        ),
        heavyBalls=listOf(HeavyBall(Vector2D(180f,350f))),
        rollingStones=listOf(RollingStone(Vector2D(430f,350f))),
        pressurePlates=listOf(
            PressurePlate("heavy-key",Vector2D(180f,940f),width=180f,minimumMass=4f),
            PressurePlate("rolling-key",Vector2D(700f,940f),width=180f,minimumMass=3f)
        ),
        creatureGates=listOf(
            CreatureGate(Vector2D(700f,980f),Vector2D(1100f,980f),
                requiredPlateIds=listOf("heavy-key","rolling-key"))
        ),
        dangerPits=listOf(
            DangerPit(Rect2D(40f,1100f,700f,1540f))
        ),
        goalZone=GoalZone(Vector2D(900f,1360f),170f),
        primaryMechanics="Twin physical keys, an AND gate, a rescue bridge and a catcher",
        newConceptIntroduced="The finale opens only when both keys arrive. One pull starts the whole machine.",
        solutionPinId=PinId.PIN_C
    )
}
