package ch.stenzel.tim.polleninfo.core.measurement.data.mapper

import ch.stenzel.tim.polleninfo.core.measurement.data.remote.dto.SpeciesReadingDto
import ch.stenzel.tim.polleninfo.core.measurement.data.remote.dto.StationMeasurementDto
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.StationMeasurement
import kotlin.time.Instant

fun StationMeasurementDto.toDomain(): StationMeasurement = StationMeasurement(
    stationAbbr = stationAbbr,
    // A malformed timestamp throws, which the repository turns into a `Failure`. There is no safe
    // default: a reading of unknown age cannot be labelled either fresh or stale.
    measuredAt = Instant.parse(measuredAt),
    unit = unit,
    species = species.map { it.toDomain() },
)

fun SpeciesReadingDto.toDomain(): SpeciesReading = SpeciesReading(
    id = id,
    name = name,
    concentration = concentration,
    severity = severity?.toPollenSeverity(),
)

/**
 * The five severity wire strings live here and in [toWireName] below, and nowhere else.
 *
 * Spelled out rather than delegated to `valueOf` or to enum deserialisation: the app declares its
 * own [PollenSeverity] because it cannot depend on `:server`, so something has to hold the two
 * copies together. A `when` over literals does — renaming a constant leaves this list untouched and
 * breaks the test that pins each name against its string.
 *
 * An unrecognised string throws, which the repository turns into a `Failure`. Defaulting to
 * [PollenSeverity.NONE] would be worse than an error: a backend that started sending a band we do
 * not know about would be rendered as a calm day.
 */
internal fun String.toPollenSeverity(): PollenSeverity = when (this) {
    "NONE" -> PollenSeverity.NONE
    "LOW" -> PollenSeverity.LOW
    "MODERATE" -> PollenSeverity.MODERATE
    "HIGH" -> PollenSeverity.HIGH
    "VERY_HIGH" -> PollenSeverity.VERY_HIGH
    else -> throw IllegalArgumentException("unknown pollen severity '$this'")
}

/** The reverse of [toPollenSeverity], for requests that send a severity, such as an alarm's minimum. */
internal fun PollenSeverity.toWireName(): String = when (this) {
    PollenSeverity.NONE -> "NONE"
    PollenSeverity.LOW -> "LOW"
    PollenSeverity.MODERATE -> "MODERATE"
    PollenSeverity.HIGH -> "HIGH"
    PollenSeverity.VERY_HIGH -> "VERY_HIGH"
}
