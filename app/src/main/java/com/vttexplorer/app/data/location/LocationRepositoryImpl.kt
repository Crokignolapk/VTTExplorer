package com.vttexplorer.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.LocationState
import com.vttexplorer.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationState())
    override val locationState: Flow<LocationState> = _locationState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (locationCallback != null) return
        if (!hasLocationPermission()) return

        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(3f)
                .setWaitForAccurateLocation(false)
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
        } catch (e: SecurityException) {
            e.printStackTrace()
            locationCallback = null
        } catch (e: Exception) {
            e.printStackTrace()
            locationCallback = null
        }
    }

    override fun stopLocationUpdates() {
        try {
            locationCallback?.let {
                fusedClient.removeLocationUpdates(it)
            }
        } catch (_: Exception) {
        }
        locationCallback = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LatLng? {
        if (!hasLocationPermission()) return null
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
