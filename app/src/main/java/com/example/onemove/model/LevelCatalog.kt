package com.example.onemove.model

import com.example.onemove.ui.theme.OneMoveVisualTheme

/** Twelve handcrafted physical layouts. Expected answers are test metadata only. */
object LevelCatalog {
    private fun v(x: Float, y: Float) = Vector2D(x, y)
    private fun rail(x1: Float, y1: Float, x2: Float, y2: Float) = Platform(v(x1,y1), v(x2,y2), 20f)
    private val colors = listOf(OneMoveVisualTheme.Pins.pinA, OneMoveVisualTheme.Pins.pinB, OneMoveVisualTheme.Pins.pinC, OneMoveVisualTheme.Pins.pinD)

    private fun makePin(id: PinId, left: Float, right: Float, y: Float, outwardRight: Boolean = false): Pin = Pin(
        id = id, name = id.name.takeLast(1), description = "Puxe para retirar esta trava",
        start = v(left,y), end = v(right,y), thickness = 20f, length = right-left,
        pullDirection = v(if (outwardRight) 1f else -1f,0f), color = colors[id.ordinal],
        handlePosition = v(if (outwardRight) right + 42f else left - 42f,y)
    )

    private fun bowl(goalX: Float): List<Platform> = listOf(
        rail((goalX - 440f).coerceAtLeast(80f), 1040f, goalX - 115f, 1300f),
        rail((goalX + 440f).coerceAtMost(1120f), 1040f, goalX + 115f, 1300f),
        rail(goalX - 180f,1450f,goalX + 180f,1450f),
        rail(goalX - 180f,1380f,goalX - 180f,1450f),
        rail(goalX + 180f,1380f,goalX + 180f,1450f)
    )

    private fun level(number: Int, name: String, answer: PinId, count: Int, spawnX: Float = 600f, goalX: Float = 600f,
                      tracks: List<Platform> = emptyList(), springs: List<SpringBumper> = emptyList(),
                      levers: List<SeesawLever> = emptyList(), balls: List<HeavyBall> = emptyList(),
                      stones: List<RollingStone> = emptyList(), gate: Boolean = false, hint: String): LevelDefinition {
        val ids = PinId.values().take(count)
        var decoy = 0
        val pins = ids.map { id ->
            if (id == answer) {
                if (gate) makePin(id, 795f, 960f, 760f, true)
                else makePin(id,spawnX-170f,spawnX+170f,360f,number % 2 == 0)
            } else {
                val index = decoy++
                when(index) {
                    0 -> makePin(id,120f,290f,170f)
                    1 -> makePin(id,910f,1080f,170f,true)
                    else -> makePin(id,490f,700f,120f)
                }
            }
        }
        val start = if (gate) 600f else spawnX
        val c = listOf(
            Creature(CreatureId.PIP,v(start-100f,260f),radius=40f),
            Creature(CreatureId.MOCHI,v(start,260f),radius=40f),
            Creature(CreatureId.BLOBBO,v(start+100f,260f),radius=40f)
        )
        val gates = if (gate) listOf(CreatureGate(pivot=v(795f,680f),closedEnd=v(405f,680f),thickness=22f,releasePinId=answer)) else emptyList()
        val hazards = listOf(DangerPit(Rect2D(42f,1400f,150f,1570f)),DangerPit(Rect2D(1050f,1400f,1158f,1570f)))
        return LevelDefinition(number=number,name=name,initialCreatures=c,pins=pins,
            platforms=tracks+bowl(goalX),dangerPits=hazards,springBumpers=springs,seesaws=levers,
            heavyBalls=balls,rollingStones=stones,creatureGates=gates,
            goalZone=GoalZone(v(goalX,1350f),radius=190f),primaryMechanics=hint,newConceptIntroduced=hint,solutionPinId=answer)
    }

    fun createLevel01() = level(1,"Primeiro voo",PinId.PIN_A,2,
        tracks=listOf(rail(340f,710f,475f,940f),rail(860f,710f,725f,940f)),
        hint="Uma trava. Três amigos. Encontre o caminho para casa.")
    fun createLevel02() = level(2,"Curva de mel",PinId.PIN_B,2,spawnX=350f,goalX=760f,
        tracks=listOf(rail(150f,520f,710f,950f)),
        hint="A rampa transforma a queda em movimento para o lado.")
    fun createLevel03() = level(3,"Caminho de volta",PinId.PIN_C,3,spawnX=830f,goalX=440f,
        tracks=listOf(rail(1040f,520f,470f,950f)),
        hint="Observe a inclinação antes de escolher.")
    fun createLevel04() = level(4,"Porta do refúgio",PinId.PIN_C,3,gate=true,
        tracks=listOf(rail(330f,420f,475f,575f),rail(870f,420f,725f,575f)),
        hint="A trava lateral libera a dobradiça. Não o chão.")
    fun createLevel05() = level(5,"Pulo de alegria",PinId.PIN_C,3,spawnX=420f,goalX=770f,
        springs=listOf(SpringBumper(v(430f,735f),v(0.6f,-0.8f),width=340f)),
        tracks=listOf(rail(980f,650f,970f,1060f)),
        hint="A mola devolve energia: antecipe o salto.")
    fun createLevel06() = level(6,"Peso de amizade",PinId.PIN_A,3,spawnX=450f,
        levers=listOf(SeesawLever(v(600f,820f),halfLength=290f,angle=-0.12f)),
        hint="O peso dos amigos inclina a gangorra.")
    fun createLevel07() = level(7,"Gigante de ferro",PinId.PIN_A,3,spawnX=650f,goalX=650f,
        tracks=listOf(rail(120f,620f,310f,900f),rail(310f,900f,340f,900f),rail(340f,750f,340f,900f)),
        balls=listOf(HeavyBall(v(210f,460f),radius=48f)),
        hint="Mantenha o peso de ferro fora do caminho dos amigos.")
    fun createLevel08() = level(8,"Pedra viajante",PinId.PIN_B,3,spawnX=750f,goalX=680f,
        tracks=listOf(rail(200f,430f,490f,750f),rail(490f,750f,520f,930f),rail(430f,940f,520f,940f)),
        stones=listOf(RollingStone(v(290f,350f),radius=42f)),
        hint="Pedras e amigos têm massa: colisões mudam o movimento.")
    fun createLevel09() = level(9,"Passagem secreta",PinId.PIN_A,3,gate=true,goalX=650f,
        tracks=listOf(rail(370f,890f,520f,1040f)),
        hint="Abra a passagem e acompanhe a queda até o refúgio.")
    fun createLevel10() = level(10,"Duplo impulso",PinId.PIN_B,4,spawnX=400f,goalX=700f,
        springs=listOf(SpringBumper(v(420f,650f),v(0.6f,-0.8f),width=320f),SpringBumper(v(900f,1050f),v(-0.6f,-0.8f),width=170f)),
        hint="Cada contato com a mola tem direção e intensidade.")
    fun createLevel11() = level(11,"Zigue-zague",PinId.PIN_A,4,spawnX=360f,goalX=500f,
        tracks=listOf(rail(190f,490f,730f,820f),rail(990f,850f,500f,1080f)),
        hint="Siga a sequência das rampas, não só a primeira queda.")
    fun createLevel12() = level(12,"A grande máquina",PinId.PIN_C,4,gate=true,
        levers=listOf(SeesawLever(v(600f,910f),halfLength=270f,angle=-0.28f)),
        stones=listOf(RollingStone(v(185f,100f),radius=40f)),
        tracks=listOf(rail(80f,550f,210f,710f)),
        hint="Dobradiça, peso e gravidade. Um único movimento.")

    val ALL_LEVELS = listOf(createLevel01(),createLevel02(),createLevel03(),createLevel04(),createLevel05(),createLevel06(),createLevel07(),createLevel08(),createLevel09(),createLevel10(),createLevel11(),createLevel12())
    fun getLevel(number: Int) = ALL_LEVELS[(number-1).coerceIn(0,ALL_LEVELS.lastIndex)]
}
