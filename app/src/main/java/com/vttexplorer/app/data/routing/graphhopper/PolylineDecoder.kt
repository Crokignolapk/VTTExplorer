package com.vttexplorer.app.data.routing.graphhopper

import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.RoutePoint

/**
 * Décode une polyline encodée GraphHopper (format Google / precision 1e5 ou 1e6).
 * Si elevation=true, les points sont [lon, lat, ele] et la précision reste 1e5 pour lat/lon,
 * 1e2 pour l'altitude (comportement GraphHopper).
 */
object PolylineDecoder {

    fun decode(encoded: String, withElevation: Boolean = true): List<RoutePoint> {
        val coordinates = mutableListOf<RoutePoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        var ele = 0
        var cumulative = 0.0
        var prevLat = 0.0
        var prevLng = 0.0
        var first = true

        while (index < len) {
            // Latitude
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            // Longitude
            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            var altitude: Double? = null
            if (withElevation && index < len) {
                result = 0
                shift = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dele = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
                ele += dele
                altitude = ele / 100.0
            }

            val latitude = lat / 1e5
            val longitude = lng / 1e5

            if (!first) {
                cumulative += haversine(prevLat, prevLng, latitude, longitude)
            }
            first = false
            prevLat = latitude
            prevLng = longitude

            coordinates.add(
                RoutePoint(
                    latLng = LatLng(latitude, longitude),
                    elevation = altitude,
                    distanceFromStart = cumulative
                )
            )
        }
        return coordinates
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2 * R * kotlin.math.asin(kotlin.math.sqrt(a))
    }
}
