package com.example

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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** CI configures a disposable 720x1280, 320dpi screen at 1.3x font scale. */
@RunWith(AndroidJUnit4::class)
class OneMoveCompactDeviceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(instrumentation)
    private val output=File(instrumentation.targetContext.getExternalFilesDir(null),"qa").apply{mkdirs()}

    @Test fun controlsRemainVisibleOnSmallScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals("Compact width was not configured",720,device.displayWidth)
            assertEquals("Compact height was not configured",1280,device.displayHeight)
            assertTrue("Large font was not applied",instrumentation.targetContext.resources.configuration.fontScale>=1.29f)
            node("game_board_canvas")
            capture("compact_00_ready")
            val density=instrumentation.targetContext.resources.displayMetrics.density
            for(tag in listOf("reset_button","level_select_button")) {
                val b=node(tag).visibleBounds
                assertTrue("$tag target narrower than 48dp",b.width()>=48f*density-1f)
                assertTrue("$tag target shorter than 48dp",b.height()>=48f*density-1f)
            }
            node("reset_button").click()
            tap(PinId.PIN_B)
            node("failure_overlay",12_000L)
            capture("compact_01_failure")
            node("retry_button").click()
            assertTrue(device.wait(Until.hasObject(By.text("1 MOVE LEFT")),5_000L))
            assertFalse(device.hasObject(By.res("failure_overlay")))
            tap(PinId.PIN_A)
            node("success_overlay",12_000L)
            capture("compact_02_success")
            node("next_level_button").click()
            node("game_board_canvas")
            node("level_select_button").click()
            node("level_select_sheet")
            capture("compact_03_level_select")
            node("level_1").click()
            assertTrue(device.wait(Until.hasObject(By.text(LevelCatalog.getLevel(1).name)),5_000L))
            assertFalse(device.hasObject(By.res("level_select_sheet")))
            capture("compact_04_return")
            File(output,"compact-configuration.txt").writeText("720x1280; density=$density; fontScale=${instrumentation.targetContext.resources.configuration.fontScale}. Actual Android UI interactions.\n")
        }
    }
    private fun tap(id:PinId) {
        val pin=LevelCatalog.getLevel(1).pins.first{it.id==id}
        val b=node("game_board_canvas").visibleBounds
        val x=b.left+pin.handlePosition.x/LevelDefinition.WORLD_WIDTH*b.width()
        val y=b.top+pin.handlePosition.y/LevelDefinition.WORLD_HEIGHT*b.height()
        assertTrue(device.click(x.toInt(),y.toInt()))
    }
    private fun node(tag:String,timeout:Long=5_000L):UiObject2 {
        val found=device.wait(Until.findObject(By.res(tag)),timeout)
        if(found==null){capture("compact_missing_$tag");fail("Missing compact-screen element $tag")}
        val bounds=found!!.visibleBounds
        assertTrue("Offscreen or empty element $tag",bounds.width()>0 && bounds.height()>0 &&
            bounds.left>=0 && bounds.top>=0 && bounds.right<=device.displayWidth && bounds.bottom<=device.displayHeight)
        return found
    }
    private fun capture(name:String){assertTrue(device.takeScreenshot(File(output,"$name.png")))}
}
