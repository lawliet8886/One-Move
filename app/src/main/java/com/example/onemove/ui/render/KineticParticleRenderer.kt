package com.example.onemove.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.onemove.model.Particle
import com.example.onemove.model.ParticleType

object KineticParticleRenderer {
    fun drawParticles(scope:DrawScope,particles:List<Particle>) {
        with(scope) {
            for(p in particles) {
                if(!p.isAlive) continue
                val alpha=p.alpha.coerceIn(0f,1f)
                val c=p.color.copy(alpha=alpha)
                val pos=Offset(p.position.x,p.position.y)
                when(p.type) {
                    ParticleType.CONFETTI -> withTransform({translate(pos.x,pos.y);rotate(p.rotation,pivot=Offset.Zero)}) {
                        drawRect(c,Offset(-p.size*.5f,-p.size*.25f),Size(p.size,p.size*.5f))
                    }
                    ParticleType.SPARK -> {drawCircle(c,p.size*.5f,pos);drawCircle(Color.White.copy(alpha=alpha),p.size*.25f,pos)}
                    ParticleType.DUST,ParticleType.SMOKE -> drawCircle(c,p.size*(1.5f-p.progress*.5f),pos)
                    else -> drawCircle(c,p.size*.5f,pos)
                }
            }
        }
    }
}
