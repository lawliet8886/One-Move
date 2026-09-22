package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import java.io.File

/** Independent switch contact, necessity and stability contracts. Not device gameplay. */
object WeightSwitchContractCases {
    fun run(): Int {
        var simulations = 0
        val rows = mutableListOf("case,level,pin,state,reason,seconds,latched")
        val schedules = listOf(floatArrayOf(1f/30f), floatArrayOf(1f/60f), floatArrayOf(1f/120f),
            floatArrayOf(0.009f, 0.022f, 0.016f, 0.031f, 0.012f))
        fun simulate(l: LevelDefinition, pin: PinId, frames: FloatArray, label: String,
                     requireBoundedChoice: Boolean = true): PhysicsWorld {
            val w = PhysicsWorld(l); check(w.pullPin(pin)); var i = 0
            while (w.state == SimulationState.RUNNING && i < 4000) w.step(frames[i++ % frames.size])
            simulations++
            check(w.state != SimulationState.RUNNING) { "Unbounded $label" }
            if (requireBoundedChoice) check(w.failureReason != "SIMULATION_TIMEOUT") { "Timeout $label" }
            check(w.creatures.all { it.position.x.isFinite() && it.position.y.isFinite() })
            rows += "$label,${l.number},$pin,${w.state},${w.failureReason},${w.simulationTime},${w.pressurePlates.count{it.isLatched}}"
            return w
        }
        val weight = WeightSwitchLevel.create()
        val bridge = SwitchbackLevel.create()
        for (l in listOf(bridge, weight)) for (frames in schedules) {
            for (pin in l.pins) repeat(4) {
                val w = simulate(l, pin.id, frames, "choice")
                check(w.state == if (pin.id == l.solutionPinId) SimulationState.SUCCESS else SimulationState.FAILED)
            }
            for (dx in listOf(-4f, 0f, 4f)) for (dy in listOf(-4f, 0f, 4f)) {
                val varied = l.copy(initialCreatures = l.initialCreatures.map { it.copy(position=it.position+Vector2D(dx,dy)) })
                val variant = simulate(varied, l.solutionPinId, frames, "spawn_${dx}_$dy")
                check(variant.state == SimulationState.SUCCESS) { "Spawn instability level ${l.number} at $dx/$dy: ${variant.failureReason}" }
            }
            val before = simulate(l, l.solutionPinId, frames, "label_original")
            val after = simulate(l.copy(solutionPinId=l.pins.first{it.id!=l.solutionPinId}.id), l.solutionPinId, frames, "label_changed")
            check(before.state == after.state && before.simulationTime == after.simulationTime)
            check(before.creatures.map{it.position} == after.creatures.map{it.position})
        }
        for (frames in schedules) {
            val correct = simulate(weight, PinId.PIN_A, frames, "switch_causal_chain")
            val contact = correct.events.single { it.kind == "plate_latched" }
            val opening = correct.events.single { it.kind == "gate_opened" }
            check(contact.time >= weight.pressurePlates.single().holdSeconds)
            check(opening.time > contact.time)
            check(correct.events.filter{it.kind=="rescued"}.all{it.time>opening.time})
            val missing = listOf("no_weight" to weight.copy(heavyBalls=emptyList()),
                "no_switch" to weight.copy(pressurePlates=emptyList()),
                "disconnected" to weight.copy(creatureGates=weight.creatureGates.map{it.copy(requiredPlateIds=listOf("missing"))}),
                "no_floor" to weight.copy(pins=weight.pins.filter{it.id!=PinId.PIN_C}))
            for ((label, candidate) in missing) {
                val w = simulate(candidate, PinId.PIN_A, frames, label, false)
                check(w.state == SimulationState.FAILED) { "Decorative mechanism: $label" }
                if (label != "no_floor") check(w.creatureGates.none{it.isOpen})
            }
            for (pin in listOf(PinId.PIN_A, PinId.PIN_B)) {
                val w = simulate(bridge.copy(pins=bridge.pins.filter{it.id!=pin}), PinId.PIN_C, frames, "no_$pin", false)
                check(w.state == SimulationState.FAILED) { "Bridge not necessary: $pin" }
            }
        }
        fun sensorFixture(heavy: List<HeavyBall>, friends: List<Creature> = emptyList(),
                          ids: List<String> = listOf("one")): PhysicsWorld {
            val l = LevelDefinition(90, name="sensor contract", initialCreatures=friends, heavyBalls=heavy,
                pins=listOf(Pin(PinId.PIN_A,"A",start=Vector2D(30f,30f),end=Vector2D(80f,30f))),
                pressurePlates=listOf(PressurePlate("one",Vector2D(220f,600f)), PressurePlate("two",Vector2D(650f,600f))),
                creatureGates=listOf(CreatureGate(Vector2D(900f,500f),Vector2D(900f,900f),requiredPlateIds=ids)),
                goalZone=GoalZone(Vector2D(-1000f,-1000f)))
            val w=PhysicsWorld(l);w.pullPin(PinId.PIN_A);repeat(200){w.step(1f/240f)};simulations++
            return w
        }
        val touched = sensorFixture(listOf(HeavyBall(Vector2D(220f,420f))))
        check(touched.pressurePlates.first().isLatched && touched.creatureGates.single().isOpen)
        check(!sensorFixture(listOf(HeavyBall(Vector2D(220f,660f)))).pressurePlates.first().isLatched)
        check(!sensorFixture(listOf(HeavyBall(Vector2D(310f,560f)))).pressurePlates.first().isLatched)
        check(!sensorFixture(emptyList(), listOf(Creature(CreatureId.PIP,Vector2D(220f,420f)))).pressurePlates.first().isLatched)
        val both = listOf("one", "two")
        check(!sensorFixture(listOf(HeavyBall(Vector2D(220f,420f))), ids=both).creatureGates.single().isOpen)
        check(sensorFixture(listOf(HeavyBall(Vector2D(220f,420f)),HeavyBall(Vector2D(650f,420f))), ids=both).creatureGates.single().isOpen)
        val snapshot = touched.currentLevel.pressurePlates.map { it.copy() }
        touched.reset()
        check(touched.pressurePlates.none{it.isLatched || it.contactTime != 0f})
        check(touched.creatureGates.none{it.isOpen || it.openProgress != 0f})
        check(touched.events.isEmpty())
        check(snapshot == touched.currentLevel.pressurePlates)
        check(runCatching{PressurePlate("",Vector2D.Zero)}.isFailure)
        check(runCatching{PressurePlate("nan",Vector2D.Zero,minimumMass=Float.NaN)}.isFailure)
        val directory=File("build/reports/physics-contract").apply{mkdirs()}
        File(directory,"weight-switch.csv").writeText(rows.joinToString("\n")+"\n")
        File(directory,"weight-switch-summary.txt").writeText("PASS: $simulations JVM simulations. Causal weight switch, two-bridge routing, reset, all choices and counterfactuals. Not Android gameplay.\n")
        return simulations
    }
}
