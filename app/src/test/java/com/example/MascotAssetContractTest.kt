package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.ui.render.SpriteMascotRenderer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MascotAssetContractTest {
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun animatedMascotAtlasesAreVerifiedTransparentBoundedAndPrepared() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (stem in listOf("pip_animated", "blobbo_animated")) {
            val bytes = context.assets.open("mascots/$stem.webp").use { it.readBytes() }
            val manifest = context.assets.open("mascots/$stem.json")
                .bufferedReader().use { JSONObject(it.readText()) }

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals("$stem hash", manifest.getString("atlas_sha256"), digest)

            val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            assertEquals("$stem width", 640, bitmap.width)
            assertEquals("$stem height", 512, bitmap.height)
            assertTrue("$stem must retain alpha", bitmap.hasAlpha())
            assertEquals("$stem decoded bytes", 1_310_720, bitmap.width * bitmap.height * 4)
            assertTrue("$stem compressed atlas unexpectedly large", bytes.size < 180_000)
            assertTrue("$stem must be explicitly approved", manifest.getBoolean("approved_for_runtime"))

            val clips = manifest.getJSONObject("clips")
            for (name in listOf("idle", "jump", "fall", "happy")) {
                val frames = clips.getJSONArray(name)
                assertEquals("$stem/$name must keep five reviewed frames", 5, frames.length())
                repeat(frames.length()) { index ->
                    val frame = frames.getJSONObject(index)
                    val rect = frame.getJSONArray("rect")
                    assertEquals(128, rect.getInt(2))
                    assertEquals(128, rect.getInt(3))
                    val bounds = frame.getJSONArray("alpha_bounds")
                    assertTrue(bounds.getInt(0) >= 2)
                    assertTrue(bounds.getInt(1) >= 2)
                    assertTrue(bounds.getInt(2) <= 126)
                    assertTrue(bounds.getInt(3) <= 126)
                }
            }
        }

        // Exercises static portrait fallback plus every approved animated atlas.
        SpriteMascotRenderer.prepare(context)
    }
}
