package com.vttexplorer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vttexplorer.app.presentation.VTTExplorerNavHost
import com.vttexplorer.app.presentation.theme.VTTExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // PAS de enableEdgeToEdge() : les barres système gardent leur place
        // → les boutons de l'app ne sont plus masqués
        setContent {
            VTTExplorerTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VTTExplorerNavHost()
                }
            }
        }
    }
}
