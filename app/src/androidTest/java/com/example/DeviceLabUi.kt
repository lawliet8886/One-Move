package com.example

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import java.io.File

/** Fresh accessibility nodes, native input and private-file recording handshake. */
object DeviceLabUi {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(instrumentation)
    val output:File get()=File(instrumentation.targetContext.filesDir,"lab").apply{mkdirs()}
    fun bounds(tag:String):Rect {
        val end=SystemClock.elapsedRealtime()+15000
        while(SystemClock.elapsedRealtime()<end) {
            try {
                device.findObject(By.res(tag))?.visibleBounds?.let { if(!it.isEmpty) return Rect(it) }
            } catch(_:StaleObjectException) { }
            SystemClock.sleep(60)
        }
        screenshot("missing-$tag.png")
        error("No stable visible bounds for $tag")
    }
    fun tap(x:Int,y:Int) {
        val downTime=SystemClock.uptimeMillis()
        for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
            val event=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,x.toFloat(),y.toFloat(),0)
            event.source=InputDevice.SOURCE_TOUCHSCREEN
            try {check(instrumentation.uiAutomation.injectInputEvent(event,true)){"Android rejected touch"}} finally {event.recycle()}
            SystemClock.sleep(20)
        }
    }
    fun click(tag:String) {val r=bounds(tag);tap(r.centerX(),r.centerY());SystemClock.sleep(100)}
    fun screenshot(name:String) {
        val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot()){"Screen capture failed"}
        File(output,name).outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        bitmap.recycle()
    }
    fun recording(command:String,level:Int) {
        require(command=="start" || command=="stop");require(level in 1..12)
        val request="$command-${level.toString().padStart(2,'0')}"
        // Avoid shell quoting/identity ambiguity: both sides address this debug app's private files.
        File(output,"record-request").writeText(request)
        val end=SystemClock.elapsedRealtime()+20000
        while(SystemClock.elapsedRealtime()<end) {
            val ack=File(output,"record-ack").let{if(it.exists()) it.readText().trim() else ""}
            if(ack==request) return
            check(!ack.startsWith("error-")){"Native recording failed: $ack"}
            SystemClock.sleep(100)
        }
        error("Host recorder did not acknowledge $request")
    }
}
