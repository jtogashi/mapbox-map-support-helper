package cc.jtogashi.mapboxmapsupporthelper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cc.jtogashi.mapboxmapsupporthelper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val exampleList = listOf(
        LayerOperationsActivity::class.java,
        GeofencingActivity::class.java,
        IsochroneActivity::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityMainBinding.inflate(layoutInflater).apply {
            listExamples.adapter = MainActivityListAdapter(exampleList)
            listExamples.layoutManager = LinearLayoutManager(this@MainActivity)
        }

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
