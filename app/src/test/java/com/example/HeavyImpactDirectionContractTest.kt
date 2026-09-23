package com.example

import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.*
import org.junit.Test

class HeavyImpactDirectionContractTest {
    private fun level(creatureVelocity: Vector2D, ballVelocity: Vector2D, creatureX: Float, ballX: Float) =
        LevelDefinition(
            number = 97,
            name = "Heavy impact direction probe",
            initialCreatures = listOf(
                Creature(CreatureId.PIP, Vector2D(creatureX, 500f), velocity = creatureVelocity, radius = 32f)
            ),
            pins = listOf(
                Pin(PinId.PIN_A, "A", start = Vector2D(40f, 100f), end = Vector2D(100f, 100f))
            ),
            heavyBalls = listOf(
                HeavyBall(Vector2D(ballX, 500f), velocity = ballVelocity, radius = 44f)
            ),
            goalZone = GoalZone(Vector2D(1100f, 1500f), radius = 60f)
        )

    private fun runForCollision(level: LevelDefinition): PhysicsWorld {
        val world = PhysicsWorld(level)
        assertTrue(world.pullPin(PinId.PIN_A))
        var frames = 0
        while (world.collisionCount == 0 && world.state == SimulationState.RUNNING && frames++ < 90) {
            world.step(1f / 120f)
        }
        assertTrue("probe never collided", world.collisionCount > 0)
        return world
    }

    @Test
    fun stationaryWeightDeflectsButMovingWeightStrikes() {
        val friendIntoWeight = runForCollision(
            level(Vector2D(700f, 0f), Vector2D.Zero, creatureX = 300f, ballX = 450f)
        )
        assertNotEquals(
            "a parked weight must not magically attack the friend that ran into it",
            "HIT_BY_HEAVY_OBJECT",
            friendIntoWeight.failureReason
        )

        val weightIntoFriend = runForCollision(
            level(Vector2D.Zero, Vector2D(700f, 0f), creatureX = 450f, ballX = 300f)
        )
        assertEquals(SimulationState.FAILED, weightIntoFriend.state)
        assertEquals("HIT_BY_HEAVY_OBJECT", weightIntoFriend.failureReason)
    }
}
