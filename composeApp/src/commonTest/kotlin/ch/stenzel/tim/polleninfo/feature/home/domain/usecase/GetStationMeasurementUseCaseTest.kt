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
import kotlin.test.assertNull

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
    fun `carries the unit and every taxon through`() = runTest {
        val species = listOf(reading("Birch", 42, PollenSeverity.MODERATE), reading("Ash"))

        val overview = overviewOf(species)

        assertEquals("grains/m3", overview.unit)
        assertEquals(species.toSet(), overview.species.toSet())
    }

    @Test
    fun `orders the taxa worst first`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Alder", 0, PollenSeverity.NONE),
                reading("Birch", 42, PollenSeverity.MODERATE),
                reading("Grasses", 20, PollenSeverity.HIGH),
            ),
        )

        assertEquals(listOf("Grasses", "Birch", "Alder"), overview.species.map { it.name })
    }

    @Test
    fun `taxa of equal severity are ordered alphabetically`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Oak", 30, PollenSeverity.MODERATE),
                reading("Ash", 50, PollenSeverity.MODERATE),
            ),
        )

        // Alphabetical, not by concentration: Ash comes first even though Oak was listed first and
        // reads lower — the tie-break is what keeps the list from reshuffling between visits.
        assertEquals(listOf("Ash", "Oak"), overview.species.map { it.name })
    }

    @Test
    fun `taxa with no reading come last and alphabetically among themselves`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Oak"),
                reading("Alder", 0, PollenSeverity.NONE),
                reading("Birch"),
                reading("Grasses", 20, PollenSeverity.HIGH),
            ),
        )

        // Below NONE, not hidden: an unmeasured taxon must still be findable in the list.
        assertEquals(listOf("Grasses", "Alder", "Birch", "Oak"), overview.species.map { it.name })
    }

    @Test
    fun `orders all seven taxa of a full reading`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Alder", 0, PollenSeverity.NONE),
                reading("Birch", 42, PollenSeverity.MODERATE),
                reading("Hazel", 0, PollenSeverity.NONE),
                reading("Beech", 3, PollenSeverity.LOW),
                reading("Ash"),
                reading("Oak", 1500, PollenSeverity.VERY_HIGH),
                reading("Grasses", 20, PollenSeverity.HIGH),
            ),
        )

        assertEquals(
            listOf("Oak", "Grasses", "Birch", "Beech", "Alder", "Hazel", "Ash"),
            overview.species.map { it.name },
        )
    }

    @Test
    fun `names the taxon responsible for the overall severity`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Birch", 42, PollenSeverity.MODERATE),
                reading("Grasses", 20, PollenSeverity.HIGH),
                reading("Alder", 0, PollenSeverity.NONE),
            ),
        )

        assertEquals("Grasses", overview.drivenBy?.name)
    }

    @Test
    fun `when several taxa share the worst severity the alphabetically first is named`() = runTest {
        val overview = overviewOf(
            listOf(
                reading("Oak", 100, PollenSeverity.HIGH),
                reading("Grasses", 20, PollenSeverity.HIGH),
            ),
        )

        assertEquals("Grasses", overview.drivenBy?.name)
    }

    @Test
    fun `a taxon with no reading is never named as responsible`() = runTest {
        // Ash sorts first alphabetically, so this fails if the driver ignores the severity.
        val overview = overviewOf(listOf(reading("Ash"), reading("Birch", 0, PollenSeverity.NONE)))

        assertEquals("Birch", overview.drivenBy?.name)
    }

    @Test
    fun `no taxon is named when nothing was measured`() = runTest {
        val overview = overviewOf(listOf(reading("Ash"), reading("Oak")))

        assertNull(overview.drivenBy)
    }

    @Test
    fun `passes a repository failure through untouched`() = runTest {
        val failure = Result.Failure(RuntimeException("no network"))
        repository.result = failure

        assertEquals(failure, useCase("PZH"))
    }
}
