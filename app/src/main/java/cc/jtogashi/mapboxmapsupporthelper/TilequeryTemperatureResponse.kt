package cc.jtogashi.mapboxmapsupporthelper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TilequeryTemperatureResponse(
    val type: String,
    val features: List<Feature>
) {
    @Serializable
    data class Feature(
        val type: String,
        val id: String?,
        val geometry: Geometry,
        val properties: Properties
    )

    @Serializable
    data class Geometry(
        val type: String,
        val coordinates: List<Double>
    ) {
        val longitude = coordinates[0]
        val latitude = coordinates[1]
    }

    @Serializable
    data class Properties(
        @SerialName("val")
        val rasterValues: List<Double>,

        val tilequery: Tilequery
    ) {
        val rasterValue = rasterValues.first()
    }

    @Serializable
    data class Tilequery(
        val layer: String,
        val band: String,
        val zoom: Int,
        val units: String
    )
}