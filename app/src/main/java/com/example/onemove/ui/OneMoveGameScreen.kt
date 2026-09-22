package com.example.onemove.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.ToyBoxRenderer
import com.example.onemove.ui.render.WorkshopRenderer
import kotlinx.coroutines.isActive

@Composable
fun OneMoveGameScreen(viewModel: OneMoveViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    // A modal consumes Back before the Activity. Closing it must not reset the world.
    BackHandler(enabled = state.showLevelSelectSheet) { viewModel.closeLevelSelect() }
    // Preserve the parallel draw-phase optimization: the HUD is not a frame clock.
    val animate = !state.showLevelSelectSheet && state.needsAnimation
    LaunchedEffect(owner, viewModel, animate) {
        viewModel.resetFrameClock()
        if (!animate) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.resetFrameClock()
            while (isActive) withFrameNanos { viewModel.onFrame(it) }
        }
    }
    OneMoveGameContent(state, viewModel.physicsWorld,
        onPinTapped = viewModel::pullPin, onReset = viewModel::resetGame,
        onNextLevel = viewModel::nextLevel, onPrevLevel = viewModel::previousLevel,
        onOpenLevelSelect = viewModel::openLevelSelect, onCloseLevelSelect = viewModel::closeLevelSelect,
        onSelectLevel = viewModel::loadLevel,
        onCanvasTapCoordinates = { x, y -> viewModel.onCanvasTap(x, y) },
        onCanvasTapWithRadius = { x, y, radius -> viewModel.onCanvasTap(x, y, radius) },
        renderFrame = { viewModel.renderTick.longValue })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OneMoveGameContent(
    uiState: GameUiState, world: PhysicsWorld,
    onPinTapped: (PinId) -> Unit = {}, onReset: () -> Unit = {}, onNextLevel: () -> Unit = {},
    onPrevLevel: () -> Unit = {}, onOpenLevelSelect: () -> Unit = {}, onCloseLevelSelect: () -> Unit = {},
    onSelectLevel: (Int) -> Unit = {}, onCanvasTapCoordinates: (Float, Float) -> Unit = { _, _ -> },
    animatePill: Boolean = true, enableGestures: Boolean = true,
    onCanvasTapWithRadius: ((Float, Float, Float) -> Unit)? = null,
    renderFrame: (() -> Long)? = null
) {
    Scaffold(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        containerColor = Color(0xFF102D29)) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF173E37), Color(0xFF091E20)))).padding(padding)) {
            Column(Modifier.fillMaxSize().widthIn(max = 600.dp).align(Alignment.Center)) {
                RescueHud(uiState, onOpenLevelSelect, onReset)
                Text(WorkshopRenderer.chapter(uiState.currentLevelNumber).name,
                    color = Color(0xFFB4C8B4), fontSize = 10.sp, letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp), textAlign = TextAlign.Center)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    val boardWidth = minOf(maxWidth, maxHeight * (LevelDefinition.WORLD_WIDTH / LevelDefinition.WORLD_HEIGHT))
                    val scaleFactor = with(LocalDensity.current) { boardWidth.toPx() } / LevelDefinition.WORLD_WIDTH
                    val hitRadius = maxOf(65f, with(LocalDensity.current) { 24.dp.toPx() } / scaleFactor.coerceAtLeast(0.001f))
                    val ready = enableGestures && uiState.simulationState == SimulationState.READY && !uiState.showLevelSelectSheet
                    Canvas(Modifier.width(boardWidth).aspectRatio(LevelDefinition.WORLD_WIDTH / LevelDefinition.WORLD_HEIGHT)
                        .clip(RoundedCornerShape(20.dp)).testTag("game_board_canvas")
                        .semantics {
                            contentDescription = "One Move playfield"
                            stateDescription = "Level ${uiState.currentLevelNumber}, ${uiState.simulationState}, ${uiState.moveCountLeft} move left"
                            customActions = if (ready) world.pins.filterNot { it.isRemoved }.map { pin ->
                                CustomAccessibilityAction("Pull pin ${pin.name}") { onPinTapped(pin.id); true }
                            } else emptyList()
                        }
                        .then(if (ready) Modifier.pointerInput(scaleFactor, hitRadius, uiState.currentLevelNumber) {
                            detectTapGestures { point ->
                                val x = point.x / scaleFactor; val y = point.y / scaleFactor
                                if (onCanvasTapWithRadius != null) onCanvasTapWithRadius(x, y, hitRadius)
                                else onCanvasTapCoordinates(x, y)
                            }
                        } else Modifier)) {
                        @Suppress("UNUSED_VARIABLE") val frame = renderFrame?.invoke() ?: uiState.frameTick
                        scale(scaleFactor, pivot = Offset.Zero) { ToyBoxRenderer.renderToyBox(this, world) }
                    }
                }
                Text(text = if (uiState.simulationState == SimulationState.READY) "Read the machine. One pull. Bring everyone home."
                    else "${uiState.creatures.count { it.isInsideGoal }} / ${uiState.creatures.size} friends safe",
                    color = Color(0xFFABC3B8), fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("rescue_status"))
            }
            // Dismiss immediately; an old result must never cover the next READY phase.
            if (uiState.showResultOverlay) RescueResultPanel(uiState, onReset, onNextLevel)
            if (uiState.showLevelSelectSheet) {
                RescueLevelSelect(uiState, { onSelectLevel(it); onCloseLevelSelect() }, onCloseLevelSelect)
            }
        }
    }
}
