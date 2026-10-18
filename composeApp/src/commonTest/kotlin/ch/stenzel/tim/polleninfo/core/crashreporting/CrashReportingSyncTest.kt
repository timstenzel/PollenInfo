package ch.stenzel.tim.polleninfo.core.crashreporting

import ch.stenzel.tim.polleninfo.core.preferences.CrashReportingPreferences
import ch.stenzel.tim.polleninfo.core.preferences.FakeCrashReportingPreferences
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrashReportingSyncTest {

    private val reporter = FakeCrashReporter()

    @Test
    fun `with nothing stored the reporter is enabled`() = runTest {
        val sync = CrashReportingSync(FakeCrashReportingPreferences(), reporter)

        backgroundScope.launch { sync.run() }
        runCurrent()

        assertEquals(listOf(true), reporter.applied)
    }

    @Test
    fun `a stored opt-out disables the reporter at start`() = runTest {
        val sync = CrashReportingSync(FakeCrashReportingPreferences(initial = false), reporter)

        backgroundScope.launch { sync.run() }
        runCurrent()

        assertEquals(listOf(false), reporter.applied)
    }

    @Test
    fun `every change of the choice reaches the reporter`() = runTest {
        val preferences = FakeCrashReportingPreferences()
        backgroundScope.launch { CrashReportingSync(preferences, reporter).run() }
        runCurrent()

        preferences.setEnabled(false)
        runCurrent()
        preferences.setEnabled(true)
        runCurrent()

        assertEquals(listOf(true, false, true), reporter.applied)
    }

    @Test
    fun `repeated equal values reach the reporter once`() = runTest {
        val preferences = object : CrashReportingPreferences {
            // A raw flow repeats values that a StateFlow would already have merged.
            override val enabled: Flow<Boolean> = flow {
                emit(false)
                emit(false)
                emit(true)
                emit(true)
                emit(false)
            }

            override suspend fun setEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        }

        CrashReportingSync(preferences, reporter).run()

        assertEquals(listOf(false, true, false), reporter.applied)
    }

    @Test
    fun `an unavailable reporter is never called`() = runTest {
        val unavailable = FakeCrashReporter(isAvailable = false)
        val preferences = FakeCrashReportingPreferences()

        // Returns at once: there is nothing to follow.
        CrashReportingSync(preferences, unavailable).run()
        preferences.setEnabled(false)

        assertTrue(unavailable.applied.isEmpty())
    }

    @Test
    fun `a failing preference neither crashes nor calls the reporter`() = runTest {
        val failing = object : CrashReportingPreferences {
            override val enabled: Flow<Boolean> = flow { throw IOException("unreadable") }

            override suspend fun setEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        }

        CrashReportingSync(failing, reporter).run()

        assertTrue(reporter.applied.isEmpty())
    }
}
