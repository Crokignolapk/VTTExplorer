package com.vttexplorer.app.domain.usecase

import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.LoopPreferences
import com.vttexplorer.app.domain.model.Route
import com.vttexplorer.app.domain.repository.RoutingEngine

class GetRouteUseCase(
    private val routingEngine: RoutingEngine
) {
    suspend operator fun invoke(
        start: LatLng,
        end: LatLng,
        preferences: LoopPreferences
    ): Result<Route> {
        return routingEngine.calculateRoute(start, end, preferences)
    }
}
