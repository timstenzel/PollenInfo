package ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.measurement.measurement
import ch.stenzel.tim.polleninfo.core.measurement.reading
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.allStations
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.allstations.PerStationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GetAllStationReadingsUseCaseTest {

    private val repository = PerStationMeasurementRepository()
    private val useCase = GetAllStationReadingsUseCase(GetStationMeasurementUseCase(repository))

    private fun List<StationReading>.byAbbr(abbr: String) = first { it.station.abbr == abbr }

    @Test
    fun `the first emission lists every station as pending in alphabetical order`() = runTest {
        val first = useCase(allStations.shuffled()).toList().first()

        assertEquals(expectedStationNamesAlphabetical, first.map { it.station.name })
        assertTrue(first.all { it is StationReading.Pending })
    }

    @Test
    fun `requests every station's reading`() = runTest {
        useCase(allStations).toList()

        assertEquals(allStations.map { it.abbr }.toSet(), repository.requested.toSet())
        assertEquals(allStations.size, repository.requested.size)
    }

    @Test
    fun `the final emission has every station resolved`() = runTest {
        val last = useCase(allStations).toList().last()

        assertTrue(last.none { it is StationReading.Pending })
        assertEquals(allStations.size, last.size)
    }

    @Test
    fun `emits once up front and once per station`() = runTest {
        assertEquals(1 + allStations.size, useCase(allStations).toList().size)
    }

    @Test
    fun `a station whose reading fails ends unavailable while the others end available`() = runTest {
        repository.failFor("PLU")

        val last = useCase(allStations).toList().last()

        assertIs<StationReading.Unavailable>(last.byAbbr("PLU"))
        assertTrue(last.filter { it.station.abbr != "PLU" }.all { it is StationReading.Available })
    }

    @Test
    fun `the order never changes between emissions`() = runTest {
        repository.failFor("PBS", "PZH")

        val emissions = useCase(allStations).toList()

        emissions.forEach { emission ->
            assertEquals(expectedStationNamesAlphabetical, emission.map { it.station.name })
        }
    }

    @Test
    fun `a held station stays pending while the others resolve`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gates["PGE"] = gate
        val emissions = mutableListOf<List<StationReading>>()

        val collecting = launch { useCase(allStations).collect { emissions += it } }
        advanceUntilIdle()

        // Every other station has answered without waiting for the held one.
        val meanwhile = emissions.last()
        assertIs<StationReading.Pending>(meanwhile.byAbbr("PGE"))
        assertTrue(meanwhile.filter { it.station.abbr != "PGE" }.all { it is StationReading.Available })
        assertTrue(collecting.isActive, "the flow completes only once every station has resolved")

        gate.complete(Unit)
        advanceUntilIdle()

        assertIs<StationReading.Available>(emissions.last().byAbbr("PGE"))
        assertTrue(collecting.isCompleted)
    }

    @Test
    fun `an available station's overall severity is its worst measured taxon`() = runTest {
        repository.results["PBE"] = Result.Success(
            measurement(
                stationAbbr = "PBE",
                species = listOf(
                    reading("Grasses", 3, PollenSeverity.LOW),
                    reading("Birch", 120, PollenSeverity.HIGH),
                    reading("Ash"),
                ),
            ),
        )

        val bern = assertIs<StationReading.Available>(useCase(allStations).toList().last().byAbbr("PBE"))

        assertEquals(PollenSeverity.HIGH, bern.overview.overallSeverity)
        assertEquals(listOf("Birch", "Grasses", "Ash"), bern.overview.species.map { it.name })
    }

    @Test
    fun `no stations gives one empty emission`() = runTest {
        assertEquals(listOf(emptyList()), useCase(emptyList()).toList())
    }
}
