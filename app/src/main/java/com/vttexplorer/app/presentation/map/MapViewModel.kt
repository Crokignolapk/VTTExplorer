package com.vttexplorer.app.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vttexplorer.app.domain.model.LocationState
import com.vttexplorer.app.domain.model.Route
import com.vttexplorer.app.domain.repository.LocationRepository
import com.vttexplorer.app.domain.usecase.GenerateLoopUseCase
import com.vttexplorer.app.domain.usecase.GetRouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val location: LocationState = LocationState(),
    val currentRoute: Route? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val followUser: Boolean = true
)

class MapViewModel(
    private val locationRepository: LocationRepository,
    private val generateLoopUseCase: GenerateLoopUseCase,
    private val getRouteUseCase: GetRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.locationState.collect { loc ->
                _uiState.update { it.copy(location = loc) }
            }
        }
        locationRepository.startLocationUpdates()
    }

    fun setRoute(route: Route?) {
        _uiState.update { it.copy(currentRoute = route) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onPermissionGranted() {
        locationRepository.startLocationUpdates()
    }

    fun toggleFollowUser() {
        _uiState.update { it.copy(followUser = !it.followUser) }
    }

    override fun onCleared() {
        super.onCleared()
        locationRepository.stopLocationUpdates()
    }
}
