package com.example

import android.content.Context
import java.security.MessageDigest
import org.json.JSONObject
import android.graphics.BitmapFactory
import android.app.Application
import com.example.onemove.ui.OneMoveViewModel
import org.junit.Assert.assertFalse
import androidx.test.core.app.ApplicationProvider
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.PinId
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ONE MOVE", appName)
  }

  @Test
  fun `test initial physics state and pin pull`() {
    val world = PhysicsWorld(
        initialLevel = LevelCatalog.getLevel(1),
        hapticManager = null
    )
    assertEquals(SimulationState.READY, world.state)
    assertEquals(3, world.creatures.size)
    assertEquals(2, world.pins.size)
    val pulled = world.pullPin(PinId.PIN_A)
    assertTrue(pulled)
    assertEquals(SimulationState.RUNNING, world.state)
    assertEquals(PinId.PIN_A, world.chosenPinId)
    val secondPull = world.pullPin(PinId.PIN_B)
    assertEquals(false, secondPull)
    world.reset()
    assertEquals(SimulationState.READY, world.state)
    assertEquals(null, world.chosenPinId)
  }

  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  @Test fun bundledHardwareAtlasLoadsWithVerifiedHashAndBoundedMemory() {
    val context=ApplicationProvider.getApplicationContext<Context>()
    val bytes=context.assets.open("hardware/atlas.webp").use { it.readBytes() }
    val manifest=context.assets.open("hardware/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
    val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    assertEquals(manifest.getString("atlas_sha256"),digest)
    assertEquals(160,manifest.getInt("cell_size"))
    assertEquals(480,manifest.getInt("atlas_width"))
    assertEquals(160,manifest.getInt("atlas_height"))
    assertEquals(307200,manifest.getInt("decoded_bytes_rgba"))
    assertEquals(2.4,manifest.getDouble("framing"),0.0001)
    assertTrue(manifest.has("runtime_downsample"))
    val bitmap=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size))
    assertEquals(480,bitmap.width)
    assertEquals(160,bitmap.height)
    assertTrue(bitmap.hasAlpha())
    assertEquals(307200,bitmap.allocationByteCount)
    val sprites=manifest.getJSONArray("sprites")
    assertEquals(3,sprites.length())
    repeat(sprites.length()) { index ->
      val sprite=sprites.getJSONObject(index)
      val bounds=sprite.getJSONArray("alpha_bounds")
      assertTrue(bounds.getInt(0) >= 1)
      assertTrue(bounds.getInt(1) >= 1)
      assertTrue(bounds.getInt(2) <= 159)
      assertTrue(bounds.getInt(3) <= 159)
    }
    com.example.onemove.ui.render.HardwareSpriteRenderer.prepare(context)
    assertTrue(com.example.onemove.ui.render.HardwareSpriteRenderer.isPrepared)
    assertEquals(307200,com.example.onemove.ui.render.HardwareSpriteRenderer.residentDecodedBytesForTest())
  }

  @Test fun activeFramesInvalidateDrawingWithoutPublishingEveryHudFrame() {
    val model = OneMoveViewModel(ApplicationProvider.getApplicationContext<Application>())
    model.loadLevel(5); model.pullPin(PinId.PIN_C)
    var last = model.uiState.value
    var uiPublications = 0
    val tick = model.renderTick.longValue
    model.onFrame(1_000_000_000L)
    repeat(120) { index ->
      model.onFrame(1_000_000_000L + (index + 1) * 16_666_667L)
      if (last !== model.uiState.value) uiPublications++
      last = model.uiState.value
    }
    assertTrue("Drawing stopped during gameplay", model.renderTick.longValue > tick + 90)
    assertTrue("HUD still changes every frame: $uiPublications", uiPublications < 20)
    assertEquals(0L, model.uiState.value.frameTick)
  }

  @Test fun terminalEffectsQuiesceAndResetInvalidatesTheBoard() {
    val model = OneMoveViewModel(ApplicationProvider.getApplicationContext<Application>())
    model.pullPin(PinId.PIN_A)
    model.onFrame(1_000_000_000L)
    repeat(900) { model.onFrame(1_000_000_000L + (it + 1) * 16_666_667L) }
    assertEquals(SimulationState.SUCCESS, model.uiState.value.simulationState)
    assertTrue(model.uiState.value.showResultOverlay)
    assertEquals(3, model.uiState.value.rescuedCount)
    assertFalse("Animation did not settle", model.uiState.value.needsAnimation)
    val tick = model.renderTick.longValue
    model.onFrame(20_000_000_000L)
    assertEquals(tick, model.renderTick.longValue)
    model.resetGame()
    assertTrue(model.renderTick.longValue > tick)
    assertEquals(SimulationState.READY, model.uiState.value.simulationState)
    assertFalse(model.uiState.value.showResultOverlay)
    assertFalse(model.uiState.value.needsAnimation)
    assertEquals(0, model.uiState.value.rescuedCount)
  }
}
