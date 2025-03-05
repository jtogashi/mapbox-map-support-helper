package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityIsochroneBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class IsochroneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIsochroneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityIsochroneBinding.inflate(layoutInflater).apply {
            mapIsochrone.mapboxMap.apply {
                setCamera(
                    cameraOptions {
                        center(DefinedLocation.UNIVERSITY_OF_TOKYO)
                        zoom(15.0)
                    }
                )
            }
        }

        addAnnotation(DefinedLocation.UNIVERSITY_OF_TOKYO)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.isochrone)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addAnnotation(point: Point) {
        val image = ContextCompat.getDrawable(this, R.drawable.marker)?.toBitmap() ?: return
        val annotationPlugin = binding.mapIsochrone.annotations
        val manager = annotationPlugin.createPointAnnotationManager()
        manager.create(
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(image)
                .withIconSize(2.5)
        )
    }
}
