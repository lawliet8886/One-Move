package com.example.onemove.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.Creature
import com.example.onemove.model.GameProgressRepository
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.model.Vector2D
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.HomeNestRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val creatures: List<Creature> = emptyList()
)

class OneMoveViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepo = GameProgressRepository(application.applicationContext)
    private val hapticManager = HapticManager(application.applicationContext)

    val physicsWorld = PhysicsWorld(
        initialLevel = LevelCatalog.getLevel(1),
        hapticManager = hapticManager
    )

    private val _uiState = MutableStateFlow(
        GameUiState(
            currentLevelNumber = 1,
            currentLevel = physicsWorld.currentLevel,
            highestUnlockedLevel = progressRepo.getHighestUnlockedLevel(),
            completedLevels = (1..12).filter { progressRepo.isLevelCompleted(it) }.toSet(),
            creatures = physicsWorld.creatures
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var overlayDelayJob: Job? = null

    init {
        val initialLevel = LevelCatalog.getLevel(1)
        loadLevel(initialLevel.levelNumber)
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val deltaSeconds = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastTimeNanos = now

                val prevState = physicsWorld.state

                // Step physics simulation
                physicsWorld.step(deltaSeconds)

                val currentState = physicsWorld.state

                // Handle State Transition: SUCCESS / FAILED
                if (prevState == SimulationState.RUNNING && (currentState == SimulationState.SUCCESS || currentState == SimulationState.FAILED)) {
                    if (currentState == SimulationState.SUCCESS) {
                        progressRepo.markLevelCompleted(
                            _uiState.value.currentLevelNumber,
                            physicsWorld.simulationTime
                        )
                        _uiState.update {
                            it.copy(
                                highestUnlockedLevel = progressRepo.getHighestUnlockedLevel(),
                                completedLevels = (1..12).filter { lvl -> progressRepo.isLevelCompleted(lvl) }.toSet()
                            )
                        }
                    }
                    scheduleOverlayDisplay(currentState)
                }

                // Update UI state
                _uiState.update { current ->
                    current.copy(
                        simulationState = physicsWorld.state,
                        chosenPin = physicsWorld.chosenPinId,
                        failureReason = physicsWorld.failureReason,
                        screenShake = physicsWorld.screenShake,
                        moveCountLeft = if (physicsWorld.state == SimulationState.READY) 1 else 0,
                        frameTick = current.frameTick + 1,
                        creatures = physicsWorld.creatures
                    )
                }

                // ~60 FPS target
                delay(16)
            }
        }
    }

    private fun scheduleOverlayDisplay(state: SimulationState) {
        overlayDelayJob?.cancel()
        overlayDelayJob = viewModelScope.launch {
            val delayMs = if (state == SimulationState.SUCCESS) 550L else 320L
            delay(delayMs)
            _uiState.update { it.copy(showResultOverlay = true) }
        }
    }

    /**
     * Loads a specific level.
     */
    fun loadLevel(levelNumber: Int) {
        overlayDelayJob?.cancel()
        HomeNestRenderer.reset()
        val level = LevelCatalog.getLevel(levelNumber)
        physicsWorld.loadLevel(level)
        progressRepo.recordAttempt(levelNumber)

        _uiState.update {
            it.copy(
                currentLevelNumber = level.levelNumber,
                currentLevel = level,
                simulationState = SimulationState.READY,
                chosenPin = null,
                failureReason = "",
                screenShake = 0f,
                moveCountLeft = 1,
                showResultOverlay = false,
                showLevelSelectSheet = false,
                creatures = physicsWorld.creatures
            )
        }
    }

    /**
     * Advances to the next level.
     */
    fun nextLevel() {
        val nextLvl = (_uiState.value.currentLevelNumber + 1).coerceAtMost(LevelCatalog.ALL_LEVELS.size)
        loadLevel(nextLvl)
    }

    /**
     * Goes to the previous level.
     */
    fun previousLevel() {
        val prevLvl = (_uiState.value.currentLevelNumber - 1).coerceAtLeast(1)
        loadLevel(prevLvl)
    }

    /**
     * Handle user tap on the interactive Canvas.
     */
    fun onCanvasTap(worldX: Float, worldY: Float) {
        if (_uiState.value.simulationState != SimulationState.READY) return

        val tapPos = Vector2D(worldX, worldY)
        for (pin in physicsWorld.pins) {
            if (pin.isRemoved) continue

            // Check distance to handle
            val distToHandle = tapPos.distanceTo(pin.handlePosition)
            if (distToHandle <= 65f) {
                pullPin(pin.id)
                return
            }

            // Check distance to pin rod
            val seg = pin.end - pin.start
            val segLenSq = seg.lengthSquared()
            if (segLenSq > 0f) {
                val toTap = tapPos - pin.start
                val t = (toTap.dot(seg) / segLenSq).coerceIn(0f, 1f)
                val closest = pin.start + seg * t
                if (tapPos.distanceTo(closest) <= 50f) {
                    pullPin(pin.id)
                    return
                }
            }
        }
    }

    fun pullPin(pinId: PinId) {
        if (physicsWorld.pullPin(pinId)) {
            _uiState.update {
                it.copy(
                    simulationState = physicsWorld.state,
                    chosenPin = pinId,
                    moveCountLeft = 0,
                    showResultOverlay = false
                )
            }
        }
    }

    /**
     * Instant Reset: restarts the current level instantly (< 1ms).
     */
    fun resetGame() {
        overlayDelayJob?.cancel()
        HomeNestRenderer.reset()
        physicsWorld.reset()
        progressRepo.recordRetry(_uiState.value.currentLevelNumber)
        _uiState.update {
            it.copy(
                simulationState = SimulationState.READY,
                chosenPin = null,
                failureReason = "",
                screenShake = 0f,
                moveCountLeft = 1,
                showResultOverlay = false,
                creatures = physicsWorld.creatures
            )
        }
    }

    fun openLevelSelect() {
        _uiState.update { it.copy(showLevelSelectSheet = true) }
    }

    fun closeLevelSelect() {
        _uiState.update { it.copy(showLevelSelectSheet = false) }
    }

    fun toggleDebugBounds() {
        _uiState.update { it.copy(showDebugBounds = !it.showDebugBounds) }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        overlayDelayJob?.cancel()
    }
}
