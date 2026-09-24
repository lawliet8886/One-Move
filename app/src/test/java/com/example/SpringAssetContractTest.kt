package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.ui.render.SpringBumperRenderer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpringAssetContractTest {
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun springAtlasKeepsItsPhysicalDeckFixedAcrossCompression() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = context.assets.open("hardware/spring_atlas.webp").use { it.readBytes() }
        val manifest = context.assets.open("hardware/spring_manifest.json")
            .bufferedReader().use { JSONObject(it.readText()) }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(manifest.getString("atlas_sha256"), digest)
        assertTrue(manifest.getBoolean("approved_for_runtime"))
        assertTrue("Spring atlas should stay tiny", bytes.size < 50_000)

        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        assertEquals(512, bitmap.width)
        assertEquals(128, bitmap.height)
        assertTrue(bitmap.hasAlpha())
        assertEquals(262_144, bitmap.width * bitmap.height * 4)

        val pivot = manifest.getJSONArray("pivot_cell")
        assertEquals(64, pivot.getInt(0))
        assertEquals(89, pivot.getInt(1))
        assertEquals(94, manifest.getInt("physical_width_pixels"))

        val frames = manifest.getJSONArray("frames")
        assertEquals(4, frames.length())
        var priorCompression = -1.0
        var commonBottom: Int? = null
        repeat(frames.length()) { index ->
            val frame = frames.getJSONObject(index)
            val compression = frame.getDouble("compression")
            assertTrue(compression > priorCompression)
            priorCompression = compression
            val bounds = frame.getJSONArray("alpha_bounds")
            assertTrue(bounds.getInt(0) >= 2)
            assertTrue(bounds.getInt(1) >= 2)
            assertTrue(bounds.getInt(2) <= 126)
            assertTrue(bounds.getInt(3) <= 126)
            if (commonBottom == null) commonBottom = bounds.getInt(3)
            assertEquals("Spring base must not slide while compressing", commonBottom, bounds.getInt(3))
        }

        SpringBumperRenderer.prepare(context)
        assertTrue(SpringBumperRenderer.isPrepared)
        assertEquals(262_144, SpringBumperRenderer.DECODED_BYTES)
    }
}
