package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.ui.render.RescueSanctuaryRenderer
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
class SanctuaryAssetContractTest {
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun rescueHaloIsVerifiedCompactAndPresentationOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = context.assets.open("sanctuary/rescue_halo.webp").use { it.readBytes() }
        val manifest = context.assets.open("sanctuary/rescue_halo.json")
            .bufferedReader().use { JSONObject(it.readText()) }

        assertEquals(manifest.getString("atlas_sha256"), sha256(bytes))
        assertTrue(manifest.getBoolean("approved_for_runtime"))
        assertTrue("Sanctuary runtime sprite should stay tiny", bytes.size < 80_000)

        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
        assertTrue(bitmap.hasAlpha())
        assertEquals(262_144, bitmap.allocationByteCount)

        val bounds = manifest.getJSONArray("alpha_bounds")
        assertTrue(bounds.getInt(0) >= 2)
        assertTrue(bounds.getInt(1) >= 2)
        assertTrue(bounds.getInt(2) <= 254)
        assertTrue(bounds.getInt(3) <= 254)
        assertEquals(2.18, manifest.getDouble("framing"), 0.001)
        assertEquals(55, manifest.getInt("tripo_generation_credits"))
        assertEquals(
            "c0db6f29-86eb-430a-84ec-481a1a46405e",
            manifest.getString("source_model_id")
        )

        RescueSanctuaryRenderer.prepare(context)
        assertTrue(RescueSanctuaryRenderer.isPrepared)
        assertEquals(262_144, RescueSanctuaryRenderer.DECODED_BYTES)
    }
}
