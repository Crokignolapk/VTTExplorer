package com.vttexplorer.app.data.maps

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.Route

/**
 * Abstraction du fournisseur cartographique.
 * Permet de remplacer MapLibre par une autre solution (Google Maps, osmdroid…)
 * sans réécrire toute l'UI.
 */
interface MapProvider {
    @Composable
    fun MapView(
        modifier: Modifier,
        center: LatLng?,
        userLocation: LatLng?,
        userBearing: Float?,
        route: Route?,
        onMapReady: () -> Unit = {},
        onCameraMove: (LatLng, Double) -> Unit = { _, _ -> }
    )
}

class MapLibreProvider : MapProvider {
    @Composable
    override fun MapView(
        modifier: Modifier,
        center: LatLng?,
        userLocation: LatLng?,
        userBearing: Float?,
        route: Route?,
        onMapReady: () -> Unit,
        onCameraMove: (LatLng, Double) -> Unit
    ) {
        MapLibreMapComposable(
            modifier = modifier,
            center = center,
            userLocation = userLocation,
            userBearing = userBearing,
            route = route,
            onMapReady = onMapReady,
            onCameraMove = onCameraMove
        )
    }
}
