package ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model

data class PollenReading(
    val type: PollenType,
    val level: PollenLevel,
    val valueGrainsPerM3: Float,
)

data class PollenForecast(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val currentReadings: List<PollenReading>,
    val hourlyForecast: List<HourlyPollenEntry>,
)

data class HourlyPollenEntry(
    val hour: String,
    val readings: List<PollenReading>,
)
