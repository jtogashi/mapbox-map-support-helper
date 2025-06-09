package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityCustomLayerBinding
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.CustomLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.customLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style

class CustomLayerActivity : AppCompatActivity() {
    private lateinit var mapboxMap: MapboxMap
    private lateinit var binding: ActivityCustomLayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomLayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapCustom.mapboxMap
        mapboxMap.loadStyle(
            style(Style.MAPBOX_STREETS) {
                +geoJsonSource(LINE_SOURCE_ID) {
                    generateId(true)
                    feature(Feature.fromGeometry(
                        LineString.fromLngLats(
                        listOf(
                            Point.fromLngLat(139.767049,35.677493),
                            Point.fromLngLat(139.77084, 35.68359),
                        )
                    )))
                }
                +lineLayer(LINE_LAYER_ID, LINE_SOURCE_ID) {
                    lineColor("#0000FF")
                    lineWidth(8.0)
                    lineOpacity(1.0)
                }
                +layerAtPosition(
                    customLayer(
                        layerId = CUSTOM_LAYER_ID,
                        host = WhiteGradientCustomLayer()
                    ),
                    below = LINE_LAYER_ID
                )

                +projection(ProjectionName.MERCATOR)
            }
        ) {
            mapboxMap.setCamera(CAMERA)
            initFab()
        }
    }

    private fun initFab() {
        binding.fabCustom.setOnClickListener {
            swapCustomLayer()
        }
    }

    private fun swapCustomLayer() {
        mapboxMap.style?.let { style ->
            if (style.styleLayerExists(CUSTOM_LAYER_ID)) {
                style.removeStyleLayer(CUSTOM_LAYER_ID)
            } else {
                style.addLayerBelow(
                    CustomLayer(CUSTOM_LAYER_ID, WhiteGradientCustomLayer()),
                    below = LINE_LAYER_ID
                )
            }
        }
    }

    companion object {
        private const val LINE_LAYER_ID = "lineId"
        private const val LINE_SOURCE_ID = "lineSourceId"
        private const val CUSTOM_LAYER_ID = "customId"
        private val CAMERA =
            CameraOptions.Builder().center(Point.fromLngLat(139.76900, 35.68049)).pitch(0.0).zoom(14.0).build()
    }
}