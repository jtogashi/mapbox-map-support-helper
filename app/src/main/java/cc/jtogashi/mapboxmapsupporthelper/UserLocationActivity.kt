package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityUserLocationBinding
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

class UserLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityUserLocationBinding.inflate(layoutInflater).apply {
            mapUserLocation.location.apply {
                enabled = true
                locationPuck = createDefault2DPuck(this@UserLocationActivity, true)
            }
        }

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.isochrone)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}