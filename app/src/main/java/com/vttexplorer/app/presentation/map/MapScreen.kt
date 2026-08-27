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
import com.vttexplorer.app.domain.model.LatLng
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

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Carte
        mapProvider.MapView(
            modifier = Modifier.fillMaxSize(),
            center = if (uiState.followUser) uiState.location.position else null,
            userLocation = uiState.location.position,
            userBearing = uiState.location.bearing,
            route = uiState.currentRoute
        )

        // Barre de recherche / titre
        TopAppBar(
            title = {
                Text(
                    "VTT Explorer",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            actions = {
                IconButton(onClick = onHistory) {
                    Icon(Icons.Default.History, contentDescription = "Historique", tint = Color.White)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.55f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bouton recentrer
        FloatingActionButton(
            onClick = { viewModel.toggleFollowUser() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 180.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CircleShape
        ) {
            Icon(
                if (uiState.followUser) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                contentDescription = "Recentrer"
            )
        }

        // Panneau bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            // Recherche
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Où souhaitez-vous aller ?") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                readOnly = true // MVP : ouvre destination
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateLoop,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.DirectionsBike, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer une boucle", fontWeight = FontWeight.SemiBold)
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

            // Affichage route si présente
            uiState.currentRoute?.let { route ->
                Spacer(modifier = Modifier.height(12.dp))
                RouteSummaryCard(route = route, onStart = onStartNavigation)
            }
        }

        // Permission rationale
        if (!locationPermission.status.isGranted) {
            PermissionBanner(
                rationale = locationPermission.status.shouldShowRationale,
                onRequest = { locationPermission.launchPermissionRequest() }
            )
        }

        // Erreur
        uiState.error?.let { err ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
