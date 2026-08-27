package com.vttexplorer.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vttexplorer.app.domain.model.LocationState
import com.vttexplorer.app.domain.model.Route
import com.vttexplorer.app.domain.repository.LocationRepository
import com.vttexplorer.app.domain.repository.RoutingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavigationUiState(
    val location: LocationState = LocationState(),
    val route: Route? = null,
    val distanceRemaining: Double = 0.0,
    val nextInstruction: String = "",
    val distanceToNext: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val distanceTraveled: Double = 0.0,
    val elevationGain: Double = 0.0,
    val isRecalculating: Boolean = false
)

class NavigationViewModel(
    private val locationRepository: LocationRepository,
    private val routingEngine: RoutingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.locationState.collect { loc ->
                _uiState.update {
                    it.copy(
                        location = loc,
                        currentSpeedKmh = (loc.speed * 3.6f)
                    )
                }
            }
        }
        locationRepository.startLocationUpdates()
    }

    fun setRoute(route: Route) {
        _uiState.update {
            it.copy(
                route = route,
                distanceRemaining = route.distanceMeters,
                nextInstruction = route.instructions.firstOrNull()?.text ?: "Suivre le tracé",
                distanceToNext = route.instructions.firstOrNull()?.distanceMeters ?: 0.0
            )
        }
    }
}
