package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.core.diary.FakeDiaryRepository
import ch.stenzel.tim.polleninfo.core.diary.FakeFeelingPromptPreferences
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
import ch.stenzel.tim.polleninfo.core.session.FakeSessionRepository
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.testSession
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val zurich = SelectedStation(abbr = "PZH", name = "Zürich")
    private val lugano = SelectedStation(abbr = "PLU", name = "Lugano")

    private var selectedStationRepository = FakeSelectedStationRepository(initial = zurich)
    private val measurementRepository = FakeStationMeasurementRepository()
    /** Signed out unless a test logs in; the readings do not depend on it. */
    private val sessionRepository = FakeSessionRepository()
    private val diaryRepository = FakeDiaryRepository(today = LocalDate(2026, 8, 1))
    private val feelingPromptPreferences = FakeFeelingPromptPreferences()

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
        sessionRepository,
        diaryRepository,
        feelingPromptPreferences,
        clock,
    )

    /** Drains the one-shot event channel; see `OnboardingViewModelTest.collectEvents`. */
    private fun TestScope.events(viewModel: HomeViewModel): List<HomeEvent> {
        val received = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { received += it }
        }
        return received
    }

    private fun HomeViewModel.content() = assertIs<HomeUiState.Content>(uiState.value)

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
        assertEquals("Grasses", state.drivenBy?.name)
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

    // --- The feeling prompt, signed out ---

    @Test
    fun `signed out the feeling prompt is shown on Content unless dismissed`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.content()
        assertTrue(state.showFeelingPrompt)
        assertNull(state.feelingSaveError)
        // Nobody to ask: the account's diary is never contacted.
        assertEquals(0, diaryRepository.todayCalls)
    }

    @Test
    fun `the feeling prompt is never part of Loading or Error`() = runTest {
        // Neither state has the field at all; what is pinned here is that the ViewModel does not
        // leave Loading or Error just because the prompt's inputs answered first.
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
    fun `signed out the feeling prompt is hidden when it was dismissed today`() = runTest {
        feelingPromptPreferences.dismiss(today)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `a dismissal outlives the ViewModel that made it`() = runTest {
        val first = viewModel()
        advanceUntilIdle()
        first.onFeelingPromptDismissed()
        advanceUntilIdle()

        // The same stored preferences under a new ViewModel: the app started again.
        val second = viewModel()
        advanceUntilIdle()

        assertFalse(second.content().showFeelingPrompt)
    }

    @Test
    fun `a dismissal from an earlier day does not hide the feeling prompt`() = runTest {
        feelingPromptPreferences.dismiss(LocalDate(2026, 7, 31))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `signed out a feeling tap asks for the login once and records nothing`() = runTest {
        val viewModel = viewModel()
        val events = events(viewModel)
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenAccount), events)
        assertEquals(emptyList(), diaryRepository.recorded)
        // Nothing happened the user did not choose: back from the login, the prompt is still up.
        assertTrue(viewModel.content().showFeelingPrompt)
        assertNull(feelingPromptPreferences.storedDismissedOn)
    }

    @Test
    fun `dismissing hides the prompt for today and records no answer`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingPromptDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)
        assertEquals(today, feelingPromptPreferences.storedDismissedOn)
        assertEquals(emptyList(), diaryRepository.recorded)
        assertEquals(listOf("PZH"), measurementRepository.requested)
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

        assertFalse(viewModel.content().showFeelingPrompt)
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

        assertTrue(viewModel.content().showFeelingPrompt)
    }

    // --- The feeling prompt, signed in ---

    @Test
    fun `signed in the prompt stays hidden until the account says today is unanswered`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.todayGate = CompletableDeferred()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)

        diaryRepository.todayGate?.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `signed in the prompt is hidden when the account has today's answer`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.stored[today] = Feeling.GOOD
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `signed in a failed check hides the prompt until a refresh succeeds`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.todayFailure = IOException("no network")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)

        diaryRepository.todayFailure = null
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `signed in a dismissal today hides the unanswered prompt`() = runTest {
        sessionRepository.current = testSession
        feelingPromptPreferences.dismiss(today)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)
    }

    @Test
    fun `signed in answering sends the feeling and hides the prompt without a reload`() = runTest {
        sessionRepository.current = testSession
        val viewModel = viewModel()
        val events = events(viewModel)
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        assertEquals(listOf(Feeling.BAD), diaryRepository.recorded)
        assertEquals(Feeling.BAD, diaryRepository.stored[today])
        assertFalse(viewModel.content().showFeelingPrompt)
        assertEquals(emptyList(), events)
        assertEquals(listOf("PZH"), measurementRepository.requested)
    }

    @Test
    fun `signed in a tap while an answer is being sent is ignored`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.recordGate = CompletableDeferred()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()
        viewModel.onFeelingSelected(Feeling.VERY_GOOD)
        diaryRepository.recordGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(Feeling.BAD), diaryRepository.recorded)
    }

    @Test
    fun `signed in a failed answer keeps the prompt with its error until the next attempt`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.recordFailure = IOException("no network")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        val failed = viewModel.content()
        assertTrue(failed.showFeelingPrompt)
        assertEquals(AppError.Network, failed.feelingSaveError)

        // The next attempt clears the error at once, while it is still being sent.
        diaryRepository.recordFailure = null
        diaryRepository.recordGate = CompletableDeferred()
        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()
        assertNull(viewModel.content().feelingSaveError)
        assertTrue(viewModel.content().showFeelingPrompt)

        diaryRepository.recordGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.content().showFeelingPrompt)
        assertNull(viewModel.content().feelingSaveError)
    }

    @Test
    fun `a refused session on answering leaves the app signed out and the next tap asks for the login`() = runTest {
        sessionRepository.current = testSession
        diaryRepository.recordFailure = SignedOutException()
        // As the real repository does on a 401.
        diaryRepository.onFailure = { sessionRepository.clear() }
        val viewModel = viewModel()
        val events = events(viewModel)
        advanceUntilIdle()

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        assertNull(sessionRepository.current)
        val state = viewModel.content()
        assertTrue(state.showFeelingPrompt)
        assertNull(state.feelingSaveError)
        assertEquals(emptyList(), events)

        viewModel.onFeelingSelected(Feeling.BAD)
        advanceUntilIdle()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenAccount), events)
        assertEquals(listOf(Feeling.BAD), diaryRepository.recorded)
    }

    @Test
    fun `logging in re-checks today and logging out shows the prompt again`() = runTest {
        diaryRepository.stored[today] = Feeling.GOOD
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.content().showFeelingPrompt)

        // Logged in on the Diary while Home stays on the back stack.
        sessionRepository.current = testSession
        advanceUntilIdle()
        assertFalse(viewModel.content().showFeelingPrompt)
        assertEquals(1, diaryRepository.todayCalls)

        sessionRepository.current = null
        advanceUntilIdle()
        assertTrue(viewModel.content().showFeelingPrompt)
        assertEquals(1, diaryRepository.todayCalls)
    }

    @Test
    fun `a refresh picks up an answer given on another phone`() = runTest {
        sessionRepository.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.content().showFeelingPrompt)

        diaryRepository.stored[today] = Feeling.VERY_GOOD
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.content().showFeelingPrompt)
        assertEquals(2, diaryRepository.todayCalls)
    }

    @Test
    fun `signed in an answer from yesterday lets the prompt return on the next Swiss day`() = runTest {
        sessionRepository.current = testSession
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onFeelingSelected(Feeling.GOOD)
        advanceUntilIdle()
        assertFalse(viewModel.content().showFeelingPrompt)

        clock.now += 24.hours
        diaryRepository.today = LocalDate(2026, 8, 2)
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.content().showFeelingPrompt)
    }
}
