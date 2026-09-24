package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.ui.render.SpriteMascotRenderer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun sharedTrioAtlasIsApprovedBoundedAndTheOnlyPackagedMascotTexture() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mascotAssets = context.assets.list("mascots")?.toSet().orEmpty()

        assertTrue("Shared runtime atlas missing", "trio_runtime.webp" in mascotAssets)
        assertTrue("Shared runtime manifest missing", "trio_runtime.json" in mascotAssets)
        for (legacy in listOf(
            "atlas.webp", "manifest.json",
            "pip_animated.webp", "pip_animated.json",
            "mochi_animated.webp", "mochi_animated.json",
            "blobbo_animated.webp", "blobbo_animated.json"
        )) {
            assertFalse("$legacy must not be packaged in the APK", legacy in mascotAssets)
        }

        val bytes = context.assets.open("mascots/trio_runtime.webp").use { it.readBytes() }
        val manifest = context.assets.open("mascots/trio_runtime.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        assertEquals(manifest.getString("atlas_sha256"), sha256(bytes))
        assertTrue(manifest.getBoolean("approved_for_runtime"))

        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        assertEquals(96, manifest.getInt("cell_size"))
        assertEquals(5, manifest.getInt("columns"))
        assertEquals(12, manifest.getInt("rows"))
        assertEquals(480, bitmap.width)
        assertEquals(1152, bitmap.height)
        assertTrue(bitmap.hasAlpha())
        assertEquals(2_211_840, bitmap.allocationByteCount)
        assertTrue("Shared trio atlas unexpectedly large", bytes.size < 320_000)
        val downsample = manifest.getJSONObject("runtime_downsample")
        assertEquals(128, downsample.getInt("source_cell_size"))
        assertEquals(96, downsample.getInt("runtime_cell_size"))
        assertEquals("Lanczos", downsample.getString("filter"))

        val characters = manifest.getJSONObject("characters")
        val expected = listOf("PIP" to 0, "MOCHI" to 4, "BLOBBO" to 8)
        for ((id, baseRow) in expected) {
            val spec = characters.getJSONObject(id)
            assertEquals(baseRow, spec.getInt("base_row"))
            assertTrue(spec.getBoolean("approved_for_runtime"))
            assertTrue(spec.getDouble("framing") in 3.0..4.0)
            assertTrue(spec.getString("source_atlas_sha256").matches(Regex("[0-9a-f]{64}")))

            val clips = spec.getJSONObject("clips")
            for (name in listOf("idle", "jump", "fall", "happy")) {
                val frames = clips.getJSONArray(name)
                assertEquals("$id/$name must keep five reviewed frames", 5, frames.length())
                repeat(frames.length()) { frameIndex ->
                    val frame = frames.getJSONObject(frameIndex)
                    val rect = frame.getJSONArray("rect")
                    assertEquals(96, rect.getInt(2))
                    assertEquals(96, rect.getInt(3))
                    val bounds = frame.getJSONArray("alpha_bounds")
                    assertTrue(bounds.getInt(0) >= 1)
                    assertTrue(bounds.getInt(1) >= 1)
                    assertTrue(bounds.getInt(2) <= 95)
                    assertTrue(bounds.getInt(3) <= 95)
                }
            }
        }

        SpriteMascotRenderer.prepare(context)
        assertEquals(
            "96px runtime atlas must reduce the reviewed RGBA memory budget",
            2_211_840,
            SpriteMascotRenderer.residentDecodedBytesForTest()
        )
        assertEquals("All three mascots must share one runtime texture", 1, SpriteMascotRenderer.runtimeTextureCountForTest())
    }
}
