package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityIsochroneBinding
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.isochrone.IsochroneCriteria
import com.mapbox.api.isochrone.MapboxIsochrone
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class IsochroneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIsochroneBinding
    private var currentAnnotationPoint: Point? = null
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private var isochroneCriteria = IsochroneCriteria.PROFILE_DEFAULT_USER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIsochroneBinding.inflate(layoutInflater).apply {
            polygonAnnotationManager = mapIsochrone.annotations.createPolygonAnnotationManager()
            pointAnnotationManager = mapIsochrone.annotations.createPointAnnotationManager()

//            mapIsochrone.mapboxMap.apply {
//                setCamera(
//                    cameraOptions {
//                        center(DefinedLocation.UNIVERSITY_OF_TOKYO)
//                        zoom(15.0)
//                    }
//                )
//
//                addOnMapLongClickListener { point ->
//                    clearCurrentAnnotation()
//                    clearCurrentIsochrone()
//                    currentAnnotationPoint = point
//                    addPointAnnotation(point)
//                    true
//                }
//            }

            spinnerProfile.apply {
                adapter = ArrayAdapter.createFromResource(
                    this@IsochroneActivity,
                    R.array.isochroneCriteria,
                    android.R.layout.simple_spinner_item
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // FIXME
                        isochroneCriteria = when (position) {
                            0 -> IsochroneCriteria.PROFILE_WALKING
                            1 -> IsochroneCriteria.PROFILE_CYCLING
                            2 -> IsochroneCriteria.PROFILE_DRIVING
                            else -> return
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        // NOP
                    }
                }
            }

            buttonIsochrone.setOnClickListener {
                val minString = editMinute.text?.toString().orEmpty()
                try {
                    val min = Integer.parseInt(minString)
                    val point = currentAnnotationPoint
                    if (point != null) {
                        clearCurrentIsochrone()
                        requestIsochrone(point, min)
                    }
                } catch (e: NumberFormatException) {
                    Snackbar.make(root, "Input valid number", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.isochrone)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        clearCurrentAnnotation()
        clearCurrentIsochrone()
        currentAnnotationPoint = null
        super.onDestroy()
    }

    private fun addPointAnnotation(point: Point) {
        val image = ContextCompat.getDrawable(this, R.drawable.marker)?.toBitmap() ?: return

        pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(image)
                .withIconSize(2.5)
        )
    }

    private fun clearCurrentAnnotation() {
        pointAnnotationManager.deleteAll()
    }

    private fun requestIsochrone(point: Point, min: Int) {
        val isochroneRequest = MapboxIsochrone.builder()
            .accessToken(resources.getString(R.string.mapbox_access_token))
            .profile(isochroneCriteria)
            .addContoursMinutes(min)
            .polygons(true)
            .coordinates(point)
            .build()

        isochroneRequest.enqueueCall(object : Callback<FeatureCollection> {
            override fun onResponse(
                call: Call<FeatureCollection>,
                response: Response<FeatureCollection>
            ) {
                val responseFeature = response.body()?.features()?.first() ?: return
                val polygon = responseFeature.geometry() as Polygon

                polygonAnnotationManager.create(
                    PolygonAnnotationOptions()
                        .withGeometry(polygon)
                        .withFillColor("#00007F")
                        .withFillOpacity(0.2)
                )

//                binding.mapIsochrone.mapboxMap.apply {
//                    cameraForCoordinates(
//                        polygon.coordinates().flatten(),
//                        cameraOptions { },
//                        null,
//                        null,
//                        null
//                    ) {
//                        flyTo(it)
//                    }
//                }
            }

            override fun onFailure(call: Call<FeatureCollection>, t: Throwable) {
                Snackbar
                    .make(binding.root, "Isochrone request failed", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun clearCurrentIsochrone() {
        polygonAnnotationManager.deleteAll()
    }
}

private const val TAG = "LOC_COMPASS"
