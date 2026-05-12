package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.style.StringValue
import com.mapbox.maps.extension.compose.style.layers.FormattedValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconAnchorValue
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.layers.generated.TextAnchorValue
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.generated.rememberVectorSourceState
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.switchCase
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.interactions.FeatureState
import com.mapbox.maps.interactions.FeaturesetFeature
import kotlinx.coroutines.launch


class FeatureStateComposeActivity : ComponentActivity() {
    @OptIn(MapboxExperimental::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val poiSource = rememberVectorSourceState(sourceId = "poi") {
                url = StringValue("mapbox://mapbox.mapbox-streets-v8")
            }

            // generate bitmap from drawable resource
            val rectBitmap = AppCompatResources.getDrawable(this, R.drawable.shape_rectangle)!!.toBitmap()
            val rectImage = rememberStyleImage(
                imageId = "rect",
                imageBitmap = rectBitmap.asImageBitmap()
            )

            val circleBitmap = AppCompatResources.getDrawable(this, R.drawable.shape_circle)!!.toBitmap()

            val mapState = rememberMapState()

            // map that maintains all selected POI features for point annotation creation
            val selectedFeatures = remember { mutableStateMapOf<String, FeaturesetFeature<FeatureState>>() }

            MapboxMap(
                Modifier.fillMaxSize(),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        zoom(15.0)
                        center(Point.fromLngLat(126.9097237, 37.5549908)) // Mangwon
                    }
                },
                mapState = mapState,
                style = {
                    MapStyle(style = "mapbox://styles/jtogashi/cmp10nxok009g01r65hc4b8y5")
                }
            ) {
                SymbolLayer(
                    sourceState = poiSource,
                    layerId = "poi-layer"
                ) {
                    sourceLayer = StringValue("poi_label")
                    iconAnchor = IconAnchorValue.CENTER
                    iconImage = ImageValue(rectImage)

                    textAnchor = TextAnchorValue.TOP
                    textRadialOffset = DoubleValue(1.0)
                    textField = FormattedValue(get(POI_PROP_KEY_NAME))

                    textOpacity = DoubleValue(
                        switchCase {
                            boolean {
                                featureState {
                                    literal(FEATURE_STATE_SELECTED)
                                }
                                literal(false)
                            }
                            literal(0.0) // invisible when selected is false
                            literal(1.0) // visible when selected is false
                        }
                    )

                    iconOpacity = DoubleValue(
                        switchCase {
                            boolean {
                                featureState {
                                    literal(FEATURE_STATE_SELECTED)
                                }
                                literal(false)
                            }
                            literal(0.0) // invisible when selected is true
                            literal(1.0) // visible when selected is false
                        }
                    )

                    interactionsState.onClicked { featuresetFeature, _ ->
                        val featureId = featuresetFeature.id?.featureId
                        if (featureId != null && featuresetFeature.geometry as? Point != null) {
                            // store selected feature to add point annotation by recomposition
                            selectedFeatures[featureId] = featuresetFeature

                            // set selected state always to true
                            // so that only clicking on point annotation can deselect
                            featuresetFeature.setFeatureState(
                                FeatureState {
                                    addBooleanState(FEATURE_STATE_SELECTED, true)
                                }
                            )
                        }

                        true
                    }
                }

                selectedFeatures.forEach { entry ->
                    val feature = entry.value
                    val point = feature.geometry as Point
                    PointAnnotation(point) {
                        iconImage = IconImage(circleBitmap)
                        iconAnchor = IconAnchor.CENTER
                        iconSize = 1.5
                        textField = feature.properties.getString(POI_PROP_KEY_NAME)
                        textAnchor = TextAnchor.TOP
                        textSize = 26.0
                        textColor = Color(0xD0800000)
                        textRadialOffset = 1.0
                        interactionsState.onClicked {
                            lifecycleScope.launch {
                                // restore original symbol
                                mapState.setFeatureState(
                                    feature,
                                    FeatureState {
                                        addBooleanState(FEATURE_STATE_SELECTED, false)
                                    }
                                )

                                // remove point annotation
                                selectedFeatures.remove(entry.key)
                            }
                            true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val FEATURE_STATE_SELECTED = "selected"
        private const val POI_PROP_KEY_NAME = "name"
    }
}