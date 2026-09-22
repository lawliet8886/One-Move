package com.example.onemove.physics

import androidx.compose.ui.graphics.Color
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureExpression
import com.example.onemove.model.CreatureGate
import com.example.onemove.model.DangerPit
import com.example.onemove.model.GoalZone
import com.example.onemove.model.HeavyBall
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.Particle
import com.example.onemove.model.ParticleType
import com.example.onemove.model.Pin
import com.example.onemove.model.PinId
import com.example.onemove.model.Platform
import com.example.onemove.model.RollingStone
import com.example.onemove.model.SeesawLever
import com.example.onemove.model.SpringBumper
import com.example.onemove.model.Vector2D

class PhysicsWorld(
    initialLevel: LevelDefinition,
    private val hapticManager: HapticManager? = null
) {
    var currentLevel: LevelDefinition = initialLevel
        private set

    var state: SimulationState = SimulationState.READY
    var chosenPinId: PinId? = null
    var failureReason: String = ""
    var screenShake: Float = 0f
    var simulationTime: Float = 0f

    val creatures: MutableList<Creature> = mutableListOf()
    val pins: MutableList<Pin> = mutableListOf()
    val platforms: MutableList<Platform> = mutableListOf()
    val dangerPits: MutableList<DangerPit> = mutableListOf()
    val springBumpers: MutableList<SpringBumper> = mutableListOf()
    val seesaws: MutableList<SeesawLever> = mutableListOf()
    val heavyBalls: MutableList<HeavyBall> = mutableListOf()
    val rollingStones: MutableList<RollingStone> = mutableListOf()
    val creatureGates: MutableList<CreatureGate> = mutableListOf()
    var goalZone: GoalZone = initialLevel.goalZone
    val particles: MutableList<Particle> = mutableListOf()

    val heavyBall: HeavyBall? get() = heavyBalls.firstOrNull()
    val seesaw: SeesawLever? get() = seesaws.firstOrNull()
    val rollingStone: RollingStone? get() = rollingStones.firstOrNull()

    init {
        loadLevel(initialLevel)
    }

    fun loadLevel(level: LevelDefinition) {
        currentLevel = level
        goalZone = level.goalZone
        reset()
    }

    fun reset() {
        state = SimulationState.READY
        chosenPinId = null
        failureReason = ""
        screenShake = 0f
        simulationTime = 0f
        particles.clear()

        creatures.clear()
        currentLevel.initialCreatures.forEach { c ->
            creatures.add(
                Creature(
                    id = c.id,
                    position = c.position,
                    velocity = Vector2D.Zero,
                    radius = c.radius,
                    expression = CreatureExpression.NORMAL,
                    isInsideGoal = false,
                    inGoalTime = 0f,
                    isGrounded = false,
                    isTrapped = false
                )
            )
        }

        pins.clear()
        currentLevel.pins.forEach { p ->
            pins.add(p.copy(isRemoved = false, isSelected = false, pullProgress = 0f))
        }

        platforms.clear()
        platforms.addAll(currentLevel.platforms)

        dangerPits.clear()
        dangerPits.addAll(currentLevel.dangerPits)

        springBumpers.clear()
        currentLevel.springBumpers.forEach { springBumpers.add(it.copy()) }

        seesaws.clear()
        currentLevel.seesaws.forEach { seesaws.add(it.copy()) }

        heavyBalls.clear()
        currentLevel.heavyBalls.forEach { heavyBalls.add(it.copy()) }

        rollingStones.clear()
        currentLevel.rollingStones.forEach { rollingStones.add(it.copy()) }

        creatureGates.clear()
        currentLevel.creatureGates.forEach { creatureGates.add(it.copy()) }
    }

    fun pullPin(pinId: PinId): Boolean {
        if (state != SimulationState.READY) return false
        val pin = pins.find { it.id == pinId } ?: return false

        pin.isRemoved = true
        pin.isSelected = true
        pin.pullProgress = 1f
        chosenPinId = pinId
        state = SimulationState.RUNNING
        hapticManager?.playPinPull()

        // Spawn pull sparks
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    position = pin.handlePosition,
                    velocity = Vector2D((Math.random().toFloat() - 0.5f) * 200f, (Math.random().toFloat() - 0.5f) * 200f),
                    color = pin.color,
                    size = 6f,
                    maxLife = 0.4f,
                    type = ParticleType.SPARK
                )
            )
        }
        return true
    }

    fun step(dt: Float) {
        if (state != SimulationState.RUNNING) return

        simulationTime += dt
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 2.5f).coerceAtLeast(0f)
        }

        // Update particles
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.update(dt)
            if (!p.isAlive) pIter.remove()
        }

        val isWinner = (chosenPinId == currentLevel.solutionPinId)

        if (isWinner) {
            // Winning solution: creatures sequentially drop and reach the goal zone
            val targetGoal = goalZone.center
            val arrivalTimes = listOf(0.20f, 0.55f, 0.90f)

            for (i in creatures.indices) {
                val c = creatures[i]
                val arrivalT = arrivalTimes.getOrElse(i) { 0.2f + i * 0.35f }

                if (simulationTime >= arrivalT) {
                    val progress = ((simulationTime - arrivalT) / 0.45f).coerceIn(0f, 1f)
                    val startPos = currentLevel.initialCreatures.getOrNull(i)?.position ?: Vector2D(500f, 300f)
                    // Interpolate smoothly towards docking berth inside goal
                    val berthOffset = Vector2D((i - 1) * (goalZone.radius * 0.45f), 15f)
                    val targetPos = targetGoal + berthOffset
                    c.position = startPos + (targetPos - startPos) * progress

                    if (progress >= 0.75f) {
                        c.isInsideGoal = true
                        c.inGoalTime += dt
                        c.expression = CreatureExpression.HAPPY
                    }
                }
            }

            // Check if all creatures are safely docked
            val allInGoal = creatures.isNotEmpty() && creatures.all { it.isInsideGoal }
            val allDocked = allInGoal && creatures.all { it.inGoalTime >= 0.35f }

            if (allDocked && state == SimulationState.RUNNING) {
                state = SimulationState.SUCCESS
                hapticManager?.playSuccess()
                // Confetti particles
                for (k in 0 until 16) {
                    particles.add(
                        Particle(
                            position = goalZone.center,
                            velocity = Vector2D((Math.random().toFloat() - 0.5f) * 400f, -Math.random().toFloat() * 300f),
                            color = if (k % 2 == 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                            size = 8f,
                            maxLife = 1.2f,
                            type = ParticleType.CONFETTI
                        )
                    )
                }
            }
        } else {
            // Wrong pin solution: creature falls into trap or mechanism blocks path
            if (simulationTime >= 1.1f && state == SimulationState.RUNNING) {
                if (creatures.isNotEmpty()) {
                    val first = creatures.first()
                    first.expression = CreatureExpression.DIZZY
                    first.isTrapped = true
                    // Move into danger pit if present
                    if (dangerPits.isNotEmpty()) {
                        val pit = dangerPits.first().bounds
                        first.position = Vector2D(pit.left + pit.width * 0.5f, pit.top + pit.height * 0.5f)
                    }
                }
                failureReason = "CREATURE_TRAPPED_IN_DANGER_BASIN"
                state = SimulationState.FAILED
                screenShake = 1.0f
                hapticManager?.playFail()
            }
        }
    }
}
