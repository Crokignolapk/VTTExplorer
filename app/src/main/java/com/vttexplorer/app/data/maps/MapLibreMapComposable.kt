package com.vttexplorer.app.data.maps

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.vttexplorer.app.domain.model.LatLng
import com.vttexplorer.app.domain.model.Route
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MLLatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Composable MapLibre.
 * Style OSM libre (pas de clé obligatoire pour le style de démonstration).
 * Pour production, utiliser un style MapTiler / OpenMapTiles avec clé.
 */
@Composable
fun MapLibreMapComposable(
    modifier: Modifier = Modifier,
    center: LatLng?,
    userLocation: LatLng?,
    userBearing: Float?,
    route: Route?,
    onMapReady: () -> Unit = {},
    onCameraMove: (LatLng, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Init MapLibre once
    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { mv ->
                mapView = mv
                mv.onCreate(null)
                mv.getMapAsync { map ->
                    mapLibreMap = map
                    // Style OSM libre (OpenFreeMap / demo)
                    map.setStyle(
                        Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
                    ) { style ->
                        // Source pour l'itinéraire
                        style.addSource(GeoJsonSource("route-source"))
                        style.addLayer(
                            LineLayer("route-layer", "route-source").withProperties(
                                PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                                PropertyFactory.lineWidth(5f),
                                PropertyFactory.lineOpacity(0.9f)
                            )
                        )
                        onMapReady()
                    }

                    map.addOnCameraMoveListener {
                        val pos = map.cameraPosition.target
                        onCameraMove(
                            LatLng(pos.latitude, pos.longitude),
                            map.cameraPosition.zoom
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { mv ->
            val map = mapLibreMap ?: return@AndroidView

            // Centre
            center?.let { c ->
                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(MLLatLng(c.latitude, c.longitude))
                            .zoom(14.0)
                            .build()
                    )
                )
            }

            // Itinéraire
            route?.let { r ->
                if (r.points.isNotEmpty()) {
                    val coords = r.points.map {
                        Point.fromLngLat(it.latLng.longitude, it.latLng.latitude)
                    }
                    val line = LineString.fromLngLats(coords)
                    val feature = Feature.fromGeometry(line)
                    map.style?.getSourceAs<GeoJsonSource>("route-source")
                        ?.setGeoJson(FeatureCollection.fromFeature(feature))
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }

    // Lifecycle handling simplified – in production use MapView lifecycle methods properly
    // via a LifecycleObserver.
}
