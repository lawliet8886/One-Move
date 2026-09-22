package com.example

import android.content.Context
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

    // Cannot pull another pin once running
    val secondPull = world.pullPin(PinId.PIN_B)
    assertEquals(false, secondPull)

    // Reset restores ready state instantly
    world.reset()
    assertEquals(SimulationState.READY, world.state)
    assertEquals(null, world.chosenPinId)
  }
}
