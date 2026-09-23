package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.*
import org.junit.Test

class GatewayKeyLevelContractTest {
    private val schedules = listOf(
        floatArrayOf(1f / 30f), floatArrayOf(1f / 60f), floatArrayOf(1f / 120f),
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
        assertNotEquals("physical outcome timed out: $pin", "SIMULATION_TIMEOUT", world.failureReason)
        return world
    }

    @Test fun rollingKeyMustBeCausalStableAndIndependentOfAnswerLabel() {
        val level = GatewayKeyLevel.create()
        var reference: String? = null
        for (schedule in schedules) {
            for (pin in level.pins.map { it.id }) {
                val world = run(level, pin, schedule)
                if (pin == PinId.PIN_C) {
                    assertEquals("C must win; reason=${world.failureReason}", SimulationState.SUCCESS, world.state)
                    assertTrue(world.pressurePlates.single().isLatched)
                    assertTrue(world.creatureGates.single().isOpen)
                    assertTrue(world.creatures.all { it.isInsideGoal })
                    val latch = world.events.first { it.kind == "plate_latched" }.time
                    val gate = world.events.first { it.kind == "gate_opened" }.time
                    val rescue = world.events.first { it.kind == "rescued" }.time
                    assertTrue(latch <= gate && gate < rescue)
                    val signature = "${world.simulationTime}/${world.creatures.map { it.position }}/${world.rollingStones.map { it.position }}"
                    if (reference == null) reference = signature
                    assertEquals("frame schedule changed the gateway", reference, signature)
                } else assertEquals("wrong pin won: $pin", SimulationState.FAILED, world.state)
            }

            for (dx in listOf(-2f, 0f, 2f)) for (dy in listOf(-2f, 0f, 2f)) {
                val varied = level.copy(
                    initialCreatures = level.initialCreatures.map { it.copy(position = it.position + Vector2D(dx, dy)) },
                    rollingStones = level.rollingStones.map { it.copy(position = it.position + Vector2D(dx, dy)) }
                )
                assertEquals("spawn sensitive: $dx/$dy", SimulationState.SUCCESS,
                    run(varied, PinId.PIN_C, schedule).state)
            }

            val broken = listOf(
                "no_key" to level.copy(rollingStones = emptyList()),
                "no_switch" to level.copy(pressurePlates = emptyList()),
                "no_key_bridge" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_B }),
                "no_floor" to level.copy(pins = level.pins.filter { it.id != PinId.PIN_A })
            )
            for ((label, candidate) in broken) {
                assertEquals("decorative mechanism: $label", SimulationState.FAILED,
                    run(candidate, PinId.PIN_C, schedule).state)
            }

            val actual = run(level, PinId.PIN_C, schedule)
            val fake = run(level.copy(solutionPinId = PinId.PIN_A), PinId.PIN_C, schedule)
            assertEquals(actual.state, fake.state)
            assertEquals(actual.events, fake.events)
            assertEquals(actual.creatures.map { it.position }, fake.creatures.map { it.position })
            assertEquals(actual.rollingStones.map { it.position }, fake.rollingStones.map { it.position })
        }
    }
}
