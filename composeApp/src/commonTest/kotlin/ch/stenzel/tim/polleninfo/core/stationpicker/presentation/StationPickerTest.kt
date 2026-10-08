package ch.stenzel.tim.polleninfo.core.stationpicker.presentation

import ch.stenzel.tim.polleninfo.core.location.CoarseLocationResult
import ch.stenzel.tim.polleninfo.core.location.FakeCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.location.LOCATION_TIMEOUT
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.FakeStationRepository
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.core.station.station
import ch.stenzel.tim.polleninfo.core.stationpicker.domain.usecase.FindNearestStationUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

/**
 * The picker runs on its own scope over the test scheduler — so the timeout is driven under virtual
 * time and `Loading` is observable before the list arrives. Not `backgroundScope`: work there is
 * skipped by `advanceUntilIdle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StationPickerTest {

    private val repository = FakeStationRepository()
    private val locationProvider = FakeCoarseLocationProvider()

    private var initialAbbrCalls = 0

    private fun TestScope.picker(initialAbbr: String? = null) = StationPicker(
        repository,
        locationProvider,
        FindNearestStationUseCase(),
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job()),
    ) {
        initialAbbrCalls++
        initialAbbr
    }

    // --- loading ---------------------------------------------------------------------------

    @Test
    fun `starts in Loading before the station list arrives`() = runTest {
        assertIs<StationPickerState.Loading>(picker().state.value)
    }

    @Test
    fun `loads the stations and offers them alphabetically`() = runTest {
        val picker = picker()
        advanceUntilIdle()

        assertEquals(expectedStationNamesAlphabetical, picker.content().stations.map { it.name })
        assertEquals(1, repository.callCount)
    }

    @Test
    fun `emits a Network Error when the station list cannot be reached`() = runTest {
        repository.result = Result.Failure(IOException("Connection refused"))

        val picker = picker()
        advanceUntilIdle()

        assertEquals(StationPickerState.Error(AppError.Network), picker.state.value)
    }

    @Test
    fun `emits an Unknown Error for any other failure`() = runTest {
        repository.result = Result.Failure(RuntimeException())

        val picker = picker()
        advanceUntilIdle()

        assertEquals(StationPickerState.Error(AppError.Unknown), picker.state.value)
    }

    @Test
    fun `retrying from Error goes back through Loading`() = runTest {
        repository.result = Result.Failure(RuntimeException())
        val picker = picker()
        advanceUntilIdle()

        picker.retry()

        assertIs<StationPickerState.Loading>(picker.state.value)
    }

    @Test
    fun `retry re-requests the stations and recovers from Error`() = runTest {
        repository.result = Result.Failure(RuntimeException())
        val picker = picker()
        advanceUntilIdle()
        assertIs<StationPickerState.Error>(picker.state.value)

        repository.result = Result.Success(listOf(station()))
        picker.retry()
        advanceUntilIdle()

        assertEquals(listOf("Zürich"), picker.content().stations.map { it.name })
        assertEquals(2, repository.callCount)
    }

    // --- the initial selection -------------------------------------------------------------

    @Test
    fun `has nothing selected when there is no initial station`() = runTest {
        val picker = picker(initialAbbr = null)
        advanceUntilIdle()

        assertNull(picker.content().selected)
    }

    @Test
    fun `starts on the initial station when it is listed`() = runTest {
        val picker = picker(initialAbbr = "PLU")
        advanceUntilIdle()

        assertEquals("Lugano", picker.content().selected?.name)
    }

    @Test
    fun `has nothing selected when the initial station is not listed`() = runTest {
        val picker = picker(initialAbbr = "PXX")
        advanceUntilIdle()

        assertNull(picker.content().selected)
    }

    @Test
    fun `a retry starts on the same initial station without asking for it again`() = runTest {
        repository.result = Result.Failure(RuntimeException())
        val picker = picker(initialAbbr = "PBE")
        advanceUntilIdle()

        repository.result = FakeStationRepository().result
        picker.retry()
        advanceUntilIdle()

        assertEquals("PBE", picker.content().selected?.abbr)
        assertEquals(1, initialAbbrCalls)
    }

    // --- picking ---------------------------------------------------------------------------

    @Test
    fun `records the station the user picks`() = runTest {
        val picker = picker()
        advanceUntilIdle()
        val bern = picker.content().stations.single { it.abbr == "PBE" }

        picker.onStationSelected(bern)

        assertEquals(bern, picker.content().selected)
    }

    @Test
    fun `picking replaces the initial station`() = runTest {
        val picker = picker(initialAbbr = "PZH")
        advanceUntilIdle()
        val bern = picker.content().stations.single { it.abbr == "PBE" }

        picker.onStationSelected(bern)

        assertEquals(bern, picker.content().selected)
    }

    @Test
    fun `picking a second station replaces the first`() = runTest {
        val picker = picker()
        advanceUntilIdle()
        val stations = picker.content().stations

        picker.onStationSelected(stations.first())
        picker.onStationSelected(stations.last())

        assertEquals(stations.last(), picker.content().selected)
    }

    @Test
    fun `selecting a station leaves the offered list untouched`() = runTest {
        val picker = picker()
        advanceUntilIdle()
        val before = picker.content().stations

        picker.onStationSelected(before[3])

        assertEquals(before, picker.content().stations)
    }

    @Test
    fun `a selection made while the picker is in Error is ignored`() = runTest {
        repository.result = Result.Failure(RuntimeException())
        val picker = picker()
        advanceUntilIdle()

        picker.onStationSelected(station())

        assertIs<StationPickerState.Error>(picker.state.value)
    }

    // --- the location shortcut -------------------------------------------------------------

    @Test
    fun `a granted permission fills in the nearest station`() = runTest {
        // The fake answers with a position near Winterthur.
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = picker.content()
        assertEquals("PZH", state.selected?.abbr)
        assertNull(state.locationError)
        assertTrue(!state.isLocating)
    }

    @Test
    fun `a lookup replaces the initial station with the nearest one`() = runTest {
        val picker = picker(initialAbbr = "PGE")
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals("PZH", picker.content().selected?.abbr)
    }

    @Test
    fun `the in-progress flag is set while the lookup runs and cleared when it ends`() = runTest {
        locationProvider.answerDelay = 5.seconds
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        assertTrue(picker.content().isLocating, "set as soon as the lookup starts")

        advanceTimeBy(1.seconds)
        assertTrue(picker.content().isLocating, "and still set while the fix is outstanding")

        advanceUntilIdle()
        assertTrue(!picker.content().isLocating)
        assertEquals("PZH", picker.content().selected?.abbr)
    }

    @Test
    fun `a refused permission produces the permission error and asks for no position`() = runTest {
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = false)
        advanceUntilIdle()

        val state = picker.content()
        assertEquals(LocationError.PERMISSION_DENIED, state.locationError)
        assertTrue(!state.isLocating)
        assertNull(state.selected)
        assertEquals(0, locationProvider.callCount, "a refusal must not reach the platform")
    }

    @Test
    fun `a permission revoked below the prompt produces the permission error`() = runTest {
        locationProvider.result = CoarseLocationResult.PermissionDenied
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(LocationError.PERMISSION_DENIED, picker.content().locationError)
    }

    @Test
    fun `an unavailable position produces the could-not-determine error`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = picker.content()
        assertEquals(LocationError.UNAVAILABLE, state.locationError)
        assertTrue(!state.isLocating)
        assertNull(state.selected)
    }

    @Test
    fun `a failed lookup keeps the initial station`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val picker = picker(initialAbbr = "PLU")
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals("PLU", picker.content().selected?.abbr)
    }

    @Test
    fun `a lookup that never answers produces the could-not-determine error once it times out`() =
        runTest {
            // Virtual time, not a real sleep: the ten seconds pass instantly.
            locationProvider.neverAnswers = true
            val picker = picker()
            advanceUntilIdle()

            picker.onPermissionResult(granted = true)
            advanceTimeBy(LOCATION_TIMEOUT - 1.seconds)
            assertTrue(picker.content().isLocating, "still waiting just before the deadline")
            assertNull(picker.content().locationError)

            advanceUntilIdle()

            val state = picker.content()
            assertEquals(LocationError.UNAVAILABLE, state.locationError)
            assertTrue(!state.isLocating)
        }

    @Test
    fun `a successful lookup clears a location error left by an earlier attempt`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val picker = picker()
        advanceUntilIdle()
        picker.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.UNAVAILABLE, picker.content().locationError)

        locationProvider.result = CoarseLocationResult.Success(46.2000, 7.3000)
        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = picker.content()
        assertNull(state.locationError)
        assertEquals("PSN", state.selected?.abbr)
    }

    @Test
    fun `a lookup leaves the offered station list untouched`() = runTest {
        val picker = picker()
        advanceUntilIdle()
        val before = picker.content().stations

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(before, picker.content().stations)
    }

    @Test
    fun `a permission answer arriving before the stations are loaded is ignored`() = runTest {
        repository.result = Result.Failure(RuntimeException())
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        // There is no list to find a nearest station in, so there is nothing to say.
        assertIs<StationPickerState.Error>(picker.state.value)
        assertEquals(0, locationProvider.callCount)
    }

    // --- recovering from a failed lookup -----------------------------------------------------

    @Test
    fun `picking a station clears the permission error it replaces`() = runTest {
        locationProvider.result = CoarseLocationResult.PermissionDenied
        val picker = picker()
        advanceUntilIdle()
        picker.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.PERMISSION_DENIED, picker.content().locationError)

        picker.onStationSelected(picker.content().stations.first())

        val state = picker.content()
        assertNull(state.locationError, "the message must not outlive the pick that answers it")
        assertEquals(state.stations.first(), state.selected)
    }

    @Test
    fun `picking a station clears the could-not-determine error it replaces`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val picker = picker()
        advanceUntilIdle()
        picker.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.UNAVAILABLE, picker.content().locationError)

        picker.onStationSelected(picker.content().stations.last())

        assertNull(picker.content().locationError)
    }

    @Test
    fun `retrying the lookup clears the previous error as soon as the attempt starts`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        val picker = picker()
        advanceUntilIdle()
        picker.onPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(LocationError.UNAVAILABLE, picker.content().locationError)

        locationProvider.answerDelay = 5.seconds
        picker.onPermissionResult(granted = true)

        // Not "once the retry succeeds" — the stale message would otherwise read as this attempt
        // having already failed.
        val state = picker.content()
        assertNull(state.locationError)
        assertTrue(state.isLocating)
    }

    @Test
    fun `the location shortcut still works after the permission was refused`() = runTest {
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = false)
        advanceUntilIdle()
        // Nothing about the refusal blocks another attempt: the action is bound to isLocating
        // alone, so the device may still prompt if it permits one.
        assertTrue(!picker.content().isLocating)

        picker.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = picker.content()
        assertEquals(1, locationProvider.callCount)
        assertEquals("PZH", state.selected?.abbr)
        assertNull(state.locationError)
    }

    @Test
    fun `a repeated refusal leaves the permission message in place`() = runTest {
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = false)
        picker.onPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(LocationError.PERMISSION_DENIED, picker.content().locationError)
    }

    // --- the manual path is never overruled by a lookup ---------------------------------------

    @Test
    fun `a fix that lands after a manual pick leaves the picked station in place`() = runTest {
        // The fake would answer with a position near Winterthur — i.e. propose PZH — but only
        // after the user has already picked Bern.
        locationProvider.answerDelay = 5.seconds
        val picker = picker()
        advanceUntilIdle()
        val bern = picker.content().stations.single { it.abbr == "PBE" }

        picker.onPermissionResult(granted = true)
        advanceTimeBy(1.seconds)
        picker.onStationSelected(bern)
        advanceUntilIdle()

        val state = picker.content()
        assertEquals(bern, state.selected, "a late fix must not silently re-pick for the user")
        assertTrue(!state.isLocating)
        assertNull(state.locationError)
    }

    @Test
    fun `a manual pick stops the lookup rather than letting it run on`() = runTest {
        locationProvider.answerDelay = 5.seconds
        val picker = picker()
        advanceUntilIdle()

        picker.onPermissionResult(granted = true)
        advanceTimeBy(1.seconds)
        picker.onStationSelected(picker.content().stations.first())
        advanceUntilIdle()

        assertTrue(locationProvider.wasCancelled, "cancellation must reach the platform request")
    }

    @Test
    fun `a failed lookup that lands after a manual pick raises no error`() = runTest {
        locationProvider.result = CoarseLocationResult.Unavailable
        locationProvider.answerDelay = 5.seconds
        val picker = picker()
        advanceUntilIdle()
        val sion = picker.content().stations.single { it.abbr == "PSN" }

        picker.onPermissionResult(granted = true)
        picker.onStationSelected(sion)
        advanceUntilIdle()

        val state = picker.content()
        assertNull(state.locationError, "the lookup was called off; it has nothing left to report")
        assertEquals(sion, state.selected)
    }
}

private fun StationPicker.content(): StationPickerState.Content =
    assertIs<StationPickerState.Content>(state.value)
