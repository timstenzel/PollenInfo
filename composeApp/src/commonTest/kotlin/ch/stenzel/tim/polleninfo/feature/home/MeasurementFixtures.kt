package ch.stenzel.tim.polleninfo.feature.home

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.feature.home.domain.repository.StationMeasurementRepository
import kotlinx.datetime.Instant

/** Records the abbreviations it was asked for and replays a scripted result. */
class FakeStationMeasurementRepository(
    var result: Result<StationMeasurement> = Result.Success(measurement()),
) : StationMeasurementRepository {

    val requested = mutableListOf<String>()

    override suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement> {
        requested += stationAbbr
        return result
    }
}

fun reading(
    name: String,
    concentration: Int? = null,
    severity: PollenSeverity? = null,
) = SpeciesReading(
    id = name.uppercase(),
    name = name,
    concentration = concentration,
    severity = severity,
)

val MEASURED_AT: Instant = Instant.parse("2026-08-01T09:00:00Z")

fun measurement(
    stationAbbr: String = "PZH",
    measuredAt: Instant = MEASURED_AT,
    species: List<SpeciesReading> = listOf(
        reading("Birch", 42, PollenSeverity.MODERATE),
        reading("Grasses", 20, PollenSeverity.HIGH),
        reading("Ash"),
    ),
) = StationMeasurement(
    stationAbbr = stationAbbr,
    measuredAt = measuredAt,
    unit = "grains/m3",
    species = species,
)

/**
 * The seven-taxon body the backend actually sends, with one taxon the station does not report.
 * Written out rather than generated so a change to the wire shape has to be made here too.
 */
val measurementJson = """
    {
      "stationAbbr": "PZH",
      "measuredAt": "2026-08-01T09:00:00Z",
      "unit": "grains/m3",
      "species": [
        { "id": "ALDER",   "name": "Alder",   "latinName": "Alnus",    "concentration": 0,  "severity": "NONE" },
        { "id": "BIRCH",   "name": "Birch",   "latinName": "Betula",   "concentration": 42, "severity": "MODERATE" },
        { "id": "HAZEL",   "name": "Hazel",   "latinName": "Corylus",  "concentration": 0,  "severity": "NONE" },
        { "id": "BEECH",   "name": "Beech",   "latinName": "Fagus",    "concentration": 3,  "severity": "LOW" },
        { "id": "ASH",     "name": "Ash",     "latinName": "Fraxinus", "concentration": null, "severity": null },
        { "id": "OAK",     "name": "Oak",     "latinName": "Quercus",  "concentration": 1500, "severity": "VERY_HIGH" },
        { "id": "GRASSES", "name": "Grasses", "latinName": "Poaceae",  "concentration": 20, "severity": "HIGH" }
      ]
    }
""".trimIndent()
