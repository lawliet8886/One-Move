package com.example.onemove.physics

import androidx.compose.ui.graphics.Color
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Deterministic, fixed-step circle/segment solver for the small One Move boards.
 * Gameplay never reads solutionPinId: a rescue requires physical entry into the nest.
 * Cosmetic randomness is seeded and never participates in collision/outcome decisions.
 */
class PhysicsWorld(initialLevel: LevelDefinition, private val hapticManager: HapticManager? = null) {
    var currentLevel: LevelDefinition = initialLevel
        private set
    var state = SimulationState.READY
    var chosenPinId: PinId? = null
    var failureReason = ""
    var screenShake = 0f
    var simulationTime = 0f
    var visualTime = 0f
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
    val pressurePlates = mutableListOf<PressurePlate>()
    var goalZone = initialLevel.goalZone
    val particles = mutableListOf<Particle>()
    val heavyBall: HeavyBall? get() = heavyBalls.firstOrNull()
    val seesaw: SeesawLever? get() = seesaws.firstOrNull()
    val rollingStone: RollingStone? get() = rollingStones.firstOrNull()
    val events = mutableListOf<PhysicsEvent>()
    var collisionCount = 0
        private set
    private var accumulator = 0.0
    private var ticks = 0L
    private var quietTime = 0f
    private var random = Random(0)
    private val springCooldown = mutableMapOf<Pair<Int, Int>, Float>()

    data class PhysicsEvent(val time: Float, val kind: String, val objectId: String)
    private data class Body(var p: Vector2D, var v: Vector2D, val r: Float, val invMass: Float,
                            val creature: Creature? = null, val ball: HeavyBall? = null, val stone: RollingStone? = null)
    private data class Rail(val a: Vector2D, val b: Vector2D, val thickness: Float,
                            val restitution: Float = 0.04f, val lever: SeesawLever? = null)

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
        screenShake = 0f
        simulationTime = 0f
        visualTime = 0f
        accumulator = 0.0
        ticks = 0
        quietTime = 0f
        collisionCount = 0
        random = Random(currentLevel.number)
        particles.clear()
        events.clear()
        springCooldown.clear()
        creatures.clear()
        creatures.addAll(currentLevel.initialCreatures.map {
            it.copy(isInsideGoal = false, inGoalTime = 0f, isGrounded = false,
                    isTrapped = false, expression = CreatureExpression.NORMAL)
        })
        pins.clear(); pins.addAll(currentLevel.pins.map { it.copy(isRemoved = false, isSelected = false, pullProgress = 0f) })
        platforms.clear(); platforms.addAll(currentLevel.platforms)
        dangerPits.clear(); dangerPits.addAll(currentLevel.dangerPits)
        springBumpers.clear(); springBumpers.addAll(currentLevel.springBumpers.map { it.copy(compression = 0f, flashTimer = 0f) })
        seesaws.clear(); seesaws.addAll(currentLevel.seesaws.map { it.copy() })
        heavyBalls.clear(); heavyBalls.addAll(currentLevel.heavyBalls.map { it.copy() })
        rollingStones.clear(); rollingStones.addAll(currentLevel.rollingStones.map { it.copy() })
        creatureGates.clear(); creatureGates.addAll(currentLevel.creatureGates.map { it.copy() })
        pressurePlates.clear(); pressurePlates.addAll(currentLevel.pressurePlates.map {
            it.copy(contactTime = 0f, isLatched = false)
        })
    }

    fun pullPin(pinId: PinId): Boolean {
        if (state != SimulationState.READY) return false
        val pin = pins.firstOrNull { it.id == pinId && !it.isRemoved } ?: return false
        pin.isRemoved = true
        pin.isSelected = true
        pin.pullProgress = 0f
        chosenPinId = pinId
        state = SimulationState.RUNNING
        event("pin_pulled", pinId.name)
        hapticManager?.playPinPull()
        burst(pin.handlePosition, pin.color, ParticleType.SPARK, 8)
        return true
    }

    /** Long application pauses must be handled by lifecycle; catch-up is bounded to 250 ms. */
    fun step(dt: Float) {
        if (!dt.isFinite() || dt <= 0f) return
        val elapsed = min(dt, 0.25f)
        visualTime += elapsed
        updateEffects(elapsed)
        if (state != SimulationState.RUNNING) return
        accumulator += elapsed.toDouble()
        while (accumulator + 1e-9 >= STEP.toDouble() && state == SimulationState.RUNNING) {
            fixedStep(STEP)
            accumulator -= STEP.toDouble()
        }
    }

    private fun updateEffects(dt: Float) {
        screenShake = max(0f, screenShake - dt * 3f)
        for (pin in pins) if (pin.isRemoved) pin.pullProgress = min(1f, pin.pullProgress + dt * 5f)
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next(); p.update(dt)
            if (!p.isAlive) it.remove()
        }
        for (spring in springBumpers) {
            spring.compression *= exp(-12f * dt)
            spring.flashTimer = max(0f, spring.flashTimer - dt)
        }
    }

    private fun fixedStep(dt: Float) {
        ticks++
        simulationTime = ticks * STEP
        val bodies = mutableListOf<Body>()
        for (c in creatures) if (!c.isTrapped) {
            c.isGrounded = false
            bodies.add(Body(c.position, c.velocity, c.radius, if (c.isInsideGoal) 0f else 1f, creature = c))
        }
        for (b in heavyBalls) bodies.add(Body(b.position, b.velocity, b.radius, 0.15f, ball = b))
        for (b in rollingStones) bodies.add(Body(b.position, b.velocity, b.radius, 0.3f, stone = b))
        val rails = mutableListOf<Rail>()
        for (p in platforms) rails.add(Rail(p.start, p.end, p.thickness, if (p.isBouncy) 0.62f else 0.04f))
        for (p in pins) if (!p.isRemoved) rails.add(Rail(p.start, p.end, p.thickness))
        for (plate in pressurePlates) rails.add(Rail(
            plate.center - Vector2D(plate.width * 0.5f, 0f),
            plate.center + Vector2D(plate.width * 0.5f, 0f), plate.thickness))
        for (g in creatureGates) {
            val platesReady = g.requiredPlateIds.isNotEmpty() && g.requiredPlateIds.all { id ->
                pressurePlates.any { it.id == id && it.isLatched }
            }
            if (platesReady && !g.isOpen) {
                g.isOpen = true
                event("gate_opened", g.requiredPlateIds.joinToString("+"))
            }
            // Gate motion is a physical linkage, never an answer-key check.
            if (g.releasePinId != null && pins.any { it.id == g.releasePinId && it.isRemoved }) g.isOpen = true
            g.openProgress = (g.openProgress + (if (g.isOpen) 2.5f else -2.5f) * dt).coerceIn(0f, 1f)
            if (g.openProgress < 0.95f) rails.add(Rail(g.pivot,
                g.pivot + (g.closedEnd - g.pivot) * (1f - g.openProgress), g.thickness))
        }
        for (s in seesaws) {
            s.angularVelocity = ((s.angularVelocity - s.angle * 1.4f * dt) * exp(-1.6f * dt)).coerceIn(-1.8f, 1.8f)
            s.angle = (s.angle + s.angularVelocity * dt).coerceIn(-0.48f, 0.48f)
            val arm = Vector2D.fromAngle(s.angle, s.halfLength)
            rails.add(Rail(s.pivot - arm, s.pivot + arm, s.thickness, lever = s))
        }
        // World side walls are not a floor: falling below the board is an actual loss.
        rails.add(Rail(Vector2D(18f, 0f), Vector2D(18f, LevelDefinition.WORLD_HEIGHT), 12f))
        rails.add(Rail(Vector2D(LevelDefinition.WORLD_WIDTH - 18f, 0f), Vector2D(LevelDefinition.WORLD_WIDTH - 18f, LevelDefinition.WORLD_HEIGHT), 12f))
        for (b in bodies) {
            if (b.invMass == 0f) continue
            b.v = limitSpeed((b.v + Vector2D(0f, GRAVITY * dt)) * exp(-0.09f * dt))
            b.p += b.v * dt
        }
        repeat(5) {
            for (b in bodies) for (r in rails) resolveRail(b, r)
            for (i in bodies.indices) for (j in i + 1 until bodies.size) resolvePair(bodies[i], bodies[j])
        }
        updatePressurePlates(bodies, dt)
        for ((index, body) in bodies.withIndex()) {
            if (body.invMass == 0f) continue
            applySprings(body, index)
            body.creature?.let { c ->
                c.position = body.p; c.velocity = body.v
                c.expression = if (body.v.y > 500f) CreatureExpression.SURPRISED else CreatureExpression.NORMAL
                if (!body.p.x.isFinite() || !body.p.y.isFinite()) fail("INVALID_PHYSICS_STATE", c)
                else if (dangerPits.any { overlapsPit(body.p, body.r, it) }) fail("CREATURE_TRAPPED_IN_DANGER_BASIN", c)
                else if (body.p.y - body.r > LevelDefinition.WORLD_HEIGHT) fail("CREATURE_FELL_OUTSIDE_BOARD", c)
                else if (body.p.distanceTo(goalZone.center) + body.r <= goalZone.radius) {
                    // A trigger volume is crossed naturally. There is no scripted travel to the goal.
                    c.inGoalTime = if (body.v.lengthSquared() < 75f * 75f) c.inGoalTime + dt else 0f
                    if (c.inGoalTime >= 0.20f) {
                        c.isInsideGoal = true; c.velocity = Vector2D.Zero
                        c.expression = CreatureExpression.HAPPY
                        event("rescued", c.id.name)
                        burst(c.position, Color(0xFF34D399), ParticleType.GOAL_SPARKLE, 5)
                    }
                } else c.inGoalTime = 0f
            }
            body.ball?.let { b -> b.position = body.p; b.velocity = body.v; b.rotation += body.v.x * dt / max(b.radius, 1f) * 57.29578f }
            body.stone?.let { b -> b.position = body.p; b.velocity = body.v; b.rotation += body.v.x * dt / max(b.radius, 1f) * 57.29578f }
        }
        for (c in creatures) if (c.isInsideGoal) c.inGoalTime += dt
        if (state != SimulationState.RUNNING) return
        if (creatures.isNotEmpty() && creatures.all { it.isInsideGoal && it.inGoalTime >= 0.35f }) {
            state = SimulationState.SUCCESS
            event("success", "all_creatures_rescued")
            hapticManager?.playSuccess()
            burst(goalZone.center, Color(0xFFFBBF24), ParticleType.CONFETTI, 36)
            return
        }
        val motionless = bodies.all { it.v.lengthSquared() < 64f } && seesaws.all { abs(it.angularVelocity) < 0.025f }
        quietTime = if (motionless) quietTime + dt else 0f
        if (quietTime > 1.25f && simulationTime > 1.5f) fail("PATH_BLOCKED")
        // Explicit bounded timeout, reported distinctly from a physical hazard.
        if (simulationTime >= 8f) fail("SIMULATION_TIMEOUT")
    }

    private fun updatePressurePlates(bodies: List<Body>, dt: Float) {
        for (plate in pressurePlates) {
            if (plate.isLatched) continue
            val supported = bodies.any { body ->
                body.invMass > 0f && 1f / body.invMass >= plate.minimumMass &&
                    abs(body.p.x - plate.center.x) <= plate.width * 0.5f &&
                    abs(body.p.y + body.r + plate.thickness * 0.5f - plate.center.y) <= 1.5f &&
                    abs(body.v.y) < 80f
            }
            plate.contactTime = if (supported) plate.contactTime + dt else 0f
            if (plate.contactTime >= plate.holdSeconds) {
                plate.isLatched = true
                event("plate_latched", plate.id)
                burst(plate.center, Color(0xFF34D399), ParticleType.SPARK, 8)
            }
        }
    }

    private fun resolveRail(b: Body, rail: Rail) {
        if (b.invMass == 0f) return
        val d = rail.b - rail.a
        val denom = d.lengthSquared()
        if (denom < 1e-8f) return
        val t = ((b.p - rail.a).dot(d) / denom).coerceIn(0f, 1f)
        val nearest = rail.a + d * t
        val delta = b.p - nearest
        val distance = delta.length()
        val required = b.r + rail.thickness * 0.5f
        if (distance >= required) return
        val normal = if (distance > 0.00001f) delta / distance else {
            val n = Vector2D(d.y, -d.x).normalized()
            if (b.v.dot(n) <= 0f) n else -n
        }
        b.p += normal * (required - distance + 0.002f)
        val normalSpeed = b.v.dot(normal)
        if (normalSpeed < 0f) {
            val bounce = if (abs(normalSpeed) > 65f) rail.restitution else 0f
            b.v -= normal * ((1f + bounce) * normalSpeed)
            // Small tangential friction leaves ramps usable while avoiding endless jitter.
            val tangent = b.v - normal * b.v.dot(normal)
            b.v -= tangent * 0.006f
            collisionCount++
            rail.lever?.let { lever ->
                val arm = nearest.x - lever.pivot.x
                lever.angularVelocity += (arm * abs(normalSpeed) / max(b.invMass, 0.01f)) * 0.0000015f
            }
        }
        if (normal.y < -0.4f) b.creature?.isGrounded = true
    }

    private fun resolvePair(a: Body, b: Body) {
        if (a.invMass + b.invMass == 0f) return
        val d = b.p - a.p
        val distance = d.length()
        val required = a.r + b.r
        if (distance >= required) return
        val n = if (distance > 0.00001f) d / distance else Vector2D(1f, 0f)
        val massSum = a.invMass + b.invMass
        val correction = n * ((required - distance + 0.002f) / massSum)
        a.p -= correction * a.invMass; b.p += correction * b.invMass
        val closing = (b.v - a.v).dot(n)
        if (closing < 0f) {
            val impulse = n * (-1.12f * closing / massSum)
            a.v -= impulse * a.invMass; b.v += impulse * b.invMass
            collisionCount++
            // A heavy-object failure must be caused by the heavy body striking the friend.
            // Relative speed alone is not enough: a friend running into a parked weight is
            // a collision/deflection, not the weight magically "attacking" the friend.
            val heavyImpactThreshold = 340f
            if (a.creature != null && (b.ball != null || b.stone != null)) {
                val heavyTowardCreature = -b.v.dot(n)
                if (heavyTowardCreature > heavyImpactThreshold) fail("HIT_BY_HEAVY_OBJECT", a.creature)
            }
            if (b.creature != null && (a.ball != null || a.stone != null)) {
                val heavyTowardCreature = a.v.dot(n)
                if (heavyTowardCreature > heavyImpactThreshold) fail("HIT_BY_HEAVY_OBJECT", b.creature)
            }
        }
    }

    private fun applySprings(b: Body, bodyIndex: Int) {
        for ((springIndex, s) in springBumpers.withIndex()) {
            val key = bodyIndex to springIndex
            if (simulationTime < (springCooldown[key] ?: 0f)) continue
            val normal = s.direction.normalized()
            if (normal.lengthSquared() < 0.5f) continue
            val delta = b.p - s.position
            val distance = delta.dot(normal)
            val tangent = Vector2D(-normal.y, normal.x)
            if (abs(delta.dot(tangent)) > s.width * 0.5f + b.r * 0.5f) continue
            if (distance < -b.r || distance > b.r + s.restHeight || b.v.dot(normal) >= -20f) continue
            b.v = normal * 780f + tangent * (b.v.dot(tangent) * 0.3f)
            s.compression = 1f; s.flashTimer = 0.3f
            springCooldown[key] = simulationTime + 0.3f
            screenShake = max(screenShake, 0.3f)
            event("spring", springIndex.toString())
        }
    }

    private fun overlapsPit(p: Vector2D, r: Float, pit: DangerPit): Boolean {
        val b = pit.bounds
        val nearest = Vector2D(p.x.coerceIn(b.left, b.right), p.y.coerceIn(b.top, b.bottom))
        return p.distanceSquaredTo(nearest) < r * r
    }

    private fun fail(reason: String, creature: Creature? = null) {
        if (state != SimulationState.RUNNING) return
        state = SimulationState.FAILED
        failureReason = reason
        creature?.let { it.isTrapped = true; it.expression = CreatureExpression.DIZZY }
        screenShake = 0.8f
        event("failed", reason)
        hapticManager?.playFail()
    }

    private fun event(kind: String, id: String) {
        if (events.size < 512) events.add(PhysicsEvent(simulationTime, kind, id))
    }

    private fun burst(position: Vector2D, color: Color, type: ParticleType, count: Int) {
        repeat(min(count, 160 - particles.size).coerceAtLeast(0)) {
            particles.add(Particle(position = position,
                velocity = Vector2D((random.nextFloat() - 0.5f) * 420f, -random.nextFloat() * 380f),
                color = color, size = if (type == ParticleType.CONFETTI) 7f else 5f,
                maxLife = if (type == ParticleType.CONFETTI) 1.6f else 0.55f,
                type = type, rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 360f))
        }
    }

    private fun limitSpeed(v: Vector2D): Vector2D {
        if (!v.x.isFinite() || !v.y.isFinite()) return Vector2D.Zero
        val speed = v.length()
        return if (speed > 2100f) v * (2100f / speed) else v
    }

    companion object {
        const val STEP = 1f / 240f
        const val GRAVITY = 1100f
    }
}
