package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import kotlin.math.abs

/** Runs the production physics engine. No Android simulator or visual claims are made here. */
object PhysicsRegressionSuite {
    data class Report(val simulations: Int, val checks: Int, val rows: List<String>)
    fun run(): Report {
        var checks = 0
        var simulations = 0
        val rows = mutableListOf("level,pin,cadence,repetition,state,terminal_seconds,rescued,reason")
        fun verify(ok: Boolean, message: String) { checks++; check(ok) { message } }
        val cadences = listOf(listOf(1f/30),listOf(1f/60),listOf(1f/90),listOf(1f/120),listOf(.010f,.025f,.012f,.019f),listOf(.016f,.016f,.100f,.008f))
        fun simulate(level: LevelDefinition, pin: PinId, cadence: List<Float>): PhysicsWorld {
            val w = PhysicsWorld(level)
            verify(w.pullPin(pin), "Initial pull must work")
            verify(!w.pullPin(pin), "Only one move is permitted")
            var steps = 0
            while(w.state == SimulationState.RUNNING && steps < 5000) {
                w.step(cadence[steps % cadence.size]); steps++
                verify(w.creatures.all { c -> c.position.x.isFinite() && c.position.y.isFinite() && c.velocity.x.isFinite() && c.velocity.y.isFinite() }, "Nonfinite body at ${level.number}/$pin")
            }
            simulations++
            verify(w.state != SimulationState.RUNNING, "Bounded simulation must terminate")
            return w
        }
        for(level in LevelCatalog.ALL_LEVELS) {
            var winners = 0
            for(pin in level.pins) {
                var reference: PhysicsWorld? = null
                for((c, cadence) in cadences.withIndex()) for(rep in 1..3) {
                    val w = simulate(level,pin.id,cadence)
                    val wanted = if(pin.id == level.solutionPinId) SimulationState.SUCCESS else SimulationState.FAILED
                    verify(w.state == wanted,"Level ${level.number} ${pin.id}: $wanted != ${w.state} ${w.failureReason}")
                    verify(w.failureReason != "TIME_LIMIT_REACHED", "Level ${level.number} stalls until timeout")
                    if(w.state == SimulationState.SUCCESS) {
                        verify(w.creatures.all { it.isInsideGoal && it.inGoalTime >= .3f && it.position.distanceTo(w.goalZone.center) <= w.goalZone.radius }, "False rescue")
                    }
                    reference?.let { previous ->
                        verify(abs(previous.terminalTime-w.terminalTime) < .001f,"Frame-rate-dependent outcome time")
                        verify(previous.creatures.zip(w.creatures).all { (a,b) -> a.position.distanceTo(b.position) < .01f },"Frame-rate-dependent final position")
                    }
                    if(reference == null) reference = w
                    rows += "${level.number},${pin.id},$c,$rep,${w.state},${w.terminalTime},${w.creatures.count{it.isInsideGoal}},${w.failureReason}"
                }
                if(reference!!.state == SimulationState.SUCCESS) winners++
                // The declared answer must have no influence on the actual simulation.
                val otherAnswer = PinId.entries.first { it != level.solutionPinId }
                val tampered = simulate(level.copy(solutionPinId=otherAnswer),pin.id,cadences[1])
                verify(tampered.state == reference!!.state,"Engine consulted answer metadata")
                verify(tampered.creatures.zip(reference!!.creatures).all { (a,b) -> a.position.distanceTo(b.position) < .001f },"Metadata changed trajectories")
            }
            verify(winners == 1,"Every level needs exactly one physical winning choice")
        }
        val base = LevelCatalog.createLevel01()
        val wall = base.copy(platforms=base.platforms+Platform(Vector2D(40f,600f),Vector2D(1160f,600f),thickness=10f))
        verify(simulate(wall,base.solutionPinId,cadences[1]).state == SimulationState.FAILED,"A solid barrier must block the advertised solution")
        val fast = base.copy(initialCreatures=listOf(Creature(CreatureId.PIP,Vector2D(600f,480f),Vector2D(0f,1500f),radius=16f)),platforms=listOf(Platform(Vector2D(40f,520f),Vector2D(1160f,520f),thickness=2f)),dangerPits=emptyList())
        val stopped = simulate(fast,fast.solutionPinId,cadences[0])
        verify(stopped.creatures.single().position.y <= 503.1f,"Fast body tunneled through a thin rail")
        val dangerous = base.copy(initialCreatures=listOf(Creature(CreatureId.PIP,Vector2D(600f,500f),radius=30f)),platforms=emptyList(),dangerPits=listOf(DangerPit(Rect2D(550f,540f,650f,600f))))
        val lost = simulate(dangerous,dangerous.solutionPinId,cadences[1])
        verify(lost.state == SimulationState.FAILED && lost.terminalTime < 1f,"Danger must depend on contact, not a fixed timer")
        val ready=PhysicsWorld(base); ready.step(10f)
        verify(ready.simulationTime==0f && ready.creatures.map{it.position}==base.initialCreatures.map{it.position},"Ready state advanced")
        ready.pullPin(base.solutionPinId)
        for(dt in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f,0f)) ready.step(dt)
        verify(ready.simulationTime==0f,"Invalid dt mutated world")
        repeat(20){ready.step(1f/60)}
        verify(ready.creatures.none{it.isInsideGoal},"Premature rescue/teleport")
        verify(ready.creatures.all { it.position.distanceTo(base.goalZone.center)>500f },"Instant move to goal")
        ready.reset()
        verify(ready.creatures.map{it.position}==base.initialCreatures.map{it.position} && ready.pins.none{it.isRemoved},"Reset failed")
        verify(base.pins.none{it.isRemoved},"Runtime leaked into catalog")
        val terminal=simulate(base,base.solutionPinId,cadences[1]);val ended=terminal.terminalTime
        repeat(180){terminal.step(1f/60)}
        verify(terminal.simulationTime>ended+2.9f && terminal.terminalTime==ended,"Terminal animation clock froze")
        verify(terminal.particles.isEmpty(),"Confetti never expired")
        for(level in LevelCatalog.ALL_LEVELS) for(gate in level.creatureGates) {
            val originalLength=gate.closedEnd.distanceTo(gate.pivot)
            for(progress in listOf(0f,.3f,.7f,1f)) {
                val copy=gate.copy(openProgress=progress)
                verify(abs(copy.currentEnd().distanceTo(copy.pivot)-originalLength)<.001f,"Gate shortened instead of rotating")
            }
        }
        return Report(simulations,checks,rows)
    }
}
