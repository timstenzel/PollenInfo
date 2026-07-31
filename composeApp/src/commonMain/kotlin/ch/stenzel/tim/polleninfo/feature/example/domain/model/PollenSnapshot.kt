package ch.stenzel.tim.polleninfo.feature.example.domain.model

data class PollenReading(
    val type: PollenType,
    val level: PollenLevel,
    val valueGrainsPerM3: Float,
)

/**
 * Current measured pollen state for a location, plus the hourly readings it was derived from.
 *
 * [currentReadings] is the most recent hour. Nothing here predicts ahead.
 */
data class PollenSnapshot(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val currentReadings: List<PollenReading>,
    val hourlyReadings: List<HourlyPollenEntry>,
)

data class HourlyPollenEntry(
    val hour: String,
    val readings: List<PollenReading>,
)
