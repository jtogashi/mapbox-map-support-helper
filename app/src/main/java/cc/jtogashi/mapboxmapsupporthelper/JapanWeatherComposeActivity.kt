package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedRasterQueryOptions
import com.mapbox.maps.SourceDataLoadedType
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.coroutine.sourceDataLoadedEvents
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleRangeValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.LongValue
import com.mapbox.maps.extension.compose.style.StringValue
import com.mapbox.maps.extension.compose.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.compose.style.layers.generated.RasterResamplingValue
import com.mapbox.maps.extension.compose.style.projection.generated.Projection
import com.mapbox.maps.extension.compose.style.sources.generated.rememberRasterArraySourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.StandardStyleState
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.sources.generated.RasterArraySource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class)
class JapanWeatherComposeActivity : ComponentActivity() {
    private val httpClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val temperatureSourceState = rememberRasterArraySourceState(
                sourceId = SOURCE_ID_TEMP_0_39
            ) {
                url = StringValue("mapbox://$TILESET_ID_TEMP_0_39")
                tileSize = LongValue(256)
            }

            val mapState = rememberMapState()

            var bands by remember { mutableStateOf(listOf<String>()) }
            var title by remember { mutableStateOf("Temperature") }
            var sliderPosition by remember { mutableFloatStateOf(0f) }

            var rasterValue by remember { mutableFloatStateOf(Float.MIN_VALUE) }
            var rasterValuePoint by remember { mutableStateOf<Point?>(null) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MapboxMap(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        mapViewportState = rememberMapViewportState {
                            setCameraOptions {
                                zoom(4.0)
                                center(JAPAN)
                            }
                        },
                        mapState = mapState,
                        style = {
                            MapboxStandardStyle(
                                standardStyleState = remember {
                                    StandardStyleState().apply {
                                        projection = Projection.MERCATOR

                                        configurationsState.apply {
                                            showPedestrianRoads = BooleanValue(false)
                                            showPointOfInterestLabels = BooleanValue(false)
                                            showRoadLabels = BooleanValue(false)
                                            showTransitLabels = BooleanValue(false)
                                            show3dObjects = BooleanValue(false)
                                            show3dBuildings = BooleanValue(false)
                                            show3dTrees = BooleanValue(false)
                                            show3dLandmarks = BooleanValue(false)
                                            showLandmarkIconLabels = BooleanValue(false)
                                        }

                                        interactionsState.onMapClicked { interactionContext ->
                                            lifecycleScope.launch {
                                                // clear existing view annotation
                                                rasterValue = 0.0f
                                                rasterValuePoint = null

                                                val expected = mapState.queryRenderedRasterValues(
                                                    interactionContext.screenCoordinate,
                                                    RenderedRasterQueryOptions.Builder()
                                                        .layers(listOf(LAYER_ID_TEMP_0_39))
                                                        .build()
                                                )

                                                if (!expected.isError) {
                                                    expected.value?.let { value ->
                                                        rasterValue =
                                                            value.layers[LAYER_ID_TEMP_0_39]?.first()
                                                                ?: Float.MIN_VALUE
                                                        rasterValuePoint =
                                                            interactionContext.coordinateInfo.coordinate
                                                    }
                                                }
                                            }
                                            true
                                        }

                                        interactionsState.onMapLongClicked { interactionState ->
                                            val lon =
                                                interactionState.coordinateInfo.coordinate.longitude()
                                            val lat =
                                                interactionState.coordinateInfo.coordinate.latitude()
                                            val accessToken =
                                                getString(R.string.mapbox_access_token)

                                            // https://docs.mapbox.com/api/maps/tilequery/#rasterarray-specific-query-parameters
                                            val request = Request.Builder()
                                                .url("https://api.mapbox.com/v4/$TILESET_ID_TEMP_0_39/tilequery/$lon,$lat.json?layers=$SOURCE_LAYER_ID_TEMP_0_39&access_token=$accessToken")
                                                .build()

                                            httpClient.newCall(request).enqueue(
                                                object : Callback {
                                                    override fun onResponse(
                                                        call: Call,
                                                        response: Response
                                                    ) {
                                                        response.body?.string()?.let { json ->
                                                            val result =
                                                                Json.decodeFromString<TilequeryTemperatureResponse>(
                                                                    json
                                                                )
                                                            val firstGeometry =
                                                                result.features.first().geometry
                                                            Log.i(
                                                                TAG,
                                                                "(lon,lat) = (${firstGeometry.longitude}, ${firstGeometry.latitude})"
                                                            )
                                                            Log.i(
                                                                TAG,
                                                                result.features
                                                                    .joinToString(separator = System.lineSeparator()) { feature ->
                                                                        "${feature.properties.tilequery.band}: ${feature.properties.rasterValue}"
                                                                    }
                                                            )
                                                        }
                                                    }

                                                    override fun onFailure(
                                                        call: Call,
                                                        e: IOException
                                                    ) {
                                                        // error handling
                                                    }
                                                }
                                            )

                                            true
                                        }

                                    }
                                }
                            )
                        }
                    ) {
                        MapEffect { mapView ->
                            mapView.mapboxMap.sourceDataLoadedEvents.collect { event ->
                                if (event.type == SourceDataLoadedType.METADATA && event.sourceId == SOURCE_ID_TEMP_0_39) {
                                    mapView.mapboxMap.getSourceAs<RasterArraySource>(event.sourceId)
                                        ?.let { source ->
                                            val newBands = source.rasterLayers?.first()?.bands
                                            if (!newBands.isNullOrEmpty()) {
                                                bands = newBands
                                                title = formatBandDateTime(newBands.first())
                                            }
                                        }
                                }
                            }
                        }

                        RasterLayer(
                            sourceState = temperatureSourceState,
                            layerId = LAYER_ID_TEMP_0_39,
                        ) {
                            sourceLayer = StringValue(SOURCE_LAYER_ID_TEMP_0_39)

                            // ref. https://www.jma.go.jp/jma/kishou/info/colorguide/HPColorGuide_202007.pdf
                            rasterColor = ColorValue(
                                interpolate {
                                    linear()
                                    rasterValue()
                                    literal(-6.0)
                                    rgb(0.0, 32.0, 128.0)
                                    literal(-5.0)
                                    rgb(0.0, 65.0, 255.0)
                                    literal(0.0)
                                    rgb(0.0, 150.0, 255.0)
                                    literal(5.0)
                                    rgb(185.0, 235.0, 255.0)
                                    literal(10.0)
                                    rgb(255.0, 255.0, 240.0)
                                    literal(15.0)
                                    rgb(255.0, 255.0, 150.0)
                                    literal(20.0)
                                    rgb(250.0, 245.0, 0.0)
                                    literal(25.0)
                                    rgb(255.0, 153.0, 0.0)
                                    literal(30.0)
                                    rgb(255.0, 40.0, 0.0)
                                    literal(35.0)
                                    rgb(180.0, 0.0, 104.0)
                                }
                            )
                            rasterOpacity = DoubleValue(0.6)
                            rasterResampling = RasterResamplingValue.LINEAR
                            rasterColorRange = DoubleRangeValue(-7.0, 36.0)
                            if (bands.isNotEmpty()) {
                                rasterArrayBand = StringValue(bands[sliderPosition.toInt()])
                            }
                        }

                        rasterValuePoint?.let { point ->
                            ViewAnnotation(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .background(color = Color(0xffffffff)),
                                options = ViewAnnotationOptions.Builder()
                                    .geometry(point)
                                    .annotationAnchor {
                                        anchor(ViewAnnotationAnchor.CENTER)
                                    }
                                    .build()
                            ) {
                                Text(
                                    text = rasterValue.toString(),
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .wrapContentSize()
                                )
                            }
                        }
                    }

                    if (bands.size >= 2) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { newValue ->
                                sliderPosition = newValue
                                val index = newValue.toInt().coerceIn(0, bands.lastIndex)
                                title = formatBandDateTime(bands[index])
                            },
                            valueRange = 0f..(bands.lastIndex).toFloat(),
                            steps = bands.size - 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }
                }
            }

        }
    }

    private fun formatBandDateTime(band: String): String {
        val time =
            Instant.ofEpochSecond(band.toLong())
        val timeLocal = time.atZone(ZoneId.of("Asia/Tokyo"))
        val formatted =
            timeLocal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return "Temperature $formatted"
    }

    companion object {
        private val JAPAN: Point = Point.fromLngLat(138.0, 35.8)
        private const val TAG = "TILEQUERY"
        private const val TILESET_ID_TEMP_0_39 = "mapbox.weather-jp-temperature-0-39"
        private const val SOURCE_LAYER_ID_TEMP_0_39 = "temperature"
        private const val SOURCE_ID_TEMP_0_39 = "source-japan-temperature-0-39"
        private const val LAYER_ID_TEMP_0_39 = "layer-japan-temperature-0-39"
    }
}