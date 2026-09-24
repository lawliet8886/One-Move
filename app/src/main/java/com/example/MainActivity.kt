package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.onemove.ui.OneMoveGameScreen
import com.example.onemove.ui.render.SpriteMascotRenderer
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One small bundled atlas, decoded once per process before the first frame.
        SpriteMascotRenderer.prepare(applicationContext)
        com.example.onemove.ui.render.HardwareSpriteRenderer.prepare(applicationContext)
        com.example.onemove.ui.render.SpringBumperRenderer.prepare(applicationContext)
        com.example.onemove.ui.render.RescueSanctuaryRenderer.prepare(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme(darkTheme = true) {
                OneMoveGameScreen()
            }
        }
    }
}
