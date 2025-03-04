package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityGeofencingBinding
import com.google.gson.JsonObject
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.geofencing.GeofencingError
import com.mapbox.common.geofencing.GeofencingEvent
import com.mapbox.common.geofencing.GeofencingFactory
import com.mapbox.common.geofencing.GeofencingObserver
import com.mapbox.common.geofencing.GeofencingOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.rgb
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation


@MapboxExperimental
class GeofencingActivity : AppCompatActivity() {

    private val geofencingService by lazy {
        GeofencingFactory.getOrCreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityGeofencingBinding.inflate(layoutInflater).apply {
            mapGeofencing.apply {
                location.enabled = true
                location.locationPuck = createDefault2DPuck()
            }

            mapGeofencing.mapboxMap.apply {
                setCamera(
                    CameraOptions.Builder()
                        .center(DefinedLocation.TOKYO_TOWER)
                        .zoom(13.0)
                        .build()
                )

                loadStyle(
                    style(style = Style.LIGHT) {
                        +image(
                            IMAGE_ID,
                            ContextCompat
                                .getDrawable(this@GeofencingActivity, R.drawable.marker)!!
                                .toBitmap()
                        ) {
                            sdf(true)
                        }

                        +geoJsonSource(id = SOURCE_ID) {
                            featureCollection(
                                FeatureCollection.fromFeatures(pointsOfGeofencing.map { it.pointFeature })
                            )
                            cluster(false)
                        }

                        +symbolLayer(layerId = LAYER_SYMBOL_ID, sourceId = SOURCE_ID) {
                            iconImage(
                                IMAGE_ID
                            )
                            iconColor(
                                rgb(get(PROPERTY_RGB_R), get(PROPERTY_RGB_G), get(PROPERTY_RGB_B))
                            )
                            iconSize(2.0)
                            iconAllowOverlap(true)
                        }

                        +geoJsonSource(id = SOURCE_FILL_ID) {
                            featureCollection(
                                FeatureCollection.fromFeatures(geofencingCircles)
                            )
                        }
                        +fillLayer(LAYER_FILL_ID, SOURCE_FILL_ID) {
                            fillColor(
                                rgb(get(PROPERTY_RGB_R), get(PROPERTY_RGB_G), get(PROPERTY_RGB_B))
                            )
                            fillOpacity(0.3)
                        }
                    }
                )
            }

            buttonStartGeofencing.setOnClickListener {
                startGeofencing()
            }

            buttonStopGeofencing.setOnClickListener {
                stopGeofencing()
            }
        }
        setContentView(binding.root)


        geofencingService.configure(
            GeofencingOptions.Builder()
                .build()
        ) { e ->
            Log.e(LOG_TAG, e.toString())
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layerOperations)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addGeofencingFeatures() {
        geofencingCircles.forEach { circle ->
            geofencingService.addFeature(circle) {
                // NOP
            }
        }
    }

    private fun clearGeofencingFeatures() {
        geofencingService.clearFeatures {
            // NOP
        }
    }

    private fun startGeofencing() {
        geofencingService.addObserver(PoiGeofencingObserver) {
            // NOP
        }
        addGeofencingFeatures()
    }

    private fun stopGeofencing() {
        geofencingService.removeObserver(PoiGeofencingObserver) {
            // NOP
        }
        clearGeofencingFeatures()
    }

    private object PoiGeofencingObserver: GeofencingObserver {
        override fun onEntry(event: GeofencingEvent) {
            Log.i(LOG_TAG, "entry to ${event.feature.getProperty(PROPERTY_NAME)} on ${event.timestamp}")
        }

        override fun onDwell(event: GeofencingEvent) {
            Log.i(LOG_TAG, "dwell ${event.feature.getProperty(PROPERTY_NAME)} on ${event.timestamp}")
        }

        override fun onExit(event: GeofencingEvent) {
            Log.i(LOG_TAG, "exit ${event.feature.getProperty(PROPERTY_NAME)} on ${event.timestamp}")
        }

        override fun onUserConsentChanged(isConsentGiven: Boolean) {
            Log.i(LOG_TAG, "user consent changed to $isConsentGiven")
        }

        override fun onError(error: GeofencingError) {
            Log.i(LOG_TAG, "error: ${error.message}")
        }
    }

    companion object {
        private const val LOG_TAG = "GEOFENCING"
        private const val IMAGE_ID = "image-marker"
        private const val LAYER_SYMBOL_ID = "layer-marker"
        private const val SOURCE_ID = "source-geofencing"
        private const val LAYER_FILL_ID = "layer-fill"
        private const val SOURCE_FILL_ID = "source-fill"
        private const val PROPERTY_NAME = "name"
        private const val PROPERTY_RADIUS = "radius"
        private const val PROPERTY_RGB_R = "rgb-r"
        private const val PROPERTY_RGB_G = "rgb-g"
        private const val PROPERTY_RGB_B = "rgb-b"

        private val pointsOfGeofencing = listOf(
            PointOfGeofencing(
                lng = 139.7450,
                lat = 35.6500,
                radius = 1200.0,
                name = "red",
                rgbR = 127.0,
                rgbG = 0.0,
                rgbB = 0.0
            ),
            PointOfGeofencing(
                lng = 139.7550,
                lat = 35.6600,
                radius = 500.0,
                name = "green",
                rgbR = 0.0,
                rgbG = 127.0,
                rgbB = 0.0
            ),
            PointOfGeofencing(
                lng = 139.7500,
                lat = 35.6700,
                radius = 700.0,
                name = "blue",
                rgbR = 0.0,
                rgbG = 0.0,
                rgbB = 127.0
            )
        )
    }

    private class PointOfGeofencing(
        lng: Double,
        lat: Double,
        radius: Double,
        name: String,
        rgbR: Double,
        rgbG: Double,
        rgbB: Double
    ) {
        val point: Point = Point.fromLngLat(lng, lat)

        val pointFeature: Feature = Feature.fromGeometry(
            point,
            JsonObject().apply {
                addProperty(PROPERTY_NAME, name)
                addProperty(PROPERTY_RADIUS, radius)
                addProperty(PROPERTY_RGB_R, rgbR)
                addProperty(PROPERTY_RGB_G, rgbG)
                addProperty(PROPERTY_RGB_B, rgbB)
           },
            name
        )

        val circle: Polygon = TurfTransformation.circle(
            point,
            radius,
            TurfConstants.UNIT_METERS
        )

        val circleFeature: Feature = Feature.fromGeometry(
            circle,
            JsonObject().apply {
                addProperty(PROPERTY_NAME, name)
                addProperty(PROPERTY_RADIUS, radius)
                addProperty(PROPERTY_RGB_R, rgbR)
                addProperty(PROPERTY_RGB_G, rgbG)
                addProperty(PROPERTY_RGB_B, rgbB)
            },
            name
        )
    }

    private val geofencingCircles = pointsOfGeofencing.map {
        it.circleFeature
    }

}
