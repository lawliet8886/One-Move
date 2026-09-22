package com.example.onemove.ui

import android.app.Application
import android.view.Choreographer
import androidx.lifecycle.AndroidViewModel
import com.example.onemove.audio.HapticManager
import com.example.onemove.model.*
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.HomeNestRenderer
import kotlinx.coroutines.flow.MutableStateFlow
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
    val creatures: List<Creature> = emptyList()
)

/** Frame-paced simulation; a backgrounded app or an open level picker never advances the game. */
class OneMoveViewModel(application: Application) : AndroidViewModel(application) {
    private val progressRepo = GameProgressRepository(application.applicationContext)
    private val haptics = HapticManager(application.applicationContext)
    val physicsWorld = PhysicsWorld(LevelCatalog.getLevel(1), haptics)
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    private val choreographer = Choreographer.getInstance()
    private var foreground = false
    private var lastFrame = 0L
    private var callbackPending = false
    private val frameCallback = Choreographer.FrameCallback { nanos ->
        callbackPending = false
        if (foreground) {
            val dt = if (lastFrame == 0L) 0f else ((nanos-lastFrame)/1_000_000_000f).coerceIn(0f,0.1f)
            lastFrame = nanos
            if (!_uiState.value.showLevelSelectSheet && physicsWorld.state != SimulationState.READY) {
                val before = physicsWorld.state
                val needsFrame = before == SimulationState.RUNNING || physicsWorld.simulationTime - physicsWorld.terminalTime < 2.5f
                if (needsFrame) {
                    physicsWorld.step(dt)
                    if (before == SimulationState.RUNNING && physicsWorld.state == SimulationState.SUCCESS) {
                        progressRepo.markLevelCompleted(_uiState.value.currentLevelNumber, physicsWorld.terminalTime)
                    }
                    publish()
                }
            }
            requestFrame()
        }
    }

    init { loadLevel(1) }

    private fun requestFrame() {
        if (foreground && !callbackPending) { callbackPending = true; choreographer.postFrameCallback(frameCallback) }
    }
    fun setForegroundActive(active: Boolean) {
        foreground = active
        lastFrame = 0L
        if (active) requestFrame() else { choreographer.removeFrameCallback(frameCallback); callbackPending = false }
    }
    private fun publish() {
        val w = physicsWorld
        val terminal = w.state == SimulationState.SUCCESS || w.state == SimulationState.FAILED
        _uiState.update { it.copy(
            currentLevel = w.currentLevel,
            simulationState = w.state, chosenPin = w.chosenPinId, failureReason = w.failureReason,
            screenShake = w.screenShake, moveCountLeft = if(w.state==SimulationState.READY) 1 else 0,
            frameTick = it.frameTick+1,
            showResultOverlay = terminal && w.simulationTime-w.terminalTime >= 0.75f,
            creatures = w.creatures.map { c -> c.copy() },
            highestUnlockedLevel = progressRepo.getHighestUnlockedLevel(),
            completedLevels = (1..LevelCatalog.ALL_LEVELS.size).filter(progressRepo::isLevelCompleted).toSet()
        ) }
    }
    fun loadLevel(levelNumber: Int) {
        if (levelNumber !in 1..LevelCatalog.ALL_LEVELS.size || !progressRepo.isLevelUnlocked(levelNumber)) return
        HomeNestRenderer.reset()
        physicsWorld.loadLevel(LevelCatalog.getLevel(levelNumber))
        progressRepo.recordAttempt(levelNumber)
        lastFrame = 0L
        _uiState.update { it.copy(currentLevelNumber=levelNumber,showLevelSelectSheet=false,showResultOverlay=false) }
        publish()
    }
    fun nextLevel() {
        if (_uiState.value.currentLevelNumber < LevelCatalog.ALL_LEVELS.size) loadLevel(_uiState.value.currentLevelNumber+1)
        else openLevelSelect()
    }
    fun previousLevel() = loadLevel((_uiState.value.currentLevelNumber-1).coerceAtLeast(1))
    fun onCanvasTap(worldX: Float, worldY: Float) {
        if (physicsWorld.state != SimulationState.READY || !worldX.isFinite() || !worldY.isFinite()) return
        val point = Vector2D(worldX,worldY)
        val nearest = physicsWorld.pins.filter { !it.isRemoved }.map { pin ->
            val segment = pin.end-pin.start
            val t = if(segment.lengthSquared()>0f) ((point-pin.start).dot(segment)/segment.lengthSquared()).coerceIn(0f,1f) else 0f
            pin to minOf(point.distanceTo(pin.handlePosition),point.distanceTo(pin.start+segment*t))
        }.filter { it.second <= 70f }.minByOrNull { it.second }
        nearest?.let { pullPin(it.first.id) }
    }
    fun pullPin(pinId: PinId) { if (physicsWorld.pullPin(pinId)) { lastFrame=0L; publish() } }
    fun resetGame() {
        HomeNestRenderer.reset(); physicsWorld.reset(); lastFrame=0L
        progressRepo.recordRetry(_uiState.value.currentLevelNumber)
        publish()
    }
    fun openLevelSelect() { lastFrame=0L; _uiState.update { it.copy(showLevelSelectSheet=true) } }
    fun closeLevelSelect() { lastFrame=0L; _uiState.update { it.copy(showLevelSelectSheet=false) } }
    fun toggleDebugBounds() { _uiState.update { it.copy(showDebugBounds=!it.showDebugBounds) } }
    override fun onCleared() { setForegroundActive(false); super.onCleared() }
}
