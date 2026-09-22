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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemove.model.LevelCatalog
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.HeroCreatureRenderer

/** Presentation only: reuse the original atlas; never mutate a physical body for a portrait. */
@Composable
private fun RescuePortraits(state: GameUiState, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        state.creatures.forEach { creature ->
            Canvas(Modifier.size(size).semantics {
                contentDescription = "${creature.id}: " + if (creature.isInsideGoal) "rescued" else "waiting for rescue"
            }) {
                HeroCreatureRenderer.drawCreature(this, creature.copy(radius = this.size.minDimension * 0.34f), center)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RescueHud(state: GameUiState, levels: () -> Unit, reset: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ONE MOVE  /  ${state.currentLevelNumber.toString().padStart(2, '0')}", color = Color(0xFFE9BD79), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(state.currentLevel.name, color = Color(0xFFFFEDCB), fontSize = 23.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("level_title"))
            }
            IconButton(levels, Modifier.testTag("level_select_button")) { Icon(Icons.AutoMirrored.Filled.List, "Level Select", tint = Color(0xFFE2E8F0)) }
            IconButton(reset, Modifier.testTag("reset_button")) { Icon(Icons.Default.Refresh, "Instant Reset", tint = Color(0xFFFBBF24)) }
        }
        Spacer(Modifier.height(8.dp))
        // Reflow, rather than shrinking the user's font or squeezing portraits off screen.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = if (state.moveCountLeft > 0) Color(0xFF115E59) else Color(0xFF1E293B),
                shape = RoundedCornerShape(24.dp), modifier = Modifier.testTag("move_counter_pill")) {
                Text(if (state.moveCountLeft > 0) "1 MOVE LEFT" else "0 MOVES", color = Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Box(Modifier.heightIn(min = 36.dp), contentAlignment = Alignment.Center) { RescuePortraits(state, 24.dp) }
            Box(Modifier.heightIn(min = 36.dp), contentAlignment = Alignment.Center) {
                Text("${state.completedLevels.size}/${state.totalLevels}", color = Color(0xFFABC3B8), fontSize = 12.sp)
            }
        }
    }
}

private fun rescueReason(reason: String) = when (reason) {
    "PATH_BLOCKED" -> "That pin leaves the route closed. Follow the rails and try another support."
    "CREATURE_TRAPPED_IN_DANGER_BASIN" -> "A friend reached the danger basin. Look for a safe route to the nest."
    "CREATURE_FELL_OUTSIDE_BOARD" -> "A friend fell outside the board. Keep the path connected."
    "HIT_BY_HEAVY_OBJECT" -> "A heavy object hit a friend. Check which support holds it."
    "SIMULATION_TIMEOUT" -> "The mechanism did not settle in time. Retry or choose another pin."
    "INVALID_PHYSICS_STATE" -> "The simulation needs a reset. Your progress is safe."
    else -> "The route was not safe for everyone. Try a different pin."
}

@Composable
internal fun RescueResultPanel(state: GameUiState, reset: () -> Unit, next: () -> Unit) {
    val success = state.simulationState == SimulationState.SUCCESS
    val final = state.currentLevelNumber == state.totalLevels
    val accent = if (success) Color(0xFF79DBB6) else Color(0xFFE9BD79)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xCC060B15))
        .pointerInput(Unit) { detectTapGestures { } }, contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF193D35),
            modifier = Modifier.padding(20.dp).widthIn(max = 380.dp).fillMaxWidth()
                .heightIn(max = (maxHeight - 40.dp).coerceAtLeast(120.dp))
                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .testTag(if (success) "success_overlay" else "failure_overlay")
                .semantics { contentDescription = if (success) "Level completed" else "Retry available" }) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Only the explanation scrolls. Retry/Next remains anchored and reachable.
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (success) "${state.creatures.count { it.isInsideGoal }} / ${state.creatures.size}" else "ONE MORE TRY",
                        color = accent, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFF102E29), shape = RoundedCornerShape(22.dp)) {
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { RescuePortraits(state, 54.dp) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(if (success && final) "ALL 12 LEVELS RESCUED!" else if (success) "SANCTUARY REACHED!" else "A DIFFERENT WAY",
                        color = Color(0xFFFFEDCB), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(if (success && final) "Pip, Mochi and Blobbo are home. Revisit a favourite level."
                        else if (success) "Three friends. One clever move."
                        else rescueReason(state.failureReason), color = Color(0xFFCBD5E1), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = if (success) next else reset,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(if (success) "next_level_button" else "retry_button")) {
                    Text(if (success && final) "REVISIT LEVELS" else if (success) "NEXT LEVEL" else "TRY AGAIN",
                        color = Color(0xFF07121E), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
internal fun RescueLevelSelect(state: GameUiState, select: (Int) -> Unit, close: () -> Unit) {
    val columns = if (LocalDensity.current.fontScale >= 1.6f) 2 else 3
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xEE0A0F1D)).clickable(onClick = close), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFF1B3E37), shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth()
                .heightIn(max = (maxHeight - 32.dp).coerceAtLeast(160.dp)).clickable { }.testTag("level_select_sheet")) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR LITTLE ADVENTURE", color = Color(0xFFFFEDCB), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, modifier = Modifier.weight(1f))
                    IconButton(close, Modifier.testTag("close_levels_button")) { Icon(Icons.Default.Close, "Close levels", tint = Color.White) }
                }
                Text("${state.completedLevels.size} of ${state.totalLevels} sanctuaries reached", color = Color(0xFFABC3B8), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(GridCells.Fixed(columns), modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp).testTag("level_grid"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(LevelCatalog.ALL_LEVELS, key = { it.number }) { level ->
                        val unlocked = level.number <= state.highestUnlockedLevel
                        val complete = level.number in state.completedLevels
                        val current = level.number == state.currentLevelNumber
                        val top = if (current) Color(0xFF91663F) else if (complete) Color(0xFF24584B) else Color(0xFF34514A)
                        val bottom = if (current) Color(0xFF604126) else Color(0xFF153930)
                        val status = if (complete) "CLEAR" else if (unlocked) "READY" else "LOCKED"
                        Column(Modifier.heightIn(min = 102.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(top, bottom)))
                            .border(if (current) 2.dp else 1.dp, if (current) Color(0xFFE9BD79) else Color(0xFF54756A), RoundedCornerShape(14.dp))
                            .clickable(enabled = unlocked, onClickLabel = "Open ${level.name}") { select(level.number) }
                            .testTag("level_${level.number}").semantics { stateDescription = if (current) "Current level, $status" else status }
                            .padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(level.number.toString().padStart(2, '0'), color = if (unlocked) Color(0xFFFFEDCB) else Color(0xFFABC3B8),
                                fontSize = 21.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                            Text(level.name, color = Color(0xFFE1EADB), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(status, color = if (complete) Color(0xFF89EAC5) else Color(0xFFCBD5E1), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
