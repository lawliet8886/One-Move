package com.example.onemove.ui.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.onemove.model.Creature
import com.example.onemove.model.CreatureExpression
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Original offline Blender art. The body, not ears/leaves, matches the physics circle. */
object SpriteMascotRenderer {
    private const val CELL=192
    private const val FRAMING=3.6f
    @Volatile private var atlas: ImageBitmap? = null

    @Synchronized fun prepare(context: Context) {
        if(atlas!=null) return
        val bytes=context.assets.open("mascots/atlas.webp").use{it.readBytes()}
        val manifest=context.assets.open("mascots/manifest.json").bufferedReader().use{JSONObject(it.readText())}
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        check(digest==manifest.getString("atlas_sha256")){"Mascot atlas integrity failure"}
        val bitmap=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size)){"Cannot decode mascot atlas"}
        check(bitmap.width==CELL*4 && bitmap.height==CELL*3){"Incorrect atlas geometry"}
        check(bitmap.hasAlpha()){ "Mascot atlas must retain transparency" }
        atlas=bitmap.asImageBitmap()
        Log.i("OneMoveAssets","Loaded reviewed 12-expression atlas: ${bitmap.width}x${bitmap.height}; ${bytes.size} bytes; body framing $FRAMING")
    }

    /** False only for isolated vector preview tests which did not create MainActivity. */
    fun draw(scope:DrawScope, creature:Creature, center:Offset):Boolean {
        val image=atlas ?: return false
        val column=when(creature.expression){
            CreatureExpression.HAPPY -> 1
            CreatureExpression.SCARED,CreatureExpression.PANIC,CreatureExpression.SURPRISED -> 2
            CreatureExpression.DIZZY,CreatureExpression.DISAPPOINTED -> 3
            else -> 0
        }
        val side=(creature.radius*FRAMING).roundToInt().coerceAtLeast(1)
        with(scope){
            drawOval(Color(0xFF20382C).copy(alpha=0.15f),
                center+Offset(-creature.radius*0.85f,creature.radius*0.72f),
                Size(creature.radius*1.7f,creature.radius*0.34f))
            drawImage(image=image,
                srcOffset=IntOffset(column*CELL,creature.id.ordinal*CELL),srcSize=IntSize(CELL,CELL),
                dstOffset=IntOffset((center.x-side/2f).roundToInt(),(center.y-side/2f).roundToInt()),
                dstSize=IntSize(side,side),filterQuality=FilterQuality.Medium)
        }
        return true
    }
}
