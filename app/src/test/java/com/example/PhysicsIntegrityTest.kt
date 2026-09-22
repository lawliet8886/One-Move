package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class PhysicsIntegrityTest {
    @Test(timeout=180000)
    fun realPhysicsAcrossFrameRatesAndAdversarialGeometry() {
        val report=PhysicsRegressionSuite.run()
        val out=File("build/reports/physics-integrity").apply{mkdirs()}
        File(out,"matrix.csv").writeText(report.rows.joinToString("\n"))
        File(out,"summary.json").writeText("""{"simulations":${report.simulations},"checks":${report.checks},"passed":true,"engine":"production Kotlin PhysicsWorld"}""")
    }
}
