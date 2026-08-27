package com.vttexplorer.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class LocationState(
    val position: LatLng? = null,
    val accuracy: Float = 0f,
    val bearing: Float? = null,
    val speed: Float = 0f, // m/s
    val altitude: Double? = null,
    val isGpsAvailable: Boolean = false
)

enum class Difficulty { EASY, INTERMEDIATE, HARD, EXPERT }

enum class BikeType { MTB, TREKKING, LEISURE, ROAD }

data class LoopPreferences(
    val targetDistanceKm: Float = 20f,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val bikeType: BikeType = BikeType.MTB,
    val favorPaths: Float = 0.8f,          // 0..1
    val favorCycleways: Float = 0.7f,
    val favorSecondaryRoads: Float = 0.3f,
    val elevationPreference: Float = 0.5f, // 0 = flat, 1 = hilly
    val avoidMainRoads: Boolean = true,
    val avoidPaved: Boolean = true,
    val allowHardSections: Boolean = false
)

data class RoutePoint(
    val latLng: LatLng,
    val elevation: Double? = null,
    val distanceFromStart: Double = 0.0 // meters
)

data class RouteInstruction(
    val text: String,
    val distanceMeters: Double,
    val bearing: Float? = null,
    val type: InstructionType = InstructionType.CONTINUE
)

enum class InstructionType {
    TURN_LEFT, TURN_RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT,
    CONTINUE, ARRIVE, ROUNDABOUT, UTURN
}

data class Route(
    val id: String,
    val points: List<RoutePoint>,
    val instructions: List<RouteInstruction> = emptyList(),
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGain: Double,
    val elevationLoss: Double = 0.0,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val pathPercentage: Float = 0f,
    val cyclewayPercentage: Float = 0f,
    val roadPercentage: Float = 0f,
    val isLoop: Boolean = false
)

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateMillis: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGain: Double,
    val averageSpeed: Float,
    val gpxPath: String?,
    val thumbnailPath: String? = null
)

data class RideSummary(
    val id: Long,
    val name: String,
    val dateMillis: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGain: Double
)
