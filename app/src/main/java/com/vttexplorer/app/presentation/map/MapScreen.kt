package com.vttexplorer.app.presentation.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.vttexplorer.app.data.maps.MapProvider
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onCreateLoop: () -> Unit,
    onDestination: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onStartNavigation: () -> Unit,
    viewModel: MapViewModel = koinViewModel(),
    mapProvider: MapProvider = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            viewModel.onPermissionGranted()
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre titre
        TopAppBar(
            title = { Text("VTT Explorer", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Default.History, "Historique")
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Paramètres")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // Zone carte (prend tout l'espace restant)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            mapProvider.MapView(
                modifier = Modifier.fillMaxSize(),
                center = if (uiState.followUser) uiState.location.position else null,
                userLocation = uiState.location.position,
                userBearing = uiState.location.bearing,
                route = uiState.currentRoute
            )

            FloatingActionButton(
                onClick = { viewModel.toggleFollowUser() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ) {
                Icon(
                    if (uiState.followUser) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Recentrer"
                )
            }

            if (!locationPermission.status.isGranted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = if (locationPermission.status.shouldShowRationale)
                            "La localisation est nécessaire pour le GPS VTT."
                        else
                            "Autorisez la localisation pour utiliser VTT Explorer.",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                    Button(
                        onClick = { locationPermission.launchPermissionRequest() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Autoriser")
                    }
                }
            }
        }

        // Panneau actions EN BAS — hors de la carte, au-dessus de la barre système
        // (safeDrawingPadding sur MainActivity + Column structure = plus de masquage)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Où souhaitez-vous aller ?") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                readOnly = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateLoop,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DirectionsBike, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Boucle", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onDestination,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Place, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Destination")
                }
            }

            uiState.currentRoute?.let { route ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "${"%.1f".format(route.distanceMeters / 1000)} km  •  " +
                        "${route.durationSeconds / 60} min  •  +${route.elevationGain.toInt()} m",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Démarrer la navigation")
                }
            }
        }
    }
}
