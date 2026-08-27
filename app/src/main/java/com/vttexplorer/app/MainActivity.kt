package com.vttexplorer.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vttexplorer.app.presentation.VTTExplorerNavHost
import com.vttexplorer.app.presentation.theme.VTTExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("VTTExplorer", "enableEdgeToEdge failed", e)
        }
        setContent {
            VTTExplorerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    try {
                        VTTExplorerNavHost()
                    } catch (e: Exception) {
                        Log.e("VTTExplorer", "UI crash", e)
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Erreur au démarrage:\n${e.message}")
                        }
                    }
                }
            }
        }
    }
}
