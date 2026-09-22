package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import java.io.File

/** Production solver only. Relabelling the answer must not alter a single trajectory. */
object FlyingKeyContractCases {
    fun run(): Int {
        val level=FlyingKeyLevel.create()
        // A grip must not visually claim an unrelated bridge. Native review found D over B.
        val catcher=level.pins.first { it.id==PinId.PIN_D }
        val bridge=level.pins.first { it.id==PinId.PIN_B }
        val segment=bridge.end-bridge.start
        val t=((catcher.handlePosition-bridge.start).dot(segment)/segment.lengthSquared()).coerceIn(0f,1f)
        val separation=catcher.handlePosition.distanceTo(bridge.start+segment*t)
        check(separation >= 36f+bridge.thickness/2f+12f) { "D grip obscures unrelated B bridge: $separation" }
        // The established minimum board width is250dp: keep48dp between handle centres.
        for(i in level.pins.indices) for(j in i+1 until level.pins.size) {
            val gap=level.pins[i].handlePosition.distanceTo(level.pins[j].handlePosition)
            check(gap*250f/LevelDefinition.WORLD_WIDTH>=48f) { "Crowded grip targets: $i/$j" }
        }
        val schedules=listOf(floatArrayOf(1f/30f),floatArrayOf(1f/60f),floatArrayOf(1f/120f),floatArrayOf(0.009f,0.022f,0.016f,0.031f))
        val rows=mutableListOf("case,pin,state,reason,seconds,spring_contacts,latched")
        var simulations=0
        fun simulate(l:LevelDefinition,id:PinId,frames:FloatArray,label:String):PhysicsWorld {
            val w=PhysicsWorld(l); check(w.pullPin(id)); check(!w.pullPin(id))
            var frame=0
            while(w.state==SimulationState.RUNNING && frame<4000) w.step(frames[frame++%frames.size])
            simulations++
            check(w.state!=SimulationState.RUNNING && w.failureReason!="SIMULATION_TIMEOUT") { "Unsettled flying key: $label/$id/${w.failureReason}" }
            check(w.creatures.all { it.position.x.isFinite() && it.position.y.isFinite() })
            rows+="$label,$id,${w.state},${w.failureReason},${w.simulationTime},${w.events.count { it.kind=="spring" }},${w.pressurePlates.count { it.isLatched }}"
            return w
        }
        var reference:String?=null
        for(schedule in schedules) {
            for(pin in level.pins) repeat(3) {
                val w=simulate(level,pin.id,schedule,"all_choices")
                check(w.state==if(pin.id==PinId.PIN_A) SimulationState.SUCCESS else SimulationState.FAILED) { "Unexpected choice outcome: ${pin.id}/${w.state}" }
                if(pin.id==PinId.PIN_A) {
                    val spring=w.events.first { it.kind=="spring" }.time
                    val latch=w.events.first { it.kind=="plate_latched" }.time
                    val gate=w.events.first { it.kind=="gate_opened" }.time
                    check(spring<latch && latch<gate && w.creatures.all { it.isInsideGoal })
                    val signature="${w.simulationTime}/${w.creatures.map { it.position }}/${w.heavyBalls.map { it.position }}"
                    if(reference==null) reference=signature
                    check(reference==signature) { "The key depends on frame schedule" }
                } else {
                    check(w.pressurePlates.none { it.isLatched } && w.creatureGates.none { it.isOpen })
                    check(w.creatures.none { it.isInsideGoal })
                }
            }
            for(dx in listOf(-4f,-2f,0f,2f,4f)) for(dy in listOf(-4f,-2f,0f,2f,4f)) {
                val varied=level.copy(initialCreatures=level.initialCreatures.map { it.copy(position=it.position+Vector2D(dx,dy)) },
                    heavyBalls=level.heavyBalls.map { it.copy(position=it.position+Vector2D(dx,dy)) })
                check(simulate(varied,PinId.PIN_A,schedule,"spawn_${dx}_$dy").state==SimulationState.SUCCESS) { "Spawn-sensitive key: $dx/$dy" }
            }
            val counterfactuals=listOf(
                "no_spring" to level.copy(springBumpers=emptyList()),
                "no_weight" to level.copy(heavyBalls=emptyList()),
                "no_switch" to level.copy(pressurePlates=emptyList()),
                "no_catcher" to level.copy(pins=level.pins.filter { it.id!=PinId.PIN_D }),
                "no_bridge" to level.copy(pins=level.pins.filter { it.id!=PinId.PIN_B }),
                "no_floor" to level.copy(pins=level.pins.filter { it.id!=PinId.PIN_C }))
            for((label,broken) in counterfactuals) {
                check(simulate(broken,PinId.PIN_A,schedule,label).state==SimulationState.FAILED) { "Decorative mechanism: $label" }
            }
            val actual=simulate(level,PinId.PIN_A,schedule,"answer_original")
            val relabelled=simulate(level.copy(solutionPinId=PinId.PIN_D),PinId.PIN_A,schedule,"answer_relabelled")
            check(actual.simulationTime==relabelled.simulationTime && actual.events==relabelled.events)
            check(actual.creatures.map { it.position }==relabelled.creatures.map { it.position })
            check(actual.heavyBalls.map { it.position }==relabelled.heavyBalls.map { it.position })
            actual.reset()
            check(actual.state==SimulationState.READY && actual.simulationTime==0f)
            check(actual.pins.none { it.isRemoved } && actual.pressurePlates.none { it.isLatched })
            check(actual.creatureGates.none { it.isOpen } && actual.events.isEmpty())
            check(actual.creatures.map { it.position }==level.initialCreatures.map { it.position })
        }
        val out=File("build/reports/physics-contract").apply { mkdirs() }
        File(out,"flying-key.csv").writeText(rows.joinToString("\n")+"\n")
        File(out,"flying-key-summary.txt").writeText("PASS: $simulations actual JVM simulations; four choices, four frame schedules, 25 spawn offsets, six mechanism removals, answer relabelling and reset. NOT Android gameplay.\n")
        return simulations
    }
}
