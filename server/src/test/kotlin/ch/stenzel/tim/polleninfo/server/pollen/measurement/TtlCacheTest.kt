package ch.stenzel.tim.polleninfo.server.pollen.measurement

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TtlCacheTest {

    private val ttl = Duration.ofMinutes(30)
    private val clock = MutableClock()
    private val cache = TtlCache<String, String>(ttl, clock)

    /** Hands out "v1", "v2", … and counts its calls; throws [failure] instead while it is set. */
    private class CountingLoader {
        var calls = 0
        var failure: Exception? = null

        suspend fun load(key: String): String {
            calls++
            failure?.let { throw it }
            return "$key-v$calls"
        }
    }

    private val loader = CountingLoader()

    @Test
    fun `a miss runs the loader and returns its value as fresh`() = runTest {
        assertEquals(CacheResult.Fresh("a-v1"), cache.get("a", loader::load))
        assertEquals(1, loader.calls)
    }

    @Test
    fun `a hit inside the time to live does not run the loader again`() = runTest {
        cache.get("a", loader::load)
        clock.advanceBy(ttl.minusSeconds(1))

        assertEquals(CacheResult.Fresh("a-v1"), cache.get("a", loader::load))
        assertEquals(1, loader.calls)
    }

    @Test
    fun `an entry exactly as old as the time to live is reloaded`() = runTest {
        cache.get("a", loader::load)
        clock.advanceBy(ttl)

        assertEquals(CacheResult.Fresh("a-v2"), cache.get("a", loader::load))
        assertEquals(2, loader.calls)
    }

    @Test
    fun `keys are cached independently`() = runTest {
        cache.get("a", loader::load)

        assertEquals(CacheResult.Fresh("b-v2"), cache.get("b", loader::load))
        assertEquals(CacheResult.Fresh("a-v1"), cache.get("a", loader::load))
        assertEquals(2, loader.calls)
    }

    @Test
    fun `a failed load with nothing cached is a failure carrying the cause`() = runTest {
        val cause = IOException("unreachable")
        loader.failure = cause

        val result = assertIs<CacheResult.Failed>(cache.get("a", loader::load))
        assertSame(cause, result.cause)
    }

    @Test
    fun `a failed reload returns the previous value as stale`() = runTest {
        cache.get("a", loader::load)
        clock.advanceBy(ttl)
        loader.failure = IOException("unreachable")

        assertEquals(CacheResult.Stale("a-v1"), cache.get("a", loader::load))
    }

    @Test
    fun `a failed reload keeps the original age so the next call tries again`() = runTest {
        cache.get("a", loader::load)
        clock.advanceBy(ttl)
        loader.failure = IOException("unreachable")
        cache.get("a", loader::load)

        // Had the failure re-stamped the entry, this would be a fresh hit with no load.
        assertEquals(CacheResult.Stale("a-v1"), cache.get("a", loader::load))
        assertEquals(3, loader.calls)

        loader.failure = null
        assertEquals(CacheResult.Fresh("a-v4"), cache.get("a", loader::load))
    }

    @Test
    fun `a failure is not cached`() = runTest {
        loader.failure = IOException("unreachable")
        cache.get("a", loader::load)
        loader.failure = null

        assertEquals(CacheResult.Fresh("a-v2"), cache.get("a", loader::load))
    }

    @Test
    fun `concurrent misses on one key run the loader once and share its value`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val slowLoader: suspend (String) -> String = {
            calls++
            gate.await()
            "loaded"
        }

        val results = List(5) { async { cache.get("a", slowLoader) } }
        runCurrent()
        gate.complete(Unit)

        assertEquals(List(5) { CacheResult.Fresh("loaded") }, results.awaitAll())
        assertEquals(1, calls)
    }

    @Test
    fun `concurrent misses on one key share a failure instead of retrying in turn`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val failingLoader: suspend (String) -> String = {
            calls++
            gate.await()
            throw IOException("unreachable")
        }

        val results = List(5) { async { cache.get("a", failingLoader) } }
        runCurrent()
        gate.complete(Unit)

        assertTrue(results.awaitAll().all { it is CacheResult.Failed })
        assertEquals(1, calls)
    }

    @Test
    fun `a slow load for one key does not delay a load for another`() = runTest {
        val neverDone = CompletableDeferred<String>()
        val slow = async { cache.get("slow", { neverDone.await() }) }
        runCurrent()

        assertEquals(CacheResult.Fresh("other-v1"), cache.get("other", loader::load))
        assertFalse(slow.isCompleted)

        neverDone.complete("eventually")
        assertEquals(CacheResult.Fresh("eventually"), slow.await())
    }
}
