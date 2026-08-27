package com.vttexplorer.app.presentation.route_generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vttexplorer.app.domain.model.BikeType
import com.vttexplorer.app.domain.model.Difficulty
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteGeneratorScreen(
    onBack: () -> Unit,
    onStart: () -> Unit,
    viewModel: RouteGeneratorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer une boucle VTT") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Distance
            Text("Distance : ${"%.0f".format(prefs.targetDistanceKm)} km", fontWeight = FontWeight.Bold)
            Slider(
                value = prefs.targetDistanceKm,
                onValueChange = { viewModel.updateDistance(it) },
                valueRange = 5f..100f,
                steps = 18
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "5 km", style = MaterialTheme.typography.bodySmall)
                Text(text = "100 km", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Difficulté
            Text("Difficulté", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    FilterChip(
                        selected = prefs.difficulty == d,
                        onClick = { viewModel.updateDifficulty(d) },
                        label = {
                            Text(
                                when (d) {
                                    Difficulty.EASY -> "Facile"
                                    Difficulty.INTERMEDIATE -> "Moyen"
                                    Difficulty.HARD -> "Difficile"
                                    Difficulty.EXPERT -> "Expert"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Type
            Text("Type de parcours", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    BikeType.MTB to "VTT",
                    BikeType.TREKKING to "VTC",
                    BikeType.LEISURE to "Vélo"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = prefs.bikeType == type,
                        onClick = { viewModel.updateBikeType(type) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Préférences", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            PreferenceSlider("Chemins", prefs.favorPaths) { viewModel.updateFavorPaths(it) }
            PreferenceSlider("Pistes cyclables", prefs.favorCycleways) { viewModel.updateFavorCycleways(it) }
            PreferenceSlider("Dénivelé", prefs.elevationPreference) { viewModel.updateElevation(it) }

            Spacer(modifier = Modifier.height(16.dp))

            CheckboxRow("Éviter les grands axes", prefs.avoidMainRoads) {
                viewModel.toggleAvoidMainRoads(it)
            }
            CheckboxRow("Éviter les routes goudronnées", prefs.avoidPaved) {
                viewModel.toggleAvoidPaved(it)
            }
            CheckboxRow("Autoriser portions difficiles", prefs.allowHardSections) {
                viewModel.toggleAllowHard(it)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isGenerating
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Génération…")
                } else {
                    Text("GÉNÉRER LA BOUCLE", fontWeight = FontWeight.Bold)
                }
            }

            // Résultat
            uiState.generatedRoute?.let { route ->
                Spacer(modifier = Modifier.height(24.dp))
                ResultCard(route = route, onStart = onStart, onRegenerate = { viewModel.generate() })
            }

            uiState.error?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(err, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Vérifiez toujours les conditions d'accès et la praticabilité des chemins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreferenceSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label  ${(value * 100).toInt()} %")
        Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = { onChecked(!checked) },
                role = Role.Checkbox
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ResultCard(
    route: com.vttexplorer.app.domain.model.Route,
    onStart: () -> Unit,
    onRegenerate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${"%.1f".format(route.distanceMeters / 1000)} km",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${route.durationSeconds / 60} min  •  +${route.elevationGain.toInt()} m",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Chemins ${(route.pathPercentage * 100).toInt()} %  •  " +
                    "Pistes ${(route.cyclewayPercentage * 100).toInt()} %  •  " +
                    "Routes ${(route.roadPercentage * 100).toInt()} %")
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Text("Démarrer")
                }
                OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f)) {
                    Text("Régénérer")
                }
            }
        }
    }
}
