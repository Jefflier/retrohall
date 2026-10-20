package com.retrohall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.retrohall.app.ui.RetroHallRoot
import com.retrohall.app.ui.theme.RetroHallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroHallTheme {
                RetroHallRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Play statistics are written by the game process; pick them up when coming back.
        AppGraph.games.refresh()
        AppGraph.bios.refresh()
    }
}
