package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.*
import java.io.File

/** Tests design causes, not the old level answer-key animation. */
object LevelDesignContractCases {
    fun run() {
        everyChoiceHasARepeatablePhysicalConsequence()
        bridgeMustExistToCrossTheDangerBasin()
        springMustActuallyLaunchAllThreeFriendsAcrossTheGap()
        missingShieldProducesHeavyImpactNotAnAnswerKeyFailure()
        relabellingTheAnswerCannotChangeTheTrajectory()
        winningRoutesTolerateSmallPlacementVariation()
        val out=File("build/reports/physics-contract").apply { mkdirs() }
        File(out,"level-design-summary.txt").writeText("PASS: 135 offline/JVM design simulations; three causal puzzle prototypes. Not Android gameplay or a fun certification.\n")
    }
    private fun simulate(level: LevelDefinition, pin: PinId, frames: FloatArray): PhysicsWorld {
        val world = PhysicsWorld(level)
        check(world.pullPin(pin))
        var frame = 0
        while (world.state == SimulationState.RUNNING && frame < 3000) {
            world.step(frames[frame % frames.size]); frame++
        }
        check(world.state != SimulationState.RUNNING) { "Unbounded simulation: ${level.number}/$pin" }
        check(world.failureReason != "SIMULATION_TIMEOUT") { "Timeout is not a designed consequence: ${level.number}/$pin" }
        return world
    }
    private val schedules = listOf(floatArrayOf(1f/30f),floatArrayOf(1f/60f),floatArrayOf(1f/120f),
        floatArrayOf(0.012f,0.020f,0.008f,0.025f))

    private fun everyChoiceHasARepeatablePhysicalConsequence() {
        val rows = mutableListOf("level,pin,schedule,repeat,result,reason,rescued,seconds")
        for (number in listOf(2,5,8)) {
            val level=LevelCatalog.getLevel(number)
            for (pin in level.pins) for ((schedule,frames) in schedules.withIndex()) repeat(3) { attempt ->
                val world=simulate(level,pin.id,frames)
                val expected=if(pin.id==level.solutionPinId) SimulationState.SUCCESS else SimulationState.FAILED
                check(world.state==expected) { "$number/${pin.id}: ${world.state} != $expected" }
                if(expected==SimulationState.SUCCESS) check(world.creatures.all { it.isInsideGoal })
                rows.add("$number,${pin.id},$schedule,$attempt,${world.state},${world.failureReason},${world.creatures.count { it.isInsideGoal }},${world.simulationTime}")
            }
        }
        val directory=File("build/reports/physics-contract/level-design").apply { mkdirs() }
        File(directory,"choices.csv").writeText(rows.joinToString("\n")+"\n")
    }

    private fun bridgeMustExistToCrossTheDangerBasin() {
        val level=LevelCatalog.getLevel(2)
        val broken=level.copy(pins=level.pins.filter { it.id!=PinId.PIN_A })
        val world=simulate(broken,PinId.PIN_B,schedules[1])
        check(world.state==SimulationState.FAILED)
        check(world.failureReason=="CREATURE_TRAPPED_IN_DANGER_BASIN")
    }

    private fun springMustActuallyLaunchAllThreeFriendsAcrossTheGap() {
        val level=LevelCatalog.getLevel(5)
        val correct=simulate(level,PinId.PIN_C,schedules[1])
        check(correct.state==SimulationState.SUCCESS)
        check(correct.events.count { it.kind=="spring" }>=3)
        val withoutSpring=simulate(level.copy(springBumpers=emptyList()),PinId.PIN_C,schedules[1])
        check(withoutSpring.state==SimulationState.FAILED)
        check(withoutSpring.failureReason=="CREATURE_TRAPPED_IN_DANGER_BASIN")
    }

    private fun missingShieldProducesHeavyImpactNotAnAnswerKeyFailure() {
        val level=LevelCatalog.getLevel(8)
        val impact=simulate(level,PinId.PIN_A,schedules[1])
        check(impact.failureReason=="HIT_BY_HEAVY_OBJECT")
        val withoutWeight=simulate(level.copy(heavyBalls=emptyList()),PinId.PIN_A,schedules[1])
        check(withoutWeight.failureReason=="PATH_BLOCKED")
        val missingShield=level.copy(pins=level.pins.filter { it.id!=PinId.PIN_A })
        val openExit=simulate(missingShield,PinId.PIN_B,schedules[1])
        check(openExit.failureReason=="HIT_BY_HEAVY_OBJECT")
    }

    private fun relabellingTheAnswerCannotChangeTheTrajectory() {
        for(number in listOf(2,5,8)) {
            val level=LevelCatalog.getLevel(number)
            val before=simulate(level,level.solutionPinId,schedules[1])
            val relabelled=level.copy(solutionPinId=level.pins.first { it.id!=level.solutionPinId }.id)
            val after=simulate(relabelled,level.solutionPinId,schedules[1])
            check(before.state==after.state)
            check(before.simulationTime==after.simulationTime)
            check(before.creatures.map { it.position }==after.creatures.map { it.position })
        }
    }

    private fun winningRoutesTolerateSmallPlacementVariation() {
        for(number in listOf(2,5,8)) for(dx in listOf(-2f,0f,2f)) for(dy in listOf(-2f,0f,2f)) {
            val level=LevelCatalog.getLevel(number)
            val varied=level.copy(initialCreatures=level.initialCreatures.map {
                it.copy(position=it.position+Vector2D(dx,dy))
            })
            val world=simulate(varied,level.solutionPinId,schedules[1])
            check(world.state==SimulationState.SUCCESS) { "Unstable route: $number at $dx/$dy" }
        }
    }
}
