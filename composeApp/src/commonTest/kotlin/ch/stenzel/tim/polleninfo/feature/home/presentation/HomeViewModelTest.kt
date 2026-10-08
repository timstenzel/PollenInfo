package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.core.diary.FakeDiaryRepository
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.measurement.FakeStationMeasurementRepository
import ch.stenzel.tim.polleninfo.core.measurement.MEASURED_AT
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.measurement.measurement
import ch.stenzel.tim.polleninfo.core.measurement.reading
import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import ch.stenzel.tim.polleninfo.core.preferences.FakeSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val zurich = SelectedStation(abbr = "PZH", name = "Zürich")
    private val lugano = SelectedStation(abbr = "PLU", name = "Lugano")

    private var selectedStationRepository = FakeSelectedStationRepository(initial = zurich)
    private val measurementRepository = FakeStationMeasurementRepository()
    private var diaryRepository = FakeDiaryRepository()

    /** Wall-clock time the ViewModel stamps on each received reading; moved by hand, never ticks. */
    private val clock = object : Clock {
        var now = Instant.parse("2026-08-01T09:12:00Z")
        override fun now() = now
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(
        selectedStationRepository,
        GetStationMeasurementUseCase(measurementRepository),
        diaryRepository,
        clock,
    )

    @Test
    fun `starts in Loading before the readings arrive`() {
        assertIs<HomeUiState.Loading>(viewModel().uiState.value)
    }

    @Test
    fun `resolves to Content with the overall severity`() = runTest {
        measurementRepository.result = Result.Success(
            measurement(
                species = listOf(
                    reading("Birch", 42, PollenSeverity.MODERATE),
                    reading("Grasses", 250, PollenSeverity.VERY_HIGH),
                ),
            ),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertEquals(PollenSeverity.VERY_HIGH, state.overallSeverity)
    }

    @Test
    fun `Content carries the ordered taxa the unit and the responsible taxon`() = runTest {
        measurementRepository.result = Result.Success(
            measurement(
                species = listOf(
                    reading("Ash"),
                    reading("Birch", 42, PollenSeverity.MODERATE),
                    reading("Grasses", 20, PollenSeverity.HIGH),
                ),
            ),
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("Grasses", "Birch", "Ash"), state.species.map { it.name })
        assertEquals("Grasses", state.drivenBy)
        assertEquals("grains/m3", state.unit)
    }

    @Test
    fun `Content carries the reading's timestamp unchanged`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(MEASURED_AT, assertIs<HomeUiState.Content>(viewModel.uiState.value).measuredAt)
    }

    @Test
    fun `requests the readings for the stored station`() = runTest {
        viewModel()

        advanceUntilIdle()

        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `names the station while the readings are still loading`() = runTest {
        val viewModel = viewModel()

        // Enough for the stored selection to arrive, but the load is still in flight: the top bar
        // has to be able to label itself already.
        advanceUntilIdle()

        assertEquals("Zürich", viewModel.uiState.value.stationName)
    }

    @Test
    fun `names the station on every state including the failure`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Error>(viewModel.uiState.value)
        assertEquals("Zürich", state.stationName)
    }

    @Test
    fun `a transport failure resolves to a Network Error`() = runTest {
        measurementRepository.result = Result.Failure(IOException("no network"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(HomeUiState.Error("Zürich", AppError.Network), viewModel.uiState.value)
    }

    @Test
    fun `a server error resolves to a ServerUnavailable Error`() = runTest {
        measurementRepository.result = Result.Failure(HttpStatusException(502))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(HomeUiState.Error("Zürich", AppError.ServerUnavailable), viewModel.uiState.value)
    }

    @Test
    fun `any other failure resolves to an Unknown Error`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException())
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(HomeUiState.Error("Zürich", AppError.Unknown), viewModel.uiState.value)
    }

    @Test
    fun `reloads when the stored station changes`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        selectedStationRepository.select(lugano)
        advanceUntilIdle()

        // The station name and the readings must never come from different stations.
        assertEquals(listOf("PZH", "PLU"), measurementRepository.requested)
        assertEquals("Lugano", viewModel.uiState.value.stationName)
    }

    @Test
    fun `does not reload when the stored station is written again unchanged`() = runTest {
        viewModel()
        advanceUntilIdle()

        selectedStationRepository.select(zurich)
        advanceUntilIdle()

        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `refreshing keeps the previous readings on screen while the reload runs`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = assertIs<HomeUiState.Content>(viewModel.uiState.value)

        val gate = CompletableDeferred<Unit>()
        measurementRepository.gate = gate
        measurementRepository.result = Result.Success(
            measurement(species = listOf(reading("Grasses", 250, PollenSeverity.VERY_HIGH))),
        )
        viewModel.refresh()
        advanceUntilIdle()

        // Held in flight: the old readings are still the ones shown, only flagged.
        assertEquals(before.copy(isRefreshing = true), viewModel.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()

        val after = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertFalse(after.isRefreshing)
        assertEquals(PollenSeverity.VERY_HIGH, after.overallSeverity)
        assertEquals(listOf("PZH", "PZH"), measurementRepository.requested)
    }

    @Test
    fun `Content is stamped with the time the reading was received`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(clock.now, assertIs<HomeUiState.Content>(viewModel.uiState.value).refreshedAt)
    }

    @Test
    fun `refreshing inside the cache period leaves the reading and its age unchanged`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = assertIs<HomeUiState.Content>(viewModel.uiState.value)

        // The backend answers a refresh inside its cache period with the very same reading; only
        // the refresh time moves, which is how the user can tell the refresh happened at all.
        clock.now += 5.minutes
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(before.copy(refreshedAt = clock.now), viewModel.uiState.value)
        assertEquals(MEASURED_AT, assertIs<HomeUiState.Content>(viewModel.uiState.value).measuredAt)
    }

    @Test
    fun `an instant refresh stays flagged for the minimum indicator time`() = runTest {
        // Regression: a cached response landed before the next frame, so the screen never saw
        // `isRefreshing` become true and PullToRefreshBox never retracted its indicator.
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = assertIs<HomeUiState.Content>(viewModel.uiState.value)

        viewModel.refresh()
        advanceTimeBy(HomeViewModel.MIN_REFRESH_INDICATOR - 1.milliseconds)
        runCurrent()
        assertTrue(assertIs<HomeUiState.Content>(viewModel.uiState.value).isRefreshing)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `a failed refresh resolves to Error`() = runTest {
        // Matches the reference feature: there is no one-shot channel on this screen yet, and an
        // Error with a retry is honest, where silently keeping the old readings would not be.
        val viewModel = viewModel()
        advanceUntilIdle()

        measurementRepository.result = Result.Failure(IOException("no network"))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(AppError.Network, assertIs<HomeUiState.Error>(viewModel.uiState.value).error)
    }

    @Test
    fun `retrying from Error shows Loading and then the readings`() = runTest {
        measurementRepository.result = Result.Failure(RuntimeException("no network"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertIs<HomeUiState.Error>(viewModel.uiState.value)

        measurementRepository.result = Result.Success(measurement())
        viewModel.retry()

        assertEquals(HomeUiState.Loading("Zürich"), viewModel.uiState.value)

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertEquals("Zürich", state.stationName)
        assertEquals(listOf("PZH", "PZH"), measurementRepository.requested)
    }

    @Test
    fun `no stored station resolves to a NoStationSelected Error rather than a spinner`() = runTest {
        selectedStationRepository = FakeSelectedStationRepository(initial = null)
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Error>(viewModel.uiState.value)
        assertEquals(AppError.NoStationSelected, state.error)
        assertEquals(emptyList(), measurementRepository.requested)
    }

    @Test
    fun `a station change while a refresh is in flight discards the old station's response`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            val zurichRefresh = CompletableDeferred<Unit>()
            measurementRepository.gate = zurichRefresh
            viewModel.refresh()
            advanceUntilIdle()

            measurementRepository.gate = null
            measurementRepository.result = Result.Success(measurement(stationAbbr = "PLU"))
            selectedStationRepository.select(lugano)
            advanceUntilIdle()

            // The superseded Zürich refresh now answers late, and with a failure: it must have been
            // cancelled, or it would overwrite Lugano's readings with an error.
            measurementRepository.result = Result.Failure(RuntimeException("late"))
            zurichRefresh.complete(Unit)
            advanceUntilIdle()

            val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
            assertEquals("Lugano", state.stationName)
            assertFalse(state.isRefreshing)
        }

    // --- The feeling prompt ---------------------------------------------------------------------

    /** The Swiss date of [clock]'s starting time, 09:12 UTC on 1 August. */
    private val today = LocalDate(2026, 8, 1)

    @Test
    fun `the feeling prompt is shown on Content when today has no answer`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertTrue(state.showFeelingPrompt)
        assertFalse(state.feelingSaveError)
    }

    @Test
    fun `the feeling prompt is never part of Loading or Error`() = runTest {
        // Neither state has the field at all; what is pinned here is that the ViewModel does not
        // leave Loading or Error just because the diary answered first.
        measurementRepository.gate = CompletableDeferred()
        val loading = viewModel()
        advanceUntilIdle()
        assertIs<HomeUiState.Loading>(loading.uiState.value)

        measurementRepository.gate = null
        measurementRepository.result = Result.Failure(RuntimeException("no network"))
        val failing = viewModel()
        advanceUntilIdle()
        assertIs<HomeUiState.Error>(failing.uiState.value)
    }

    @Test
    fun `the feeling prompt is hidden when today already has an answer`() = runTest {
        diaryRepository = FakeDiaryRepository(initialEntries = listOf(DiaryEntry(today, Feeling.GOOD)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `the feeling prompt is hidden when it was dismissed today`() = runTest {
        diaryRepository = FakeDiaryRepository(initialDismissedOn = today)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `answers from earlier days do not hide the feeling prompt`() = runTest {
        val yesterday = LocalDate(2026, 7, 31)
        diaryRepository = FakeDiaryRepository(
            initialEntries = listOf(DiaryEntry(yesterday, Feeling.BAD)),
            initialDismissedOn = yesterday,
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `answering records today's feeling and hides the prompt without a reload`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        assertEquals(listOf(DiaryEntry(today, Feeling.BAD)), diaryRepository.storedEntries)
        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `an answer stored from elsewhere hides the prompt through the diary`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        diaryRepository.record(today, Feeling.VERY_GOOD)
        advanceUntilIdle()

        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `dismissing hides the prompt for today and records no answer`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingPromptDismissed()
        advanceUntilIdle()

        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
        assertEquals(today, diaryRepository.storedDismissedOn)
        assertEquals(emptyList(), diaryRepository.storedEntries)
    }

    @Test
    fun `a dismissed prompt stays hidden after a refresh later the same Swiss day`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onFeelingPromptDismissed()
        advanceUntilIdle()

        // 21:59 UTC is 23:59 in Zürich: still 1 August.
        clock.now = Instant.parse("2026-08-01T21:59:00Z")
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `the prompt returns once the clock is on the next Swiss day`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onFeelingPromptDismissed()
        advanceUntilIdle()

        // 22:00 UTC is midnight in Zürich (CEST): 2 August there, still 1 August in UTC.
        clock.now = Instant.parse("2026-08-01T22:00:00Z")
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `an answer from yesterday lets the prompt return the next day`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onFeelingSelected(Feeling.GOOD)
        advanceUntilIdle()

        clock.now += 24.hours
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(assertIs<HomeUiState.Content>(viewModel.uiState.value).showFeelingPrompt)
    }

    @Test
    fun `a failed save keeps the prompt and reports the error`() = runTest {
        diaryRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertTrue(state.showFeelingPrompt)
        assertTrue(state.feelingSaveError)
        assertEquals(emptyList(), diaryRepository.storedEntries)
    }

    @Test
    fun `a successful retry after a failed save clears the error and hides the prompt`() = runTest {
        diaryRepository.failWrite = true
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        diaryRepository.failWrite = false
        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Content>(viewModel.uiState.value)
        assertFalse(state.showFeelingPrompt)
        assertFalse(state.feelingSaveError)
    }
}
