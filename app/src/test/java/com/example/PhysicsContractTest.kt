package com.example

import org.junit.Test
import java.io.File

class PhysicsContractTest {
    @Test fun physicsMustBeCausalDeterministicAndCollisionSafe() {
        val report = PhysicsContractCases.run()
        val directory = File("build/reports/physics-contract").apply { mkdirs() }
        File(directory, "campaign.csv").writeText(report.rows.joinToString("\n") + "\n")
        File(directory, "summary.txt").writeText("PASS: ${report.simulations} simulations; ${report.checks} checks; longest winning route ${report.maxWinSeconds}s. JVM physics contracts; not Android gameplay.\n")
    }

    @Test fun puzzleChoicesMustHaveIndependentPhysicalCauses() {
        LevelDesignContractCases.run()
    }

    @Test fun counterweightMustBeNecessaryAndStable() {
        CounterweightContractCases.run()
    }

    @Test fun weightSwitchAndReturnBridgesMustBeCausalAndStable() {
        WeightSwitchContractCases.run()
    }
    @Test fun flyingKeyMustNeedSpringCatcherSwitchBridgeAndFloor() {
        FlyingKeyContractCases.run()
    }
    @Test fun returnFlightMustNeedTheSpringBridgeGuardAndFloor() {
        ReturnFlightContractCases.run()
    }
}
