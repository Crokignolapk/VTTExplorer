package com.vttexplorer.app.domain.repository

import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.LocationState
import com.vttexplorer.app.domain.model.LoopPreferences
import com.vttexplorer.app.domain.model.RideEntity
import com.vttexplorer.app.domain.model.RideSummary
import com.vttexplorer.app.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    val locationState: Flow<LocationState>
    fun startLocationUpdates()
    fun stopLocationUpdates()
    suspend fun getLastKnownLocation(): LatLng?
}

interface RideRepository {
    fun getAllRides(): Flow<List<RideSummary>>
    suspend fun saveRide(ride: RideEntity): Long
    suspend fun deleteRide(id: Long)
    suspend fun renameRide(id: Long, newName: String)
    suspend fun getRide(id: Long): RideEntity?
}

interface RoutingEngine {
    suspend fun calculateRoute(
        start: LatLng,
        end: LatLng,
        preferences: LoopPreferences
    ): Result<Route>

    suspend fun generateLoop(
        center: LatLng,
        preferences: LoopPreferences
    ): Result<Route>

    suspend fun recalculate(
        current: LatLng,
        remainingPoints: List<LatLng>,
        preferences: LoopPreferences
    ): Result<Route>
}
