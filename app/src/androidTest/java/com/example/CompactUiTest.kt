package com.example

import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import com.example.onemove.model.PinId
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.OneMoveViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CompactUiTest {
    @Test(timeout=90000)
    fun compactScreenAndLargerTextKeepControlsUsableAndPickerPauses() {
        val ins=InstrumentationRegistry.getInstrumentation();val device=UiDevice.getInstance(ins)
        Configurator.getInstance().waitForIdleTimeout=0
        device.executeShellCommand("wm size 720x1280")
        device.executeShellCommand("wm density 320")
        device.executeShellCommand("settings put system font_scale 1.15")
        SystemClock.sleep(1000)
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        fun time():Float {var t=0f;scenario.onActivity{t=ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld.simulationTime};return t}
        try {
            val board=DeviceLabUi.bounds("game_board_canvas")
            assertEquals("Board aspect ratio",.75f,board.width().toFloat()/board.height(),.015f)
            for(tag in listOf("pin_A_button","pin_B_button","reset_button","level_select_button")) {
                val r=DeviceLabUi.bounds(tag)
                assertTrue("48dp target: $tag",r.width()>=96 && r.height()>=96)
                assertTrue("On screen: $tag",r.top>=0 && r.bottom<=1280 && r.left>=0 && r.right<=720)
            }
            DeviceLabUi.screenshot("compact-720x1280-font115.png")
            device.click(board.centerX(),board.centerY());SystemClock.sleep(150)
            scenario.onActivity{assertEquals(SimulationState.READY,ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld.state)}
            DeviceLabUi.click("pin_A_button");DeviceLabUi.click("pin_B_button")
            scenario.onActivity{assertEquals("Only one move",PinId.PIN_A,ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld.chosenPinId)}
            DeviceLabUi.click("level_select_button")
            val paused=time();SystemClock.sleep(500)
            assertEquals("Picker pauses gameplay",paused,time(),.001f)
            DeviceLabUi.click("close_level_picker")
            val deadline=SystemClock.elapsedRealtime()+5000
            while(time()<=paused && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
            assertTrue("Picker resumes gameplay",time()>paused)
            DeviceLabUi.click("reset_button")
            scenario.onActivity {
                val w=ViewModelProvider(it)[OneMoveViewModel::class.java].physicsWorld
                assertEquals(SimulationState.READY,w.state);assertNull(w.chosenPinId);assertEquals(0f,w.simulationTime,0f)
            }
            File(DeviceLabUi.output,"compact-ui-checks.json").writeText("""{"width":720,"height":1280,"font_scale":1.15,"touch_targets_48dp":true,"miss_does_not_spend_move":true,"double_tap_guard":true,"picker_pauses":true,"reset_during_run":true,"passed":true}""")
        } finally {
            DeviceLabUi.screenshot("compact-last-screen.png")
            scenario.close()
            device.executeShellCommand("settings put system font_scale 1.0")
            device.executeShellCommand("wm size 1080x1920")
            device.executeShellCommand("wm density 320")
            SystemClock.sleep(800)
        }
    }
}
