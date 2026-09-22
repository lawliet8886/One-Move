package com.example

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.onemove.ui.OneMoveGameScreen
import com.example.onemove.ui.OneMoveViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var game: OneMoveViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(Color.TRANSPARENT))
        game=ViewModelProvider(this)[OneMoveViewModel::class.java]
        setContent { MyApplicationTheme { OneMoveGameScreen(game) } }
    }
    override fun onResume() { super.onResume(); if(::game.isInitialized) game.setForegroundActive(true) }
    override fun onPause() { if(::game.isInitialized) game.setForegroundActive(false); super.onPause() }
}
