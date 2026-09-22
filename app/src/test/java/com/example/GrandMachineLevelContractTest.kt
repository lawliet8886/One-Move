package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.*
import org.junit.Test

class GrandMachineLevelContractTest {
    private fun run(level: LevelDefinition, pin: PinId): PhysicsWorld {
        val world=PhysicsWorld(level)
        assertTrue("pin rejected: $pin",world.pullPin(pin))
        var steps=0
        while(world.state==SimulationState.RUNNING && steps++<1500) world.step(1f/60f)
        assertNotEquals("nonterminal: $pin",SimulationState.RUNNING,world.state)
        assertNotEquals("physical outcome timed out: $pin","SIMULATION_TIMEOUT",world.failureReason)
        return world
    }

    @Test fun twinKeysMustPhysicallyOpenTheFinalGate() {
        val level=GrandMachineLevel.create()
        for(pin in level.pins.map { it.id }) {
            val world=run(level,pin)
            if(pin==PinId.PIN_C) {
                assertEquals(SimulationState.SUCCESS,world.state)
                assertTrue(world.pressurePlates.all { it.isLatched })
                assertTrue(world.creatureGates.single().isOpen)
                assertTrue(world.events.count { it.kind=="plate_latched" }>=2)
                assertTrue(world.events.any { it.kind=="gate_opened" })
                assertTrue(world.creatures.all { it.isInsideGoal })
            } else {
                assertEquals("wrong pin won: $pin",SimulationState.FAILED,world.state)
            }
        }
    }

    @Test fun finalMachineDoesNotReadTheAnswerLabel() {
        val level=GrandMachineLevel.create()
        val real=run(level,PinId.PIN_C)
        val fake=run(level.copy(solutionPinId=PinId.PIN_A),PinId.PIN_C)
        assertEquals(real.state,fake.state)
        assertEquals(real.creatures.map { it.position },fake.creatures.map { it.position })
        assertEquals(real.pressurePlates.map { it.isLatched },fake.pressurePlates.map { it.isLatched })
    }
}
