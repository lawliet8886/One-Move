package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import kotlin.math.abs

/** Reusable contracts: run unchanged under JUnit and the documented headless JVM adapter. */
object PhysicsContractCases {
    data class Report(val simulations: Int, val checks: Int, val maxWinSeconds: Float, val rows: List<String>)
    private val winners = listOf(PinId.PIN_A, PinId.PIN_B, PinId.PIN_C, PinId.PIN_C,
        PinId.PIN_C, PinId.PIN_A, PinId.PIN_A, PinId.PIN_B, PinId.PIN_A, PinId.PIN_B, PinId.PIN_A, PinId.PIN_C)

    fun run(): Report {
        var checks = 0
        var simulations = 0
        var maxWin = 0f
        val rows = mutableListOf("level,pin,schedule,repetition,state,time_seconds,rescued,contacts,reason")
        fun verify(value: Boolean, message: String) { checks++; check(value) { message } }
        fun execute(level: LevelDefinition, pin: PinId, timing: FloatArray): PhysicsWorld {
            simulations++
            val world = PhysicsWorld(level)
            verify(world.pullPin(pin), "Valid pin was rejected: ${level.number}/$pin")
            var frame = 0
            while (world.state == SimulationState.RUNNING && frame < 4000) world.step(timing[frame++ % timing.size])
            verify(world.state != SimulationState.RUNNING, "No terminal outcome: ${level.number}/$pin")
            return world
        }
        val schedules = listOf(floatArrayOf(1f/30f), floatArrayOf(1f/60f), floatArrayOf(1f/90f),
            floatArrayOf(1f/120f), floatArrayOf(0.009f,0.022f,0.016f,0.031f,0.012f))
        for (level in LevelCatalog.ALL_LEVELS) for (pin in level.pins) {
            var signature: String? = null
            for ((scheduleIndex, timing) in schedules.withIndex()) repeat(3) { rep ->
                val w = execute(level, pin.id, timing)
                val expected = if (pin.id == winners[level.number - 1]) SimulationState.SUCCESS else SimulationState.FAILED
                verify(w.state == expected, "Wrong outcome ${level.number}/${pin.id}: ${w.state}/${w.failureReason}")
                verify(w.failureReason != "SIMULATION_TIMEOUT", "Campaign exceeded physical outcome budget")
                for (c in w.creatures) {
                    verify(c.position.x.isFinite() && c.position.y.isFinite(), "Non-finite position")
                    if (c.isInsideGoal) verify(c.position.distanceTo(w.goalZone.center) + c.radius <= w.goalZone.radius + 0.1f,
                        "Rescued outside the actual goal: ${level.number}/${c.id}")
                }
                for (i in w.creatures.indices) for (j in i+1 until w.creatures.size) {
                    val a=w.creatures[i]; val b=w.creatures[j]
                    verify(a.position.distanceTo(b.position) >= a.radius+b.radius-0.4f, "Creature overlap: ${level.number}")
                }
                val sig = "${w.state}/${w.simulationTime}/${w.failureReason}/" + w.creatures.joinToString { "${it.position}/${it.isInsideGoal}" }
                if (signature == null) signature = sig
                verify(signature == sig, "Physics depends on render frame rate or reset seed: ${level.number}/${pin.id}/$scheduleIndex")
                if(w.state==SimulationState.SUCCESS) {
                    maxWin = maxOf(maxWin,w.simulationTime)
                    verify(w.collisionCount > 0, "Winning route never contacted physical geometry")
                }
                rows += "${level.number},${pin.id},$scheduleIndex,$rep,${w.state},${w.simulationTime},${w.creatures.count{it.isInsideGoal}},${w.collisionCount},${w.failureReason}"
            }
        }
        // Counterfactual test: label changes may NOT change the simulated trajectory.
        for (l in LevelCatalog.ALL_LEVELS) {
            val expectedPin = winners[l.number-1]
            val fakeKey = PinId.values().first { it != expectedPin }
            val real = execute(l, expectedPin, schedules[1])
            val relabelled = execute(l.copy(solutionPinId = fakeKey), expectedPin, schedules[1])
            verify(real.state == relabelled.state && real.creatures.map { it.position } == relabelled.creatures.map { it.position },
                "The engine reads the answer key")
            val noNest = execute(l.copy(goalZone = GoalZone(Vector2D(-5000f,-5000f))), expectedPin, schedules[1])
            verify(noNest.state != SimulationState.SUCCESS && noNest.creatures.none { it.isInsideGoal }, "Teleport to moved nest")
        }
        val initial = LevelCatalog.createLevel01()
        val reset = PhysicsWorld(initial)
        verify(!reset.pullPin(PinId.PIN_D), "Nonexistent pin accepted")
        verify(reset.state == SimulationState.READY && reset.chosenPinId == null, "Invalid input consumed a move")
        reset.pullPin(PinId.PIN_A)
        verify(!reset.pullPin(PinId.PIN_B), "Second move accepted")
        repeat(80) { reset.step(1f/60f) }
        reset.reset()
        verify(reset.state == SimulationState.READY && reset.events.isEmpty() && reset.pins.none { it.isRemoved }, "Reset leaks state")
        verify(reset.creatures.map { it.position } == initial.initialCreatures.map { it.position }, "Reset mutates level templates")
        for (bad in listOf(Float.NaN,Float.POSITIVE_INFINITY,-0.1f,0f)) reset.step(bad)
        verify(reset.simulationTime==0f && reset.visualTime.isFinite(), "Invalid delta time poisons state")
        // A thin wall must block the declared winning pin. This catches fake success and tunnelling.
        val wall = Platform(Vector2D(40f,800f), Vector2D(1160f,800f), thickness=2f)
        val blocked = execute(initial.copy(platforms=initial.platforms+wall), PinId.PIN_A, schedules[0])
        verify(blocked.state==SimulationState.FAILED && blocked.creatures.all { it.position.y + it.radius <= 800f }, "Crossed a sealed barrier")
        // Explicit high-speed tests, with no level answer involved.
        for (speed in listOf(300f,900f,2100f,20000f)) {
            val isolated = LevelDefinition(99,name="thin wall",initialCreatures=listOf(Creature(CreatureId.PIP,Vector2D(600f,200f),Vector2D(0f,speed))),
                pins=listOf(Pin(PinId.PIN_A,"A",start=Vector2D(30f,30f),end=Vector2D(90f,30f))),
                platforms=listOf(Platform(Vector2D(40f,500f),Vector2D(1160f,500f),2f)),
                goalZone=GoalZone(Vector2D(600f,1400f)))
            val w=execute(isolated,PinId.PIN_A,floatArrayOf(0.25f))
            verify(w.creatures.single().position.y+32f <= 500f,"High-speed wall tunnelling at $speed")
        }
        val victory=execute(initial,PinId.PIN_A,schedules[1])
        val before= victory.particles.map { it.life }
        victory.step(0.1f)
        verify(victory.particles.isNotEmpty() && victory.particles.map { it.life } != before,"Celebration freezes at SUCCESS")
        repeat(25) { victory.step(0.1f) }
        verify(victory.particles.isEmpty() && victory.screenShake==0f,"Effects leak after terminal state")
        // A support is held until release; spring bounce is caused by contact and approach speed.
        val springLevel=LevelDefinition(98,name="spring",initialCreatures=listOf(Creature(CreatureId.PIP,Vector2D(600f,350f))),
            pins=listOf(Pin(PinId.PIN_A,"A",start=Vector2D(20f,20f),end=Vector2D(90f,20f))),
            springBumpers=listOf(SpringBumper(Vector2D(600f,650f))))
        val spring=PhysicsWorld(springLevel); spring.pullPin(PinId.PIN_A)
        var bounced=false
        repeat(100) { spring.step(1f/120f); if(spring.creatures.single().velocity.y < -100f) bounced=true }
        simulations++
        verify(bounced && spring.events.any{it.kind=="spring"},"Spring does not respond to physical impact")
        return Report(simulations,checks,maxWin,rows)
    }
}
