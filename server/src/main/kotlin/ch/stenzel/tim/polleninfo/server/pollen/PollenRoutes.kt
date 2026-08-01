package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.SpeciesMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.measurement.StationMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesReadingDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationMeasurementDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/** The unit every concentration on this API is expressed in, stated once so the endpoints agree. */
private const val CONCENTRATION_UNIT = "grains/m3"

fun Route.pollenRoutes(
    thresholds: PollenThresholds,
    measurementService: MeasurementService,
) {
    route("/pollen") {
        get("/stations") {
            call.respond(PollenStation.entries.map { it.toDto() })
        }

        get("/stations/{abbr}") {
            val abbr = call.parameters["abbr"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val station = PollenStation.fromAbbr(abbr)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(station.toDto())
        }

        /**
         * A station's latest reading, classified. Nested under the station because it is a property
         * of one.
         *
         * A station whose published file holds no usable row is a 404 for the same reason an
         * unknown abbreviation is: the client asked for a reading that does not exist. An empty
         * body with seven blank rows would be indistinguishable from a calm day.
         */
        get("/stations/{abbr}/measurements") {
            val abbr = call.parameters["abbr"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val station = PollenStation.fromAbbr(abbr)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            val measurement = measurementService.measurementFor(station)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(measurement.toDto())
        }

        get("/species") {
            call.respond(PollenSpecies.entries.map { it.toDto() })
        }

        /**
         * The severity thresholds the server classifies with, so clients can label and colour
         * their UI identically without duplicating the numbers.
         */
        get("/thresholds") {
            call.respond(
                ThresholdsDto(
                    unit = CONCENTRATION_UNIT,
                    bySpecies = thresholds.asMap().mapKeys { (species, _) -> species.name },
                ),
            )
        }
    }
}

private fun PollenStation.toDto() = StationDto(
    abbr = abbr,
    name = displayName,
    canton = canton,
    latitude = latitude,
    longitude = longitude,
    altitudeMasl = altitudeMasl,
)

private fun PollenSpecies.toDto() = SpeciesDto(
    id = name,
    name = displayName,
    latinName = latinName,
)

private fun StationMeasurement.toDto() = StationMeasurementDto(
    stationAbbr = station.abbr,
    measuredAt = measuredAt.toString(),
    unit = CONCENTRATION_UNIT,
    species = species.map { it.toDto() },
)

private fun SpeciesMeasurement.toDto() = SpeciesReadingDto(
    id = species.name,
    name = species.displayName,
    latinName = species.latinName,
    concentration = concentration,
    severity = severity,
)
