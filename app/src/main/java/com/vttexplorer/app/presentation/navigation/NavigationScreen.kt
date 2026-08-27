package com.vttexplorer.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vttexplorer.app.data.maps.MapProvider
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NavigationScreen(
    onStop: () -> Unit,
    viewModel: NavigationViewModel = koinViewModel(),
    mapProvider: MapProvider = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        mapProvider.MapView(
            modifier = Modifier.fillMaxSize(),
            center = uiState.location.position,
            userLocation = uiState.location.position,
            userBearing = uiState.location.bearing,
            route = uiState.route
        )

        // Bandeau instruction
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Dans ${uiState.distanceToNext.toInt()} m",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        uiState.nextInstruction.ifEmpty { "Suivre le tracé VTT" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Stats bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Restant", "${"%.1f".format(uiState.distanceRemaining / 1000)} km")
                StatItem("Vitesse", "${"%.0f".format(uiState.currentSpeedKmh)} km/h")
                StatItem("Parcouru", "${"%.1f".format(uiState.distanceTraveled / 1000)} km")
                StatItem("D+", "+${uiState.elevationGain.toInt()} m")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Arrêter la navigation")
            }
        }

        if (uiState.isRecalculating) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                "Recalcul de l'itinéraire…",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
