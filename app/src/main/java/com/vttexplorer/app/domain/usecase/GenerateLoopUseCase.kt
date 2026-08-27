package com.vttexplorer.app.domain.usecase

import com.vttexplorer.app.domain.model.LoopPreferences
import com.vttexplorer.app.domain.model.Route
import com.vttexplorer.app.domain.repository.LocationRepository
import com.vttexplorer.app.domain.repository.RoutingEngine

class GenerateLoopUseCase(
    private val locationRepository: LocationRepository,
    private val routingEngine: RoutingEngine
) {
    suspend operator fun invoke(preferences: LoopPreferences): Result<Route> {
        val location = locationRepository.getLastKnownLocation()
            ?: return Result.failure(IllegalStateException("Position GPS indisponible"))
        return routingEngine.generateLoop(location, preferences)
    }
}
