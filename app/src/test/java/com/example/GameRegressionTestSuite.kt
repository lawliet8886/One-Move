package com.example

import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Explicit runnable regression suite; historical artwork generators are not gameplay tests. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ExampleUnitTest::class,
    ExampleRobolectricTest::class,
    PhysicsIntegrityTest::class,
    RenderingIntegrityTest::class,
    VerticalSliceOutcomeMatrixTest::class
)
class GameRegressionTestSuite
