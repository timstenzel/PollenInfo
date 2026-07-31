package ch.stenzel.tim.polleninfo.feature.example.data.mapper

import ch.stenzel.tim.polleninfo.feature.example.data.remote.dto.ExampleResponseDto
import ch.stenzel.tim.polleninfo.feature.example.data.remote.dto.HourlyDto
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenLevel
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleMapperTest {

    private fun response(hourly: HourlyDto) = ExampleResponseDto(
        latitude = 47.3769,
        longitude = 8.5417,
        timezone = "Europe/Zurich",
        hourly = hourly,
    )

    @Test
    fun `maps every hour in the response to an hourly entry`() {
        val dto = response(
            HourlyDto(
                time = listOf("2026-07-31T00:00", "2026-07-31T01:00", "2026-07-31T02:00"),
                birch = listOf(1f, 2f, 3f),
            ),
        )

        val snapshot = dto.toDomain()

        assertEquals(3, snapshot.hourlyReadings.size)
        assertEquals(
            listOf("2026-07-31T00:00", "2026-07-31T01:00", "2026-07-31T02:00"),
            snapshot.hourlyReadings.map { it.hour },
        )
    }

    @Test
    fun `every hour carries a reading for all six pollen types`() {
        val dto = response(HourlyDto(time = listOf("2026-07-31T00:00")))

        val snapshot = dto.toDomain()

        assertEquals(
            PollenType.entries.toSet(),
            snapshot.hourlyReadings.single().readings.map { it.type }.toSet(),
        )
    }

    @Test
    fun `current readings are taken from the first hour of the snapshot`() {
        val dto = response(
            HourlyDto(
                time = listOf("2026-07-31T00:00", "2026-07-31T01:00"),
                grass = listOf(75f, 5f),
            ),
        )

        val snapshot = dto.toDomain()

        val currentGrass = snapshot.currentReadings.single { it.type == PollenType.GRASS }
        assertEquals(75f, currentGrass.valueGrainsPerM3)
        assertEquals(PollenLevel.HIGH, currentGrass.level)
        assertEquals(snapshot.hourlyReadings.first().readings, snapshot.currentReadings)
    }

    @Test
    fun `current readings are empty when the response contains no hours`() {
        val snapshot = response(HourlyDto(time = emptyList())).toDomain()

        assertTrue(snapshot.currentReadings.isEmpty())
        assertTrue(snapshot.hourlyReadings.isEmpty())
    }

    @Test
    fun `a missing series is treated as zero and NONE rather than failing`() {
        // `olive` defaults to an empty list when the API omits the series entirely.
        val dto = response(
            HourlyDto(time = listOf("2026-07-31T00:00"), birch = listOf(20f)),
        )

        val readings = dto.toDomain().currentReadings

        val olive = readings.single { it.type == PollenType.OLIVE }
        assertEquals(0f, olive.valueGrainsPerM3)
        assertEquals(PollenLevel.NONE, olive.level)

        val birch = readings.single { it.type == PollenType.BIRCH }
        assertEquals(20f, birch.valueGrainsPerM3)
        assertEquals(PollenLevel.MODERATE, birch.level)
    }

    @Test
    fun `a null value inside a series is treated as zero and NONE`() {
        val dto = response(
            HourlyDto(time = listOf("2026-07-31T00:00"), ragweed = listOf(null)),
        )

        val ragweed = dto.toDomain().currentReadings.single { it.type == PollenType.RAGWEED }

        assertEquals(0f, ragweed.valueGrainsPerM3)
        assertEquals(PollenLevel.NONE, ragweed.level)
    }

    @Test
    fun `a series shorter than the time axis falls back to zero for the missing hours`() {
        val dto = response(
            HourlyDto(
                time = listOf("2026-07-31T00:00", "2026-07-31T01:00"),
                alder = listOf(60f),
            ),
        )

        val hours = dto.toDomain().hourlyReadings

        assertEquals(60f, hours[0].readings.single { it.type == PollenType.ALDER }.valueGrainsPerM3)
        assertEquals(0f, hours[1].readings.single { it.type == PollenType.ALDER }.valueGrainsPerM3)
    }

    @Test
    fun `coordinates are carried over and rendered into the location label`() {
        val snapshot = response(HourlyDto(time = emptyList())).toDomain()

        assertEquals(47.3769, snapshot.latitude)
        assertEquals(8.5417, snapshot.longitude)
        assertEquals("47.38°N, 8.54°E", snapshot.location)
    }

    @Test
    fun `location label pads coordinates to two decimals`() {
        val snapshot = ExampleResponseDto(
            latitude = 47.0,
            longitude = 8.5,
            timezone = "Europe/Zurich",
            hourly = HourlyDto(time = emptyList()),
        ).toDomain()

        assertEquals("47.00°N, 8.50°E", snapshot.location)
    }
}
