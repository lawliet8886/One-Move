package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-device journeys. These tests run the actual MainActivity on an Android emulator,
 * tap the rendered Canvas at the pin handle coordinates, and wait for the same result
 * overlays a player sees.
 */
@RunWith(AndroidJUnit4::class)
class OneMoveDeviceJourneyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun wrongPinFailsAndRetryRestoresReadyState() {
        composeRule.onNodeWithTag("game_board_canvas").assertIsDisplayed()
        composeRule.onNodeWithText("First Drop").assertIsDisplayed()

        tapPin(levelNumber = 1, pinId = PinId.PIN_B)
        waitForTag("failure_overlay", 5_000L)

        composeRule.onNodeWithTag("retry_button").assertIsDisplayed().performClick()
        waitForText("1 MOVE LEFT", 3_000L)
        composeRule.onNodeWithTag("game_board_canvas").assertIsDisplayed()
    }

    @Test
    fun all12WinningPinsCompleteCampaignThroughRealUi() {
        composeRule.onNodeWithTag("game_board_canvas").assertIsDisplayed()

        for (levelNumber in 1..12) {
            val level = LevelCatalog.getLevel(levelNumber)
            waitForText(level.name, 3_000L)

            tapPin(levelNumber, level.solutionPinId)
            waitForTag("success_overlay", 6_000L)

            if (levelNumber < 12) {
                composeRule.onNodeWithTag("next_level_button")
                    .assertIsDisplayed()
                    .performClick()
                waitForText(LevelCatalog.getLevel(levelNumber + 1).name, 3_000L)
            }
        }
    }

    private fun tapPin(levelNumber: Int, pinId: PinId) {
        val level = LevelCatalog.getLevel(levelNumber)
        val pin = level.pins.first { it.id == pinId }
        val board = composeRule.onNodeWithTag("game_board_canvas")
        val bounds = board.fetchSemanticsNode().boundsInRoot

        val localX = (pin.handlePosition.x / LevelDefinition.WORLD_WIDTH) * bounds.width
        val localY = (pin.handlePosition.y / LevelDefinition.WORLD_HEIGHT) * bounds.height

        board.performTouchInput {
            click(Offset(localX, localY))
        }
    }

    private fun waitForTag(tag: String, timeoutMillis: Long) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
