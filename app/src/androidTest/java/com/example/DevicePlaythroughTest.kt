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

/** Real Android coordinate/button input following a scripted coverage plan, not live AI decisions. */
@RunWith(AndroidJUnit4::class)
class DevicePlaythroughTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(instrumentation)
    private val rows=JSONArray()
    private lateinit var scenario:ActivityScenario<MainActivity>
    private data class Snapshot(val level:Int,val state:SimulationState,val rescued:Int,val time:Float,val reason:String,val highest:Int)
    private fun snapshot():Snapshot {
        var result:Snapshot?=null
        scenario.onActivity{activity->
            val vm=ViewModelProvider(activity)[OneMoveViewModel::class.java];val s=vm.uiState.value
            result=Snapshot(s.currentLevelNumber,s.simulationState,s.creatures.count{it.isInsideGoal},vm.physicsWorld.simulationTime,s.failureReason,s.highestUnlockedLevel)
        }
        return checkNotNull(result)
    }
    private fun waitFor(label:String,timeout:Long=25000,predicate:()->Boolean) {
        val end=SystemClock.elapsedRealtime()+timeout
        while(SystemClock.elapsedRealtime()<end) {if(predicate()) return;SystemClock.sleep(60)}
        DeviceLabUi.screenshot("failure-${label.replace(' ','-')}.png")
        error("Timeout $label: ${snapshot()}")
    }
    private fun click(tag:String)=DeviceLabUi.click(tag)
    private fun boardTap(level:Int,pin:PinId) {
        val bounds=DeviceLabUi.bounds("game_board_canvas")
        assertTrue(bounds.width()>100 && bounds.height()>100)
        val handle=LevelCatalog.getLevel(level).pins.first{it.id==pin}.handlePosition
        val x=bounds.left+(handle.x/1200f*bounds.width()).toInt()
        val y=bounds.top+(handle.y/1600f*bounds.height()).toInt()
        assertTrue("Handle inside visible board",bounds.contains(x,y))
        assertTrue(device.click(x,y))
        waitFor("pin-accepted-$level"){snapshot().state!=SimulationState.READY}
    }
    private fun saveRows(){File(DeviceLabUi.output,"playthrough.json").writeText(JSONObject().put("method","Android UIAutomator coordinate taps and native screenrecord; scripted choices, not live AI control").put("rows",rows).toString(2))}
    private fun addRow(level:Int,pin:PinId,expected:SimulationState,kind:String) {
        val s=snapshot()
        rows.put(JSONObject().put("level",level).put("pin",pin.name).put("kind",kind).put("expected",expected.name).put("actual",s.state.name).put("rescued",s.rescued).put("time",s.time.toDouble()).put("reason",s.reason))
        saveRows()
        assertEquals("Actual gameplay $level/$pin",expected,s.state)
        if(expected==SimulationState.SUCCESS) assertEquals(3,s.rescued)
    }
    @Test(timeout=600000)
    fun campaignByRealTouchesThenWrongChoicesAndLifecycle() {
        Configurator.getInstance().waitForIdleTimeout=0
        instrumentation.targetContext.getSharedPreferences("one_move_game_progress",Context.MODE_PRIVATE).edit().clear().commit()
        scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            waitFor("initial-screen"){snapshot().state==SimulationState.READY}
            for(level in LevelCatalog.ALL_LEVELS) {
                waitFor("level-${level.number}"){snapshot().level==level.number && snapshot().state==SimulationState.READY}
                DeviceLabUi.recording("start",level.number)
                try {
                    SystemClock.sleep(850)
                    DeviceLabUi.screenshot("level-${level.number.toString().padStart(2,'0')}-initial.png")
                    boardTap(level.number,level.solutionPinId)
                    waitFor("terminal-${level.number}"){snapshot().state in listOf(SimulationState.SUCCESS,SimulationState.FAILED)}
                    addRow(level.number,level.solutionPinId,SimulationState.SUCCESS,"campaign-board-touch")
                    val panel=DeviceLabUi.bounds("success_overlay")
                    val board=DeviceLabUi.bounds("game_board_canvas")
                    assertTrue("Result must not cover board",panel.top>=board.bottom)
                    SystemClock.sleep(1600)
                    DeviceLabUi.screenshot("level-${level.number.toString().padStart(2,'0')}-final.png")
                } finally {DeviceLabUi.recording("stop",level.number)}
                if(level.number<12) click("next_level_button")
            }
            assertEquals(12,snapshot().highest)
            scenario.recreate()
            waitFor("recreated"){snapshot().highest==12}
            for(level in LevelCatalog.ALL_LEVELS) {
                click("level_select_button");click("level_card_${level.number}")
                waitFor("selected-${level.number}"){snapshot().level==level.number && snapshot().state==SimulationState.READY}
                for(pin in level.pins.filter{it.id!=level.solutionPinId}) {
                    if(pin.id.ordinal%2==0) boardTap(level.number,pin.id) else click("pin_${pin.name}_button")
                    waitFor("wrong-${level.number}-${pin.name}"){snapshot().state in listOf(SimulationState.SUCCESS,SimulationState.FAILED)}
                    addRow(level.number,pin.id,SimulationState.FAILED,"wrong-choice")
                    click("reset_button")
                    waitFor("reset"){snapshot().state==SimulationState.READY && snapshot().rescued==0}
                }
            }
            click("pin_C_button");SystemClock.sleep(200)
            scenario.moveToState(Lifecycle.State.CREATED)
            val paused=snapshot().time;SystemClock.sleep(600)
            assertEquals("Background pause",paused,snapshot().time,.001f)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitFor("resume"){snapshot().time>paused}
            click("reset_button");click("level_select_button")
            DeviceLabUi.screenshot("campaign-complete-level-picker.png")
            device.pressBack()
            waitFor("picker-dismissed"){device.findObject(By.res("close_level_picker"))==null}
            File(DeviceLabUi.output,"device-checks.json").writeText("""{"campaign_levels":12,"all_pin_choices":${rows.length()},"background_pause":true,"progress_persistence":true,"result_does_not_cover_board":true,"passed":true}""")
        } finally {
            saveRows()
            device.dumpWindowHierarchy(File(DeviceLabUi.output,"last-window.xml"))
            scenario.close()
        }
    }
}
