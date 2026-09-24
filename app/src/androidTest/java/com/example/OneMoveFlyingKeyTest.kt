package com.example

import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.example.onemove.model.*
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.OneMoveViewModel
import com.example.onemove.ui.render.HardwareSpriteRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Screen-coordinate scripted regression, not unscripted exploratory play. */
@RunWith(AndroidJUnit4::class)
class OneMoveFlyingKeyTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(instrumentation)
    private val out get()=File(instrumentation.targetContext.getExternalFilesDir(null),"qa").apply { mkdirs() }
    @Test fun realSpringDeliversWeightAndAllOtherChoicesStayLocked() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: OneMoveViewModel
            scenario.onActivity { model=ViewModelProvider(it)[OneMoveViewModel::class.java] }
            assertTrue("The real Activity did not load the bundled hardware",HardwareSpriteRenderer.isPrepared)
            for(id in listOf(PinId.PIN_A,PinId.PIN_B,PinId.PIN_C,PinId.PIN_D)) {
                instrumentation.runOnMainSync { model.loadLevel(11) }
                assertTrue(device.wait(Until.hasObject(By.text("The Flying Key")),5_000L))
                capture("flying_key_${id}_00_ready")
                val pin=LevelCatalog.getLevel(11).pins.first { it.id==id }
                val b=node("game_board_canvas").visibleBounds
                val x=b.left+pin.handlePosition.x/LevelDefinition.WORLD_WIDTH*b.width()
                val y=b.top+pin.handlePosition.y/LevelDefinition.WORLD_HEIGHT*b.height()
                assertTrue("Real pin touch failed",device.click(x.toInt(),y.toInt()))
                if(id==PinId.PIN_A) {
                    var latched=false
                    val deadline=SystemClock.uptimeMillis()+12_000L
                    while(!latched && SystemClock.uptimeMillis()<deadline) {
                        instrumentation.runOnMainSync { latched=model.physicsWorld.pressurePlates.single().isLatched }
                        if(!latched) SystemClock.sleep(40)
                    }
                    assertTrue("The airborne weight never pressed the switch",latched)
                    capture("flying_key_01_latched")
                    node("success_overlay",18_000L)
                    instrumentation.runOnMainSync {
                        val events=model.physicsWorld.events
                        assertTrue(events.first { it.kind=="spring" }.time < events.first { it.kind=="plate_latched" }.time)
                        assertTrue(events.first { it.kind=="plate_latched" }.time < events.first { it.kind=="gate_opened" }.time)
                        assertEquals(3,model.physicsWorld.creatures.count { it.isInsideGoal })
                    }
                    capture("flying_key_02_success")
                } else {
                    node("failure_overlay",18_000L)
                    instrumentation.runOnMainSync {
                        assertEquals(SimulationState.FAILED,model.physicsWorld.state)
                        assertEquals("PATH_BLOCKED",model.physicsWorld.failureReason)
                        assertTrue(model.physicsWorld.pressurePlates.none { it.isLatched })
                        assertTrue(model.physicsWorld.creatureGates.none { it.isOpen })
                        assertEquals(0,model.physicsWorld.creatures.count { it.isInsideGoal })
                    }
                    capture("flying_key_${id}_01_failed")
                    node("retry_button").click()
                    waitForRetryReady(model, 11)
                    SystemClock.sleep(300)
                    instrumentation.runOnMainSync {
                        assertEquals(SimulationState.READY,model.physicsWorld.state)
                        assertEquals(0f,model.physicsWorld.simulationTime,0.001f)
                        assertEquals(1,model.uiState.value.moveCountLeft)
                        assertTrue(model.physicsWorld.pins.none { it.isRemoved })
                    }
                    capture("flying_key_${id}_02_retry")
                }
            }
            File(out,"hardware-gfxinfo.txt").writeText(device.executeShellCommand("dumpsys gfxinfo com.example.onemove framestats"))
            File(out,"flying-key-native.txt").writeText("PASS: real screen-coordinate touches; one spring-to-switch success and three locked-route failures with physical retry assertions. NOT free exploration.\n")
        }
    }
    private fun waitForRetryReady(model: OneMoveViewModel, expectedLevel: Int, timeout: Long = 5_000L) {
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
        assertTrue(
            "Retry READY state was not reflected by the adaptive HUD; counter='${counter!!.text}'",
            counter.text in labels || labels.any { counter.findObject(By.text(it)) != null }
        )
    }

    private fun node(tag:String,timeout:Long=5_000L):UiObject2 {
        val found=device.wait(Until.findObject(By.res(tag)),timeout)
        if(found==null) {
            capture("flying_key_missing_$tag")
            device.dumpWindowHierarchy(File(out,"flying_key_missing_$tag.xml"))
            fail("Missing actual Android element: $tag")
        }
        return found!!
    }
    private fun capture(name:String) { assertTrue(device.takeScreenshot(File(out,"$name.png"))) }
}
