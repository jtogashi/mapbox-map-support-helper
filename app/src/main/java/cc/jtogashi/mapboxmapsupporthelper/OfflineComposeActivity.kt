package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.OfflineSwitch
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.mapsOptions
import com.mapbox.maps.plugin.animation.flyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow

class OfflineComposeActivity : ComponentActivity() {
    private val offlineMapStateFlow = MutableStateFlow(OfflineMapState.NOT_LOADED)

    private val tileStore = MapboxOptions.mapsOptions.tileStore!!
    private val offlineManager = OfflineManager()
    private val downloadCancelables = mutableListOf<Cancelable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val offlineMapState by offlineMapStateFlow.collectAsState()

            MapboxMap(
                Modifier.windowInsetsPadding(WindowInsets.systemBars),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        zoom(22.0)
                        center(Point.fromLngLat(0.0, 0.0))
                    }
                },
                style = {
                    MapStyle(style = Style.SATELLITE)
                }
            ) {
                MapEffect { mapView ->
                    offlineMapStateFlow.collect { state ->
                        when (state) {
                            OfflineMapState.NOT_LOADED -> {
                                // NOP
                            }

                            OfflineMapState.DOWNLOADING -> {
                                OfflineSwitch.getInstance().isMapboxStackConnected = true

                                downloadStylePack(this@MapEffect) {
                                    downloadTileRegion(this@MapEffect) {
                                        // disconnect Mapbox stack from the network to ensure offline access
                                        OfflineSwitch.getInstance().isMapboxStackConnected = false
                                        offlineMapStateFlow.value = OfflineMapState.DOWNLOADED
                                    }
                                }
                            }

                            OfflineMapState.DOWNLOADED -> {
                                mapView.mapboxMap.flyTo(
                                    cameraOptions {
                                        center(HELSINKI)
                                        zoom(10.0)
                                    }
                                )
                            }
                        }
                    }
                }

                CircleAnnotation(HELSINKI) {
                    circleColor = Color.Yellow
                    circleRadius = 10.0
                }
            }

            Column(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Button(
                    enabled = offlineMapState == OfflineMapState.NOT_LOADED,
                    onClick = {
                        when (offlineMapState) {
                            OfflineMapState.NOT_LOADED -> {
                                offlineMapStateFlow.value = OfflineMapState.DOWNLOADING
                            }

                            OfflineMapState.DOWNLOADING,
                            OfflineMapState.DOWNLOADED -> {
                                // NOP
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                        when (offlineMapState) {
                            OfflineMapState.NOT_LOADED -> "download"
                            OfflineMapState.DOWNLOADING -> "downloading..."
                            OfflineMapState.DOWNLOADED -> "downloaded"
                        }
                    )
                }

                Button(
                    enabled = offlineMapState != OfflineMapState.NOT_LOADED,
                    onClick = {
                        when (offlineMapState) {
                            OfflineMapState.NOT_LOADED -> {
                                // NOP
                            }

                            OfflineMapState.DOWNLOADING -> {
                                downloadCancelables.forEach { it.cancel() }
                                offlineMapStateFlow.value = OfflineMapState.NOT_LOADED
                                OfflineSwitch.getInstance().isMapboxStackConnected = false
                            }

                            OfflineMapState.DOWNLOADED -> {
                                deleteOfflineMap()
                                offlineMapStateFlow.value = OfflineMapState.NOT_LOADED
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                        when (offlineMapState) {
                            OfflineMapState.NOT_LOADED,
                            OfflineMapState.DOWNLOADED -> "delete"

                            OfflineMapState.DOWNLOADING -> "cancel"
                        }
                    )
                }
            }
        }
    }

    private fun downloadStylePack(
        coroutineScope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        downloadCancelables.add(
            offlineManager.loadStylePack(
                Style.SATELLITE,
                StylePackLoadOptions.Builder()
                    .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                    .metadata(Value(STYLE_PACK_SATELLITE_STREET_METADATA))
                    .build(),
                { progress ->
                    coroutineScope.ensureActive()
                    Log.i(
                        TAG,
                        "progress counts: ${progress.completedResourceCount} / ${progress.requiredResourceCount} completed"
                    )
                    Log.i(
                        TAG,
                        "progress size: ${progress.completedResourceSize} completed, ${progress.loadedResourceSize} loaded"
                    )
                },
                { stylePackExpected ->
                    if (stylePackExpected.isError) {
                        Log.e(TAG, "error: ${stylePackExpected.error}")
                    }

                    if (stylePackExpected.isValue) {
                        coroutineScope.ensureActive()
                        onComplete()
                    }
                }
            )
        )
    }

    private fun downloadTileRegion(
        coroutineScope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(Style.SATELLITE)
                .pixelRatio(2f)
                .minZoom(10)
                .maxZoom(16)
                .build()
        )

        downloadCancelables.add(
            tileStore.loadTileRegion(
                TILE_REGION_ID,
                TileRegionLoadOptions.Builder()
                    .geometry(HELSINKI)
                    .descriptors(listOf(tilesetDescriptor))
                    .metadata(Value(TILE_REGION_METADATA))
                    .acceptExpired(true)
                    .networkRestriction(NetworkRestriction.NONE)
                    .build(),
                { progress ->
                    coroutineScope.ensureActive()
                    Log.i(
                        TAG,
                        "progress counts: ${progress.completedResourceCount} / ${progress.requiredResourceCount} completed"
                    )
                    Log.i(
                        TAG,
                        "progress size: ${progress.completedResourceSize} completed, ${progress.loadedResourceSize} loaded"
                    )
                },
                { tileRegionExpected ->
                    if (tileRegionExpected.isError) {
                        Log.e(TAG, "error: ${tileRegionExpected.error}")
                    }

                    if (tileRegionExpected.isValue) {
                        coroutineScope.ensureActive()
                        onComplete()
                    }
                }
            )
        )
    }

    private fun deleteOfflineMap() {
        tileStore.removeTileRegion(TILE_REGION_ID)
        offlineManager.removeStylePack(Style.SATELLITE)
        MapboxMap.clearData {
            if (it.isError) {
                Log.e(TAG, "error in clearData: ${it.error}")
            }
        }
        tileStore.clearAmbientCache {
            if (it.isError) {
                Log.e(TAG, "error in clearAmbientCache: ${it.error}")
            }
        }
    }

    private enum class OfflineMapState {
        NOT_LOADED,
        DOWNLOADING,
        DOWNLOADED
    }

    companion object {
        private val HELSINKI: Point = Point.fromLngLat(24.941526986277893, 60.17099952463323)
        private const val TAG = "OFFLINECOMPOSE"
        private const val STYLE_PACK_SATELLITE_STREET_METADATA = "my-satellite-street-style-pack"
        private const val TILE_REGION_ID = "myTileRegion"
        private const val TILE_REGION_METADATA = "my-offline-region"
    }
}