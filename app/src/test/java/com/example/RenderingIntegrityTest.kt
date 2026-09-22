package com.example

import androidx.compose.ui.geometry.Offset
import com.example.onemove.model.*
import com.example.onemove.physics.SimulationState
import com.example.onemove.ui.render.HomeNestRenderer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RenderingIntegrityTest {
    @Test fun presentationNeverTeleportsAnUndockedCreature() {
        val level=LevelCatalog.createLevel01()
        for(time in listOf(0f,.3f,.9f,2f,9f)) for(state in SimulationState.entries) for(c in level.initialCreatures) {
            assertEquals(Offset(c.position.x,c.position.y),HomeNestRenderer.getCreatureRenderPosition(c,level.goalZone,level.initialCreatures,time,state))
        }
    }
}
