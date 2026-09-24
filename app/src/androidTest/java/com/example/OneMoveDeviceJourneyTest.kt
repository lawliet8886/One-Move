package com.example

import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import android.util.Log
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
import com.example.onemove.ui.OneMoveViewModel
import com.example.onemove.physics.SimulationState
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real clock, real Activity and injected screen-coordinate touches. No forced outcomes. */
@RunWith(AndroidJUnit4::class)
class OneMoveDeviceJourneyTest {
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var model: OneMoveViewModel
    private lateinit var activity: MainActivity
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val output: File get() = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "qa").apply { mkdirs() }

    @Before fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        node("game_board_canvas")
        scenario.onActivity { activity = it; model = ViewModelProvider(it)[OneMoveViewModel::class.java] }
    }
    @After fun close() {
        // Capture metrics while the Activity/process still exists, not after teardown.
        val stamp=SystemClock.uptimeMillis()
        runCatching {
            File(output,"gfxinfo_$stamp.txt").writeText(device.executeShellCommand("dumpsys gfxinfo com.example.onemove framestats"))
            File(output,"meminfo_$stamp.txt").writeText(device.executeShellCommand("dumpsys meminfo com.example.onemove"))
        }.onFailure { File(output,"metrics_error_$stamp.txt").writeText(it.toString()) }
        scenario.close()
    }

    @Test fun all12WinningPinsCompleteCampaignThroughRealUi() {
        for (number in 1..12) {
            val level = LevelCatalog.getLevel(number)
            assertTrue("Missing level title $number", device.wait(Until.hasObject(By.text(level.name)), 5_000L))
            assertFalse("Old failure card flashed over the new READY level",device.hasObject(By.res("failure_overlay")))
            capture("level_${number.toString().padStart(2,'0')}_00_ready")
            tapPin(number,level.solutionPinId)
            SystemClock.sleep(250)
            capture("level_${number.toString().padStart(2,'0')}_01_motion")
            SystemClock.sleep(500)
            capture("level_${number.toString().padStart(2,'0')}_02_motion")
            if (number == 9) {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val deadline = SystemClock.uptimeMillis() + 6_000L
                var latched = false
                while (!latched && SystemClock.uptimeMillis() < deadline) {
                    instrumentation.runOnMainSync {
                        latched = model.physicsWorld.pressurePlates.single().isLatched
                    }
                    if (!latched) SystemClock.sleep(20)
                }
                assertTrue("The weight never pressed the real switch", latched)
                capture("level_09_02_switch_latched")
            }
            node("success_overlay",12_000L)
            capture("level_${number.toString().padStart(2,'0')}_03_success")
            File(output,"journey.tsv").appendText("$number\t${level.solutionPinId}\tSUCCESS\t${SystemClock.elapsedRealtime()}\n")
            if(number<12) node("next_level_button").click()
        }
        assertTrue(device.hasObject(By.text("ALL 12 LEVELS RESCUED!")))
        node("next_level_button").click()
        node("level_select_sheet")
        node("campaign_complete_banner")
        val finalCard = node("level_12").visibleBounds
        assertTrue("Level 12 is clipped in the completed campaign sheet: $finalCard",
            finalCard.width() > 0 && finalCard.height() > 0 &&
                finalCard.left >= 0 && finalCard.top >= 0 &&
                finalCard.right <= device.displayWidth && finalCard.bottom <= device.displayHeight)
        capture("campaign_complete")
    }

    @Test fun wrongPinFailsAndRetryRestoresReadyState() {
        tapPin(1,PinId.PIN_B)
        node("failure_overlay",12_000L)
        capture("wrong_pin_failure")
        node("retry_button").click()
        waitForRetryReady(1)
        node("game_board_canvas")
        assertFalse("Retry retained the old failure card",device.hasObject(By.res("failure_overlay")))
        capture("retry_ready")
    }

    @Test fun causalPrototypeWrongChoicesFailForVisiblePhysicalReasonsAndRetryCleanly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val cases = listOf(
            Triple(2, PinId.PIN_A, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(3, PinId.PIN_A, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(3, PinId.PIN_B, "PATH_BLOCKED"),
            Triple(5, PinId.PIN_A, "PATH_BLOCKED"),
            Triple(5, PinId.PIN_B, "PATH_BLOCKED"),
            Triple(6, PinId.PIN_B, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(6, PinId.PIN_C, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(8, PinId.PIN_A, "HIT_BY_HEAVY_OBJECT"),
            Triple(8, PinId.PIN_C, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(9, PinId.PIN_B, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(9, PinId.PIN_C, "PATH_BLOCKED"),
            Triple(10, PinId.PIN_A, "CREATURE_TRAPPED_IN_DANGER_BASIN"),
            Triple(10, PinId.PIN_C, "PATH_BLOCKED"),
            Triple(10, PinId.PIN_D, "CREATURE_TRAPPED_IN_DANGER_BASIN")
        )
        for ((number, pinId, expectedReason) in cases) {
            instrumentation.runOnMainSync { model.loadLevel(number) }
            val level = LevelCatalog.getLevel(number)
            assertTrue("Missing prototype level title $number", device.wait(Until.hasObject(By.text(level.name)), 5_000L))
            assertFalse("Prototype level $number opened with a stale failure card", device.hasObject(By.res("failure_overlay")))
            capture("level_${number.toString().padStart(2,'0')}_wrong_${pinId.name}_00_ready")
            tapPin(number, pinId)
            node("failure_overlay", 12_000L)
            instrumentation.runOnMainSync {
                assertEquals("Wrong choice must end in a real failed simulation", SimulationState.FAILED, model.physicsWorld.state)
                assertEquals("Unexpected physical failure reason for level $number / $pinId", expectedReason, model.physicsWorld.failureReason)
                assertEquals("A failed route must not rescue any friend", 0, model.physicsWorld.creatures.count { it.isInsideGoal })
            }
            capture("level_${number.toString().padStart(2,'0')}_wrong_${pinId.name}_01_failure")
            node("retry_button").click()
            waitForRetryReady(number)
            node("game_board_canvas")
            instrumentation.runOnMainSync {
                assertEquals(SimulationState.READY, model.physicsWorld.state)
                assertEquals(number, model.uiState.value.currentLevelNumber)
                assertTrue("Retry retained a failure reason", model.physicsWorld.failureReason.isEmpty())
            }
            assertFalse("Retry retained the prototype failure card", device.hasObject(By.res("failure_overlay")))
            capture("level_${number.toString().padStart(2,'0')}_wrong_${pinId.name}_02_retry")
        }
    }

    @Test fun backgroundPauseDoesNotAdvanceTheSimulation() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { model.loadLevel(5) }
        assertTrue(device.wait(Until.hasObject(By.text(LevelCatalog.getLevel(5).name)),5_000L))
        val pin = LevelCatalog.getLevel(5).pins.first { it.id == PinId.PIN_C }
        val bounds = node("game_board_canvas").visibleBounds
        val x = bounds.left + pin.handlePosition.x / LevelDefinition.WORLD_WIDTH * bounds.width()
        val y = bounds.top + pin.handlePosition.y / LevelDefinition.WORLD_HEIGHT * bounds.height()
        val automation = instrumentation.uiAutomation
        val downTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue("Raw touch was not injected", automation.injectInputEvent(event, true)) }
            finally { event.recycle() }
        }
        var beforeHome = 0f
        var started = false
        val deadline = SystemClock.uptimeMillis() + 2000L
        while (!started && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                assertNotEquals("The pause gesture arrived after the run ended", SimulationState.SUCCESS, model.physicsWorld.state)
                beforeHome = model.physicsWorld.simulationTime
                started = model.physicsWorld.state == SimulationState.RUNNING && beforeHome > 0f
            }
            if (!started) SystemClock.sleep(8L)
        }
        assertTrue("The real touch never started the simulation", started)
        Log.i("OneMoveQA","Lifecycle: RUNNING before HOME at $beforeHome")
        // The two-argument KeyEvent constructor had timestamp zero; Android rejected
        // it as stale. Timestamp actual HOME key events instead of dropping assertions.
        val homeDown=SystemClock.uptimeMillis()
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val event=KeyEvent(homeDown,SystemClock.uptimeMillis(),action,KeyEvent.KEYCODE_HOME,0)
            event.source=InputDevice.SOURCE_KEYBOARD
            assertTrue("HOME was not injected",automation.injectInputEvent(event,true))
        }
        assertTrue("App did not leave foreground",device.wait(Until.gone(By.res("game_board_canvas")),5_000L))
        var pausedAt=0f
        instrumentation.runOnMainSync {
            assertNotEquals(Lifecycle.State.RESUMED,activity.lifecycle.currentState)
            assertEquals("Simulation completed before lifecycle pause could be checked",SimulationState.RUNNING,model.physicsWorld.state)
            pausedAt=model.physicsWorld.simulationTime
            assertTrue("Simulation time moved backwards",pausedAt>=beforeHome)
        }
        SystemClock.sleep(800)
        instrumentation.runOnMainSync {
            assertEquals("Physics advanced in the background",pausedAt,model.physicsWorld.simulationTime,0.005f)
        }
        Log.i("OneMoveQA","Lifecycle: background clock held at $pausedAt")
        val context=instrumentation.targetContext
        val intent=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
        node("success_overlay",12_000L)
        capture("resume_success")
        Log.i("OneMoveQA","Lifecycle: resumed and visibly completed")
    }

    private fun waitForRetryReady(expectedLevel: Int, timeout: Long = 5_000L) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeout
        var ready = false
        while (!ready && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                ready = model.physicsWorld.state == SimulationState.READY &&
                    model.uiState.value.currentLevelNumber == expectedLevel &&
                    model.uiState.value.moveCountLeft == 1 &&
                    model.physicsWorld.failureReason.isEmpty()
            }
            if (!ready) SystemClock.sleep(20L)
        }
        assertTrue("Retry did not restore READY state for level $expectedLevel", ready)
        assertTrue("Retry retained the failure overlay", device.wait(Until.gone(By.res("failure_overlay")), timeout))
        val counter = device.wait(Until.findObject(By.res("move_counter_pill")), timeout)
        assertNotNull("Retry did not restore the move counter", counter)
        val labels = listOf("1 MOVE LEFT", "1 MOVE", "1 PULL")
        val visibleOneMove = counter!!.text in labels || labels.any { counter.findObject(By.text(it)) != null }
        assertTrue("Retry READY state was not reflected by the adaptive HUD; counter='${counter.text}'", visibleOneMove)
    }

    private fun tapPin(number: Int, id: PinId) {
        val pin=LevelCatalog.getLevel(number).pins.first{it.id==id}
        val bounds=node("game_board_canvas").visibleBounds
        val x=bounds.left+pin.handlePosition.x/LevelDefinition.WORLD_WIDTH*bounds.width()
        val y=bounds.top+pin.handlePosition.y/LevelDefinition.WORLD_HEIGHT*bounds.height()
        assertTrue("Tap could not be injected",device.click(x.toInt(),y.toInt()))
    }
    private fun node(tag: String, timeout: Long=5_000L): UiObject2 {
        val found=device.wait(Until.findObject(By.res(tag)),timeout)
        if(found==null) {
            capture("missing_$tag")
            device.dumpWindowHierarchy(File(output,"missing_$tag.xml"))
            fail("Missing visible Android element: $tag")
        }
        return found!!
    }
    private fun capture(name: String) {
        assertTrue("Screenshot failed: $name",device.takeScreenshot(File(output,"$name.png")))
    }
}
