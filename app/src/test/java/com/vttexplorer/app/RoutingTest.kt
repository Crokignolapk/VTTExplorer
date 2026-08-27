package com.vttexplorer.app

import com.vttexplorer.app.data.routing.GraphHopperRoutingEngine
import com.vttexplorer.app.domain.model.BikeType
import com.vttexplorer.app.domain.model.Difficulty
import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.LoopPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoutingTest {

    private val engine = GraphHopperRoutingEngine()

    @Test
    fun generateLoop_returnsClosedRouteNearTargetDistance() = runBlocking {
        val prefs = LoopPreferences(
            targetDistanceKm = 20f,
            difficulty = Difficulty.INTERMEDIATE,
            bikeType = BikeType.MTB,
            avoidMainRoads = true,
            avoidPaved = true
        )
        val result = engine.generateLoop(LatLng(46.58, 0.34), prefs) // Poitiers approx
        assertTrue(result.isSuccess)
        val route = result.getOrThrow()
        assertTrue(route.isLoop)
        assertTrue(route.points.size >= 10)
        // Tolérance ±30 %
        val km = route.distanceMeters / 1000.0
        assertTrue("Distance $km hors tolérance", km in 14.0..26.0)
        assertTrue(route.elevationGain >= 0)
    }

    @Test
    fun calculateRoute_hasPoints() = runBlocking {
        val result = engine.calculateRoute(
            LatLng(46.58, 0.34),
            LatLng(46.60, 0.36),
            LoopPreferences()
        )
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().points.size > 5)
    }
}
