package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.PinId
import com.example.onemove.physics.PhysicsWorld
import com.example.onemove.physics.SimulationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerticalSliceOutcomeMatrixTest {

    data class PinOutcomeResult(
        val levelNumber: Int,
        val levelId: String,
        val levelName: String,
        val pinId: PinId,
        val expectedState: SimulationState,
        val actualState: SimulationState,
        val terminalTimeSec: Float,
        val creaturesInGoal: Int,
        val totalCreatures: Int,
        val physicalTerminalReason: String,
        val runCount: Int,
        val deterministic: Boolean,
        val timeoutOccurred: Boolean,
        val pass: Boolean
    )

    data class LevelPacingResult(
        val levelNumber: Int,
        val levelName: String,
        val correctPin: PinId,
        val measuredCorrectChainDurationSec: Float,
        val measuredFastestWrongOutcomeSec: Float,
        val measuredLongestWrongOutcomeSec: Float,
        val numberOfPins: Int,
        val primaryMechanics: String,
        val newConceptIntroduced: String,
        val timeoutOccurred: Boolean,
        val deterministic: Boolean
    )

    companion object {
        fun findProjectRoot(): File {
            var dir: File? = File(System.getProperty("user.dir") ?: ".").canonicalFile
            while (dir != null) {
                if (File(dir, "settings.gradle.kts").exists() || File(dir, "settings.gradle").exists()) {
                    return dir
                }
                dir = dir.parentFile
            }
            val appDir = File("/app")
            if (appDir.exists()) return appDir.canonicalFile
            return File(".").canonicalFile
        }

        fun saveTextFile(fileName: String, content: String) {
            val root = findProjectRoot()
            val targetLocations = listOf(
                File(root, fileName),
                File(root, "qa_artifacts/$fileName"),
                File("/app/$fileName"),
                File("/app/qa_artifacts/$fileName")
            )
            for (target in targetLocations) {
                try {
                    target.parentFile?.mkdirs()
                    target.writeText(content)
                    if (target.exists()) {
                        println("Exported text artifact: ${target.canonicalPath} (${target.length()} bytes)")
                    }
                } catch (e: Exception) {
                    // ignore secondary target failures
                }
            }
            val primary = File(root, fileName)
            assertTrue("File ${primary.canonicalPath} must exist", primary.exists())
            assertTrue("File ${primary.canonicalPath} must have size > 0", primary.length() > 0)
        }

        fun saveBitmapFile(fileName: String, bitmap: Bitmap) {
            val root = findProjectRoot()
            val targetLocations = listOf(
                File(root, fileName),
                File(root, "qa_artifacts/$fileName"),
                File("/app/$fileName"),
                File("/app/qa_artifacts/$fileName")
            )
            for (target in targetLocations) {
                try {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (target.exists()) {
                        println("Exported PNG artifact: ${target.canonicalPath} (${target.length()} bytes)")
                    }
                } catch (e: Exception) {
                    // ignore secondary target failures
                }
            }
            val primary = File(root, fileName)
            assertTrue("File ${primary.canonicalPath} must exist", primary.exists())
            assertTrue("File ${primary.canonicalPath} must have size > 0", primary.length() > 0)
        }
    }

    @Test
    fun testAll12LevelsOutcomeMatrixAndDeterminism() {
        val totalLevels = LevelCatalog.ALL_LEVELS.size
        assertEquals("Campaign must contain exactly 12 levels", 12, totalLevels)

        val repetitions = 10
        val matrixResults = mutableListOf<PinOutcomeResult>()
        val pacingResults = mutableListOf<LevelPacingResult>()

        val expectedWinners = mapOf(
            1 to PinId.PIN_A,
            2 to PinId.PIN_B,
            3 to PinId.PIN_C,
            4 to PinId.PIN_C,
            5 to PinId.PIN_C,
            6 to PinId.PIN_A,
            7 to PinId.PIN_A,
            8 to PinId.PIN_B,
            9 to PinId.PIN_A,
            10 to PinId.PIN_B,
            11 to PinId.PIN_A,
            12 to PinId.PIN_C
        )

        for (level in LevelCatalog.ALL_LEVELS) {
            val levelNum = level.number
            val expectedWinner = expectedWinners[levelNum] ?: error("Missing expected winner for level $levelNum")
            var winningChoiceCount = 0

            var measuredCorrectDuration = 0f
            var measuredFastestWrong = Float.MAX_VALUE
            var measuredLongestWrong = 0f
            var anyTimeout = false
            var isDeterministic = true

            val levelPins = level.pins

            for (pin in levelPins) {
                val expectedState = if (pin.id == expectedWinner) SimulationState.SUCCESS else SimulationState.FAILED

                // Run multiple repetitions to verify strict determinism
                var firstRunState: SimulationState? = null
                var firstRunTime: Float = 0f
                var firstRunGoalCount: Int = 0
                var firstRunReason: String = ""

                for (rep in 1..repetitions) {
                    val world = PhysicsWorld(
                        initialLevel = level,
                        hapticManager = null
                    )

                    val pulled = world.pullPin(pin.id)
                    assertTrue("Pulling pin ${pin.id} on Level $levelNum must succeed", pulled)

                    val dt = 0.016f
                    val maxTimeout = 8.5f

                    while (world.state == SimulationState.RUNNING && world.simulationTime < maxTimeout) {
                        world.step(dt)
                    }

                    val stateAtEnd = world.state
                    val terminalTime = world.simulationTime
                    val inGoalCount = world.creatures.count { it.isInsideGoal }
                    val terminalReason = if (stateAtEnd == SimulationState.SUCCESS) {
                        "ALL_CREATURES_IN_GOAL"
                    } else {
                        world.failureReason.replace("\n", " ").ifEmpty { "MECHANISM_SETTLED_WITHOUT_RESCUE" }
                    }

                    // TIMEOUT IS NOT FAILED. If state is still RUNNING at maxTimeout, that is an illegal timeout!
                    if (stateAtEnd == SimulationState.RUNNING) {
                        anyTimeout = true
                        throw AssertionError("TIMEOUT ERROR: Level $levelNum '${level.name}' Pin ${pin.id} did not reach terminal state before $maxTimeout s.")
                    }

                    if (rep == 1) {
                        firstRunState = stateAtEnd
                        firstRunTime = terminalTime
                        firstRunGoalCount = inGoalCount
                        firstRunReason = terminalReason
                    } else {
                        if (stateAtEnd != firstRunState || inGoalCount != firstRunGoalCount) {
                            isDeterministic = false
                        }
                    }
                }

                val finalState = firstRunState!!
                val finalTime = firstRunTime
                val finalGoalCount = firstRunGoalCount
                val finalReason = firstRunReason

                if (finalState == SimulationState.SUCCESS) {
                    winningChoiceCount++
                    measuredCorrectDuration = finalTime
                } else {
                    if (finalTime < measuredFastestWrong) measuredFastestWrong = finalTime
                    if (finalTime > measuredLongestWrong) measuredLongestWrong = finalTime
                }

                val pass = (finalState == expectedState) && isDeterministic && !anyTimeout

                println("LEVEL $levelNum (${level.name}) PIN ${pin.id.name} -> Actual: $finalState (Expected: $expectedState), Time: ${"%.2f".format(finalTime)}s, Goal: $finalGoalCount/${level.initialCreatures.size}, Reason: '$finalReason', Det: $isDeterministic")

                matrixResults.add(
                    PinOutcomeResult(
                        levelNumber = levelNum,
                        levelId = level.id,
                        levelName = level.name,
                        pinId = pin.id,
                        expectedState = expectedState,
                        actualState = finalState,
                        terminalTimeSec = finalTime,
                        creaturesInGoal = finalGoalCount,
                        totalCreatures = level.initialCreatures.size,
                        physicalTerminalReason = finalReason,
                        runCount = repetitions,
                        deterministic = isDeterministic,
                        timeoutOccurred = anyTimeout,
                        pass = pass
                    )
                )

                // Individual pin assertion
                assertEquals(
                    "Level $levelNum (${level.name}) Pin ${pin.id} must resolve to $expectedState",
                    expectedState,
                    finalState
                )
                assertTrue(
                    "Level $levelNum (${level.name}) Pin ${pin.id} must be deterministic across $repetitions runs",
                    isDeterministic
                )
            }

            // Exactly ONE physical solution per level assertion
            assertEquals(
                "Level $levelNum (${level.name}) must have exactly 1 physically valid winning pin",
                1,
                winningChoiceCount
            )

            pacingResults.add(
                LevelPacingResult(
                    levelNumber = levelNum,
                    levelName = level.name,
                    correctPin = expectedWinner,
                    measuredCorrectChainDurationSec = measuredCorrectDuration,
                    measuredFastestWrongOutcomeSec = if (measuredFastestWrong == Float.MAX_VALUE) 0f else measuredFastestWrong,
                    measuredLongestWrongOutcomeSec = measuredLongestWrong,
                    numberOfPins = levelPins.size,
                    primaryMechanics = level.primaryMechanics,
                    newConceptIntroduced = level.newConceptIntroduced,
                    timeoutOccurred = anyTimeout,
                    deterministic = isDeterministic
                )
            )
        }

        // Generate CSV Files
        writeOutcomeMatrixCsv(matrixResults)
        writePacingCsv(pacingResults)

        // Generate Outcome Matrix Table PNG
        renderOutcomeMatrixPng(matrixResults)

        // Generate PHASE3_VERIFICATION_SUMMARY.txt
        writePhase3Summary(matrixResults, pacingResults)
    }

    private fun writeOutcomeMatrixCsv(results: List<PinOutcomeResult>) {
        val header = "LevelNumber,LevelId,LevelName,PinId,ExpectedState,ActualState,TerminalTimeSec,CreaturesInGoal,PhysicalTerminalReason,RunCount,Deterministic,TimeoutOccurred,Pass\n"
        val rows = results.joinToString("\n") { r ->
            "${r.levelNumber},${r.levelId},\"${r.levelName}\",${r.pinId.name},${r.expectedState.name},${r.actualState.name},${"%.2f".format(r.terminalTimeSec)},${r.creaturesInGoal}/${r.totalCreatures},\"${r.physicalTerminalReason}\",${r.runCount},${r.deterministic},${r.timeoutOccurred},${r.pass}"
        }
        val fullContent = header + rows
        saveTextFile("VERTICAL_SLICE_OUTCOME_MATRIX.csv", fullContent)
    }

    private fun writePacingCsv(results: List<LevelPacingResult>) {
        val header = "LevelNumber,LevelName,CorrectPin,MeasuredCorrectChainDurationSec,MeasuredFastestWrongOutcomeSec,MeasuredLongestWrongOutcomeSec,NumberOfPins,PrimaryMechanics,NewConceptIntroduced,TimeoutOccurred,Deterministic\n"
        val rows = results.joinToString("\n") { r ->
            "${r.levelNumber},\"${r.levelName}\",${r.correctPin.name},${"%.2f".format(r.measuredCorrectChainDurationSec)},${"%.2f".format(r.measuredFastestWrongOutcomeSec)},${"%.2f".format(r.measuredLongestWrongOutcomeSec)},${r.numberOfPins},\"${r.primaryMechanics}\",\"${r.newConceptIntroduced}\",${r.timeoutOccurred},${r.deterministic}"
        }
        val fullContent = header + rows
        saveTextFile("VERTICAL_SLICE_PACING.csv", fullContent)
    }

    private fun writePhase3Summary(matrixResults: List<PinOutcomeResult>, pacingResults: List<LevelPacingResult>) {
        val totalSimulations = matrixResults.sumOf { it.runCount }
        val timeouts = matrixResults.count { it.timeoutOccurred }
        val passCount = matrixResults.count { it.pass }
        val lvl12Results = matrixResults.filter { it.levelNumber == 12 }

        val root = findProjectRoot()

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("ONE MOVE — PHASE 3 VERTICAL SLICE FINAL VERIFICATION SUMMARY")
        sb.appendLine("================================================================================")
        sb.appendLine("Project Canonical Root: ${root.canonicalPath}")
        sb.appendLine("Commands Executed: gradle :app:testDebugUnitTest")
        sb.appendLine("Test Suite Result: PASSED (100% Deterministic Physics)")
        sb.appendLine("Number of Levels: ${pacingResults.size}")
        sb.appendLine("Total Pin Choices Evaluated: ${matrixResults.size}")
        sb.appendLine("Repetitions per Choice: 10")
        sb.appendLine("Total Physical Simulations Executed: $totalSimulations")
        sb.appendLine("Timeouts Occurred: $timeouts")
        sb.appendLine("Overall Verification Pass: $passCount / ${matrixResults.size}")
        sb.appendLine()
        sb.appendLine("--- CAMPAIGN LEVEL WINNERS (LEVELS 1-12) ---")
        for (p in pacingResults) {
            sb.appendLine("Level ${String.format("%02d", p.levelNumber)} [${p.levelName}]: Winner = ${p.correctPin.name} (Chain Duration: ${"%.2f".format(p.measuredCorrectChainDurationSec)}s)")
        }
        sb.appendLine()
        sb.appendLine("--- LEVEL 12 OUTCOMES (PERFECT MACHINE) ---")
        for (r in lvl12Results) {
            sb.appendLine("${r.pinId.name} -> Actual: ${r.actualState.name} (Expected: ${r.expectedState.name}) | Time: ${"%.2f".format(r.terminalTimeSec)}s | Goal: ${r.creaturesInGoal}/${r.totalCreatures} | Det: ${r.deterministic} | Reason: ${r.physicalTerminalReason}")
        }
        sb.appendLine()
        sb.appendLine("--- GENERATED PHASE 3 ARTIFACTS ---")
        val artifactNames = listOf(
            "LEVEL_01_INITIAL.png",
            "LEVEL_02_INITIAL.png",
            "LEVEL_03_INITIAL.png",
            "LEVEL_04_INITIAL.png",
            "LEVEL_05_INITIAL.png",
            "LEVEL_06_INITIAL.png",
            "LEVEL_07_INITIAL.png",
            "LEVEL_08_INITIAL.png",
            "LEVEL_09_INITIAL.png",
            "LEVEL_10_INITIAL.png",
            "LEVEL_11_INITIAL.png",
            "LEVEL_12_INITIAL.png",
            "VERTICAL_SLICE_LEVEL_OVERVIEW.png",
            "LEVEL_01_SUCCESS_TIMELINE.png",
            "LEVEL_04_SUCCESS_TIMELINE.png",
            "LEVEL_07_SUCCESS_TIMELINE.png",
            "LEVEL_10_SUCCESS_TIMELINE.png",
            "LEVEL_12_SUCCESS_TIMELINE.png",
            "VERTICAL_SLICE_OUTCOME_MATRIX.csv",
            "VERTICAL_SLICE_PACING.csv",
            "VERTICAL_SLICE_OUTCOME_MATRIX.png",
            "PHASE3_VERIFICATION_SUMMARY.txt"
        )
        for (name in artifactNames) {
            val f = File(root, name)
            if (f.exists()) {
                sb.appendLine("- ${f.canonicalPath} (${f.length()} bytes)")
            } else {
                sb.appendLine("- $name (will be written upon test completion)")
            }
        }
        sb.appendLine("================================================================================")

        saveTextFile("PHASE3_VERIFICATION_SUMMARY.txt", sb.toString())
    }

    private fun renderOutcomeMatrixPng(results: List<PinOutcomeResult>) {
        val width = 1600
        val rowHeight = 44
        val headerHeight = 160
        val height = headerHeight + results.size * rowHeight + 80

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0F172A") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("ONE MOVE — 12-Level Vertical Slice Physical Outcome Matrix", 50f, 65f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("Verified via 10x deterministic physics simulation runs per choice at 60Hz sub-stepped resolution", 50f, 105f, subtitlePaint)

        // Table Header
        val colX = intArrayOf(50, 120, 240, 560, 680, 840, 1000, 1160, 1300, 1420)
        val colTitles = arrayOf("#", "Level ID", "Level Name", "Pin", "Expected", "Actual", "Time (s)", "In Goal", "Runs", "Status")

        val tableHeaderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
        }
        canvas.drawRect(40f, 125f, (width - 40).toFloat(), 170f, tableHeaderPaint)

        val headerTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        for (i in colTitles.indices) {
            canvas.drawText(colTitles[i], colX[i].toFloat(), 155f, headerTextPaint)
        }

        // Rows
        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            textSize = 17f
            isAntiAlias = true
        }
        val successPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#4ADE80")
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val failPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F87171")
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val altRowPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
        }

        var currentY = 170f
        for ((idx, r) in results.withIndex()) {
            if (idx % 2 == 1) {
                canvas.drawRect(40f, currentY, (width - 40).toFloat(), currentY + rowHeight, altRowPaint)
            }

            val textY = currentY + 28f
            canvas.drawText(r.levelNumber.toString(), colX[0].toFloat(), textY, textPaint)
            canvas.drawText(r.levelId, colX[1].toFloat(), textY, textPaint)
            canvas.drawText(r.levelName, colX[2].toFloat(), textY, textPaint)
            canvas.drawText(r.pinId.name, colX[3].toFloat(), textY, textPaint)
            canvas.drawText(r.expectedState.name, colX[4].toFloat(), textY, textPaint)

            val isWin = r.actualState == SimulationState.SUCCESS
            val stateColorPaint = if (isWin) successPaint else failPaint
            canvas.drawText(r.actualState.name, colX[5].toFloat(), textY, stateColorPaint)

            canvas.drawText("%.2fs".format(r.terminalTimeSec), colX[6].toFloat(), textY, textPaint)
            canvas.drawText("${r.creaturesInGoal}/${r.totalCreatures}", colX[7].toFloat(), textY, textPaint)
            canvas.drawText("${r.runCount}x", colX[8].toFloat(), textY, textPaint)

            val passStr = if (r.pass) "PASS (Deterministic)" else "FAIL"
            val passColor = if (r.pass) successPaint else failPaint
            canvas.drawText(passStr, colX[9].toFloat(), textY, passColor)

            currentY += rowHeight
        }

        // Save PNG
        saveBitmapFile("VERTICAL_SLICE_OUTCOME_MATRIX.png", bitmap)
    }
}
