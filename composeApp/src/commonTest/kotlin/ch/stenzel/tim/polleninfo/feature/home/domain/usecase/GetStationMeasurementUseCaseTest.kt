package ch.stenzel.tim.polleninfo.feature.home.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.FakeStationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationPollenOverview
import ch.stenzel.tim.polleninfo.feature.home.measurement
import ch.stenzel.tim.polleninfo.feature.home.reading
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetStationMeasurementUseCaseTest {

    private val repository = FakeStationMeasurementRepository()
    private val useCase = GetStationMeasurementUseCase(repository)

    private suspend fun overviewOf(species: List<SpeciesReading>): StationPollenOverview {
        repository.result = Result.Success(measurement(species = species))
        return assertIs<Result.Success<StationPollenOverview>>(useCase("PZH")).data
    }

    @Test
    fun `asks the repository for the station it was given`() = runTest {
        useCase("PLU")

        assertEquals(listOf("PLU"), repository.requested)
    }

    @Test
    fun `the overall severity is the worst among the taxa`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Alder", 0, PollenSeverity.NONE),
                reading("Grasses", 250, PollenSeverity.VERY_HIGH),
                reading("Birch", 42, PollenSeverity.MODERATE),
            ),
        )

        // A severe reading must never hide behind mild ones, so this is a maximum, not an average.
        assertEquals(PollenSeverity.VERY_HIGH, overview.overallSeverity)
    }

    @Test
    fun `a taxon with no reading does not affect the overall severity`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Ash"),
                reading("Birch", 42, PollenSeverity.MODERATE),
            ),
        )

        assertEquals(PollenSeverity.MODERATE, overview.overallSeverity)
    }

    @Test
    fun `an unmeasured taxon beside nothing but calm readings still reads as none`() = runTest {
        // The absent measurement must not be counted as NONE either — it simply takes no part.
        val overview = overviewOf(
            listOf(
                reading("Ash"),
                reading("Birch", 0, PollenSeverity.NONE),
                reading("Grasses", 0, PollenSeverity.NONE),
            ),
        )

        assertEquals(PollenSeverity.NONE, overview.overallSeverity)
    }

    @Test
    fun `a reading in which nothing was measured resolves to none rather than crashing`() = runTest {
        // The endpoint answers 404 for this, so it should not arise; it is spelled out because a
        // crash would be a worse answer than a calm one.
        val overview = overviewOf(listOf(reading("Ash"), reading("Oak")))

        assertEquals(PollenSeverity.NONE, overview.overallSeverity)
    }

    @Test
    fun `carries the unit and the taxa through`() = runTest {
        val species = listOf(reading("Birch", 42, PollenSeverity.MODERATE), reading("Ash"))

        val overview = overviewOf(species)

        assertEquals("grains/m3", overview.unit)
        assertEquals(species, overview.species)
    }

    @Test
    fun `passes a repository failure through untouched`() = runTest {
        val failure = Result.Failure(RuntimeException("no network"))
        repository.result = failure

        assertEquals(failure, useCase("PZH"))
    }
}
