package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityLayerOperationsBinding
import com.google.gson.JsonObject
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.geojson.Feature
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.LongClickInteraction
import com.mapbox.maps.Style
import com.mapbox.maps.debugoptions.MapViewDebugOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.rgb
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.removeGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener

private const val LOG_TAG = "LOC_APP"
private const val LAYER_SYMBOL_ID = "user-adding-symbols"
private const val SOURCE_ID = "user-adding-source"
private const val IMAGE_ID = "marker-image"
private const val PROPERTY_LABEL = "label"
private const val PROPERTY_COLOR_R = "color-r"
private const val PROPERTY_COLOR_G = "color-g"
private const val PROPERTY_COLOR_B = "color-b"

@MapboxExperimental
class LayerOperationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityLayerOperationsBinding.inflate(layoutInflater).apply {
            mapLayerOperations.debugOptions = setOf(
                MapViewDebugOptions.COLLISION,
                MapViewDebugOptions.TILE_BORDERS
            )

            mapLayerOperations.mapboxMap.apply {
                setCamera(
                    CameraOptions.Builder()
                        .center(DefinedLocation.TOKYO_TOWER)
                        .zoom(16.5)
                        .build()
                )

                addInteraction(
                    LongClickInteraction.layer(LAYER_SYMBOL_ID) { feature, context ->
                        (getSource(SOURCE_ID) as? GeoJsonSource)?.apply {
                            removeGeoJSONSourceFeatures(listOf(feature.id!!.featureId))
                        }
                        true
                    }
                )

                loadStyle(
                    style(style = Style.LIGHT) {
                        +image(
                            IMAGE_ID,
                            ContextCompat.getDrawable(
                                this@LayerOperationsActivity,
                                R.drawable.marker
                            )!!.toBitmap()
                        ) {
                            sdf(true)
                        }

                        +geoJsonSource(id = SOURCE_ID) {
                            cluster(false)
                        }

                        +symbolLayer(layerId = LAYER_SYMBOL_ID, sourceId = SOURCE_ID) {
                            iconImage(
                                IMAGE_ID
                            )
                            iconSize(4.0)
                            iconAllowOverlap(true)
                            iconAnchor(IconAnchor.RIGHT)
                            iconColor(
                                rgb(get(PROPERTY_COLOR_R), get(PROPERTY_COLOR_G), get(PROPERTY_COLOR_B))
                            )
                            symbolAvoidEdges(true)

                            textField(
                                get(PROPERTY_LABEL)
                            )
                            textColor(
                                rgb(get(PROPERTY_COLOR_R), get(PROPERTY_COLOR_G), get(PROPERTY_COLOR_B))
                            )
                            textAnchor(TextAnchor.LEFT)
                            textAllowOverlap(false)
                            textOptional(true)
                        }
                    }
                )

                addOnMapLongClickListener { point ->
                    (getSource(SOURCE_ID) as? GeoJsonSource)?.apply {
                        val feature = Feature.fromGeometry(
                            point,
                            JsonObject().apply {
                                addProperty(PROPERTY_LABEL, "qwertyuiopasdfghjklzxcvbnm")
                                addProperty(PROPERTY_COLOR_R, (0..127).random())
                                addProperty(PROPERTY_COLOR_G, (0..127).random())
                                addProperty(PROPERTY_COLOR_B, (0..127).random())
                            },
                            point.toString()
                        )
                        addGeoJSONSourceFeatures(listOf(feature))
                    }

                    true
                }
            }
        }
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layerOperations)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
