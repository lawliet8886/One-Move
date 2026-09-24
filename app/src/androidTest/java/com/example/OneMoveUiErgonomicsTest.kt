package com.example

import android.os.SystemClock
import android.view.KeyEvent
import android.view.KeyCharacterMap
import android.view.InputDevice
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.OneMoveViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Scripted regression checks, not unscripted exploratory play. Uses real Android touches. */
@RunWith(AndroidJUnit4::class)
class OneMoveUiErgonomicsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var model: OneMoveViewModel
    private lateinit var activity: MainActivity
    private val output get() = File(instrumentation.targetContext.getExternalFilesDir(null), "qa").apply { mkdirs() }

    @Before fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        node("game_board_canvas")
        scenario.onActivity { activity = it; model = ViewModelProvider(it)[OneMoveViewModel::class.java] }
    }
    @After fun close() {
        runCatching { File(output, "ergonomics-gfxinfo-${SystemClock.uptimeMillis()}.txt").writeText(device.executeShellCommand("dumpsys gfxinfo com.example.onemove framestats")) }
        scenario.close()
    }

    @Test fun systemBackClosesLevelsWithoutLeavingOrResettingTheGame() {
        repeat(2) { attempt ->
            node("level_select_button").click()
            node("level_select_sheet")
            capture("back_${attempt}_sheet")
            pressSystemBack()
            assertTrue("Back left the level selector open", device.wait(Until.gone(By.res("level_select_sheet")), 5_000L))
            node("game_board_canvas")
            instrumentation.runOnMainSync {
                assertEquals("Back must close the sheet, not exit the Activity", Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertFalse(model.uiState.value.showLevelSelectSheet)
                assertEquals(SimulationState.READY, model.physicsWorld.state)
                assertEquals(1, model.uiState.value.moveCountLeft)
                assertEquals(0f, model.physicsWorld.simulationTime, 0.001f)
            }
            capture("back_${attempt}_returned")
        }
    }

    @Test fun largeFontResultsStayReachableAndRepeatedTouchesCannotSpendTwoMoves() {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        val font = instrumentation.targetContext.resources.configuration.fontScale
        assertEquals("This case must actually run at 200% font", 2f, font, 0.02f)
        assertTrue("This case must actually use a narrow screen", device.displayWidth / metrics.density <= 321f)
        File(output, "large-font-configuration.txt").writeText("${device.displayWidth}x${device.displayHeight}; density=${metrics.density}; fontScale=$font; scripted Android UI regression")
        instrumentation.runOnMainSync { model.loadLevel(2) }
        val largeTitle = node("level_title").visibleBounds
        assertTrue(
            "Large-font title still has only one-line room: $largeTitle",
            largeTitle.height() / metrics.density >= 60f
        )
        assertTarget("reset_button")
        assertTarget("level_select_button")
        assertPlayableBoard()
        capture("large_font_00_ready")
        node("level_select_button").click()
        node("level_select_sheet")
        assertTarget("close_levels_button")
        assertTarget("level_1")
        val largeCard = node("level_1").visibleBounds
        assertTrue(
            "Large-font level card cannot fit three title lines: $largeCard",
            largeCard.height() / metrics.density >= 125f
        )
        capture("large_font_01_named_levels")
        pressSystemBack()
        assertTrue(device.wait(Until.gone(By.res("level_select_sheet")), 5_000L))
        node("game_board_canvas")
        // Five rapid down/up pairs at the same visible pin. Only the first may spend a move.
        tapPin(2, PinId.PIN_A, repeats = 5)
        node("failure_overlay", 12_000L)
        instrumentation.runOnMainSync {
            assertEquals(SimulationState.FAILED, model.physicsWorld.state)
            assertEquals(0, model.uiState.value.moveCountLeft)
            assertEquals("Repeated touches removed more than one pin", 1, model.physicsWorld.pins.count { it.isRemoved })
        }
        assertTarget("retry_button")
        capture("large_font_02_failure")
        node("retry_button").click()
        node("game_board_canvas")
        instrumentation.runOnMainSync {
            assertEquals(SimulationState.READY, model.physicsWorld.state)
            assertEquals(1, model.uiState.value.moveCountLeft)
            assertTrue(model.physicsWorld.pins.none { it.isRemoved })
        }
        tapPin(2, PinId.PIN_B)
        node("success_overlay", 12_000L)
        assertTarget("next_level_button")
        capture("large_font_03_success")
        node("next_level_button").click()
        node("game_board_canvas")
        instrumentation.runOnMainSync { assertEquals(3, model.uiState.value.currentLevelNumber) }
        assertFalse(device.hasObject(By.res("failure_overlay")))
        assertTarget("reset_button")
        assertPlayableBoard()
        capture("large_font_04_next_level")
    }

    private fun pressSystemBack() {
        // UiDevice.pressBack also waits for a specific accessibility event.
        // Assert actual key injection separately from the strict visible/lifecycle outcomes.
        val down = SystemClock.uptimeMillis()
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val event = KeyEvent(down, SystemClock.uptimeMillis(), action, KeyEvent.KEYCODE_BACK,
                0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD)
            assertTrue("System Back event was not injected: $action", instrumentation.uiAutomation.injectInputEvent(event, true))
        }
    }

    private fun assertPlayableBoard() {
        val bounds = node("game_board_canvas").visibleBounds
        val density = instrumentation.targetContext.resources.displayMetrics.density
        File(output, "large-font-board-bounds.txt").appendText("$bounds; ${bounds.width()/density} x ${bounds.height()/density} dp\n")
        assertTrue("The HUD consumed the board width: $bounds", bounds.width() / density >= 250f)
        assertTrue("The HUD consumed the board height: $bounds", bounds.height() / density >= 333f)
        instrumentation.runOnMainSync {
            val bytes=com.example.onemove.ui.render.WorkshopBackdropCache.cachedPixelBytes
            File(output,"display-backdrop-memory.txt").appendText("board=$bounds cachedBytes=$bytes legacyBytes=7680000\n")
            assertTrue("No native background raster was created",bytes>0)
            assertTrue("Large-font board still allocates the full world texture",bytes<2_000_000)
        }
    }

    private fun assertTarget(tag: String) {
        val bounds = node(tag).visibleBounds
        val minimum = 48f * instrumentation.targetContext.resources.displayMetrics.density
        assertTrue("$tag is clipped or smaller than 48dp: $bounds", bounds.width() >= minimum - 1 && bounds.height() >= minimum - 1)
        assertTrue("$tag escaped the screen", bounds.left >= 0 && bounds.top >= 0 && bounds.right <= device.displayWidth && bounds.bottom <= device.displayHeight)
    }
    private fun tapPin(number: Int, id: PinId, repeats: Int = 1) {
        val pin = LevelCatalog.getLevel(number).pins.first { it.id == id }
        val bounds = node("game_board_canvas").visibleBounds
        val x = bounds.left + pin.handlePosition.x / LevelDefinition.WORLD_WIDTH * bounds.width()
        val y = bounds.top + pin.handlePosition.y / LevelDefinition.WORLD_HEIGHT * bounds.height()
        repeat(repeats) {
            val down = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
            }
        }
    }
    private fun node(tag: String, timeout: Long = 5_000L): UiObject2 {
        val found = device.wait(Until.findObject(By.res(tag)), timeout)
        if (found == null) {
            capture("ergonomics_missing_$tag")
            device.dumpWindowHierarchy(File(output, "ergonomics_missing_$tag.xml"))
            fail("Missing visible Android element: $tag")
        }
        return found!!
    }
    private fun capture(name: String) { assertTrue(device.takeScreenshot(File(output, "$name.png"))) }
}
