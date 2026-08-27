package com.vttexplorer.app.data.routing

import com.vttexplorer.app.data.routing.graphhopper.*
import com.vttexplorer.app.domain.model.*
import com.vttexplorer.app.domain.repository.RoutingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.*

/**
 * Moteur de routage VTT basé sur GraphHopper Directions API.
 *
 * - Clé API présente (BuildConfig.GRAPHHOPPER_API_KEY) → appels réels
 * - Sinon → fallback local pour tester sans clé
 *
 * Profils : mtb (VTT) | bike (VTC/loisir) | racingbike
 * Boucles  : algorithm=round_trip + round_trip.distance
 * Custom   : VttCustomModelBuilder (chemins, surfaces, mtb_rating, grands axes)
 */
class GraphHopperRoutingEngine : RoutingEngine {

    override suspend fun calculateRoute(
        start: LatLng,
        end: LatLng,
        preferences: LoopPreferences
    ): Result<Route> = withContext(Dispatchers.IO) {
        if (!GraphHopperClient.isConfigured) {
            return@withContext fallbackPointToPoint(start, end, preferences)
        }
        try {
            val request = GhRouteRequest(
                points = listOf(
                    listOf(start.longitude, start.latitude),
                    listOf(end.longitude, end.latitude)
                ),
                profile = VttCustomModelBuilder.profileFor(preferences.bikeType),
                elevation = true,
                instructions = true,
                chDisable = true,
                customModel = VttCustomModelBuilder.build(preferences),
                snapPreventions = buildSnapPreventions(preferences)
            )
            val response = GraphHopperClient.api.route(GraphHopperClient.apiKey, request)
            mapResponse(response, isLoop = false, preferences)
        } catch (e: Exception) {
            Result.failure(Exception("Erreur GraphHopper : ${e.message}", e))
        }
    }

    override suspend fun generateLoop(
        center: LatLng,
        preferences: LoopPreferences
    ): Result<Route> = withContext(Dispatchers.IO) {
        if (!GraphHopperClient.isConfigured) {
            return@withContext fallbackLoop(center, preferences)
        }
        try {
            val targetMeters = (preferences.targetDistanceKm * 1000).toInt().coerceIn(2000, 150_000)
            val seed = System.currentTimeMillis() % 10_000

            val request = GhRouteRequest(
                points = listOf(listOf(center.longitude, center.latitude)),
                profile = VttCustomModelBuilder.profileFor(preferences.bikeType),
                elevation = true,
                instructions = true,
                chDisable = true,
                algorithm = "round_trip",
                roundTripDistance = targetMeters,
                roundTripSeed = seed,
                customModel = VttCustomModelBuilder.build(preferences),
                snapPreventions = buildSnapPreventions(preferences)
            )
            val response = GraphHopperClient.api.route(GraphHopperClient.apiKey, request)
            mapResponse(response, isLoop = true, preferences)
        } catch (e: Exception) {
            Result.failure(Exception("Impossible de générer la boucle : ${e.message}", e))
        }
    }

    override suspend fun recalculate(
        current: LatLng,
        remainingPoints: List<LatLng>,
        preferences: LoopPreferences
    ): Result<Route> {
        if (remainingPoints.isEmpty()) {
            return Result.failure(IllegalStateException("Aucun point restant"))
        }
        return calculateRoute(current, remainingPoints.last(), preferences)
    }

    private fun mapResponse(
        response: GhRouteResponse,
        isLoop: Boolean,
        preferences: LoopPreferences
    ): Result<Route> {
        if (response.message != null && response.paths.isNullOrEmpty()) {
            return Result.failure(Exception(response.message))
        }
        val path = response.paths?.firstOrNull()
            ?: return Result.failure(Exception("Aucun parcours retourné par GraphHopper"))

        val points = if (!path.points.isNullOrEmpty()) {
            PolylineDecoder.decode(path.points, withElevation = true)
        } else emptyList()

        val instructions = path.instructions?.map { inst ->
            RouteInstruction(
                text = inst.text,
                distanceMeters = inst.distance,
                type = signToType(inst.sign)
            )
        } ?: emptyList()

        val (pathPct, cyclePct, roadPct) = estimateSurfaceRatios(preferences)

        return Result.success(
            Route(
                id = UUID.randomUUID().toString(),
                points = points,
                instructions = instructions,
                distanceMeters = path.distance,
                durationSeconds = path.time / 1000,
                elevationGain = path.ascend ?: 0.0,
                elevationLoss = path.descend ?: 0.0,
                difficulty = preferences.difficulty,
                pathPercentage = pathPct,
                cyclewayPercentage = cyclePct,
                roadPercentage = roadPct,
                isLoop = isLoop
            )
        )
    }

    private fun signToType(sign: Int): InstructionType = when (sign) {
        -3, -2 -> InstructionType.TURN_LEFT
        -1 -> InstructionType.SLIGHT_LEFT
        0 -> InstructionType.CONTINUE
        1 -> InstructionType.SLIGHT_RIGHT
        2, 3 -> InstructionType.TURN_RIGHT
        4, 5, 6 -> InstructionType.ROUNDABOUT
        7, 8 -> InstructionType.ARRIVE
        -98 -> InstructionType.UTURN
        else -> InstructionType.CONTINUE
    }

    private fun estimateSurfaceRatios(preferences: LoopPreferences): Triple<Float, Float, Float> {
        val pathPct = when (preferences.bikeType) {
            BikeType.MTB -> 0.55f + preferences.favorPaths * 0.3f
            BikeType.TREKKING -> 0.35f + preferences.favorPaths * 0.2f
            else -> 0.20f
        }.coerceAtMost(0.9f)
        val cyclePct = preferences.favorCycleways * 0.25f
        val roadPct = (1f - pathPct - cyclePct).coerceAtLeast(0.05f)
        return Triple(pathPct, cyclePct, roadPct)
    }

    private fun buildSnapPreventions(preferences: LoopPreferences): List<String> {
        val list = mutableListOf("ferry")
        if (preferences.avoidMainRoads) list += "motorway"
        return list
    }

    // ---------- Fallback local (sans clé API) ----------

    private suspend fun fallbackLoop(
        center: LatLng,
        preferences: LoopPreferences
    ): Result<Route> = withContext(Dispatchers.Default) {
        delay(600)
        val targetMeters = preferences.targetDistanceKm * 1000.0
        val points = generateLoopPoints(center, targetMeters, preferences)
        val distance = points.lastOrNull()?.distanceFromStart ?: 0.0
        val elev = estimateElevation(points, preferences)
        Result.success(
            Route(
                id = UUID.randomUUID().toString(),
                points = points,
                instructions = generateInstructions(points),
                distanceMeters = distance,
                durationSeconds = estimateDuration(distance, preferences),
                elevationGain = elev.first,
                elevationLoss = elev.second,
                difficulty = preferences.difficulty,
                pathPercentage = when (preferences.bikeType) {
                    BikeType.MTB -> 0.70f + preferences.favorPaths * 0.15f
                    BikeType.TREKKING -> 0.45f
                    else -> 0.25f
                }.coerceAtMost(0.95f),
                cyclewayPercentage = preferences.favorCycleways * 0.25f,
                roadPercentage = (1f - preferences.favorPaths).coerceAtLeast(0.05f),
                isLoop = true
            )
        )
    }

    private suspend fun fallbackPointToPoint(
        start: LatLng,
        end: LatLng,
        preferences: LoopPreferences
    ): Result<Route> = withContext(Dispatchers.Default) {
        val points = generateSimplePath(start, end, preferences)
        val distance = points.lastOrNull()?.distanceFromStart ?: 0.0
        val elev = estimateElevation(points, preferences)
        Result.success(
            Route(
                id = UUID.randomUUID().toString(),
                points = points,
                instructions = generateInstructions(points),
                distanceMeters = distance,
                durationSeconds = estimateDuration(distance, preferences),
                elevationGain = elev.first,
                elevationLoss = elev.second,
                difficulty = preferences.difficulty,
                pathPercentage = if (preferences.avoidPaved) 0.65f else 0.35f,
                cyclewayPercentage = 0.20f,
                roadPercentage = if (preferences.avoidMainRoads) 0.15f else 0.45f,
                isLoop = false
            )
        )
    }

    private fun generateLoopPoints(
        center: LatLng,
        targetMeters: Double,
        prefs: LoopPreferences
    ): List<RoutePoint> {
        val radiusMeters = targetMeters / (2 * PI)
        val pointsCount = max(24, (targetMeters / 200).toInt())
        val points = mutableListOf<RoutePoint>()
        var cumulative = 0.0
        val noise = when (prefs.difficulty) {
            Difficulty.EASY -> 0.08
            Difficulty.INTERMEDIATE -> 0.15
            Difficulty.HARD -> 0.22
            Difficulty.EXPERT -> 0.30
        }
        for (i in 0 until pointsCount) {
            val angle = 2 * PI * i / pointsCount
            val rFactor = 1.0 + noise * sin(angle * 3) + noise * 0.5 * cos(angle * 5)
            val r = radiusMeters * rFactor
            val lat = center.latitude + (r / 111320.0) * cos(angle)
            val lon = center.longitude + (r / (111320.0 * cos(Math.toRadians(center.latitude)))) * sin(angle)
            val elev = 150.0 + 80 * prefs.elevationPreference * sin(angle * 2) +
                    40 * prefs.elevationPreference * cos(angle * 4)
            if (points.isNotEmpty()) {
                val prev = points.last().latLng
                cumulative += haversine(prev.latitude, prev.longitude, lat, lon)
            }
            points.add(RoutePoint(LatLng(lat, lon), elev, cumulative))
        }
        if (points.isNotEmpty()) {
            val first = points.first()
            cumulative += haversine(
                points.last().latLng.latitude, points.last().latLng.longitude,
                first.latLng.latitude, first.latLng.longitude
            )
            points.add(RoutePoint(first.latLng, first.elevation, cumulative))
        }
        return points
    }

    private fun generateSimplePath(
        start: LatLng,
        end: LatLng,
        prefs: LoopPreferences
    ): List<RoutePoint> {
        val steps = 40
        val points = mutableListOf<RoutePoint>()
        var cum = 0.0
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val midLat = (start.latitude + end.latitude) / 2 + 0.002 * sin(t * PI)
            val midLon = (start.longitude + end.longitude) / 2 + 0.002 * cos(t * PI)
            val lat = (1 - t) * (1 - t) * start.latitude + 2 * (1 - t) * t * midLat + t * t * end.latitude
            val lon = (1 - t) * (1 - t) * start.longitude + 2 * (1 - t) * t * midLon + t * t * end.longitude
            val elev = 120.0 + 60 * prefs.elevationPreference * sin(t * PI * 2)
            if (points.isNotEmpty()) {
                val p = points.last().latLng
                cum += haversine(p.latitude, p.longitude, lat, lon)
            }
            points.add(RoutePoint(LatLng(lat, lon), elev, cum))
        }
        return points
    }

    private fun generateInstructions(points: List<RoutePoint>): List<RouteInstruction> {
        if (points.size < 3) return emptyList()
        val instructions = mutableListOf<RouteInstruction>()
        var lastIdx = 0
        for (i in 2 until points.size step max(1, points.size / 8)) {
            val dist = points[i].distanceFromStart - points[lastIdx].distanceFromStart
            val type = when ((i * 7) % 5) {
                0 -> InstructionType.TURN_LEFT
                1 -> InstructionType.TURN_RIGHT
                2 -> InstructionType.SLIGHT_LEFT
                3 -> InstructionType.SLIGHT_RIGHT
                else -> InstructionType.CONTINUE
            }
            val text = when (type) {
                InstructionType.TURN_LEFT -> "Tourner à gauche sur le chemin forestier"
                InstructionType.TURN_RIGHT -> "Tourner à droite sur la piste"
                InstructionType.SLIGHT_LEFT -> "Légèrement à gauche"
                InstructionType.SLIGHT_RIGHT -> "Légèrement à droite"
                else -> "Continuer tout droit"
            }
            instructions.add(RouteInstruction(text, dist, type = type))
            lastIdx = i
        }
        instructions.add(RouteInstruction("Arrivée", 0.0, type = InstructionType.ARRIVE))
        return instructions
    }

    private fun estimateElevation(points: List<RoutePoint>, prefs: LoopPreferences): Pair<Double, Double> {
        var gain = 0.0
        var loss = 0.0
        for (i in 1 until points.size) {
            val d = (points[i].elevation ?: 0.0) - (points[i - 1].elevation ?: 0.0)
            if (d > 0) gain += d else loss += -d
        }
        val factor = 0.6 + prefs.elevationPreference * 0.8
        return Pair(gain * factor, loss * factor)
    }

    private fun estimateDuration(distanceMeters: Double, prefs: LoopPreferences): Long {
        val speedKmh = when (prefs.bikeType) {
            BikeType.MTB -> when (prefs.difficulty) {
                Difficulty.EASY -> 14.0
                Difficulty.INTERMEDIATE -> 12.0
                Difficulty.HARD -> 10.0
                Difficulty.EXPERT -> 8.0
            }
            BikeType.TREKKING -> 16.0
            BikeType.LEISURE -> 15.0
            BikeType.ROAD -> 22.0
        }
        return ((distanceMeters / 1000.0) / speedKmh * 3600).toLong()
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }
}
