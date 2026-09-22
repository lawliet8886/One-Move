package com.example

import android.os.SystemClock
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
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val output: File get() = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "qa").apply { mkdirs() }

    @Before fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        node("game_board_canvas")
    }
    @After fun close() { scenario.close() }

    @Test fun all12WinningPinsCompleteCampaignThroughRealUi() {
        for (number in 1..12) {
            val level = LevelCatalog.getLevel(number)
            assertTrue("Missing level title $number", device.wait(Until.hasObject(By.text(level.name)), 5_000L))
            capture("level_${number.toString().padStart(2,'0')}_00_ready")
            tapPin(number,level.solutionPinId)
            SystemClock.sleep(250)
            capture("level_${number.toString().padStart(2,'0')}_01_motion")
            SystemClock.sleep(500)
            capture("level_${number.toString().padStart(2,'0')}_02_motion")
            node("success_overlay",12_000L)
            capture("level_${number.toString().padStart(2,'0')}_03_success")
            File(output,"journey.tsv").appendText("$number\t${level.solutionPinId}\tSUCCESS\t${SystemClock.elapsedRealtime()}\n")
            if(number<12) node("next_level_button").click()
        }
        assertTrue(device.hasObject(By.text("ALL 12 LEVELS RESCUED!")))
        node("next_level_button").click()
        node("level_select_sheet")
        capture("campaign_complete")
    }

    @Test fun wrongPinFailsAndRetryRestoresReadyState() {
        tapPin(1,PinId.PIN_B)
        node("failure_overlay",12_000L)
        capture("wrong_pin_failure")
        node("retry_button").click()
        assertTrue(device.wait(Until.hasObject(By.text("1 MOVE LEFT")),5_000L))
        node("game_board_canvas")
        capture("retry_ready")
    }

    @Test fun backgroundPauseDoesNotAdvanceTheSimulation() {
        tapPin(1,PinId.PIN_A)
        SystemClock.sleep(250)
        scenario.moveToState(Lifecycle.State.STARTED)
        var before=0f
        scenario.onActivity { before=ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld.simulationTime }
        SystemClock.sleep(800)
        var after=0f
        scenario.onActivity { after=ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld.simulationTime }
        assertEquals("Physics advanced while the Activity was not resumed",before,after,0.005f)
        scenario.moveToState(Lifecycle.State.RESUMED)
        node("success_overlay",12_000L)
        capture("resume_success")
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
