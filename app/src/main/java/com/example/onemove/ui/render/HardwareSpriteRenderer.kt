package com.example.onemove.ui.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Original Blender hardware. Physical centres and radii are never mutated by art. */
object HardwareSpriteRenderer {
    enum class Kind { WRECKER, STONE, AXLE }
    private const val CELL=256
    private const val FRAMING=2.4f
    @Volatile private var atlas: ImageBitmap? = null
    val isPrepared: Boolean get() = atlas != null
    const val DECODED_BYTES=768*256*4

    @Synchronized fun prepare(context: Context) {
        if(atlas!=null) return
        val bytes=context.assets.open("hardware/atlas.webp").use { it.readBytes() }
        val manifest=context.assets.open("hardware/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(digest==manifest.getString("atlas_sha256")) { "Hardware atlas hash mismatch" }
        check(manifest.getInt("cell_size")==CELL && manifest.getDouble("framing")==2.4)
        val bitmap=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size))
        check(bitmap.width==CELL*3 && bitmap.height==CELL && bitmap.hasAlpha()) {
            "Hardware decode geometry/alpha mismatch: ${bitmap.width}x${bitmap.height}; alpha=${bitmap.hasAlpha()}"
        }
        atlas=bitmap.asImageBitmap()
        Log.i("OneMoveAssets","Hardware atlas ready: ${bitmap.width}x${bitmap.height}; ${bytes.size} bytes; $DECODED_BYTES decoded bytes")
    }

    /** False only for isolated vector previews without MainActivity's asset setup. */
    fun draw(scope: DrawScope, kind: Kind, center: Offset, radius: Float, degrees: Float=0f): Boolean {
        val image=atlas ?: return false
        require(radius.isFinite() && radius>0f && degrees.isFinite())
        val side=(radius*FRAMING).roundToInt().coerceAtLeast(1)
        with(scope) {
            withTransform({ translate(center.x,center.y); rotate(degrees,pivot=Offset.Zero) }) {
                drawImage(image=image,srcOffset=IntOffset(kind.ordinal*CELL,0),srcSize=IntSize(CELL,CELL),
                    dstOffset=IntOffset((-side/2f).roundToInt(),(-side/2f).roundToInt()),
                    dstSize=IntSize(side,side),filterQuality=FilterQuality.Medium)
            }
        }
        return true
    }
}
