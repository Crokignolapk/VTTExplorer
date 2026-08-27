package com.vttexplorer.app.data.routing.graphhopper

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit pour l'API GraphHopper Directions (POST /route).
 * Documentation : https://docs.graphhopper.com/#tag/Routing-API
 */
interface GraphHopperApi {

    @POST("route")
    suspend fun route(
        @Query("key") apiKey: String,
        @Body request: GhRouteRequest
    ): GhRouteResponse
}

@JsonClass(generateAdapter = true)
data class GhRouteRequest(
    val points: List<List<Double>>,           // [[lon, lat], ...]
    val profile: String = "mtb",
    @Json(name = "points_encoded") val pointsEncoded: Boolean = true,
    val elevation: Boolean = true,
    val instructions: Boolean = true,
    val locale: String = "fr",
    @Json(name = "ch.disable") val chDisable: Boolean = true,
    val algorithm: String? = null,            // "round_trip" pour boucles
    @Json(name = "round_trip.distance") val roundTripDistance: Int? = null,
    @Json(name = "round_trip.seed") val roundTripSeed: Long? = null,
    val details: List<String>? = listOf("road_class", "surface", "track_type"),
    @Json(name = "custom_model") val customModel: GhCustomModel? = null,
    @Json(name = "snap_preventions") val snapPreventions: List<String>? = listOf("motorway", "ferry")
)

@JsonClass(generateAdapter = true)
data class GhCustomModel(
    val priority: List<GhPriorityRule>? = null,
    val speed: List<GhSpeedRule>? = null,
    @Json(name = "distance_influence") val distanceInfluence: Double? = null
)

@JsonClass(generateAdapter = true)
data class GhPriorityRule(
    @Json(name = "if") val condition: String,
    @Json(name = "multiply_by") val multiplyBy: String
)

@JsonClass(generateAdapter = true)
data class GhSpeedRule(
    @Json(name = "if") val condition: String? = null,
    @Json(name = "else") val elseCondition: String? = null,
    @Json(name = "limit_to") val limitTo: String? = null,
    @Json(name = "multiply_by") val multiplyBy: String? = null
)

@JsonClass(generateAdapter = true)
data class GhRouteResponse(
    val paths: List<GhPath>?,
    val message: String? = null,
    val hints: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GhPath(
    val distance: Double,                     // mètres
    val time: Long,                           // millisecondes
    val ascend: Double? = null,
    val descend: Double? = null,
    val points: String? = null,               // polyline encodée (si points_encoded=true)
    @Json(name = "points_encoded") val pointsEncoded: Boolean? = true,
    val instructions: List<GhInstruction>? = null,
    val details: Map<String, List<List<Any>>>? = null,
    val bbox: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class GhInstruction(
    val text: String,
    val distance: Double,
    val time: Long,
    val interval: List<Int>,
    val sign: Int,                            // -3..8 (voir docs GraphHopper)
    @Json(name = "street_name") val streetName: String? = null
)
