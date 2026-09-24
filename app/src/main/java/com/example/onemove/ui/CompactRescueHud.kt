package com.example.onemove.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Remove secondary chrome, never the user's font scaling or the playable area. */
@Composable
internal fun CompactRescueHud(state: GameUiState, levels: () -> Unit, reset: () -> Unit) {
    val largeFont = LocalDensity.current.fontScale >= 1.6f
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = if (largeFont) 72.dp else 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.currentLevel.name, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFFFEDCB), maxLines = if (largeFont) 2 else 1,
                overflow = if (largeFont) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("level_title").semantics {
                    contentDescription = "Level ${state.currentLevelNumber}: ${state.currentLevel.name}"
                })
            IconButton(levels, Modifier.size(48.dp).testTag("level_select_button")) {
                Icon(Icons.AutoMirrored.Filled.List, "Level Select", tint = Color(0xFFE2E8F0))
            }
            IconButton(reset, Modifier.size(48.dp).testTag("reset_button")) {
                Icon(Icons.Default.Refresh, "Instant Reset", tint = Color(0xFFFBBF24))
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(color = Color(0xFF115E59), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("move_counter_pill")) {
                Text(if (state.moveCountLeft > 0) "1 PULL" else "0 PULLS",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Text("${state.creatures.count { it.isInsideGoal }}/${state.creatures.size} safe",
                color = Color(0xFFBFE5D5), fontSize = 12.sp, maxLines = 1,
                modifier = Modifier.testTag("rescue_status").semantics {
                    contentDescription = "${state.creatures.count { it.isInsideGoal }} of ${state.creatures.size} friends safe. One pull to rescue everyone."
                })
        }
    }
}
