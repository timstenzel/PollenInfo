package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.pollenRoutes(thresholds: PollenThresholds) {
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
                    unit = "grains/m3",
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
