package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.style.layers.FormattedValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconAnchorValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconTextFitValue
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.layers.generated.TextAnchorValue
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.generated.Expression
import java.util.Locale

class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // declare list of all symbols used for following GeoJson data source
            var pointList: List<Feature> by rememberSaveable {
                // declare initial data here
                mutableStateOf(
                    listOf(
                        Feature.fromGeometry(
                            HELSINKI,
                            JsonObject().apply {
                                addProperty("label", "Helsinki")
                            },
                            "Helsinki"
                        )
                    )
                )
            }

            val geoJsonSource = rememberGeoJsonSourceState(
                sourceId = "symbolLayerSource"
            ).apply {
                data = GeoJSONData(pointList)
            }

            // generate bitmap from drawable resource
            val backgroundImage = AppCompatResources.getDrawable(this, R.drawable.shape_label_background)!!.toBitmap()

            val styleImage = rememberStyleImage(
                imageId = "backgroundRect",
                imageBitmap = backgroundImage.asImageBitmap()
            )

            MapboxMap(
                Modifier.fillMaxSize(),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        zoom(10.0)
                        center(HELSINKI)
                    }
                },
                style = {
                    MapStyle(style = Style.LIGHT)
                },
                onMapLongClickListener = { point ->
                    // add a new symbol with text of clicked coordinate
                    val pointText = String.format(Locale.US, "%.5f,%.5f", point.latitude(), point.longitude())
                    Toast.makeText(this@ComposeActivity, pointText, Toast.LENGTH_SHORT).show()

                    // update pointList with the symbol added to reflect in the map
                    pointList = pointList.plus(
                        Feature.fromGeometry(
                            point,
                            JsonObject().apply {
                                addProperty("label", pointText)
                            },
                            pointText
                        )
                    )

                    true
                }
            ) {
                // symbol layer referencing the GeoJson data source
                SymbolLayer(sourceState = geoJsonSource) {
                    iconAnchor = IconAnchorValue.CENTER
                    iconImage = ImageValue(styleImage)
                    iconTextFit = IconTextFitValue.BOTH
                    iconTextFitPadding = DoubleListValue(10.0, 10.0, 10.0, 10.0)
                    textAnchor = TextAnchorValue.CENTER
                    textField = FormattedValue(get("label"))
                    textColor = ColorValue(Expression.rgb(255.0, 255.0, 255.0))

                    interactionsState.onLongClicked { featuresetFeature, _ ->
                        // long click a symbol to remove
                        featuresetFeature.id?.featureId?.let { clickedFeatureId ->
                            // update the pointList with the clicked feature ID removed to reflect in the map
                            pointList = pointList.filter { feature ->
                                feature.id() != clickedFeatureId
                            }
                        }
                        true
                    }
                }
            }
        }
    }

    companion object {
        private val HELSINKI: Point = Point.fromLngLat(24.941526986277893, 60.17099952463323)
    }
}