package com.example

import android.content.Context
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
    com.example.onemove.ui.render.HardwareSpriteRenderer.prepare(context)
    assertTrue(com.example.onemove.ui.render.HardwareSpriteRenderer.isPrepared)
    assertEquals(786432,com.example.onemove.ui.render.HardwareSpriteRenderer.DECODED_BYTES)
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
