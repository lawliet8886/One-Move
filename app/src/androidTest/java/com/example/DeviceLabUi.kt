package com.example

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import java.io.File

/** Refreshes accessibility nodes after Compose recomposition; never treats a missing target as success. */
object DeviceLabUi {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(instrumentation)
    val output:File get()=File(instrumentation.targetContext.filesDir,"lab").apply{mkdirs()}
    fun bounds(tag:String):Rect {
        val end=SystemClock.elapsedRealtime()+15000
        while(SystemClock.elapsedRealtime()<end) {
            try {
                device.findObject(By.res(tag))?.visibleBounds?.let { if(!it.isEmpty) return Rect(it) }
            } catch(_:StaleObjectException) { /* The next lookup must use the new underlying view. */ }
            SystemClock.sleep(60)
        }
        screenshot("missing-$tag.png")
        error("No stable visible bounds for $tag")
    }
    fun click(tag:String) {
        val r=bounds(tag)
        check(device.click(r.centerX(),r.centerY())){"Android rejected $tag touch"}
        SystemClock.sleep(120)
    }
    fun screenshot(name:String) {
        val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot()){"Screen capture failed"}
        File(output,name).outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        bitmap.recycle()
    }
    fun recording(command:String,level:Int) {
        require(command=="start" || command=="stop")
        require(level in 1..12)
        val request="$command-${level.toString().padStart(2,'0')}"
        device.executeShellCommand("sh -c 'echo $request > /data/local/tmp/one-move-lab/request'")
        val end=SystemClock.elapsedRealtime()+15000
        while(SystemClock.elapsedRealtime()<end) {
            val ack=device.executeShellCommand("cat /data/local/tmp/one-move-lab/ack").trim()
            if(ack==request) return
            check(!ack.startsWith("error-")){"Native recording failed: $ack"}
            SystemClock.sleep(100)
        }
        error("Host recorder did not acknowledge $request")
    }
}
