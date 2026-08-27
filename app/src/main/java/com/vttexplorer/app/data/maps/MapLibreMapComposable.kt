package com.vttexplorer.app.data.maps

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

/** Styles testés sans clé API */
private val STYLE_URLS = listOf(
    "https://tiles.openfreemap.org/styles/liberty",
    "https://demotiles.maplibre.org/style.json"
)

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
    var loading by remember { mutableStateOf(true) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Dernière position / route pour l'update
    val lastCenter = remember { mutableStateOf<LatLng?>(null) }
    val lastRouteId = remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef ?: return@LifecycleEventObserver
            try {
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    else -> Unit
                }
            } catch (_: Exception) {
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapViewRef?.onPause()
                mapViewRef?.onStop()
                mapViewRef?.onDestroy()
            } catch (_: Exception) {
            }
            mapViewRef = null
            mapLibreMap = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    try {
                        MapLibre.getInstance(ctx.applicationContext)
                        val mv = MapView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        mapViewRef = mv
                        addView(mv)
                        mv.onCreate(null)
                        mv.onStart()
                        mv.onResume()
                        mv.getMapAsync { map ->
                            mapLibreMap = map
                            map.uiSettings.isAttributionEnabled = true
                            map.uiSettings.isLogoEnabled = true
                            map.uiSettings.setCompassEnabled(true)

                            fun applyStyle(index: Int) {
                                if (index >= STYLE_URLS.size) {
                                    mainHandler.post {
                                        loading = false
                                        mapError = "Impossible de charger le fond de carte.\nVérifiez Internet."
                                    }
                                    return
                                }
                                val url = STYLE_URLS[index]
                                map.setStyle(url) { style ->
                                    try {
                                        if (style.getSource("route-source") == null) {
                                            style.addSource(GeoJsonSource("route-source"))
                                            style.addLayer(
                                                LineLayer("route-layer", "route-source").withProperties(
                                                    PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                                                    PropertyFactory.lineWidth(6f),
                                                    PropertyFactory.lineOpacity(0.95f)
                                                )
                                            )
                                        }
                                    } catch (_: Exception) {
                                    }
                                    val target = userLocation ?: center ?: LatLng(46.603354, 1.888334)
                                    val zoom = if (userLocation != null) 13.0 else 5.5
                                    map.cameraPosition = CameraPosition.Builder()
                                        .target(MLLatLng(target.latitude, target.longitude))
                                        .zoom(zoom)
                                        .build()
                                    mainHandler.post {
                                        loading = false
                                        mapError = null
                                    }
                                    onMapReady()
                                }
                                // Si le style ne charge pas en 8s, essayer le suivant
                                mainHandler.postDelayed({
                                    if (loading && map.style == null) {
                                        applyStyle(index + 1)
                                    }
                                }, 8000)
                            }
                            applyStyle(0)

                            map.addOnCameraMoveListener {
                                val pos = map.cameraPosition.target ?: return@addOnCameraMoveListener
                                onCameraMove(LatLng(pos.latitude, pos.longitude), map.cameraPosition.zoom)
                            }
                        }
                    } catch (e: Exception) {
                        mapError = e.message ?: "Erreur carte"
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                val map = mapLibreMap ?: return@AndroidView
                try {
                    val c = userLocation ?: center
                    if (c != null && c != lastCenter.value) {
                        lastCenter.value = c
                        map.easeCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                MLLatLng(c.latitude, c.longitude),
                                14.0
                            ),
                            700
                        )
                    }
                    val routeKey = route?.points?.size?.toString() + "_" + route?.distanceMeters
                    if (route != null && route.points.isNotEmpty() && routeKey != lastRouteId.value) {
                        lastRouteId.value = routeKey
                        val coords = route.points.map {
                            Point.fromLngLat(it.latLng.longitude, it.latLng.latitude)
                        }
                        map.style?.getSourceAs<GeoJsonSource>("route-source")
                            ?.setGeoJson(
                                FeatureCollection.fromFeature(
                                    Feature.fromGeometry(LineString.fromLngLats(coords))
                                )
                            )
                    }
                } catch (_: Exception) {
                }
            }
        )

        if (loading) {
            Box(
                Modifier.fillMaxSize().background(ComposeColor(0xFF1B5E20).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ComposeColor.White)
            }
        }
        if (mapError != null) {
            Box(
                Modifier.fillMaxSize().background(ComposeColor(0xFF1B5E20)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mapError!!,
                    color = ComposeColor.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}
