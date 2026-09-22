package com.example.onemove.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.HomeNestRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val currentLevelNumber: Int = 1,
    val currentLevel: LevelDefinition = LevelCatalog.createLevel01(),
    val totalLevels: Int = LevelCatalog.ALL_LEVELS.size,
    val highestUnlockedLevel: Int = 1,
    val completedLevels: Set<Int> = emptySet(),
    val simulationState: SimulationState = SimulationState.READY,
    val chosenPin: PinId? = null,
    val failureReason: String = "",
    val screenShake: Float = 0f,
    val showDebugBounds: Boolean = false,
    val moveCountLeft: Int = 1,
    val frameTick: Long = 0L,
    val showResultOverlay: Boolean = false,
    val showLevelSelectSheet: Boolean = false,
    val creatures: List<Creature> = emptyList(),
    val rescuedCount: Int = 0,
    val needsAnimation: Boolean = false
)

class OneMoveViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepo = GameProgressRepository(application.applicationContext)
    private val hapticManager = HapticManager(application.applicationContext)
    val physicsWorld = PhysicsWorld(LevelCatalog.getLevel(1), hapticManager)
    private val _uiState = MutableStateFlow(GameUiState(
        currentLevel = physicsWorld.currentLevel,
        highestUnlockedLevel = progressRepo.getHighestUnlockedLevel(),
        completedLevels = completed(), creatures = physicsWorld.creatures))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private val _renderTick = mutableLongStateOf(0L)
    val renderTick: LongState get() = _renderTick
    private var resultDelay = -1f
    private var lastFrameNanos: Long? = null

    init { loadLevel(1) }

    private fun completed(): Set<Int> = LevelCatalog.ALL_LEVELS.map { it.number }
        .filter { progressRepo.isLevelCompleted(it) }.toSet()

    fun resetFrameClock() { lastFrameNanos = null }

    /** Main-thread, display-synchronised ticks only while visible work remains. */
    fun onFrame(frameNanos: Long) {
        val previous = lastFrameNanos
        lastFrameNanos = frameNanos
        if (previous == null || _uiState.value.showLevelSelectSheet) return
        val dt = ((frameNanos - previous) / 1_000_000_000f).coerceIn(0f, 0.1f)
        if (dt <= 0f) return
        if (physicsWorld.state == SimulationState.READY && physicsWorld.particles.isEmpty()) return
        if (physicsWorld.state != SimulationState.RUNNING && resultDelay < 0f &&
            physicsWorld.particles.isEmpty() && physicsWorld.screenShake <= 0f) return
        val old = physicsWorld.state
        physicsWorld.step(dt)
        val now = physicsWorld.state
        if (old == SimulationState.RUNNING && now != SimulationState.RUNNING) {
            if (now == SimulationState.SUCCESS) {
                progressRepo.markLevelCompleted(_uiState.value.currentLevelNumber, physicsWorld.simulationTime)
                _uiState.update { it.copy(highestUnlockedLevel = progressRepo.getHighestUnlockedLevel(), completedLevels = completed()) }
            }
            resultDelay = if (now == SimulationState.SUCCESS) 0.8f else 0.4f
        } else if (resultDelay >= 0f) {
            resultDelay -= dt
            if (resultDelay <= 0f) {
                resultDelay = -1f
                _uiState.update { it.copy(showResultOverlay = true) }
            }
        }
        val rescued = physicsWorld.creatures.count { it.isInsideGoal }
        val hasWork = now == SimulationState.RUNNING || resultDelay >= 0f ||
            physicsWorld.particles.isNotEmpty() || physicsWorld.screenShake > 0f
        _uiState.update { current -> current.copy(simulationState = now, chosenPin = physicsWorld.chosenPinId,
            failureReason = physicsWorld.failureReason, screenShake = if (physicsWorld.screenShake > 0f) 1f else 0f,
            moveCountLeft = if (now == SimulationState.READY) 1 else 0,
            rescuedCount = rescued, needsAnimation = hasWork,
            creatures = if (current.rescuedCount != rescued || current.simulationState != now)
                physicsWorld.creatures.map { it.copy() } else current.creatures) }
        // Only Canvas observes this high-frequency state, not the HUD/layout tree.
        _renderTick.longValue++
    }

    fun loadLevel(levelNumber: Int) {
        resultDelay = -1f
        resetFrameClock()
        HomeNestRenderer.reset()
        val level = LevelCatalog.getLevel(levelNumber)
        physicsWorld.loadLevel(level)
        progressRepo.recordAttempt(level.number)
        _uiState.update { it.copy(currentLevelNumber = level.number, currentLevel = level,
            simulationState = SimulationState.READY, chosenPin = null, failureReason = "", screenShake = 0f,
            moveCountLeft = 1, showResultOverlay = false, showLevelSelectSheet = false,
            rescuedCount = 0, needsAnimation = false, creatures = physicsWorld.creatures.map { it.copy() }) }
        _renderTick.longValue++
    }

    fun nextLevel() {
        if (_uiState.value.currentLevelNumber >= LevelCatalog.ALL_LEVELS.size) openLevelSelect()
        else loadLevel(_uiState.value.currentLevelNumber + 1)
    }
    fun previousLevel() = loadLevel((_uiState.value.currentLevelNumber - 1).coerceAtLeast(1))
    fun onCanvasTap(worldX: Float, worldY: Float, handleRadius: Float = 65f) {
        if (_uiState.value.simulationState != SimulationState.READY || _uiState.value.showLevelSelectSheet) return
        PinHitTester.find(physicsWorld.pins, worldX, worldY, handleRadius)?.let(::pullPin)
    }
    fun pullPin(pinId: PinId) {
        if (_uiState.value.showLevelSelectSheet) return
        if (physicsWorld.pullPin(pinId)) {
            resultDelay = -1f
            _uiState.update { it.copy(simulationState = physicsWorld.state, chosenPin = pinId,
                moveCountLeft = 0, showResultOverlay = false, needsAnimation = true) }
        }
    }
    fun resetGame() {
        resultDelay = -1f
        resetFrameClock()
        HomeNestRenderer.reset()
        physicsWorld.reset()
        progressRepo.recordRetry(_uiState.value.currentLevelNumber)
        _uiState.update { it.copy(simulationState = SimulationState.READY, chosenPin = null,
            failureReason = "", screenShake = 0f, moveCountLeft = 1, showResultOverlay = false,
            creatures = physicsWorld.creatures.map { it.copy() }, rescuedCount = 0, needsAnimation = false) }
        _renderTick.longValue++
    }
    fun openLevelSelect() { _uiState.update { it.copy(showLevelSelectSheet = true) } }
    fun closeLevelSelect() { resetFrameClock(); _uiState.update { it.copy(showLevelSelectSheet = false) } }
    fun toggleDebugBounds() { _uiState.update { it.copy(showDebugBounds = !it.showDebugBounds) } }
}
