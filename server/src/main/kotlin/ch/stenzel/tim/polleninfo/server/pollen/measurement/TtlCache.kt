package ch.stenzel.tim.polleninfo.server.pollen.measurement

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/** What [TtlCache.get] could produce for a key. */
sealed interface CacheResult<out V> {

    /** Loaded within the time-to-live, either just now or by an earlier call. */
    data class Fresh<V>(val value: V) : CacheResult<V>

    /**
     * The load failed and this is the last value that succeeded. Its age is whatever it was before
     * the failure: a failed reload never makes a retained value look newer.
     */
    data class Stale<V>(val value: V) : CacheResult<V>

    /** The load failed and nothing was ever loaded for this key. [cause] is what the loader threw. */
    data class Failed(val cause: Exception) : CacheResult<Nothing>
}

/**
 * A time-to-live cache that keeps the last good value when a reload fails.
 *
 * - An entry younger than [ttl] is served without calling the loader.
 * - Concurrent misses on one key run the loader **once**; the other callers wait for it and share
 *   its outcome, success or failure. The lock is per key, so a slow load for one key never delays
 *   another.
 * - When the loader throws and a previous value exists, that value is returned as
 *   [CacheResult.Stale] and kept with its **original** load time, so it stays expired and the next
 *   call tries again. No maximum age is applied — presenting the age is the caller's job.
 *
 * Failures are not cached: every call that finds no fresh entry, and did not wait behind another
 * caller's attempt, runs the loader again.
 *
 * Generic on purpose — expiry, deduplication and stale retention have nothing to do with what is
 * being cached, and [clock] is injected so all three can be tested without waiting. Keys are never
 * evicted, so the key space must be bounded.
 */
class TtlCache<K : Any, V : Any>(
    private val ttl: Duration,
    private val clock: Clock = Clock.systemUTC(),
) {

    private class Entry<V>(val value: V, val loadedAt: Instant)

    /** Everything known about one key. Written only while holding [mutex]. */
    private class Slot<V> {
        val mutex = Mutex()

        @Volatile
        var entry: Entry<V>? = null

        /** Completed load attempts, so a waiter can tell that one finished while it queued. */
        @Volatile
        var attempts = 0L

        /** What the latest completed attempt threw, or `null` when it succeeded. */
        var lastFailure: Exception? = null
    }

    private val slots = ConcurrentHashMap<K, Slot<V>>()

    suspend fun get(key: K, loader: suspend (K) -> V): CacheResult<V> {
        val slot = slots.computeIfAbsent(key) { Slot() }
        slot.entry?.takeIf { it.isFresh() }?.let { return CacheResult.Fresh(it.value) }

        val attemptsBeforeWaiting = slot.attempts
        return slot.mutex.withLock {
            val previous = slot.entry
            if (previous != null && previous.isFresh()) return@withLock CacheResult.Fresh(previous.value)

            // Someone else's load finished while this call queued behind it. Share its outcome
            // rather than queueing a second attempt: during an outage each attempt can take as
            // long as the loader's timeout, and every waiter retrying in turn would multiply that.
            if (slot.attempts != attemptsBeforeWaiting) {
                return@withLock outcomeOf(previous, slot.lastFailure)
            }

            try {
                val value = loader(key)
                slot.entry = Entry(value, clock.instant())
                slot.lastFailure = null
                slot.attempts++
                CacheResult.Fresh(value)
            } catch (e: CancellationException) {
                // The caller went away; that is not a failed load, so a waiter gets its own attempt.
                throw e
            } catch (e: Exception) {
                slot.lastFailure = e
                slot.attempts++
                outcomeOf(previous, e)
            }
        }
    }

    private fun outcomeOf(entry: Entry<V>?, failure: Exception?): CacheResult<V> = when {
        failure == null -> CacheResult.Fresh(checkNotNull(entry).value)
        entry != null -> CacheResult.Stale(entry.value)
        else -> CacheResult.Failed(failure)
    }

    private fun Entry<V>.isFresh(): Boolean =
        Duration.between(loadedAt, clock.instant()) < ttl
}
