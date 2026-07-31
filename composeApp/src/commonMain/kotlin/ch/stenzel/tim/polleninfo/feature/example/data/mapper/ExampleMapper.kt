package ch.stenzel.tim.polleninfo.feature.example.data.mapper

import ch.stenzel.tim.polleninfo.feature.example.data.remote.dto.ExampleResponseDto
import ch.stenzel.tim.polleninfo.feature.example.data.remote.dto.HourlyDto
import ch.stenzel.tim.polleninfo.feature.example.domain.model.HourlyPollenEntry
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenReading
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenType
import ch.stenzel.tim.polleninfo.feature.example.domain.model.toPollenLevel
import kotlin.math.pow
import kotlin.math.round

fun ExampleResponseDto.toDomain(): PollenSnapshot {
    val hourlyEntries = hourly.toHourlyEntries()
    val currentReadings = hourlyEntries.firstOrNull()?.readings ?: emptyList()
    return PollenSnapshot(
        location = "${latitude.format(2)}°N, ${longitude.format(2)}°E",
        latitude = latitude,
        longitude = longitude,
        currentReadings = currentReadings,
        hourlyReadings = hourlyEntries,
    )
}

private fun HourlyDto.toHourlyEntries(): List<HourlyPollenEntry> =
    time.indices.map { i ->
        HourlyPollenEntry(
            hour = time[i],
            readings = listOf(
                PollenReading(PollenType.BIRCH, birch.getOrNull(i).toPollenLevel(), birch.getOrNull(i) ?: 0f),
                PollenReading(PollenType.GRASS, grass.getOrNull(i).toPollenLevel(), grass.getOrNull(i) ?: 0f),
                PollenReading(PollenType.MUGWORT, mugwort.getOrNull(i).toPollenLevel(), mugwort.getOrNull(i) ?: 0f),
                PollenReading(PollenType.ALDER, alder.getOrNull(i).toPollenLevel(), alder.getOrNull(i) ?: 0f),
                PollenReading(PollenType.OLIVE, olive.getOrNull(i).toPollenLevel(), olive.getOrNull(i) ?: 0f),
                PollenReading(PollenType.RAGWEED, ragweed.getOrNull(i).toPollenLevel(), ragweed.getOrNull(i) ?: 0f),
            ),
        )
    }

/**
 * Multiplatform fixed-decimal formatting. `String.format` is JVM-only and is not available
 * in `commonMain`, so it cannot be used here.
 */
private fun Double.format(digits: Int): String {
    val factor = 10.0.pow(digits)
    val rounded = round(this * factor) / factor
    val text = rounded.toString()
    val dotIndex = text.indexOf('.')
    if (dotIndex < 0) return text + "." + "0".repeat(digits)
    val decimals = text.length - dotIndex - 1
    return when {
        decimals > digits -> text.substring(0, dotIndex + 1 + digits)
        decimals < digits -> text + "0".repeat(digits - decimals)
        else -> text
    }
}
