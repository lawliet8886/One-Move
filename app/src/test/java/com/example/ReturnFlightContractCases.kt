package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import java.io.File

/** No result is forced. Every component must survive an independent removal test. */
object ReturnFlightContractCases {
    fun run(): Int {
        val level=ReturnFlightLevel.create()
        val schedules=listOf(floatArrayOf(1f/30f),floatArrayOf(1f/60f),floatArrayOf(1f/120f),floatArrayOf(0.009f,0.022f,0.016f,0.031f))
        val rows=mutableListOf("case,pin,result,reason,seconds,spring_contacts")
        var simulations=0
        fun simulate(l:LevelDefinition, pin:PinId, frames:FloatArray, label:String):PhysicsWorld {
            val w=PhysicsWorld(l);check(w.pullPin(pin));var index=0
            while(w.state==SimulationState.RUNNING && index<4000)w.step(frames[index++%frames.size])
            simulations++
            check(w.state!=SimulationState.RUNNING && w.failureReason!="SIMULATION_TIMEOUT"){"Unbounded return flight: $label"}
            check(w.creatures.all{it.position.x.isFinite() && it.position.y.isFinite()})
            rows+="$label,$pin,${w.state},${w.failureReason},${w.simulationTime},${w.events.count{it.kind=="spring"}}"
            return w
        }
        var stableSignature:String?=null
        for(frames in schedules){
            for(pin in level.pins)repeat(3){
                val w=simulate(level,pin.id,frames,"choice")
                check(w.state==if(pin.id==PinId.PIN_B)SimulationState.SUCCESS else SimulationState.FAILED)
                if(pin.id==PinId.PIN_B){
                    check(w.creatures.all{it.isInsideGoal})
                    check(w.events.count{it.kind=="spring"}>=3)
                    val signature="${w.simulationTime}/${w.creatures.map{it.position}}"
                    if(stableSignature==null)stableSignature=signature
                    check(signature==stableSignature){"Return flight depends on frame duration"}
                }
            }
            for(dx in listOf(-4f,0f,4f))for(dy in listOf(-4f,0f,4f)){
                val varied=level.copy(initialCreatures=level.initialCreatures.map{it.copy(position=it.position+Vector2D(dx,dy))})
                check(simulate(varied,PinId.PIN_B,frames,"spawn_${dx}_$dy").state==SimulationState.SUCCESS){"Placement instability: $dx/$dy"}
            }
            for((label,broken) in listOf(
                "no_spring" to level.copy(springBumpers=emptyList()),
                "no_return_bridge" to level.copy(pins=level.pins.filter{it.id!=PinId.PIN_A}),
                "no_far_guard" to level.copy(pins=level.pins.filter{it.id!=PinId.PIN_D}),
                "no_landing_floor" to level.copy(pins=level.pins.filter{it.id!=PinId.PIN_C}))){
                check(simulate(broken,PinId.PIN_B,frames,label).state==SimulationState.FAILED){"Decorative mechanism: $label"}
            }
            val actual=simulate(level,PinId.PIN_B,frames,"correct_label")
            val relabelled=simulate(level.copy(solutionPinId=PinId.PIN_A),PinId.PIN_B,frames,"wrong_label")
            check(actual.state==relabelled.state && actual.simulationTime==relabelled.simulationTime)
            check(actual.creatures.map{it.position}==relabelled.creatures.map{it.position})
            actual.reset()
            check(actual.state==SimulationState.READY && actual.pins.none{it.isRemoved})
            check(actual.events.isEmpty() && actual.creatures.map{it.position}==level.initialCreatures.map{it.position})
        }
        val out=File("build/reports/physics-contract").apply{mkdirs()}
        File(out,"return-flight.csv").writeText(rows.joinToString("\n")+"\n")
        File(out,"return-flight-summary.txt").writeText("PASS: $simulations JVM simulations. Four choices, spring/bridge/guard/floor necessity, timing invariance, reset and +/-4 world-unit spawn variation. Not Android gameplay.\n")
        return simulations
    }
}
