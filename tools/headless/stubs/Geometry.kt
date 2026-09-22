// Only value types needed to compile the production physics in a non-Android JVM.
// These adapters do NOT simulate Android, rendering or touch input.
package androidx.compose.ui.geometry
data class Offset(val x: Float, val y: Float)
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)
