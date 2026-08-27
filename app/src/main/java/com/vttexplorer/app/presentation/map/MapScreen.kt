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

    Box(modifier = Modifier.fillMaxSize()) {
        // Carte plein écran
        mapProvider.MapView(
            modifier = Modifier.fillMaxSize(),
            center = if (uiState.followUser) uiState.location.position else null,
            userLocation = uiState.location.position,
            userBearing = uiState.location.bearing,
            route = uiState.currentRoute
        )

        // Barre du haut : sous la barre de statut
        TopAppBar(
            title = {
                Text("VTT Explorer", fontWeight = FontWeight.Bold, color = Color.White)
            },
            actions = {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Default.History, "Historique", tint = Color.White)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Paramètres", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Bouton recentrer : au-dessus du panneau + barre de navigation système
        FloatingActionButton(
            onClick = { viewModel.toggleFollowUser() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 200.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CircleShape
        ) {
            Icon(
                if (uiState.followUser) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                contentDescription = "Recentrer"
            )
        }

        // Panneau bas : au-dessus des boutons de navigation Android
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Où souhaitez-vous aller ?") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                readOnly = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateLoop,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.DirectionsBike, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Boucle", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onDestination,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Place, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Destination")
                }
            }

            uiState.currentRoute?.let { route ->
                Spacer(modifier = Modifier.height(12.dp))
                RouteSummaryCard(route = route, onStart = onStartNavigation)
            }
        }

        if (!locationPermission.status.isGranted) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                PermissionBanner(
                    rationale = locationPermission.status.shouldShowRationale,
                    onRequest = { locationPermission.launchPermissionRequest() }
                )
            }
        }

        uiState.error?.let { err ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
                }
            ) { Text(err) }
        }
    }
}

@Composable
private fun RouteSummaryCard(
    route: com.vttexplorer.app.domain.model.Route,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${"%.1f".format(route.distanceMeters / 1000)} km  •  " +
                    "${route.durationSeconds / 60} min  •  +${route.elevationGain.toInt()} m",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Démarrer la navigation")
            }
        }
    }
}

@Composable
private fun PermissionBanner(rationale: Boolean, onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (rationale) {
                    "La localisation est nécessaire pour le GPS VTT."
                } else {
                    "Autorisez la localisation précise pour utiliser VTT Explorer."
                },
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequest) {
                Text("Autoriser")
            }
        }
    }
}
