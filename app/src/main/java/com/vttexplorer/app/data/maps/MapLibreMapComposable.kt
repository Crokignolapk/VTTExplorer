package com.vttexplorer.app.data.maps

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

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
    var styleLoaded by remember { mutableStateOf(false) }

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

    if (mapError != null) {
        Box(
            modifier = modifier.fillMaxSize().background(ComposeColor(0xFF1B5E20)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Carte indisponible\n$mapError\n\nVérifiez Internet.",
                color = ComposeColor.White,
                fontSize = 14.sp
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
                        // Style raster OSM via tuiles publiques (Carto Voyager – libre)
                        val tileSet = TileSet(
                            "2.2.0",
                            "https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png",
                            "https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png",
                            "https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"
                        ).apply {
                            setMaxZoom(20f)
                        }
                        map.setStyle(
                            Style.Builder()
                                .withSource(RasterSource("carto", tileSet, 256))
                                .withLayer(RasterLayer("carto-layer", "carto"))
                        ) { style ->
                            try {
                                if (style.getSource("route-source") == null) {
                                    style.addSource(GeoJsonSource("route-source"))
                                    style.addLayer(
                                        LineLayer("route-layer", "route-source").withProperties(
                                            PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                                            PropertyFactory.lineWidth(5f),
                                            PropertyFactory.lineOpacity(0.95f)
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                            }
                            styleLoaded = true
                            onMapReady()
                        }
                        map.addOnCameraMoveListener {
                            val pos = map.cameraPosition.target ?: return@addOnCameraMoveListener
                            onCameraMove(LatLng(pos.latitude, pos.longitude), map.cameraPosition.zoom)
                        }
                        val target = userLocation ?: center ?: LatLng(46.603354, 1.888334)
                        val zoom = if (userLocation != null) 14.0 else 5.5
                        map.cameraPosition = CameraPosition.Builder()
                            .target(MLLatLng(target.latitude, target.longitude))
                            .zoom(zoom)
                            .build()
                    }
                }
            } catch (e: Exception) {
                mapError = e.message ?: "Impossible de créer la carte"
                android.widget.FrameLayout(ctx)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { _ ->
            val map = mapLibreMap ?: return@AndroidView
            if (!styleLoaded) return@AndroidView
            try {
                val c = userLocation ?: center
                if (c != null) {
                    map.easeCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(MLLatLng(c.latitude, c.longitude))
                                .zoom(14.0)
                                .build()
                        ),
                        600
                    )
                }
                route?.let { r ->
                    if (r.points.isNotEmpty()) {
                        val coords = r.points.map {
                            Point.fromLngLat(it.latLng.longitude, it.latLng.latitude)
                        }
                        map.style?.getSourceAs<GeoJsonSource>("route-source")
                            ?.setGeoJson(
                                FeatureCollection.fromFeature(
                                    Feature.fromGeometry(LineString.fromLngLats(coords))
                                )
                            )
                    }
                }
            } catch (_: Exception) {
            }
        }
    )
}
