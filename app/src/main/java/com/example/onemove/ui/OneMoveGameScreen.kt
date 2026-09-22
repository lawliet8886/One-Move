package com.example.onemove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.ToyBoxRenderer

@Composable
fun OneMoveGameScreen(
    viewModel: OneMoveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    OneMoveGameContent(
        uiState = uiState,
        world = viewModel.physicsWorld,
        onPinTapped = { pinId -> viewModel.pullPin(pinId) },
        onReset = { viewModel.resetGame() },
        onNextLevel = { viewModel.nextLevel() },
        onPrevLevel = { viewModel.previousLevel() },
        onOpenLevelSelect = { viewModel.openLevelSelect() },
        onCloseLevelSelect = { viewModel.closeLevelSelect() },
        onSelectLevel = { lvl -> viewModel.loadLevel(lvl) },
        onCanvasTapCoordinates = { x, y -> viewModel.onCanvasTap(x, y) },
        animatePill = true,
        enableGestures = true
    )
}

@Composable
fun OneMoveGameContent(
    uiState: GameUiState,
    world: PhysicsWorld,
    onPinTapped: (PinId) -> Unit = {},
    onReset: () -> Unit = {},
    onNextLevel: () -> Unit = {},
    onPrevLevel: () -> Unit = {},
    onOpenLevelSelect: () -> Unit = {},
    onCloseLevelSelect: () -> Unit = {},
    onSelectLevel: (Int) -> Unit = {},
    onCanvasTapCoordinates: (Float, Float) -> Unit = { _, _ -> },
    animatePill: Boolean = true,
    enableGestures: Boolean = true
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0F1D)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .widthIn(max = 600.dp)
            ) {
                // Top Tactical HUD
                TopHudBar(
                    uiState = uiState,
                    onOpenLevelSelect = onOpenLevelSelect,
                    onReset = onReset
                )

                // Interactive Kinetic Game Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.aspectRatio(LevelDefinition.WORLD_WIDTH / LevelDefinition.WORLD_HEIGHT)
                    ) {
                        val canvasWidth = constraints.maxWidth.toFloat()
                        val scaleFactor = canvasWidth / LevelDefinition.WORLD_WIDTH

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("game_board_canvas")
                                .then(
                                    if (enableGestures) {
                                        Modifier.pointerInput(uiState.simulationState) {
                                            detectTapGestures { tapOffset ->
                                                val worldX = tapOffset.x / scaleFactor
                                                val worldY = tapOffset.y / scaleFactor
                                                onCanvasTapCoordinates(worldX, worldY)
                                            }
                                        }
                                    } else Modifier
                                )
                        ) {
                            scale(scaleFactor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                                ToyBoxRenderer.renderToyBox(this, world)
                            }
                        }
                    }
                }
            }

            // Victory / Defeat Overlay
            AnimatedVisibility(
                visible = uiState.showResultOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                ResultCardOverlay(
                    state = uiState.simulationState,
                    failureReason = uiState.failureReason,
                    onReset = onReset,
                    onNextLevel = onNextLevel
                )
            }

            // Level Select Drawer / Sheet
            AnimatedVisibility(
                visible = uiState.showLevelSelectSheet,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LevelSelectSheet(
                    currentLevel = uiState.currentLevelNumber,
                    highestUnlocked = uiState.highestUnlockedLevel,
                    completedLevels = uiState.completedLevels,
                    onSelectLevel = { lvl ->
                        onSelectLevel(lvl)
                        onCloseLevelSelect()
                    },
                    onClose = onCloseLevelSelect
                )
            }
        }
    }
}

@Composable
private fun TopHudBar(
    uiState: GameUiState,
    onOpenLevelSelect: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Level number and title
        Column {
            Text(
                text = "LEVEL ${String.format("%02d", uiState.currentLevelNumber)}",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.currentLevel.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Tactical 1-Move Badge
        Surface(
            color = if (uiState.moveCountLeft > 0) Color(0xFF0F766E) else Color(0xFF334155),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .border(
                    width = 1.5.dp,
                    color = if (uiState.moveCountLeft > 0) Color(0xFF14B8A6) else Color(0xFF475569),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("move_counter_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (uiState.moveCountLeft > 0) Color(0xFF2DD4BF) else Color(0xFF94A3B8), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.moveCountLeft > 0) "1 MOVE LEFT" else "0 MOVES",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Actions: Level Select and Reset
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenLevelSelect,
                modifier = Modifier.testTag("level_select_button")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Level Select",
                    tint = Color(0xFFE2E8F0)
                )
            }
            IconButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Instant Reset",
                    tint = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
private fun ResultCardOverlay(
    state: SimulationState,
    failureReason: String,
    onReset: () -> Unit,
    onNextLevel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(24.dp)
                .border(2.dp, if (state == SimulationState.SUCCESS) Color(0xFF34D399) else Color(0xFFEF4444), RoundedCornerShape(20.dp))
                .widthIn(max = 380.dp)
                .testTag(if (state == SimulationState.SUCCESS) "success_overlay" else "failure_overlay")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state == SimulationState.SUCCESS) {
                    Text(
                        text = "SANCTUARY REACHED!",
                        color = Color(0xFF34D399),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All mascots are safely resting in the warm home nest.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("next_level_button")
                    ) {
                        Text("NEXT LEVEL", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else {
                    Text(
                        text = "TRAPPED!",
                        color = Color(0xFFEF4444),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (failureReason.isNotEmpty()) failureReason else "Mascots were blocked or fell into danger.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("retry_button")
                    ) {
                        Text("TRY AGAIN", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelSelectSheet(
    currentLevel: Int,
    highestUnlocked: Int,
    completedLevels: Set<Int>,
    onSelectLevel: (Int) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0A0F1D))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(20.dp)
                .clickable(enabled = false) {}
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LEVEL CARTRIDGES",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(LevelCatalog.ALL_LEVELS) { level ->
                        val isUnlocked = level.number <= highestUnlocked
                        val isCompleted = completedLevels.contains(level.number)
                        val isCurrent = level.number == currentLevel

                        val bgColor = when {
                            isCurrent -> Color(0xFF0284C7)
                            isCompleted -> Color(0xFF065F46)
                            isUnlocked -> Color(0xFF334155)
                            else -> Color(0xFF0F172A)
                        }

                        Box(
                            modifier = Modifier
                                .height(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFF475569),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isUnlocked) {
                                    onSelectLevel(level.number)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%02d", level.number),
                                    color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCompleted) "CLEAR" else if (isUnlocked) "READY" else "LOCKED",
                                    color = if (isCompleted) Color(0xFF34D399) else if (isUnlocked) Color(0xFF94A3B8) else Color(0xFF475569),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
