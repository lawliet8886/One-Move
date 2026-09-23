package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.*
import org.junit.Test

class WreckerBumperLevelContractTest {
    private val schedules = listOf(
        floatArrayOf(1f / 30f),
        floatArrayOf(1f / 60f),
        floatArrayOf(1f / 120f),
        floatArrayOf(0.009f, 0.022f, 0.016f, 0.031f)
    )

    private fun run(level: LevelDefinition, pin: PinId, frames: FloatArray): PhysicsWorld {
        val world = PhysicsWorld(level)
        assertTrue("pin rejected: $pin", world.pullPin(pin))
        assertFalse("second pull accepted", world.pullPin(pin))
        var frame = 0
        while (world.state == SimulationState.RUNNING && frame < 4000) {
            world.step(frames[frame++ % frames.size])
        }
        assertNotEquals("nonterminal: $pin", SimulationState.RUNNING, world.state)
        assertNotEquals("timed out: $pin", "SIMULATION_TIMEOUT", world.failureReason)
        return world
    }

    @Test fun wreckerMustStageTheHeavySwitchAcrossSchedulesAndOffsets() {
        val level = WreckerBumperLevel.create()
        var reference: String? = null
        for (schedule in schedules) {
            for (pin in level.pins.map { it.id }) {
                val world = run(level, pin, schedule)
                if (pin == PinId.PIN_A) {
                    assertEquals(
                        "A must win; reason=${world.failureReason}; ball=${world.heavyBall?.position}; " +
                            "creatures=${world.creatures.map { it.id to it.position }}; events=${world.events}",
                        SimulationState.SUCCESS, world.state
                    )
                    assertTrue(world.creatures.all { it.isInsideGoal })
                    assertTrue(world.pressurePlates.single().isLatched)
                    assertTrue(world.creatureGates.single().isOpen)
                    val latch = world.events.first { it.kind == "plate_latched" }.time
                    val gate = world.events.first { it.kind == "gate_opened" }.time
                    val rescue = world.events.first { it.kind == "rescued" }.time
                    assertTrue("wrecker sequence must be key -> gate -> rescue", latch <= gate && gate < rescue)
                    val signature = "${world.simulationTime}/${world.creatures.map { it.position }}/${world.heavyBalls.map { it.position }}"
                    if (reference == null) reference = signature
                    assertEquals("frame schedule changed the wrecker route", reference, signature)
                } else {
                    assertEquals("wrong pin won: $pin", SimulationState.FAILED, world.state)
                }
            }

            for (dx in listOf(-2f, 0f, 2f)) for (dy in listOf(-2f, 0f, 2f)) {
                val varied = level.copy(
                    initialCreatures = level.initialCreatures.map { it.copy(position = it.position + Vector2D(dx, dy)) },
                    heavyBalls = level.heavyBalls.map { it.copy(position = it.position + Vector2D(dx, dy)) }
                )
                assertEquals("spawn sensitive: $dx/$dy", SimulationState.SUCCESS,
                    run(varied, PinId.PIN_A, schedule).state)
            }

            val broken = listOf(
                "no_weight" to level.copy(heavyBalls = emptyList()),
                "no_switch" to level.copy(pressurePlates = emptyList()),
                "no_guide" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_B }),
                "no_floor" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_C })
            )
            for ((label, candidate) in broken) {
                assertEquals("decorative mechanism: $label", SimulationState.FAILED,
                    run(candidate, PinId.PIN_A, schedule).state)
            }

            // A gate is intentionally an obstacle: deleting it should make the route easier,
            // not fail. Its causal job is to delay the trio until the heavy switch latches.
            val ungated = run(level.copy(creatureGates = emptyList()), PinId.PIN_A, schedule)
            assertEquals(SimulationState.SUCCESS, ungated.state)
            assertTrue(ungated.events.none { it.kind == "gate_opened" })

            val actual = run(level, PinId.PIN_A, schedule)
            assertTrue("gate must stage the rescue", ungated.simulationTime < actual.simulationTime)
            val relabelled = run(level.copy(solutionPinId = PinId.PIN_C), PinId.PIN_A, schedule)
            assertEquals(actual.state, relabelled.state)
            assertEquals(actual.events, relabelled.events)
            assertEquals(actual.creatures.map { it.position }, relabelled.creatures.map { it.position })
            assertEquals(actual.heavyBalls.map { it.position }, relabelled.heavyBalls.map { it.position })

            actual.reset()
            assertEquals(SimulationState.READY, actual.state)
            assertEquals(0f, actual.simulationTime)
            assertTrue(actual.pins.none { it.isRemoved })
            assertTrue(actual.events.isEmpty())
        }
    }
}
