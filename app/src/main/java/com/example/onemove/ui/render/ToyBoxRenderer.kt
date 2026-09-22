package com.example.onemove.ui.render

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.onemove.model.LevelDefinition
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState

object ToyBoxRenderer {

    fun renderToyBox(drawScope: DrawScope, world: PhysicsWorld) {
        // 1. Background chassis
        ToyBoxBackgroundRenderer.drawBackingChassis(
            drawScope,
            LevelDefinition.WORLD_WIDTH,
            LevelDefinition.WORLD_HEIGHT
        )

        // 2. Danger pits
        for (pit in world.dangerPits) {
            DangerBasinRenderer.drawDangerPit(drawScope, pit)
        }

        // 3. Home nest / Goal zone
        val rescuedCount = world.creatures.count { it.isInsideGoal }
        HomeNestRenderer.drawHomeNest(drawScope, world.goalZone, rescuedCount)

        // 4. Platforms & rails
        for (platform in world.platforms) {
            RailRenderer.drawPlatformRail(drawScope, platform)
        }

        // 5. Spring bumpers
        for (bumper in world.springBumpers) {
            SpringBumperRenderer.drawSpringBumper(drawScope, bumper)
        }

        // 6. Mechanical assemblies (Seesaws & Gates)
        for (seesaw in world.seesaws) {
            MechanicalJointRenderer.drawSeesawAssembly(drawScope, seesaw)
        }
        for (gate in world.creatureGates) {
            MechanicalJointRenderer.drawCreatureGate(drawScope, gate)
        }

        // 7. Heavy objects (Iron Wreckers & Granite Stones)
        for (stone in world.rollingStones) {
            WreckerAndStoneRenderer.drawRollingStone(drawScope, stone)
        }
        for (ball in world.heavyBalls) {
            WreckerAndStoneRenderer.drawHeavyWreckerBall(drawScope, ball)
        }

        // 8. Pins
        val isReady = world.state == SimulationState.READY
        for (pin in world.pins) {
            PinRenderer.drawPin(drawScope, pin, isReady = isReady)
        }

        // 9. Creatures
        for (creature in world.creatures) {
            HeroCreatureRenderer.drawCreature(drawScope, creature)
        }

        // 10. Particles
        KineticParticleRenderer.drawParticles(drawScope, world.particles)
    }
}
