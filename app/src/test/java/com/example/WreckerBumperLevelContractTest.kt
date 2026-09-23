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

    @Test fun wreckerMustBecomeThePhysicalBumperAcrossSchedulesAndOffsets() {
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
                "no_guide" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_B }),
                "no_floor" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_C }),
                "no_pocket" to level.copy(platforms = level.platforms.filterNot { p ->
                    (p.start == Vector2D(680f, 920f) && p.end == Vector2D(850f, 950f)) ||
                    (p.start == Vector2D(850f, 820f) && p.end == Vector2D(850f, 950f))
                })
            )
            for ((label, candidate) in broken) {
                assertEquals("decorative mechanism: $label", SimulationState.FAILED,
                    run(candidate, PinId.PIN_A, schedule).state)
            }

            val actual = run(level, PinId.PIN_A, schedule)
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
