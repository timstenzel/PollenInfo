package ch.stenzel.tim.polleninfo.feature.pollenforecast.data.mapper

import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.dto.AirQualityResponseDto
import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.dto.HourlyDto
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.HourlyPollenEntry
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenReading
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenType
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.toPollenLevel

fun AirQualityResponseDto.toDomain(): PollenForecast {
    val hourlyEntries = hourly.toHourlyEntries()
    val currentReadings = hourlyEntries.firstOrNull()?.readings ?: emptyList()
    return PollenForecast(
        location = "${latitude.format(2)}°N, ${longitude.format(2)}°E",
        latitude = latitude,
        longitude = longitude,
        currentReadings = currentReadings,
        hourlyForecast = hourlyEntries,
    )
}

private fun HourlyDto.toHourlyEntries(): List<HourlyPollenEntry> =
    time.indices.map { i ->
        HourlyPollenEntry(
            hour = time[i],
            readings = listOf(
                PollenReading(PollenType.BIRCH, birchPollen.getOrNull(i).toPollenLevel(), birchPollen.getOrNull(i) ?: 0f),
                PollenReading(PollenType.GRASS, grassPollen.getOrNull(i).toPollenLevel(), grassPollen.getOrNull(i) ?: 0f),
                PollenReading(PollenType.MUGWORT, mugwortPollen.getOrNull(i).toPollenLevel(), mugwortPollen.getOrNull(i) ?: 0f),
                PollenReading(PollenType.ALDER, alderPollen.getOrNull(i).toPollenLevel(), alderPollen.getOrNull(i) ?: 0f),
                PollenReading(PollenType.OLIVE, olivePollen.getOrNull(i).toPollenLevel(), olivePollen.getOrNull(i) ?: 0f),
                PollenReading(PollenType.RAGWEED, ragweedPollen.getOrNull(i).toPollenLevel(), ragweedPollen.getOrNull(i) ?: 0f),
            ),
        )
    }

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
