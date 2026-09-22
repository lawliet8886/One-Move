package com.example.onemove.physics

import androidx.compose.ui.graphics.Color
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.*
import kotlin.math.*
import kotlin.random.Random

/** Fixed-step circle/capsule dynamics. Level solution metadata is NEVER read here. */
class PhysicsWorld(initialLevel: LevelDefinition, private val hapticManager: HapticManager? = null) {
    var currentLevel = initialLevel
        private set
    var state = SimulationState.READY
    var chosenPinId: PinId? = null
    var failureReason = ""
    var screenShake = 0f
    var simulationTime = 0f
    var terminalTime = 0f
        private set
    val creatures = mutableListOf<Creature>()
    val pins = mutableListOf<Pin>()
    val platforms = mutableListOf<Platform>()
    val dangerPits = mutableListOf<DangerPit>()
    val springBumpers = mutableListOf<SpringBumper>()
    val seesaws = mutableListOf<SeesawLever>()
    val heavyBalls = mutableListOf<HeavyBall>()
    val rollingStones = mutableListOf<RollingStone>()
    val creatureGates = mutableListOf<CreatureGate>()
    var goalZone = initialLevel.goalZone
    val particles = mutableListOf<Particle>()
    val telemetry = mutableListOf<TelemetryEvent>()
    val heavyBall get() = heavyBalls.firstOrNull()
    val seesaw get() = seesaws.firstOrNull()
    val rollingStone get() = rollingStones.firstOrNull()
    private var accumulator = 0.0
    private var settledTime = 0f
    private var random = Random(0)
    private val bodies = mutableListOf<Body>()

    private class Body(val creature: Creature? = null, val ball: HeavyBall? = null, val stone: RollingStone? = null) {
        var p: Vector2D
            get() = creature?.position ?: ball?.position ?: stone!!.position
            set(value) { when { creature != null -> creature.position = value; ball != null -> ball.position = value; else -> stone!!.position = value } }
        var v: Vector2D
            get() = creature?.velocity ?: ball?.velocity ?: stone!!.velocity
            set(value) { when { creature != null -> creature.velocity = value; ball != null -> ball.velocity = value; else -> stone!!.velocity = value } }
        val radius get() = creature?.radius ?: ball?.radius ?: stone!!.radius
        val invMass get() = if (creature != null) 1f else if (ball != null) 0.2f else 0.35f
        val docked get() = creature?.isInsideGoal == true
        var springCooldown = 0f
        var grounded = false
    }

    init { loadLevel(initialLevel) }

    fun loadLevel(level: LevelDefinition) {
        currentLevel = level
        goalZone = level.goalZone
        reset()
    }

    fun reset() {
        state = SimulationState.READY
        chosenPinId = null
        failureReason = ""
        simulationTime = 0f
        terminalTime = 0f
        screenShake = 0f
        accumulator = 0.0
        settledTime = 0f
        random = Random(currentLevel.number * 101)
        particles.clear()
        telemetry.clear()
        creatures.clear()
        creatures += currentLevel.initialCreatures.map { it.copy(expression = CreatureExpression.NORMAL, isInsideGoal = false, inGoalTime = 0f, isGrounded = false, isTrapped = false) }
        pins.clear(); pins += currentLevel.pins.map { it.copy(isRemoved = false, isSelected = false, pullProgress = 0f) }
        platforms.clear(); platforms += currentLevel.platforms
        dangerPits.clear(); dangerPits += currentLevel.dangerPits
        springBumpers.clear(); springBumpers += currentLevel.springBumpers.map { it.copy(compression = 0f, flashTimer = 0f) }
        seesaws.clear(); seesaws += currentLevel.seesaws.map { it.copy() }
        heavyBalls.clear(); heavyBalls += currentLevel.heavyBalls.map { it.copy() }
        rollingStones.clear(); rollingStones += currentLevel.rollingStones.map { it.copy() }
        creatureGates.clear(); creatureGates += currentLevel.creatureGates.map { it.copy(openProgress = 0f, isOpen = false) }
        bodies.clear()
        bodies += creatures.map { Body(creature = it) }
        bodies += heavyBalls.map { Body(ball = it) }
        bodies += rollingStones.map { Body(stone = it) }
    }

    fun pullPin(pinId: PinId): Boolean {
        if (state != SimulationState.READY) return false
        val pin = pins.firstOrNull { it.id == pinId && !it.isRemoved } ?: return false
        pin.isRemoved = true
        pin.isSelected = true
        pin.pullProgress = 0f
        chosenPinId = pinId
        state = SimulationState.RUNNING
        hapticManager?.playPinPull()
        emit(pin.handlePosition, pin.color, ParticleType.SPARK, 10)
        return true
    }

    /** Fixed 240 Hz integration; long frames are bounded to avoid the spiral of death. */
    fun step(dt: Float) {
        if (!dt.isFinite() || dt <= 0f || state == SimulationState.READY) return
        accumulator += min(dt, 0.25f).toDouble()
        while (accumulator + 1e-9 >= FIXED_STEP.toDouble()) {
            accumulator -= FIXED_STEP.toDouble()
            simulationTime += FIXED_STEP
            updateEffects(FIXED_STEP)
            if (state == SimulationState.RUNNING) integrate(FIXED_STEP)
        }
    }

    private fun updateEffects(dt: Float) {
        screenShake = (screenShake - dt * 3f).coerceAtLeast(0f)
        pins.filter { it.isRemoved }.forEach { it.pullProgress = min(1f, it.pullProgress + dt * 5f) }
        particles.forEach { it.update(dt) }
        particles.removeAll { !it.isAlive }
        springBumpers.forEach {
            it.compression = max(0f, it.compression - dt * 4f)
            it.flashTimer = max(0f, it.flashTimer - dt)
        }
    }

    private fun integrate(dt: Float) {
        for (gate in creatureGates) {
            // A hinge is released by its physical retaining pin, not by a winning answer.
            if (!gate.isOpen && gate.releasePinId != null && pins.any { it.id == gate.releasePinId && it.isRemoved }) {
                gate.isOpen = true
                telemetry += TelemetryEvent(TelemetryEventType.CREATURE_GATE_RELEASE, simulationTime, gate.pivot, Vector2D.Zero)
            }
            if (gate.isOpen) gate.openProgress = min(1f, gate.openProgress + dt * 1.8f)
        }
        for (lever in seesaws) {
            lever.angularVelocity += -lever.angle * 0.15f * dt
            lever.angularVelocity *= exp(-2f * dt)
            lever.angle = (lever.angle + lever.angularVelocity * dt).coerceIn(-0.36f, 0.36f)
        }
        for (b in bodies) {
            b.grounded = false
            b.springCooldown = max(0f, b.springCooldown - dt)
            if (b.docked) continue
            b.v += Vector2D(0f, GRAVITY * dt)
            if (b.v.lengthSquared() > MAX_SPEED * MAX_SPEED) b.v = b.v.normalized() * MAX_SPEED
            b.p += b.v * dt
        }
        repeat(4) {
            for (b in bodies) {
                if (b.docked) continue
                for (p in platforms) contact(b, p.start, p.end, p.thickness, if (p.isBouncy) 0.45f else 0.06f)
                for (p in pins) if (!p.isRemoved) contact(b, p.start, p.end, p.thickness, 0.03f)
                for (g in creatureGates) {
                    contact(b, g.pivot, g.currentEnd(), g.thickness, 0.04f)
                }
                for (s in seesaws) {
                    val arm = Vector2D.fromAngle(s.angle, s.halfLength)
                    if (contact(b, s.pivot - arm, s.pivot + arm, s.thickness, 0.04f)) {
                        val torque = (b.p.x - s.pivot.x) / max(s.halfLength, 1f)
                        s.angularVelocity += torque * dt * 0.75f / b.invMass
                    }
                }
                if (b.p.x < b.radius + 40f) { b.p = Vector2D(b.radius + 40f, b.p.y); b.v = Vector2D(abs(b.v.x) * 0.3f, b.v.y) }
                if (b.p.x > LevelDefinition.WORLD_WIDTH - 40f - b.radius) { b.p = Vector2D(LevelDefinition.WORLD_WIDTH - 40f - b.radius, b.p.y); b.v = Vector2D(-abs(b.v.x) * 0.3f, b.v.y) }
            }
            for (i in bodies.indices) for (j in i + 1 until bodies.size) collideBodies(bodies[i], bodies[j])
        }
        for (b in bodies) {
            if (b.docked) continue
            for (spring in springBumpers) {
                val normal = spring.direction.normalized()
                val tangent = Vector2D(-normal.y, normal.x)
                val offset = b.p - spring.position
                if (b.springCooldown == 0f && abs(offset.dot(tangent)) < spring.width * 0.5f + b.radius && offset.dot(normal) in 0f..(b.radius + 18f) && b.v.dot(normal) < 0f) {
                    b.p += normal * max(0f, b.radius + 18f - offset.dot(normal))
                    val tangentSpeed = b.v.dot(tangent)
                    b.v = normal * 580f + tangent * tangentSpeed * 0.45f
                    b.springCooldown = 0.45f
                    spring.compression = 1f
                    spring.flashTimer = 0.3f
                    emit(spring.position, Color(0xFF67E8F9), ParticleType.SPARK, 6)
                    telemetry += TelemetryEvent(TelemetryEventType.FIRST_CREATURE_SPRING_CONTACT, simulationTime, b.p, b.v)
                }
            }
            b.ball?.let { it.rotation += b.v.x * dt / b.radius }
            b.stone?.let { it.rotation += b.v.x * dt / b.radius }
            val c = b.creature ?: continue
            c.isGrounded = b.grounded
            if (!b.p.x.isFinite() || !b.p.y.isFinite() || !b.v.x.isFinite() || !b.v.y.isFinite()) { finish(false, "INVALID_PHYSICS_STATE"); return }
            val hazard = dangerPits.any { p ->
                val x = b.p.x.coerceIn(p.bounds.left, p.bounds.right)
                val y = b.p.y.coerceIn(p.bounds.top, p.bounds.bottom)
                (b.p - Vector2D(x, y)).lengthSquared() < b.radius * b.radius
            }
            if (hazard || b.p.y > LevelDefinition.WORLD_HEIGHT + b.radius) {
                c.isTrapped = true; c.expression = CreatureExpression.DIZZY
                finish(false, if (hazard) "CREATURE_TRAPPED_IN_DANGER_BASIN" else "CREATURE_OUT_OF_BOUNDS")
                return
            }
            val inside = b.p.distanceTo(goalZone.center) <= goalZone.radius - b.radius * 0.5f
            c.inGoalTime = if (inside && b.v.length() < 180f) c.inGoalTime + dt else 0f
            if (c.inGoalTime >= 0.3f) {
                c.isInsideGoal = true; c.expression = CreatureExpression.HAPPY; b.v = Vector2D.Zero
                emit(b.p, Color(0xFF6EE7B7), ParticleType.HEART, 5)
            } else c.expression = if (b.v.y > 600f) CreatureExpression.SURPRISED else if (b.v.length() > 160f) CreatureExpression.CURIOUS else CreatureExpression.NORMAL
        }
        if (creatures.isNotEmpty() && creatures.all { it.isInsideGoal }) { finish(true); return }
        val stranded = bodies.filter { it.creature != null && !it.docked }
        val settled = stranded.isNotEmpty() && stranded.all { it.grounded && it.v.length() < 18f }
        settledTime = if (settled) settledTime + dt else 0f
        if (settledTime > 1.35f && simulationTime > 1.7f) finish(false, "PATH_BLOCKED")
        else if (simulationTime >= 8f) finish(false, "TIME_LIMIT_REACHED")
    }

    private fun contact(b: Body, start: Vector2D, end: Vector2D, thickness: Float, restitution: Float): Boolean {
        val segment = end - start
        val lengthSquared = segment.lengthSquared()
        val t = if (lengthSquared > 0f) ((b.p - start).dot(segment) / lengthSquared).coerceIn(0f, 1f) else 0f
        val closest = start + segment * t
        val delta = b.p - closest
        val distance = delta.length()
        val limit = b.radius + thickness * 0.5f
        if (distance >= limit) return false
        val n = if (distance > 0.0001f) delta / distance else Vector2D(0f, -1f)
        b.p += n * (limit - distance + 0.01f)
        val vn = b.v.dot(n)
        if (vn < 0f) b.v -= n * ((1f + restitution) * vn)
        val tangent = b.v - n * b.v.dot(n)
        b.v -= tangent * 0.006f
        if (n.y < -0.35f) b.grounded = true
        return true
    }

    private fun collideBodies(a: Body, b: Body) {
        if (a.docked && b.docked) return
        val d = b.p - a.p
        val distance = d.length()
        val limit = a.radius + b.radius
        if (distance >= limit) return
        val n = if (distance > 0.0001f) d / distance else Vector2D(1f, 0f)
        val wa = if (a.docked) 0f else a.invMass
        val wb = if (b.docked) 0f else b.invMass
        val w = wa + wb
        if (w == 0f) return
        val correction = n * ((limit - distance + 0.01f) / w)
        a.p -= correction * wa; b.p += correction * wb
        val closingSpeed = (b.v - a.v).dot(n)
        if (closingSpeed < 0f) {
            val impulse = -(1.08f * closingSpeed) / w
            a.v -= n * (impulse * wa); b.v += n * (impulse * wb)
        }
    }

    private fun finish(won: Boolean, reason: String = "") {
        if (state != SimulationState.RUNNING) return
        state = if (won) SimulationState.SUCCESS else SimulationState.FAILED
        terminalTime = simulationTime
        failureReason = reason
        if (won) { hapticManager?.playSuccess(); emit(goalZone.center, Color(0xFFFBBF24), ParticleType.CONFETTI, 32) }
        else { hapticManager?.playFail(); screenShake = 0.6f }
    }

    private fun emit(position: Vector2D, color: Color, type: ParticleType, count: Int) {
        repeat(count) {
            particles += Particle(position, Vector2D((random.nextFloat() - 0.5f) * 320f, -100f - random.nextFloat() * 280f), color, 5f + random.nextFloat() * 4f, if (type == ParticleType.CONFETTI) 1.4f else 0.6f, type = type, rotation = random.nextFloat() * 360f, rotationSpeed = (random.nextFloat() - 0.5f) * 360f)
        }
        while (particles.size > 160) particles.removeAt(0)
    }

    companion object {
        const val FIXED_STEP = 1f / 240f
        const val GRAVITY = 980f
        const val MAX_SPEED = 1600f
    }
}
