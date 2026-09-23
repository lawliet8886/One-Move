package com.example.onemove.ui.render

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState

object ToyBoxRenderer {
    fun renderToyBox(drawScope: DrawScope, world: PhysicsWorld) {
        // Static decorative background and hazards are rasterized once per layout,
        // instead of allocating and redrawing hundreds of shapes every active frame.
        WorkshopBackdropCache.draw(drawScope,world)

        WeightSwitchRenderer.draw(drawScope, world)
        val rescuedCount = world.creatures.count { it.isInsideGoal }
        RescueSanctuaryRenderer.draw(drawScope, world.goalZone, rescuedCount)
        for (platform in world.platforms) WorkshopRenderer.platform(drawScope, platform)
        for (bumper in world.springBumpers) SpringBumperRenderer.drawSpringBumper(drawScope, bumper)
        for (seesaw in world.seesaws) MechanicalJointRenderer.drawSeesawAssembly(drawScope, seesaw)
        for (gate in world.creatureGates) if (gate.requiredPlateIds.isEmpty())
            MechanicalJointRenderer.drawCreatureGate(drawScope, gate)
        for (stone in world.rollingStones) WreckerAndStoneRenderer.drawRollingStone(drawScope, stone)
        for (ball in world.heavyBalls) WreckerAndStoneRenderer.drawHeavyWreckerBall(drawScope, ball)
        val isReady = world.state == SimulationState.READY
        for (pin in world.pins) PinRenderer.drawPin(drawScope, pin, isReady = isReady)
        for (creature in world.creatures) HeroCreatureRenderer.drawCreature(drawScope, creature, world.visualTime)
        KineticParticleRenderer.drawParticles(drawScope, world.particles)
    }
}
