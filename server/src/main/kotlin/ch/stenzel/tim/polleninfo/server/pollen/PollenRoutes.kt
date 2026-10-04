package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryRange
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import ch.stenzel.tim.polleninfo.server.pollen.history.StationHistory
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.NoUsableRowException
import ch.stenzel.tim.polleninfo.server.pollen.measurement.SpeciesMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.measurement.StationMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.pollen.model.HistoryDayDto
import ch.stenzel.tim.polleninfo.server.pollen.model.HistorySpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesReadingDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationHistoryDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationMeasurementDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/** The unit every concentration on this API is expressed in, stated once so the endpoints agree. */
private const val CONCENTRATION_UNIT = "grains/m3"

fun Route.pollenRoutes(
    thresholds: PollenThresholds,
    measurementService: MeasurementService,
    historyService: HistoryService,
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
         * A reading kept from an earlier fetch is a 200 like a fresh one: its `measuredAt` says how
         * old it is, and saying so is the client's job. With nothing kept, a station whose
         * published file holds no usable row is a 404 for the same reason an unknown abbreviation
         * is — the client asked for a reading that does not exist, and seven blank rows would be
         * indistinguishable from a calm day. A file that could not be obtained is a 502.
         */
        get("/stations/{abbr}/measurements") {
            val abbr = call.parameters["abbr"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val station = PollenStation.fromAbbr(abbr)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            when (val result = measurementService.measurementFor(station)) {
                is CacheResult.Fresh -> call.respond(result.value.toDto())
                is CacheResult.Stale -> call.respond(result.value.toDto())
                is CacheResult.Failed -> if (result.cause is NoUsableRowException) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.application.log.warn("Upstream fetch for ${station.abbr} failed", result.cause)
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
        }

        /**
         * A station's daily levels over the last `week`, `month` or `year`, ending yesterday.
         *
         * The range is checked before the station, as an alarm body is checked before its device:
         * a request that could never succeed says so whatever it names. Rows kept from an earlier
         * fetch are a 200 like fresh ones; with nothing kept, a file that could not be obtained is
         * a 502.
         */
        get("/stations/{abbr}/history") {
            val range = HistoryRange.fromWireName(call.request.queryParameters["range"])
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorDto("Expected range=week, range=month or range=year"),
                )
            val abbr = call.parameters["abbr"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val station = PollenStation.fromAbbr(abbr)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            when (val result = historyService.historyFor(station, range)) {
                is CacheResult.Fresh -> call.respond(result.value.toDto())
                is CacheResult.Stale -> call.respond(result.value.toDto())
                is CacheResult.Failed -> {
                    call.application.log.warn("History fetch for ${station.abbr} failed", result.cause)
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
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

private fun StationHistory.toDto() = StationHistoryDto(
    stationAbbr = station.abbr,
    range = range.wireName,
    from = from.toString(),
    until = until.toString(),
    days = days.map { day ->
        HistoryDayDto(
            date = day.date.toString(),
            species = day.species.map {
                HistorySpeciesDto(id = it.species.name, concentration = it.concentration, severity = it.severity)
            },
        )
    },
)
