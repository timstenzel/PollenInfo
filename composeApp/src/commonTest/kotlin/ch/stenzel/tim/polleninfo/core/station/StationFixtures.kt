package ch.stenzel.tim.polleninfo.core.station

import ch.stenzel.tim.polleninfo.core.station.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.station.data.remote.dto.StationDto
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

/**
 * The real `GET /pollen/stations` payload, **in the order the server emits it** — `PollenStation`
 * enum order, which is by abbreviation and therefore not display-name alphabetical. Keeping the
 * fixture in that order is the point: it is what lets the mapper's sorting be asserted.
 *
 * Hardcoding station data is deliberate here. Production code must not (the list comes from the
 * backend), but a fixture that fetched its own expectations would assert nothing.
 */
val stationDtosInServerOrder = listOf(
    StationDto("PBE", "Bern", "BE", 46.950342, 7.424661, 546),
    StationDto("PBS", "Basel", "BS", 47.561800, 7.583931, 256),
    StationDto("PBU", "Buchs, SG", "SG", 47.173267, 9.472614, 446),
    StationDto("PCF", "La Chaux-de-Fonds", "NE", 47.113514, 6.832000, 1037),
    StationDto("PDS", "Davos / Wolfgang", "GR", 46.829092, 9.855489, 1591),
    StationDto("PGE", "Genève", "GE", 46.191969, 6.147544, 379),
    StationDto("PLO", "Locarno / Monti", "TI", 46.172547, 8.787389, 376),
    StationDto("PLS", "Lausanne", "VD", 46.524103, 6.644825, 576),
    StationDto("PLU", "Lugano", "TI", 46.004231, 8.960631, 275),
    StationDto("PLZ", "Luzern", "LU", 47.057678, 8.296803, 463),
    StationDto("PMU", "Münsterlingen", "TG", 47.630206, 9.236878, 415),
    StationDto("PNE", "Neuchâtel", "NE", 47.000269, 6.949828, 490),
    StationDto("PPY", "Payerne", "VD", 46.813403, 6.942939, 490),
    StationDto("PSN", "Sion", "VS", 46.235403, 7.384606, 494),
    StationDto("PZH", "Zürich", "ZH", 47.378225, 8.565644, 559),
)

/** The same 15 stations as the JSON body the backend actually returns, server order preserved. */
val stationsJson: String = stationDtosInServerOrder.joinToString(
    prefix = "[",
    postfix = "]",
    separator = ",",
) { dto ->
    """{"abbr":"${dto.abbr}","name":"${dto.name}","canton":"${dto.canton}",""" +
        """"latitude":${dto.latitude},"longitude":${dto.longitude},"altitudeMasl":${dto.altitudeMasl}}"""
}

/** All 15 stations as the app holds them: domain models, already sorted the way the app shows them. */
val allStations: List<Station> = stationDtosInServerOrder.toDomain()

/** Display names in the order the app must show them. */
val expectedStationNamesAlphabetical = listOf(
    "Basel",
    "Bern",
    "Buchs, SG",
    "Davos / Wolfgang",
    "Genève",
    "La Chaux-de-Fonds",
    "Lausanne",
    "Locarno / Monti",
    "Lugano",
    "Luzern",
    "Münsterlingen",
    "Neuchâtel",
    "Payerne",
    "Sion",
    "Zürich",
)

fun station(
    abbr: String = "PZH",
    name: String = "Zürich",
    latitude: Double = 47.378225,
    longitude: Double = 8.565644,
): Station = Station(abbr = abbr, name = name, latitude = latitude, longitude = longitude)
