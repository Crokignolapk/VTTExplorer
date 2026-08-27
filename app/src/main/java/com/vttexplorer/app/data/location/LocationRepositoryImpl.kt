package com.vttexplorer.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.LocationState
import com.vttexplorer.app.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationState())
    override val locationState: Flow<LocationState> = _locationState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(2f)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                updateState(loc)
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LatLng? {
        return try {
            val loc = fusedClient.lastLocation.await()
            loc?.let {
                updateState(it)
                LatLng(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun updateState(loc: Location) {
        val bearing = if (loc.hasBearing()) loc.bearing else null
        _locationState.value = LocationState(
            position = LatLng(loc.latitude, loc.longitude),
            accuracy = loc.accuracy,
            bearing = bearing,
            speed = if (loc.hasSpeed()) loc.speed else 0f,
            altitude = if (loc.hasAltitude()) loc.altitude else null,
            isGpsAvailable = true
        )
    }
}
