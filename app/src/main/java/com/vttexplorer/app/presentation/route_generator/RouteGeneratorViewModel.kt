package com.vttexplorer.app.presentation.route_generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vttexplorer.app.domain.model.*
import com.vttexplorer.app.domain.usecase.GenerateLoopUseCase
import com.vttexplorer.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RouteGeneratorUiState(
    val preferences: LoopPreferences = LoopPreferences(),
    val generatedRoute: Route? = null,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class RouteGeneratorViewModel(
    private val generateLoopUseCase: GenerateLoopUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteGeneratorUiState())
    val uiState: StateFlow<RouteGeneratorUiState> = _uiState.asStateFlow()

    fun updateDistance(km: Float) {
        _uiState.update { it.copy(preferences = it.preferences.copy(targetDistanceKm = km)) }
    }

    fun updateDifficulty(d: Difficulty) {
        _uiState.update { it.copy(preferences = it.preferences.copy(difficulty = d)) }
    }

    fun updateBikeType(t: BikeType) {
        _uiState.update { it.copy(preferences = it.preferences.copy(bikeType = t)) }
    }

    fun updateFavorPaths(v: Float) {
        _uiState.update { it.copy(preferences = it.preferences.copy(favorPaths = v)) }
    }

    fun updateFavorCycleways(v: Float) {
        _uiState.update { it.copy(preferences = it.preferences.copy(favorCycleways = v)) }
    }

    fun updateElevation(v: Float) {
        _uiState.update { it.copy(preferences = it.preferences.copy(elevationPreference = v)) }
    }

    fun toggleAvoidMainRoads(v: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(avoidMainRoads = v)) }
    }

    fun toggleAvoidPaved(v: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(avoidPaved = v)) }
    }

    fun toggleAllowHard(v: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(allowHardSections = v)) }
    }

    fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val result = generateLoopUseCase(_uiState.value.preferences)
            result.fold(
                onSuccess = { route ->
                    _uiState.update { it.copy(generatedRoute = route, isGenerating = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            error = e.message ?: "Impossible de générer la boucle"
                        )
                    }
                }
            )
        }
    }

    fun clearRoute() {
        _uiState.update { it.copy(generatedRoute = null) }
    }
}
