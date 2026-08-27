package com.vttexplorer.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Navigation", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Voix de navigation", state.voiceEnabled) { viewModel.setVoice(it) }
            SwitchRow("Recalcul automatique", state.autoRecalc) { viewModel.setAutoRecalc(it) }

            Spacer(Modifier.height(24.dp))
            Text("Carte", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Thème sombre carte", state.darkMap) { viewModel.setDarkMap(it) }

            Spacer(Modifier.height(24.dp))
            Text("Itinéraire VTT", style = MaterialTheme.typography.titleMedium)
            SwitchRow("Éviter grands axes", state.avoidMainRoads) { viewModel.setAvoidMain(it) }
            SwitchRow("Éviter routes goudronnées", state.avoidPaved) { viewModel.setAvoidPaved(it) }

            Spacer(Modifier.height(32.dp))
            Text(
                "Les traces GPS sont stockées localement. " +
                        "VTT Explorer ne collecte aucune donnée personnelle sans votre consentement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
