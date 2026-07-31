package ch.stenzel.tim.polleninfo.feature.example

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.domain.model.HourlyPollenEntry
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenReading
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenType
import ch.stenzel.tim.polleninfo.feature.example.domain.model.toPollenLevel
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository

/** Records the coordinates it was called with and replays a scripted result. */
class FakeExampleRepository(
    var result: Result<PollenSnapshot> = Result.Success(pollenSnapshot()),
) : ExampleRepository {

    val calls = mutableListOf<Pair<Double, Double>>()

    override suspend fun getPollenSnapshot(
        latitude: Double,
        longitude: Double,
    ): Result<PollenSnapshot> {
        calls += latitude to longitude
        return result
    }
}

fun pollenSnapshot(
    location: String = "Zürich",
    latitude: Double = 47.3769,
    longitude: Double = 8.5417,
    grassValue: Float = 12f,
): PollenSnapshot {
    val readings = listOf(
        PollenReading(PollenType.GRASS, grassValue.toPollenLevel(), grassValue),
        PollenReading(PollenType.BIRCH, 0f.toPollenLevel(), 0f),
    )
    return PollenSnapshot(
        location = location,
        latitude = latitude,
        longitude = longitude,
        currentReadings = readings,
        hourlyReadings = listOf(HourlyPollenEntry(hour = "2026-07-31T00:00", readings = readings)),
    )
}
