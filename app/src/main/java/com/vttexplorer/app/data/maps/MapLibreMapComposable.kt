package com.vttexplorer.app.data.maps

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * Carte MapLibre avec cycle de vie correct.
 * Style de démo OSM (pas de clé requise).
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }

    // S'assurer que MapLibre est initialisé
    LaunchedEffect(Unit) {
        try {
            MapLibre.getInstance(context.applicationContext)
        } catch (e: Exception) {
            mapError = e.message ?: "Erreur MapLibre"
        }
    }

    // Lier le cycle de vie Android ↔ MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef ?: return@LifecycleEventObserver
            try {
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                    else -> Unit
                }
            } catch (_: Exception) {
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapViewRef?.onDestroy()
            } catch (_: Exception) {
            }
            mapViewRef = null
            mapLibreMap = null
        }
    }

    if (mapError != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ComposeColor(0xFF1B5E20)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Carte indisponible\n${mapError}",
                color = ComposeColor.White
            )
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            try {
                MapLibre.getInstance(ctx.applicationContext)
                MapView(ctx).also { mv ->
                    mapViewRef = mv
                    mv.onCreate(null)
                    mv.onStart()
                    mv.onResume()
                    mv.getMapAsync { map ->
                        mapLibreMap = map
                        try {
                            map.setStyle(
                                Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
                            ) { style ->
                                try {
                                    if (style.getSource("route-source") == null) {
                                        style.addSource(GeoJsonSource("route-source"))
                                        style.addLayer(
                                            LineLayer("route-layer", "route-source").withProperties(
                                                PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                                                PropertyFactory.lineWidth(5f),
                                                PropertyFactory.lineOpacity(0.9f)
                                            )
                                        )
                                    }
                                } catch (_: Exception) {
                                }
                                onMapReady()
                            }
                            map.addOnCameraMoveListener {
                                val pos = map.cameraPosition.target ?: return@addOnCameraMoveListener
                                onCameraMove(
                                    LatLng(pos.latitude, pos.longitude),
                                    map.cameraPosition.zoom
                                )
                            }
                            // Position initiale France si pas de GPS
                            val target = userLocation ?: center ?: LatLng(46.6, 2.4)
                            map.cameraPosition = CameraPosition.Builder()
                                .target(MLLatLng(target.latitude, target.longitude))
                                .zoom(6.0)
                                .build()
                        } catch (e: Exception) {
                            mapError = e.message
                        }
                    }
                }
            } catch (e: Exception) {
                mapError = e.message ?: "Impossible de créer la carte"
                // Vue vide de secours
                android.widget.FrameLayout(ctx)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { _ ->
            val map = mapLibreMap ?: return@AndroidView
            try {
                val c = center ?: userLocation
                if (c != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(MLLatLng(c.latitude, c.longitude))
                                .zoom(14.0)
                                .build()
                        )
                    )
                }
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
            } catch (_: Exception) {
            }
        }
    )
}
