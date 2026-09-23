package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.ui.render.SpriteMascotRenderer
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
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

    private fun pixels(bitmap: Bitmap): IntArray =
        IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun sharedTrioAtlasIsPixelIdenticalApprovedAndSingleTexture() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mascotAssets = context.assets.list("mascots")?.toSet().orEmpty()
        assertFalse("Legacy atlas must not ship once every mascot has a reviewed replacement", "atlas.webp" in mascotAssets)
        assertFalse("Legacy atlas manifest must not ship without its runtime atlas", "manifest.json" in mascotAssets)

        val trioBytes = context.assets.open("mascots/trio_runtime.webp").use { it.readBytes() }
        val trioManifest = context.assets.open("mascots/trio_runtime.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        assertEquals(trioManifest.getString("atlas_sha256"), sha256(trioBytes))
        assertTrue(trioManifest.getBoolean("approved_for_runtime"))

        val trio = checkNotNull(BitmapFactory.decodeByteArray(trioBytes, 0, trioBytes.size))
        assertEquals(640, trio.width)
        assertEquals(1536, trio.height)
        assertTrue(trio.hasAlpha())
        assertEquals(3 * 640 * 512 * 4, trio.allocationByteCount)

        val trioPixels = pixels(trio)
        val characters = trioManifest.getJSONObject("characters")
        val sources = listOf(
            "PIP" to "pip_animated",
            "MOCHI" to "mochi_animated",
            "BLOBBO" to "blobbo_animated"
        )
        for ((index, pair) in sources.withIndex()) {
            val (id, stem) = pair
            val bytes = context.assets.open("mascots/$stem.webp").use { it.readBytes() }
            val manifest = context.assets.open("mascots/$stem.json")
                .bufferedReader().use { JSONObject(it.readText()) }
            assertEquals("$stem hash", manifest.getString("atlas_sha256"), sha256(bytes))
            assertTrue("$stem must be explicitly approved", manifest.getBoolean("approved_for_runtime"))

            val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            assertEquals("$stem width", 640, bitmap.width)
            assertEquals("$stem height", 512, bitmap.height)
            assertTrue("$stem alpha", bitmap.hasAlpha())

            val sourcePixels = pixels(bitmap)
            val trioOffset = index * 640 * 512
            val sharedSlice = trioPixels.copyOfRange(trioOffset, trioOffset + sourcePixels.size)
            assertArrayEquals("$stem pixels differ from shared atlas", sourcePixels, sharedSlice)

            val spec = characters.getJSONObject(id)
            assertEquals(index * 4, spec.getInt("base_row"))
            assertEquals(manifest.getDouble("framing"), spec.getDouble("framing"), 0.0001)
            assertEquals(manifest.getString("atlas_sha256"), spec.getString("source_atlas_sha256"))
            assertTrue(spec.getBoolean("approved_for_runtime"))

            val clips = spec.getJSONObject("clips")
            for (name in listOf("idle", "jump", "fall", "happy")) {
                val frames = clips.getJSONArray(name)
                assertEquals("$id/$name must keep five reviewed frames", 5, frames.length())
                repeat(frames.length()) { frameIndex ->
                    val frame = frames.getJSONObject(frameIndex)
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

        SpriteMascotRenderer.prepare(context)
        assertEquals(
            "Shared atlas must preserve the same decoded RGBA budget",
            3 * 640 * 512 * 4,
            SpriteMascotRenderer.residentDecodedBytesForTest()
        )
        assertEquals("All three mascots must share one runtime texture", 1, SpriteMascotRenderer.runtimeTextureCountForTest())
    }
}
