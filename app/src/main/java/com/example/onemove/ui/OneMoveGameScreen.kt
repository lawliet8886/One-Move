package com.example.onemove.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.ToyBoxRenderer
import kotlinx.coroutines.isActive

@Composable
fun OneMoveGameScreen(viewModel: OneMoveViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    val animate = !state.showLevelSelectSheet && (
        state.simulationState == SimulationState.RUNNING ||
        (state.simulationState != SimulationState.READY && !state.showResultOverlay) ||
        viewModel.physicsWorld.particles.isNotEmpty() || state.screenShake > 0f)
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
        onCanvasTapWithRadius = { x, y, radius -> viewModel.onCanvasTap(x, y, radius) })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OneMoveGameContent(
    uiState: GameUiState, world: PhysicsWorld,
    onPinTapped: (PinId) -> Unit = {}, onReset: () -> Unit = {}, onNextLevel: () -> Unit = {},
    onPrevLevel: () -> Unit = {}, onOpenLevelSelect: () -> Unit = {}, onCloseLevelSelect: () -> Unit = {},
    onSelectLevel: (Int) -> Unit = {}, onCanvasTapCoordinates: (Float, Float) -> Unit = { _, _ -> },
    animatePill: Boolean = true, enableGestures: Boolean = true,
    onCanvasTapWithRadius: ((Float, Float, Float) -> Unit)? = null
) {
    Scaffold(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        containerColor = Color(0xFF0A0F1D)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().widthIn(max = 600.dp).align(Alignment.Center)) {
                TopHudBar(uiState, onOpenLevelSelect, onReset)
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
                        @Suppress("UNUSED_VARIABLE") val frame = uiState.frameTick
                        scale(scaleFactor, pivot = Offset.Zero) { ToyBoxRenderer.renderToyBox(this, world) }
                    }
                }
                Text(text = if (uiState.simulationState == SimulationState.READY) "Trace the route. Pull one pin. Rescue all three."
                    else "${uiState.creatures.count { it.isInsideGoal }} / ${uiState.creatures.size} friends safe",
                    color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("rescue_status"))
            }
            // Dismiss immediately. An exiting result must never render the next level's READY state as a defeat.
            if (uiState.showResultOverlay) {
                ResultCardOverlay(uiState, onReset, onNextLevel)
            }
            if (uiState.showLevelSelectSheet) {
                LevelSelectSheet(uiState, { onSelectLevel(it); onCloseLevelSelect() }, onCloseLevelSelect)
            }
        }
    }
}

@Composable
private fun TopHudBar(state: GameUiState, levels: () -> Unit, reset: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ONE MOVE  /  ${state.currentLevelNumber.toString().padStart(2, '0')}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(state.currentLevel.name, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("level_title"))
            }
            IconButton(levels, Modifier.testTag("level_select_button")) { Icon(Icons.Default.List, "Level Select", tint = Color(0xFFE2E8F0)) }
            IconButton(reset, Modifier.testTag("reset_button")) { Icon(Icons.Default.Refresh, "Instant Reset", tint = Color(0xFFFBBF24)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = if (state.moveCountLeft > 0) Color(0xFF115E59) else Color(0xFF1E293B),
                shape = RoundedCornerShape(24.dp), modifier = Modifier.testTag("move_counter_pill")) {
                Text(if (state.moveCountLeft > 0) "1 MOVE LEFT" else "0 MOVES", color = Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            repeat(state.creatures.size) { index ->
                Box(Modifier.size(11.dp).background(if (state.creatures[index].isInsideGoal) Color(0xFF34D399)
                    else Color(0xFF334155), CircleShape))
            }
            Text("${state.completedLevels.size}/${state.totalLevels}", color = Color(0xFF94A3B8), fontSize = 12.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

private fun reasonText(reason: String) = when (reason) {
    "PATH_BLOCKED" -> "That pin leaves the route closed. Follow the rails and try another support."
    "CREATURE_TRAPPED_IN_DANGER_BASIN" -> "A friend reached the danger basin. Look for a safe route to the nest."
    "CREATURE_FELL_OUTSIDE_BOARD" -> "A friend fell outside the board. Keep the path connected."
    "HIT_BY_HEAVY_OBJECT" -> "A heavy object hit a friend. Check which support holds it."
    "SIMULATION_TIMEOUT" -> "The mechanism did not settle in time. Retry or choose another pin."
    "INVALID_PHYSICS_STATE" -> "The simulation needs a reset. Your progress is safe."
    else -> "The route was not safe for everyone. Try a different pin."
}

@Composable
private fun ResultCardOverlay(state: GameUiState, reset: () -> Unit, next: () -> Unit) {
    val success = state.simulationState == SimulationState.SUCCESS
    val final = state.currentLevelNumber == state.totalLevels
    val accent = if (success) Color(0xFF34D399) else Color(0xFFFBBF24)
    Box(Modifier.fillMaxSize().background(Color(0xCC060B15)).pointerInput(Unit) { detectTapGestures { } }, contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF101D30),
            modifier = Modifier.padding(24.dp).widthIn(max = 380.dp)
                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .testTag(if (success) "success_overlay" else "failure_overlay")
                .semantics { contentDescription = if (success) "Level completed" else "Retry available" }) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (success) "3 / 3" else "ONE MORE TRY", color = accent, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(if (success && final) "ALL 12 LEVELS RESCUED!" else if (success) "SANCTUARY REACHED!" else "A DIFFERENT WAY",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(if (success && final) "Pip, Mochi and Blobbo are home. Revisit a favourite level."
                    else if (success) "Three friends. One clever move."
                    else reasonText(state.failureReason), color = Color(0xFFCBD5E1), fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                Button(onClick = if (success) next else reset,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(if (success) "next_level_button" else "retry_button")) {
                    Text(if (success && final) "REVISIT LEVELS" else if (success) "NEXT LEVEL" else "TRY AGAIN", color = Color(0xFF07121E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LevelSelectSheet(state: GameUiState, select: (Int) -> Unit, close: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xEE0A0F1D)).clickable(onClick = close), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF152239), shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().clickable { }.testTag("level_select_sheet")) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR LITTLE ADVENTURE", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, modifier = Modifier.weight(1f))
                    IconButton(close) { Icon(Icons.Default.Close, "Close levels", tint = Color.White) }
                }
                Text("${state.completedLevels.size} of ${state.totalLevels} sanctuaries reached", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.heightIn(max = 340.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(LevelCatalog.ALL_LEVELS, key = { it.number }) { level ->
                        val unlocked = level.number <= state.highestUnlockedLevel
                        val complete = level.number in state.completedLevels
                        val current = level.number == state.currentLevelNumber
                        val bg = if (current) Color(0xFF075985) else if (complete) Color(0xFF064E3B) else Color(0xFF24344F)
                        Column(Modifier.height(76.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                            .border(if (current) 2.dp else 1.dp, if (current) Color(0xFF38BDF8) else Color(0xFF3E526F), RoundedCornerShape(14.dp))
                            .clickable(enabled = unlocked) { select(level.number) }.testTag("level_${level.number}"),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(level.number.toString().padStart(2, '0'), color = if (unlocked) Color.White else Color(0xFF94A3B8), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text(if (complete) "CLEAR" else if (unlocked) "READY" else "LOCKED", color = if (complete) Color(0xFF6EE7B7) else Color(0xFFCBD5E1), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
