package com.example

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureExpression
import com.example.onemove.model.CreatureId
import com.example.onemove.model.HeavyBall
import com.example.onemove.model.LevelCatalog
import com.example.onemove.model.LevelDefinition
import com.example.onemove.model.Pin
import com.example.onemove.model.PinId
import com.example.onemove.model.Platform
import com.example.onemove.model.RollingStone
import com.example.onemove.model.SeesawLever
import com.example.onemove.model.SpringBumper
import com.example.onemove.model.Vector2D
import com.example.onemove.physics.PhysicsWorld
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 4B: Official Art Direction Audition Generator.
 * Generates all 14 required concept art sheets, mockups, and comparison boards.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ConceptArtAuditionGeneratorTest {

    companion object {
        fun exportArtifact(name: String, bitmap: Bitmap) {
            val root = File(System.getProperty("user.dir") ?: ".").canonicalFile
            val targets = listOf(
                File(root, name),
                File(root, "concept_art/$name"),
                File(root, "qa_artifacts/$name"),
                File("/concept_art/$name"),
                File("/qa_artifacts/$name"),
                File("/app/concept_art/$name"),
                File("/app/qa_artifacts/$name")
            )
            for (t in targets) {
                try {
                    t.parentFile?.mkdirs()
                    FileOutputStream(t).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (_: Exception) {}
            }
            val primary = File(root, "concept_art/$name")
            println("Exported Concept Artifact: $name (${bitmap.width}x${bitmap.height})")
        }
    }

    private fun renderToBitmap(w: Int, h: Int, block: DrawScope.() -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val androidCanvas = AndroidCanvas(bmp)
        val composeCanvas = ComposeCanvas(androidCanvas)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(w.toFloat(), h.toFloat())
        ) {
            block()
        }
        return bmp
    }

    @Test
    fun generateAllPhase4BConcepts() {
        // 1. Three Gameplay Mockups (Level 4 / The Lever)
        generateGameplayMockups()

        // 2. Character Master Sheets (A, B, C)
        generateCharacterMasterSheets()

        // 3. Home Nest Exploration
        generateHomeNestConcepts()

        // 4. Danger Basin Exploration
        generateDangerBasinConcepts()

        // 5. Pin / Interaction System
        generatePinSystemConcepts()

        // 6. Mechanism Asset Sheet
        generateMechanismAssetConcepts()

        // 7. Level-Select Design Exploration
        generateLevelSelectConcepts()

        // 8. Brand / Logo Exploration
        generateLogoConcepts()

        // 9. App Icon Exploration (6 concepts)
        generateAppIconConcepts()

        // 10. Grand Decision Comparison Board
        generateComparisonBoard()
    }

    // ==========================================
    // 1. GAMEPLAY MOCKUPS (Directions A, B, C)
    // ==========================================
    private fun generateGameplayMockups() {
        val w = 1000
        val h = 1600
        val level4 = LevelCatalog.getLevel(4)

        // Direction A: Midnight Kinetic Lab
        val bmpA = renderToBitmap(w, h) {
            drawBackdropMidnight(w.toFloat(), h.toFloat())
            drawLevel4Puzzle(this, level4, Direction.A)
            drawHud(this, "04", "THE LEVER", "MIDNIGHT KINETIC LAB", Color(0xFF0EA5E9))
        }
        exportArtifact("CONCEPT_DIRECTION_A_GAMEPLAY.png", bmpA)

        // Direction B: Warm Kinetic Workshop
        val bmpB = renderToBitmap(w, h) {
            drawBackdropWorkshop(w.toFloat(), h.toFloat())
            drawLevel4Puzzle(this, level4, Direction.B)
            drawHud(this, "04", "THE LEVER", "WARM KINETIC WORKSHOP", Color(0xFFF59E0B))
        }
        exportArtifact("CONCEPT_DIRECTION_B_GAMEPLAY.png", bmpB)

        // Direction C: Soft Industrial Toy
        val bmpC = renderToBitmap(w, h) {
            drawBackdropSoftIndustrial(w.toFloat(), h.toFloat())
            drawLevel4Puzzle(this, level4, Direction.C)
            drawHud(this, "04", "THE LEVER", "SOFT INDUSTRIAL TOY", Color(0xFF10B981))
        }
        exportArtifact("CONCEPT_DIRECTION_C_GAMEPLAY.png", bmpC)
    }

    private enum class Direction { A, B, C }

    private fun DrawScope.drawBackdropMidnight(w: Float, h: Float) {
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))),
            size = Size(w, h)
        )
        // Modular grid sockets
        for (gx in 80..w.toInt() step 90) {
            for (gy in 140..h.toInt() step 120) {
                drawCircle(Color(0x1838BDF8), radius = 3.5f, center = Offset(gx.toFloat(), gy.toFloat()))
            }
        }
    }

    private fun DrawScope.drawBackdropWorkshop(w: Float, h: Float) {
        // Deep warm navy with walnut perimeter frame
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF161F30), Color(0xFF0B101B))),
            size = Size(w, h)
        )
        // Handcrafted wooden rim / chassis bevel
        drawRoundRect(
            color = Color(0xFF78350F),
            topLeft = Offset(16f, 16f),
            size = Size(w - 32f, h - 32f),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 12f)
        )
        drawRoundRect(
            color = Color(0xFFB45309),
            topLeft = Offset(24f, 24f),
            size = Size(w - 48f, h - 48f),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3f)
        )
        // Brass corner bracket studs
        val corners = listOf(Offset(50f, 50f), Offset(w - 50f, 50f), Offset(50f, h - 50f), Offset(w - 50f, h - 50f))
        for (c in corners) {
            drawCircle(Color(0xFFF59E0B), radius = 10f, center = c)
            drawCircle(Color(0xFF78350F), radius = 4f, center = c)
        }
    }

    private fun DrawScope.drawBackdropSoftIndustrial(w: Float, h: Float) {
        // Soft matte slate polymer with rounded modular recesses
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
            size = Size(w, h)
        )
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(20f, 20f),
            size = Size(w - 40f, h - 40f),
            cornerRadius = CornerRadius(36f, 36f),
            style = Stroke(width = 10f)
        )
        // Rounded modular soft tactile grooves
        drawRoundRect(
            color = Color(0x2264748B),
            topLeft = Offset(45f, 120f),
            size = Size(w - 90f, h - 240f),
            cornerRadius = CornerRadius(28f, 28f)
        )
    }

    private fun drawLevel4Puzzle(scope: DrawScope, level: LevelDefinition, dir: Direction) {
        with(scope) {
            // 1. Danger Pits
            for (pit in level.dangerPits) {
                val pRect = androidx.compose.ui.geometry.Rect(pit.bounds.left, pit.bounds.top, pit.bounds.right, pit.bounds.bottom)
                when (dir) {
                    Direction.A -> {
                        drawRoundRect(Color(0xDD180404), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(16f, 16f))
                        drawRoundRect(Color(0xFFEF4444), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 3f))
                    }
                    Direction.B -> {
                        drawRoundRect(Color(0xEE2A1010), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(16f, 16f))
                        drawRoundRect(Color(0xFFD97706), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 4f))
                    }
                    Direction.C -> {
                        drawRoundRect(Color(0xDD22151B), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(24f, 24f))
                        drawRoundRect(Color(0xFFF43F5E), topLeft = pRect.topLeft, size = pRect.size, cornerRadius = CornerRadius(24f, 24f), style = Stroke(width = 4f))
                    }
                }
            }

            // 2. Home Nest Rescue Cradle
            val nest = level.goalZone
            when (dir) {
                Direction.A -> {
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF0C4A6E), Color(0xFF021E36))), radius = nest.radius, center = Offset(nest.center.x, nest.center.y))
                    drawCircle(color = Color(0xFF38BDF8), radius = nest.radius, center = Offset(nest.center.x, nest.center.y), style = Stroke(width = 4f))
                }
                Direction.B -> {
                    // Padded velvet / warm leather rescue cradle with brass studs
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))), radius = nest.radius, center = Offset(nest.center.x, nest.center.y))
                    drawCircle(color = Color(0xFFF59E0B), radius = nest.radius, center = Offset(nest.center.x, nest.center.y), style = Stroke(width = 5f))
                    drawCircle(color = Color(0xFFFDE68A), radius = nest.radius * 0.75f, center = Offset(nest.center.x, nest.center.y), style = Stroke(width = 2f))
                }
                Direction.C -> {
                    // Soft-touch silicone cushioned nest with pastel mint ring
                    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF065F46), Color(0xFF022C22))), radius = nest.radius, center = Offset(nest.center.x, nest.center.y))
                    drawCircle(color = Color(0xFF34D399), radius = nest.radius, center = Offset(nest.center.x, nest.center.y), style = Stroke(width = 6f))
                }
            }

            // 3. Platforms / Rails
            for (p in level.platforms) {
                val start = Offset(p.start.x, p.start.y)
                val end = Offset(p.end.x, p.end.y)
                when (dir) {
                    Direction.A -> {
                        drawLine(Color(0x66000000), start + Offset(0f, 6f), end + Offset(0f, 6f), strokeWidth = p.thickness, cap = StrokeCap.Round)
                        drawLine(Color(0xFF475569), start, end, strokeWidth = p.thickness, cap = StrokeCap.Round)
                        drawLine(Color(0x66FFFFFF), start - Offset(0f, 2f), end - Offset(0f, 2f), strokeWidth = 3f, cap = StrokeCap.Round)
                    }
                    Direction.B -> {
                        // Painted enamel rail over warm wood core
                        drawLine(Color(0x77000000), start + Offset(0f, 7f), end + Offset(0f, 7f), strokeWidth = p.thickness + 4f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF78350F), start, end, strokeWidth = p.thickness + 2f, cap = StrokeCap.Round)
                        drawLine(Color(0xFFD97706), start, end, strokeWidth = p.thickness - 2f, cap = StrokeCap.Round)
                        drawCircle(Color(0xFFFDE68A), radius = 5f, center = start)
                        drawCircle(Color(0xFFFDE68A), radius = 5f, center = end)
                    }
                    Direction.C -> {
                        // Chunky molded polymer rail with rounded ends
                        drawLine(Color(0x55000000), start + Offset(0f, 6f), end + Offset(0f, 6f), strokeWidth = p.thickness + 4f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF3B82F6), start, end, strokeWidth = p.thickness, cap = StrokeCap.Round)
                        drawLine(Color(0xFF93C5FD), start - Offset(0f, 2f), end - Offset(0f, 2f), strokeWidth = 4f, cap = StrokeCap.Round)
                    }
                }
            }

            // 4. Seesaw Lever
            level.seesaw?.let { s ->
                val pivot = Offset(s.fulcrum.x, s.fulcrum.y)
                val angle = s.angle
                val dx = cos(angle) * s.halfLength
                val dy = sin(angle) * s.halfLength
                val p1 = pivot - Offset(dx, dy)
                val p2 = pivot + Offset(dx, dy)

                // Beam
                val beamColor = when (dir) {
                    Direction.A -> Color(0xFF0284C7)
                    Direction.B -> Color(0xFFB45309)
                    Direction.C -> Color(0xFF8B5CF6)
                }
                drawLine(beamColor, p1, p2, strokeWidth = s.thickness, cap = StrokeCap.Round)
                // Pivot Axle
                drawCircle(Color(0xFFFACC15), radius = 14f, center = pivot)
                drawCircle(Color(0xFF78350F), radius = 6f, center = pivot)
            }

            // 5. Heavy Wrecker Ball
            level.heavyBall?.let { hb ->
                val center = Offset(hb.position.x, hb.position.y)
                val r = hb.radius
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF64748B), Color(0xFF1E293B)), center = center - Offset(r * 0.3f, r * 0.3f), radius = r * 1.3f), radius = r, center = center)
                drawCircle(Color(0xFFF59E0B), radius = r * 0.4f, center = center, style = Stroke(width = 4f))
            }

            // 6. Spring Launcher
            for (sp in level.springBumpers) {
                val pos = Offset(sp.position.x, sp.position.y)
                drawRect(Color(0xFF334155), topLeft = pos - Offset(sp.width * 0.5f, 10f), size = Size(sp.width, 20f))
                drawLine(Color(0xFF38BDF8), pos - Offset(sp.width * 0.4f, 15f), pos + Offset(sp.width * 0.4f, -15f), strokeWidth = 8f, cap = StrokeCap.Round)
            }

            // 7. Hero Creatures (Pip, Mochi, Blobbo)
            for (c in level.initialCreatures) {
                drawHeroCharacter(this, c, dir)
            }

            // 8. Tactical Pins
            for (pin in level.pins) {
                drawTactilePin(this, pin, dir)
            }
        }
    }

    private fun drawHeroCharacter(scope: DrawScope, c: Creature, dir: Direction) {
        with(scope) {
            val center = Offset(c.position.x, c.position.y)
            val rad = c.radius * 1.25f

            // Ground shadow
            drawOval(Color(0x55000000), topLeft = Offset(center.x - rad * 0.9f, center.y + rad * 0.6f), size = Size(rad * 1.8f, rad * 0.5f))

            val (bodyColor, featureColor) = when (c.id) {
                CreatureId.PIP -> Pair(Color(0xFFFBBF24), Color(0xFFF472B6))
                CreatureId.MOCHI -> Pair(Color(0xFF34D399), Color(0xFF38BDF8))
                CreatureId.BLOBBO -> Pair(Color(0xFFFB7185), Color(0xFF4ADE80))
            }

            // Body
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(bodyColor, bodyColor.copy(alpha = 0.85f)),
                    center = center - Offset(rad * 0.3f, rad * 0.3f),
                    radius = rad * 1.2f
                ),
                radius = rad,
                center = center
            )
            drawCircle(Color(0xFF0F172A), radius = rad, center = center, style = Stroke(width = 3.5f))

            // Specific Character Features
            when (c.id) {
                CreatureId.PIP -> {
                    // Cat Ears
                    val earL = Path().apply {
                        moveTo(center.x - rad * 0.8f, center.y - rad * 0.3f)
                        lineTo(center.x - rad * 0.9f, center.y - rad * 1.2f)
                        lineTo(center.x - rad * 0.2f, center.y - rad * 0.8f)
                        close()
                    }
                    val earR = Path().apply {
                        moveTo(center.x + rad * 0.8f, center.y - rad * 0.3f)
                        lineTo(center.x + rad * 0.9f, center.y - rad * 1.2f)
                        lineTo(center.x + rad * 0.2f, center.y - rad * 0.8f)
                        close()
                    }
                    drawPath(earL, bodyColor)
                    drawPath(earL, Color(0xFF0F172A), style = Stroke(width = 3.5f))
                    drawPath(earR, bodyColor)
                    drawPath(earR, Color(0xFF0F172A), style = Stroke(width = 3.5f))
                }
                CreatureId.MOCHI -> {
                    // Antennae
                    drawLine(Color(0xFF0F172A), center + Offset(-rad * 0.3f, -rad * 0.7f), center + Offset(-rad * 0.45f, -rad * 1.2f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    drawLine(Color(0xFF0F172A), center + Offset(rad * 0.3f, -rad * 0.7f), center + Offset(rad * 0.45f, -rad * 1.2f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    drawCircle(featureColor, radius = 5.5f, center = center + Offset(-rad * 0.45f, -rad * 1.2f))
                    drawCircle(featureColor, radius = 5.5f, center = center + Offset(rad * 0.45f, -rad * 1.2f))
                }
                CreatureId.BLOBBO -> {
                    // 2-Leaf Sprout
                    val sprout = Path().apply {
                        moveTo(center.x, center.y - rad * 0.9f)
                        quadraticBezierTo(center.x - rad * 0.6f, center.y - rad * 1.3f, center.x - rad * 0.5f, center.y - rad * 1.5f)
                        quadraticBezierTo(center.x - rad * 0.1f, center.y - rad * 1.2f, center.x, center.y - rad * 0.9f)
                        quadraticBezierTo(center.x + rad * 0.6f, center.y - rad * 1.3f, center.x + rad * 0.5f, center.y - rad * 1.5f)
                        quadraticBezierTo(center.x + rad * 0.1f, center.y - rad * 1.2f, center.x, center.y - rad * 0.9f)
                    }
                    drawPath(sprout, featureColor)
                    drawPath(sprout, Color(0xFF0F172A), style = Stroke(width = 3f))
                }
            }

            // Expressive Eyes
            drawCircle(Color(0xFF0F172A), radius = 4.5f, center = center + Offset(-rad * 0.3f, -rad * 0.1f))
            drawCircle(Color.White, radius = 1.8f, center = center + Offset(-rad * 0.32f, -rad * 0.12f))
            drawCircle(Color(0xFF0F172A), radius = 4.5f, center = center + Offset(rad * 0.3f, -rad * 0.1f))
            drawCircle(Color.White, radius = 1.8f, center = center + Offset(rad * 0.28f, -rad * 0.12f))

            // Cute mouth
            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - 7f, center.y + 2f),
                size = Size(14f, 10f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
    }

    private fun drawTactilePin(scope: DrawScope, pin: Pin, dir: Direction) {
        with(scope) {
            val start = Offset(pin.start.x, pin.start.y)
            val end = Offset(pin.end.x, pin.end.y)
            val handle = Offset(pin.handlePosition.x, pin.handlePosition.y)

            // Pin Shaft
            drawLine(Color(0xFF475569), start, end, strokeWidth = pin.thickness, cap = StrokeCap.Round)
            drawLine(Color(0xFF94A3B8), start - Offset(0f, 1.5f), end - Offset(0f, 1.5f), strokeWidth = 3f, cap = StrokeCap.Round)

            // Handle housing
            drawCircle(Color(0xFF0F172A), radius = 30f, center = handle)
            drawCircle(pin.color, radius = 26f, center = handle)
            drawCircle(Color.White, radius = 24f, center = handle, style = Stroke(width = 2.5f))

            // Geometric Cutout Emblem
            when (pin.id) {
                PinId.PIN_A -> { // Diamond
                    val p = Path().apply {
                        moveTo(handle.x, handle.y - 12f)
                        lineTo(handle.x + 12f, handle.y)
                        lineTo(handle.x, handle.y + 12f)
                        lineTo(handle.x - 12f, handle.y)
                        close()
                    }
                    drawPath(p, Color.White)
                }
                PinId.PIN_B -> { // Square
                    drawRect(Color.White, topLeft = handle - Offset(10f, 10f), size = Size(20f, 20f))
                }
                PinId.PIN_C -> { // Circle
                    drawCircle(Color.White, radius = 10f, center = handle)
                }
                PinId.PIN_D -> { // Triangle
                    val p = Path().apply {
                        moveTo(handle.x, handle.y - 12f)
                        lineTo(handle.x + 12f, handle.y + 10f)
                        lineTo(handle.x - 12f, handle.y + 10f)
                        close()
                    }
                    drawPath(p, Color.White)
                }
            }
        }
    }

    private fun drawHud(scope: DrawScope, lvlNum: String, title: String, directionName: String, accent: Color) {
        with(scope) {
            // Header Pill
            drawRoundRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(30f, 30f),
                size = Size(940f, 80f),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(30f, 30f),
                size = Size(940f, 80f),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 2.5f)
            )
        }
    }

    // ==========================================
    // 2. CHARACTER MASTER SHEETS (A, B, C)
    // ==========================================
    private fun generateCharacterMasterSheets() {
        val w = 1400
        val h = 900
        val expressions = listOf("NEUTRAL", "CURIOUS", "SURPRISED", "FALLING", "DIZZY", "HAPPY", "DISAPPOINTED")

        for (dir in listOf(Direction.A, Direction.B, Direction.C)) {
            val bmp = renderToBitmap(w, h) {
                // Background
                val bgBrush = when (dir) {
                    Direction.A -> Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
                    Direction.B -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0B132B)))
                    Direction.C -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                }
                drawRect(bgBrush, size = Size(w.toFloat(), h.toFloat()))

                // Grid layout for 3 characters x 7 expressions
                val heroes = listOf(
                    Triple(CreatureId.PIP, "PIP (Cat-Eared Hero)", Color(0xFFFBBF24)),
                    Triple(CreatureId.MOCHI, "MOCHI (Mint Jelly Hero)", Color(0xFF34D399)),
                    Triple(CreatureId.BLOBBO, "BLOBBO (Plump Sprout Hero)", Color(0xFFFB7185))
                )

                heroes.forEachIndexed { rIdx, (cid, name, color) ->
                    val y = 200f + rIdx * 230f
                    expressions.forEachIndexed { cIdx, expr ->
                        val x = 140f + cIdx * 175f
                        val creature = Creature(cid, Vector2D(x, y), radius = 32f)
                        drawHeroCharacter(this, creature, dir)
                    }
                }
            }
            exportArtifact("CHARACTER_MASTER_${dir.name}.png", bmp)
        }
    }

    // ==========================================
    // 3. HOME NEST EXPLORATION
    // ==========================================
    private fun generateHomeNestConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            val centers = listOf(
                Pair(Offset(300f, 250f), "1. Padded Rescue Cradle"),
                Pair(Offset(900f, 250f), "2. Protected Magnetic Capsule"),
                Pair(Offset(300f, 580f), "3. Cozy Mechanical Home Nest"),
                Pair(Offset(900f, 580f), "4. Soft Hydraulic Dock Well")
            )

            for ((pos, title) in centers) {
                // Cradle hull
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF0C4A6E), Color(0xFF021B2E))), radius = 100f, center = pos)
                drawCircle(Color(0xFF38BDF8), radius = 100f, center = pos, style = Stroke(width = 5f))
                // 3 Dock wells for Pip, Mochi, Blobbo
                val dockOffsets = listOf(Offset(-45f, -15f), Offset(0f, 40f), Offset(45f, -15f))
                val dockColors = listOf(Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFFFB7185))
                for (i in 0..2) {
                    drawCircle(Color(0xFF031625), radius = 22f, center = pos + dockOffsets[i])
                    drawCircle(dockColors[i], radius = 22f, center = pos + dockOffsets[i], style = Stroke(width = 3f))
                    drawCircle(dockColors[i].copy(alpha = 0.4f), radius = 14f, center = pos + dockOffsets[i])
                }
                // Center Star
                drawCircle(Color(0xFFFACC15), radius = 12f, center = pos)
            }
        }
        exportArtifact("HOME_NEST_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 4. DANGER BASIN EXPLORATION
    // ==========================================
    private fun generateDangerBasinConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            val basins = listOf(
                Pair(androidx.compose.ui.geometry.Rect(120f, 150f, 520f, 380f), "1. Recessed Maintenance Catch Basin"),
                Pair(androidx.compose.ui.geometry.Rect(680f, 150f, 1080f, 380f), "2. Closed Gravity Trap Compartment"),
                Pair(androidx.compose.ui.geometry.Rect(120f, 480f, 520f, 710f), "3. Mechanical Dead-End Holding Cup"),
                Pair(androidx.compose.ui.geometry.Rect(680f, 480f, 1080f, 710f), "4. Soft Rubber Hazard Buffer Pit")
            )

            for ((rect, title) in basins) {
                drawRoundRect(Color(0xEE1E0B0B), topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(20f, 20f))
                drawRoundRect(Color(0xFFEF4444), topLeft = rect.topLeft, size = rect.size, cornerRadius = CornerRadius(20f, 20f), style = Stroke(width = 4f))
                // Diagonal Caution Stripes
                for (sx in rect.left.toInt()..rect.right.toInt() step 40) {
                    drawLine(Color(0x33EF4444), Offset(sx.toFloat(), rect.top), Offset((sx - 30).toFloat(), rect.bottom), strokeWidth = 12f)
                }
            }
        }
        exportArtifact("DANGER_BASIN_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 5. PIN / INTERACTION DESIGN
    // ==========================================
    private fun generatePinSystemConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            val pins = listOf(
                Pin(PinId.PIN_A, "A", "", Vector2D(150f, 350f), Vector2D(150f, 650f), Vector2D(0f, -1f), 300f, 18f, Color(0xFFF59E0B), handlePosition = Vector2D(150f, 280f)),
                Pin(PinId.PIN_B, "B", "", Vector2D(450f, 350f), Vector2D(450f, 650f), Vector2D(0f, -1f), 300f, 18f, Color(0xFF06B6D4), handlePosition = Vector2D(450f, 280f)),
                Pin(PinId.PIN_C, "C", "", Vector2D(750f, 350f), Vector2D(750f, 650f), Vector2D(0f, -1f), 300f, 18f, Color(0xFF8B5CF6), handlePosition = Vector2D(750f, 280f)),
                Pin(PinId.PIN_D, "D", "", Vector2D(1050f, 350f), Vector2D(1050f, 650f), Vector2D(0f, -1f), 300f, 18f, Color(0xFFEC4899), handlePosition = Vector2D(1050f, 280f))
            )

            for (p in pins) {
                drawTactilePin(this, p, Direction.A)
            }
        }
        exportArtifact("PIN_SYSTEM_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 6. MECHANISM ASSET SHEET
    // ==========================================
    private fun generateMechanismAssetConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            // Spring
            val sp = SpringBumper(Vector2D(250f, 250f), Vector2D(0f, -1f), width = 120f)
            drawRect(Color(0xFF334155), topLeft = Offset(190f, 250f), size = Size(120f, 24f))
            drawLine(Color(0xFF38BDF8), Offset(200f, 220f), Offset(300f, 220f), strokeWidth = 10f, cap = StrokeCap.Round)

            // Seesaw
            val seesaw = SeesawLever(Vector2D(750f, 250f), halfLength = 160f, thickness = 20f, angle = 0.2f)
            val p1 = Offset(750f - cos(0.2f) * 160f, 250f - sin(0.2f) * 160f)
            val p2 = Offset(750f + cos(0.2f) * 160f, 250f + sin(0.2f) * 160f)
            drawLine(Color(0xFF0284C7), p1, p2, strokeWidth = 20f, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFACC15), radius = 18f, center = Offset(750f, 250f))

            // Heavy Wrecker Ball
            val wrecker = HeavyBall(Vector2D(350f, 550f), radius = 50f)
            drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF64748B), Color(0xFF1E293B)), center = Offset(335f, 535f), radius = 70f), radius = 50f, center = Offset(350f, 550f))
            drawCircle(Color(0xFFF59E0B), radius = 22f, center = Offset(350f, 550f), style = Stroke(width = 5f))

            // Granite Stone
            val stone = RollingStone(Vector2D(850f, 550f), radius = 45f)
            drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569)), center = Offset(835f, 535f), radius = 60f), radius = 45f, center = Offset(850f, 550f))
        }
        exportArtifact("MECHANISM_ASSET_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 7. LEVEL-SELECT CONCEPTS
    // ==========================================
    private fun generateLevelSelectConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            // Draw 3 panel variations
            for (col in 0..2) {
                val cx = 80f + col * 370f
                drawRoundRect(
                    color = Color(0xEE1E293B),
                    topLeft = Offset(cx, 120f),
                    size = Size(330f, 600f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                drawRoundRect(
                    color = when(col) { 0 -> Color(0xFF38BDF8) 1 -> Color(0xFFF59E0B) else -> Color(0xFF10B981) },
                    topLeft = Offset(cx, 120f),
                    size = Size(330f, 600f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3f)
                )

                // 4 level slots inside
                for (row in 0..3) {
                    val ry = 180f + row * 120f
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx + 20f, ry),
                        size = Size(290f, 90f),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawCircle(Color(0xFFFACC15), radius = 16f, center = Offset(cx + 60f, ry + 45f))
                }
            }
        }
        exportArtifact("LEVEL_SELECT_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 8. LOGO CONCEPTS
    // ==========================================
    private fun generateLogoConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            // Horizontal in-game logo badge
            drawRoundRect(Color(0xEE1E293B), topLeft = Offset(200f, 200f), size = Size(800f, 140f), cornerRadius = CornerRadius(28f, 28f))
            drawRoundRect(Color(0xFF38BDF8), topLeft = Offset(200f, 200f), size = Size(800f, 140f), cornerRadius = CornerRadius(28f, 28f), style = Stroke(width = 3f))

            // Diamond pin emblem on left
            drawCircle(Color(0xFFF59E0B), radius = 40f, center = Offset(280f, 270f))
            val d = Path().apply {
                moveTo(280f, 245f)
                lineTo(305f, 270f)
                lineTo(280f, 295f)
                lineTo(255f, 270f)
                close()
            }
            drawPath(d, Color.White)

            // Splash Badge
            drawRoundRect(Color(0xEE1E293B), topLeft = Offset(350f, 440f), size = Size(500f, 220f), cornerRadius = CornerRadius(36f, 36f))
            drawRoundRect(Color(0xFFF59E0B), topLeft = Offset(350f, 440f), size = Size(500f, 220f), cornerRadius = CornerRadius(36f, 36f), style = Stroke(width = 4f))
            drawCircle(Color(0xFF38BDF8), radius = 45f, center = Offset(600f, 530f))
        }
        exportArtifact("ONE_MOVE_LOGO_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 9. APP ICON CONCEPTS (6 Variations)
    // ==========================================
    private fun generateAppIconConcepts() {
        val w = 1200
        val h = 800
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            val iconPositions = listOf(
                Offset(220f, 240f), Offset(600f, 240f), Offset(980f, 240f),
                Offset(220f, 580f), Offset(600f, 580f), Offset(980f, 580f)
            )

            for ((idx, pos) in iconPositions.withIndex()) {
                val iconRect = androidx.compose.ui.geometry.Rect(pos.x - 120f, pos.y - 120f, pos.x + 120f, pos.y + 120f)
                // App Icon Rounded Squircle
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                    topLeft = iconRect.topLeft,
                    size = iconRect.size,
                    cornerRadius = CornerRadius(50f, 50f)
                )
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = iconRect.topLeft,
                    size = iconRect.size,
                    cornerRadius = CornerRadius(50f, 50f),
                    style = Stroke(width = 4f)
                )

                // Different Icon Motif per concept
                when (idx) {
                    0 -> { // Mascot Trio peek over Pin
                        drawCircle(Color(0xFFFBBF24), radius = 35f, center = pos + Offset(-40f, -10f)) // Pip
                        drawCircle(Color(0xFF34D399), radius = 30f, center = pos + Offset(0f, -30f))  // Mochi
                        drawCircle(Color(0xFFFB7185), radius = 35f, center = pos + Offset(40f, -10f))  // Blobbo
                        // Pin in foreground
                        drawCircle(Color(0xFFF59E0B), radius = 28f, center = pos + Offset(0f, 40f))
                    }
                    1 -> { // Pip holding Golden Pin
                        drawCircle(Color(0xFFFBBF24), radius = 50f, center = pos + Offset(-20f, 0f))
                        drawCircle(Color(0xFFF59E0B), radius = 32f, center = pos + Offset(40f, 20f))
                    }
                    2 -> { // Mechanical Lever & Pin
                        drawLine(Color(0xFF0284C7), pos + Offset(-60f, 30f), pos + Offset(60f, -30f), strokeWidth = 14f, cap = StrokeCap.Round)
                        drawCircle(Color(0xFFFACC15), radius = 22f, center = pos)
                    }
                    3 -> { // Rescue Nest Cradle with Trio
                        drawCircle(Color(0xFF0C4A6E), radius = 65f, center = pos)
                        drawCircle(Color(0xFF38BDF8), radius = 65f, center = pos, style = Stroke(width = 4f))
                        drawCircle(Color(0xFFFBBF24), radius = 18f, center = pos + Offset(-25f, 0f))
                        drawCircle(Color(0xFF34D399), radius = 18f, center = pos + Offset(0f, 20f))
                        drawCircle(Color(0xFFFB7185), radius = 18f, center = pos + Offset(25f, 0f))
                    }
                    4 -> { // Giant Diamond Pin & Spring Burst
                        drawCircle(Color(0xFFF59E0B), radius = 45f, center = pos)
                        val d = Path().apply {
                            moveTo(pos.x, pos.y - 25f)
                            lineTo(pos.x + 25f, pos.y)
                            lineTo(pos.x, pos.y + 25f)
                            lineTo(pos.x - 25f, pos.y)
                            close()
                        }
                        drawPath(d, Color.White)
                    }
                    5 -> { // Mochi & Pip balanced on seesaw
                        drawLine(Color(0xFF38BDF8), pos + Offset(-60f, 20f), pos + Offset(60f, -20f), strokeWidth = 12f, cap = StrokeCap.Round)
                        drawCircle(Color(0xFFFBBF24), radius = 24f, center = pos + Offset(-50f, -5f))
                        drawCircle(Color(0xFF34D399), radius = 22f, center = pos + Offset(50f, -45f))
                    }
                }
            }
        }
        exportArtifact("APP_ICON_CONCEPTS.png", bmp)
    }

    // ==========================================
    // 10. GRAND COMPARISON BOARD
    // ==========================================
    private fun generateComparisonBoard() {
        val w = 1800
        val h = 1000
        val bmp = renderToBitmap(w, h) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF090D16), Color(0xFF020617))), size = Size(w.toFloat(), h.toFloat()))

            val colW = 520f
            val pad = 60f

            val directions = listOf(
                Triple("A — MIDNIGHT KINETIC LAB", Color(0xFF38BDF8), Direction.A),
                Triple("B — WARM KINETIC WORKSHOP", Color(0xFFF59E0B), Direction.B),
                Triple("C — SOFT INDUSTRIAL TOY", Color(0xFF10B981), Direction.C)
            )

            for ((idx, item) in directions.withIndex()) {
                val (title, accent, dir) = item
                val x = pad + idx * (colW + pad)

                // Panel Column Card
                drawRoundRect(
                    color = Color(0xEE111827),
                    topLeft = Offset(x, 100f),
                    size = Size(colW, 840f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, 100f),
                    size = Size(colW, 840f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3.5f)
                )

                // 1. Trio Lineup
                val trioY = 220f
                val p = Creature(CreatureId.PIP, Vector2D(x + 100f, trioY), radius = 28f)
                val m = Creature(CreatureId.MOCHI, Vector2D(x + 260f, trioY), radius = 28f)
                val b = Creature(CreatureId.BLOBBO, Vector2D(x + 420f, trioY), radius = 28f)
                drawHeroCharacter(this, p, dir)
                drawHeroCharacter(this, m, dir)
                drawHeroCharacter(this, b, dir)

                // 2. Machine Slice (Seesaw & Spring)
                val sliceY = 420f
                drawLine(accent, Offset(x + 80f, sliceY + 30f), Offset(x + 440f, sliceY - 30f), strokeWidth = 14f, cap = StrokeCap.Round)
                drawCircle(Color(0xFFFACC15), radius = 16f, center = Offset(x + 260f, sliceY))

                // 3. Home Nest
                val nestPos = Offset(x + 260f, 620f)
                drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF0C4A6E), Color(0xFF021B2E))), radius = 60f, center = nestPos)
                drawCircle(accent, radius = 60f, center = nestPos, style = Stroke(width = 4f))

                // 4. Pin A
                val pinA = Pin(PinId.PIN_A, "A", "", Vector2D(x + 260f, 760f), Vector2D(x + 260f, 850f), Vector2D(0f, -1f), 90f, 14f, Color(0xFFF59E0B), handlePosition = Vector2D(x + 260f, 750f))
                drawTactilePin(this, pinA, dir)
            }
        }
        exportArtifact("ART_DIRECTION_OPTIONS.png", bmp)
    }
}
