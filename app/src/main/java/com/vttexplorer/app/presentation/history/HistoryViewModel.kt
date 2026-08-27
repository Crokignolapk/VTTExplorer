package com.vttexplorer.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vttexplorer.app.domain.model.RideSummary
import com.vttexplorer.app.domain.repository.RideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val rideRepository: RideRepository
) : ViewModel() {
    val rides: StateFlow<List<RideSummary>> = rideRepository.getAllRides()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { rideRepository.deleteRide(id) }
    }
}
