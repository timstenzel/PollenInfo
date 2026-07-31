package ch.stenzel.tim.polleninfo.feature.onboarding.data.mapper

import ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.dto.StationDto
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station

/**
 * Maps the station payload to the domain model **and sorts it alphabetically by display name**.
 *
 * The sort belongs here because the server's order is `PollenStation` enum order — by
 * abbreviation, not by name — which puts `PLO` Locarno / Monti before `PLS` Lausanne. Sorting once
 * at the boundary means no consumer has to remember to.
 *
 * A plain [String] comparison is correct for these 15 names even though four of them are accented
 * (Genève, Münsterlingen, Neuchâtel, Zürich): every name starts with an ASCII letter and no two
 * names share a prefix up to an accented character, so the accents never reach the comparison.
 * **Do not "fix" this with a `Collator`** — that is `java.text`, which is JVM-only and breaks the
 * iOS build.
 */
fun List<StationDto>.toDomain(): List<Station> = map { it.toDomain() }.sortedBy { it.name }

fun StationDto.toDomain(): Station = Station(
    abbr = abbr,
    name = name,
    latitude = latitude,
    longitude = longitude,
)
