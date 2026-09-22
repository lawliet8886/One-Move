package com.example

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.PinId
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.OneMoveViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Scripted end-to-end playthrough: real Android input and native screen recordings, not live AI control. */
@RunWith(AndroidJUnit4::class)
class DevicePlaythroughTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(instrumentation)
    private val output=File(instrumentation.targetContext.getExternalFilesDir(null),"lab").apply{mkdirs()}
    private val rows=JSONArray()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private data class Snapshot(val level:Int,val state:SimulationState,val rescued:Int,val time:Float,val reason:String,val highest:Int)
    private fun snapshot(): Snapshot {
        var result: Snapshot?=null
        scenario.onActivity { activity ->
            val vm=ViewModelProvider(activity)[OneMoveViewModel::class.java]
            val s=vm.uiState.value
            result=Snapshot(s.currentLevelNumber,s.simulationState,s.creatures.count{it.isInsideGoal},vm.physicsWorld.simulationTime,s.failureReason,s.highestUnlockedLevel)
        }
        return checkNotNull(result)
    }
    private fun waitFor(label:String,timeout:Long=20000,predicate:()->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeout
        while(SystemClock.elapsedRealtime()<deadline) {
            if(predicate()) return
            SystemClock.sleep(60)
        }
        device.takeScreenshot(File(output,"failure-${label.replace(' ','-')}.png"))
        error("Timed out waiting for $label: ${snapshot()}")
    }
    private fun element(tag:String):UiObject2=checkNotNull(device.wait(Until.findObject(By.res(tag)),10000)){"Missing UI $tag"}
    private fun click(tag:String){element(tag).click();SystemClock.sleep(100)}
    private fun boardTap(level:Int,pin:PinId) {
        val bounds=element("game_board_canvas").visibleBounds
        assertTrue("Board is visible",bounds.width()>100 && bounds.height()>100)
        val handle=LevelCatalog.getLevel(level).pins.first{it.id==pin}.handlePosition
        val x=bounds.left+(handle.x/1200f*bounds.width()).toInt()
        val y=bounds.top+(handle.y/1600f*bounds.height()).toInt()
        assertTrue("Handle is inside the visible board",bounds.contains(x,y))
        assertTrue("Android accepted touch",device.click(x,y))
        waitFor("pin-accepted-L$level"){snapshot().state!=SimulationState.READY}
    }
    private fun recordStart(level:Int):String {
        device.executeShellCommand("mkdir -p /sdcard/Download/one-move-lab")
        return device.executeShellCommand("sh -c 'screenrecord --size 720x1280 --bit-rate 2500000 --time-limit 40 /sdcard/Download/one-move-lab/level-${level.toString().padStart(2,'0')}.mp4 >/data/local/tmp/one-move-record.log 2>&1 & echo \$!' ").trim()
    }
    private fun recordStop(pid:String) {
        if(pid.matches(Regex("[0-9]+"))) device.executeShellCommand("kill -2 $pid")
        SystemClock.sleep(1000)
    }
    private fun saveRows(){File(output,"playthrough.json").writeText(JSONObject().put("method","Android UIAutomator coordinate taps and screenrecord; scripted choices, not live AI decisions").put("rows",rows).toString(2))}
    private fun addRow(level:Int,pin:PinId,expected:SimulationState,kind:String) {
        val s=snapshot()
        rows.put(JSONObject().put("level",level).put("pin",pin.name).put("kind",kind).put("expected",expected.name).put("actual",s.state.name).put("rescued",s.rescued).put("time",s.time.toDouble()).put("reason",s.reason))
        saveRows()
        assertEquals("Actual gameplay L$level/$pin",expected,s.state)
        if(expected==SimulationState.SUCCESS) assertEquals("All friends really arrived",3,s.rescued)
    }
    @Test(timeout=600000)
    fun campaignByRealTouchesThenWrongChoicesAndLifecycle() {
        Configurator.getInstance().waitForIdleTimeout=0
        // This test intentionally resets ONLY this game's progress on the dedicated test device.
        instrumentation.targetContext.getSharedPreferences("one_move_game_progress",Context.MODE_PRIVATE).edit().clear().commit()
        scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            waitFor("initial-screen"){snapshot().state==SimulationState.READY}
            for(level in LevelCatalog.ALL_LEVELS) {
                waitFor("level-${level.number}"){snapshot().level==level.number && snapshot().state==SimulationState.READY}
                val pid=recordStart(level.number)
                try {
                    SystemClock.sleep(850)
                    device.takeScreenshot(File(output,"level-${level.number.toString().padStart(2,'0')}-initial.png"))
                    boardTap(level.number,level.solutionPinId)
                    waitFor("terminal-${level.number}"){snapshot().state in listOf(SimulationState.SUCCESS,SimulationState.FAILED)}
                    addRow(level.number,level.solutionPinId,SimulationState.SUCCESS,"campaign-board-touch")
                    SystemClock.sleep(1600)
                    device.takeScreenshot(File(output,"level-${level.number.toString().padStart(2,'0')}-final.png"))
                    assertTrue("Result panel exists without hiding the board",element("success_overlay").visibleBounds.top>=element("game_board_canvas").visibleBounds.bottom)
                } finally {recordStop(pid)}
                if(level.number<12) click("next_level_button")
            }
            assertEquals("Campaign unlocks all phases",12,snapshot().highest)
            scenario.recreate()
            waitFor("recreated"){snapshot().highest==12}
            for(level in LevelCatalog.ALL_LEVELS) {
                click("level_select_button");click("level_card_${level.number}")
                waitFor("select-${level.number}"){snapshot().level==level.number && snapshot().state==SimulationState.READY}
                for(pin in level.pins.filter{it.id!=level.solutionPinId}) {
                    if(pin.id.ordinal%2==0) boardTap(level.number,pin.id) else click("pin_${pin.name}_button")
                    waitFor("wrong-${level.number}-${pin.name}"){snapshot().state in listOf(SimulationState.SUCCESS,SimulationState.FAILED)}
                    addRow(level.number,pin.id,SimulationState.FAILED,"wrong-choice")
                    click("reset_button")
                    waitFor("reset"){snapshot().state==SimulationState.READY && snapshot().rescued==0}
                }
            }
            // No invisible gameplay while the Activity is stopped.
            click("pin_C_button")
            SystemClock.sleep(200)
            scenario.moveToState(Lifecycle.State.CREATED)
            val paused=snapshot().time
            SystemClock.sleep(600)
            assertEquals("Simulation paused in background",paused,snapshot().time,.001f)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor("resume"){snapshot().time>paused}
            click("reset_button")
            click("level_select_button")
            device.takeScreenshot(File(output,"campaign-complete-level-picker.png"))
            device.pressBack()
            waitFor("picker-dismissed"){device.findObject(By.res("close_level_picker"))==null}
            File(output,"device-checks.json").writeText("""{"campaign_levels":12,"all_pin_choices":${rows.length()},"background_pause":true,"progress_persistence":true,"result_does_not_cover_board":true,"passed":true}""")
        } finally {
            saveRows()
            device.dumpWindowHierarchy(File(output,"last-window.xml"))
            scenario.close()
        }
    }
}
