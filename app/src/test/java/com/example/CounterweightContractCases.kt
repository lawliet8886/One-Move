package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import java.io.File

/** Independent necessity and stability checks, not Android gameplay. */
object CounterweightContractCases {
    fun run(): Int {
        var simulations=0
        val rows=mutableListOf("case,schedule,pin,state,reason,rescued,seconds")
        val schedules=listOf(floatArrayOf(1f/30f),floatArrayOf(1f/60f),floatArrayOf(1f/120f),
            floatArrayOf(0.012f,0.020f,0.008f,0.025f))
        fun simulate(level:LevelDefinition,pin:PinId,frames:FloatArray,name:String,schedule:Int):PhysicsWorld {
            val w=PhysicsWorld(level);check(w.pullPin(pin));var i=0
            while(w.state==SimulationState.RUNNING && i<3000){w.step(frames[i%frames.size]);i++}
            simulations++
            check(w.state!=SimulationState.RUNNING && w.failureReason!="SIMULATION_TIMEOUT") { "Unbounded/timeout $name" }
            rows.add("$name,$schedule,$pin,${w.state},${w.failureReason},${w.creatures.count{it.isInsideGoal}},${w.simulationTime}")
            return w
        }
        val level=CounterweightLevel.create()
        for((s,frames) in schedules.withIndex()) {
            for(pin in level.pins) repeat(3) {
                val w=simulate(level,pin.id,frames,"choice",s)
                check(w.state==if(pin.id==PinId.PIN_A) SimulationState.SUCCESS else SimulationState.FAILED)
            }
            for(dx in listOf(-5f,-2f,0f,2f,5f)) for(dy in listOf(-5f,-2f,0f,2f,5f)) {
                val shifted=level.copy(initialCreatures=level.initialCreatures.map{it.copy(position=it.position+Vector2D(dx,dy))})
                check(simulate(shifted,PinId.PIN_A,frames,"offset_${dx}_${dy}",s).state==SimulationState.SUCCESS)
            }
            val variants=listOf(
                "missing_weight" to level.copy(heavyBalls=emptyList()),
                "missing_lever" to level.copy(seesaws=emptyList()),
                "missing_floor" to level.copy(pins=level.pins.filter{it.id!=PinId.PIN_B}),
                "missing_guard" to level.copy(pins=level.pins.filter{it.id!=PinId.PIN_C}))
            for((name,variant) in variants) {
                check(simulate(variant,PinId.PIN_A,frames,name,s).state==SimulationState.FAILED) { "Mechanism was decorative: $name" }
            }
            val before=simulate(level,PinId.PIN_A,frames,"original_label",s)
            val after=simulate(level.copy(solutionPinId=PinId.PIN_B),PinId.PIN_A,frames,"changed_label",s)
            check(before.state==after.state && before.simulationTime==after.simulationTime)
            check(before.creatures.map{it.position}==after.creatures.map{it.position})
        }
        val directory=File("build/reports/physics-contract").apply{mkdirs()}
        File(directory,"counterweight.csv").writeText(rows.joinToString("\n")+"\n")
        File(directory,"counterweight-summary.txt").writeText("PASS: $simulations counterweight JVM simulations. Four indispensable mechanisms, all choices, frame schedules and placement stability. Not Android gameplay.\n")
        return simulations
    }
}
