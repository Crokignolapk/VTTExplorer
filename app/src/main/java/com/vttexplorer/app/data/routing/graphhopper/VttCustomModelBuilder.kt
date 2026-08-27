package com.vttexplorer.app.data.routing.graphhopper

import com.vttexplorer.app.domain.model.BikeType
import com.vttexplorer.app.domain.model.Difficulty
import com.vttexplorer.app.domain.model.LoopPreferences
import kotlin.math.roundToInt

/**
 * Custom model GraphHopper optimisé pour le VTT.
 *
 * Philosophie des poids (multiply_by) :
 *   0.00          → interdit (jamais emprunté)
 *   0.05 – 0.15   → très fortement évité
 *   0.25 – 0.45   → pénalité nette
 *   0.60 – 0.85   → léger désavantage
 *   1.00          → neutre
 *   1.15 – 1.40   → léger avantage
 *   1.50 – 2.20   → fort avantage (chemins / tracks)
 *   > 2.50        → évité (risque de routes absurdes)
 *
 * Les curseurs utilisateur (0..1) interpolent linéairement entre
 * un poids « faible » et un poids « fort » pour un réglage fluide.
 *
 * Ordre des règles : du plus restrictif au plus permissif.
 * GraphHopper applique toutes les règles qui matchent (produit des multiply_by).
 */
object VttCustomModelBuilder {

    // -------------------------------------------------------------------------
    // Poids de référence (constantes calibrées)
    // -------------------------------------------------------------------------

    private object W {
        // Accès
        const val FORBIDDEN = "0"

        // Routes motorisées
        const val MOTORWAY_TRUNK = "0"
        const val PRIMARY_HARD = 0.08
        const val PRIMARY_SOFT = 0.35
        const val SECONDARY_HARD = 0.22
        const val SECONDARY_SOFT = 0.55
        const val TERTIARY_HARD = 0.45
        const val TERTIARY_SOFT = 0.80

        // Chemins & tracks
        const val PATH_MIN = 1.15
        const val PATH_MAX = 2.10
        const val TRACK_GRADE12_MIN = 1.25
        const val TRACK_GRADE12_MAX = 2.20
        const val TRACK_GRADE3_MIN = 1.05
        const val TRACK_GRADE3_MAX = 1.70
        const val TRACK_GRADE45_MIN = 0.70
        const val TRACK_GRADE45_MAX = 1.35

        // Pistes cyclables
        const val CYCLEWAY_MIN = 1.10
        const val CYCLEWAY_MAX = 1.85
        const val BIKE_NET_NAT_MIN = 1.15
        const val BIKE_NET_NAT_MAX = 1.70
        const val BIKE_NET_LOC_MIN = 1.05
        const val BIKE_NET_LOC_MAX = 1.40

        // Surfaces
        const val PAVED_HARD = 0.12
        const val PAVED_SOFT = 0.55
        const val UNPAVED_MIN = 1.20
        const val UNPAVED_MAX = 1.90
        const val GRAVEL_MIN = 1.15
        const val GRAVEL_MAX = 1.75

        // Routes résidentielles (compromis quand on évite les grands axes)
        const val RESIDENTIAL_BOOST = 1.12
        const val UNCLASSIFIED_BOOST = 1.08
    }

    fun build(preferences: LoopPreferences): GhCustomModel {
        val priority = mutableListOf<GhPriorityRule>()
        val p = preferences.favorPaths.coerceIn(0f, 1f)
        val c = preferences.favorCycleways.coerceIn(0f, 1f)
        val elev = preferences.elevationPreference.coerceIn(0f, 1f)

        // =====================================================
        // 1. INTERDICTIONS STRICTES (toujours)
        // =====================================================
        priority += rule("bike_road_access == NO", W.FORBIDDEN)
        priority += rule("!bike_access && !backward_bike_access", W.FORBIDDEN)
        priority += rule("road_class == MOTORWAY || road_class == TRUNK", W.MOTORWAY_TRUNK)
        // Escaliers : jamais en vélo
        priority += rule("road_class == STEPS", W.FORBIDDEN)

        // =====================================================
        // 2. GRANDS AXES (selon option)
        // =====================================================
        if (preferences.avoidMainRoads) {
            priority += rule("road_class == PRIMARY", lerp(W.PRIMARY_HARD, W.PRIMARY_SOFT, 1f - p))
            priority += rule("road_class == SECONDARY", lerp(W.SECONDARY_HARD, W.SECONDARY_SOFT, 1f - p))
            priority += rule("road_class == TERTIARY", lerp(W.TERTIARY_HARD, W.TERTIARY_SOFT, 1f - p * 0.7f))
            // Légère préférence pour les petites voies quand on fuit les grands axes
            priority += rule("road_class == RESIDENTIAL", W.RESIDENTIAL_BOOST.toString())
            priority += rule("road_class == UNCLASSIFIED", W.UNCLASSIFIED_BOOST.toString())
        } else {
            // Même sans l'option, on garde une légère méfiance envers les PRIMARY
            priority += rule("road_class == PRIMARY", "0.70")
        }

        // =====================================================
        // 3. CHEMINS / TRACKS / SENTIERS  (cœur VTT)
        // =====================================================
        // PATH + FOOTWAY : sentiers (attention : footway n'est pas toujours VTT-ok)
        priority += rule(
            "road_class == PATH",
            lerp(W.PATH_MIN, W.PATH_MAX, p)
        )
        // FOOTWAY : boost plus modéré (souvent piétonnier urbain)
        priority += rule(
            "road_class == FOOTWAY",
            lerp(1.05, 1.45, p * 0.6f)
        )
        // TRACK générique
        priority += rule(
            "road_class == TRACK",
            lerp(1.20, 1.95, p)
        )
        // tracktype grade1–2 : excellents pour VTT
        priority += rule(
            "track_type == GRADE1 || track_type == GRADE2",
            lerp(W.TRACK_GRADE12_MIN, W.TRACK_GRADE12_MAX, p)
        )
        // grade3 : correct
        priority += rule(
            "track_type == GRADE3",
            lerp(W.TRACK_GRADE3_MIN, W.TRACK_GRADE3_MAX, p)
        )
        // grade4–5 : plus techniques / dégradés
        priority += rule(
            "track_type == GRADE4 || track_type == GRADE5",
            lerp(W.TRACK_GRADE45_MIN, W.TRACK_GRADE45_MAX, p)
        )

        // =====================================================
        // 4. PISTES CYCLABLES & RÉSEAU VÉLO
        // =====================================================
        priority += rule(
            "road_class == CYCLEWAY",
            lerp(W.CYCLEWAY_MIN, W.CYCLEWAY_MAX, c)
        )
        priority += rule(
            "bike_network == INTERNATIONAL || bike_network == NATIONAL",
            lerp(W.BIKE_NET_NAT_MIN, W.BIKE_NET_NAT_MAX, c)
        )
        priority += rule(
            "bike_network == REGIONAL || bike_network == LOCAL",
            lerp(W.BIKE_NET_LOC_MIN, W.BIKE_NET_LOC_MAX, c)
        )

        // =====================================================
        // 5. SURFACES
        // =====================================================
        if (preferences.avoidPaved) {
            // Goudron fortement pénalisé (mais pas à 0 pour ne pas bloquer des zones)
            priority += rule(
                "surface == ASPHALT || surface == CONCRETE || surface == PAVING_STONES || surface == PAVED",
                lerp(W.PAVED_HARD, W.PAVED_SOFT, 1f - p)
            )
            // Surfaces VTT idéales
            priority += rule(
                "surface == DIRT || surface == GROUND || surface == EARTH || surface == UNPAVED",
                lerp(W.UNPAVED_MIN, W.UNPAVED_MAX, p)
            )
            priority += rule(
                "surface == GRAVEL || surface == COMPACTED || surface == FINE_GRAVEL",
                lerp(W.GRAVEL_MIN, W.GRAVEL_MAX, p)
            )
            // Surfaces médiocres mais acceptables
            priority += rule(
                "surface == GRASS || surface == SAND || surface == MUD",
                lerp(0.85, 1.25, p * 0.5f)
            )
        } else {
            // Même sans option, léger bonus unpaved pour le feeling VTT
            if (preferences.bikeType == BikeType.MTB) {
                priority += rule(
                    "surface == DIRT || surface == GROUND || surface == GRAVEL || surface == UNPAVED",
                    "1.15"
                )
            }
        }

        // =====================================================
        // 6. DIFFICULTÉ MTB (mtb:scale → mtb_rating)
        //    mtb_rating : 0=missing, 1=scale0, 2=scale1, … 7=scale6
        // =====================================================
        val maxRating = when (preferences.difficulty) {
            Difficulty.EASY -> 2          // scale ≤ 0–1
            Difficulty.INTERMEDIATE -> 3  // scale ≤ 2
            Difficulty.HARD -> 5          // scale ≤ 4
            Difficulty.EXPERT -> 7        // tout
        }
        // Interdire au-delà du plafond (sauf Expert + allowHardSections)
        if (!(preferences.difficulty == Difficulty.EXPERT && preferences.allowHardSections)) {
            priority += rule("mtb_rating > $maxRating", W.FORBIDDEN)
        }
        // Légère préférence pour les segments dans la zone de confort
        // (évite de toujours prendre le plus facile quand on a choisi Difficile)
        when (preferences.difficulty) {
            Difficulty.HARD -> {
                priority += rule("mtb_rating == 3 || mtb_rating == 4", "1.20")
            }
            Difficulty.EXPERT -> {
                priority += rule("mtb_rating >= 4", "1.25")
            }
            else -> { /* pas de boost particulier */ }
        }

        // Sentiers pédestres très techniques (sac_scale)
        when (preferences.difficulty) {
            Difficulty.EASY -> priority += rule("hike_rating > 1", W.FORBIDDEN)
            Difficulty.INTERMEDIATE -> priority += rule("hike_rating > 2", W.FORBIDDEN)
            Difficulty.HARD -> priority += rule("hike_rating > 3", "0.15")
            Difficulty.EXPERT -> priority += rule("hike_rating > 4", "0.30")
        }

        // =====================================================
        // 7. VITESSE (influence le temps, donc le choix d'itinéraire)
        // =====================================================
        val speed = mutableListOf<GhSpeedRule>()

        // Surfaces lentes
        speed += GhSpeedRule(
            condition = "surface == GRAVEL || surface == COMPACTED || surface == FINE_GRAVEL",
            multiplyBy = "0.90"
        )
        speed += GhSpeedRule(
            condition = "surface == DIRT || surface == GROUND || surface == EARTH || surface == UNPAVED",
            multiplyBy = "0.80"
        )
        speed += GhSpeedRule(
            condition = "surface == GRASS || surface == SAND || surface == MUD",
            multiplyBy = "0.55"
        )
        // Tracks dégradés
        speed += GhSpeedRule(
            condition = "track_type == GRADE3",
            multiplyBy = "0.75"
        )
        speed += GhSpeedRule(
            condition = "track_type == GRADE4 || track_type == GRADE5",
            multiplyBy = "0.55"
        )
        // Segments MTB techniques : plus lents
        speed += GhSpeedRule(
            condition = "mtb_rating >= 4",
            multiplyBy = "0.65"
        )
        speed += GhSpeedRule(
            condition = "mtb_rating >= 6",
            multiplyBy = "0.40"
        )

        // =====================================================
        // 8. DISTANCE_INFLUENCE
        //    Plus élevé → routes plus courtes privilégiées
        //    Plus bas   → on accepte des détours pour de meilleurs chemins
        // =====================================================
        val distanceInfluence = when {
            // VTT pur : on accepte des détours pour rester sur les chemins
            preferences.bikeType == BikeType.MTB && p > 0.65f -> 35.0 + (1f - p) * 40.0
            // Gros dénivelé souhaité : un peu plus de liberté
            elev > 0.7f -> 40.0
            // Mode plus « efficace »
            p < 0.3f && c < 0.3f -> 110.0
            else -> 70.0
        }

        return GhCustomModel(
            priority = priority,
            speed = speed,
            distanceInfluence = (distanceInfluence * 10).roundToInt() / 10.0
        )
    }

    /** Profil GraphHopper selon le type de vélo */
    fun profileFor(bikeType: BikeType): String = when (bikeType) {
        BikeType.MTB -> "mtb"
        BikeType.TREKKING, BikeType.LEISURE -> "bike"
        BikeType.ROAD -> "racingbike"
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun rule(condition: String, multiplyBy: String) =
        GhPriorityRule(condition, multiplyBy)

    private fun rule(condition: String, multiplyBy: Double) =
        GhPriorityRule(condition, format(multiplyBy))

    /** Interpolation linéaire entre lo et hi selon t ∈ [0,1] */
    private fun lerp(lo: Double, hi: Double, t: Float): Double {
        val x = t.coerceIn(0f, 1f).toDouble()
        return lo + (hi - lo) * x
    }

    private fun format(v: Double): String = "%.2f".format(v)
}
