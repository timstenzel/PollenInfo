package ch.stenzel.tim.polleninfo.core.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun `safeCall wraps a returned value in Success`() {
        val result = safeCall { 42 }

        assertIs<Result.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `safeCall catches exceptions and wraps them in Failure`() {
        val boom = IllegalStateException("boom")

        val result = safeCall<Int> { throw boom }

        assertIs<Result.Failure>(result)
        assertEquals(boom, result.exception)
    }

    @Test
    fun `map transforms Success payloads`() {
        val result = Result.Success(2).map { it * 10 }

        assertIs<Result.Success<Int>>(result)
        assertEquals(20, result.data)
    }

    @Test
    fun `map leaves Failure untouched and does not invoke the transform`() {
        var invoked = false
        val cause = IllegalArgumentException("nope")
        val failure: Result<Int> = Result.Failure(cause)

        val result = failure.map { invoked = true; it * 2 }

        assertIs<Result.Failure>(result)
        assertEquals(cause, result.exception)
        assertTrue(!invoked, "transform must not run for Failure")
    }

    @Test
    fun `onSuccess runs only for Success and returns the same instance`() {
        var seen: Int? = null
        val success: Result<Int> = Result.Success(7)

        val returned = success.onSuccess { seen = it }

        assertEquals(7, seen)
        assertEquals(success, returned)
    }

    @Test
    fun `onSuccess does not run for Failure`() {
        var seen: Int? = null

        Result.Failure(RuntimeException()).onSuccess { seen = 1 }

        assertNull(seen)
    }

    @Test
    fun `onFailure runs only for Failure and exposes the exception`() {
        val cause = RuntimeException("bad")
        var seen: Exception? = null

        val returned = Result.Failure(cause).onFailure { seen = it }

        assertEquals(cause, seen)
        assertIs<Result.Failure>(returned)
    }

    @Test
    fun `onFailure does not run for Success`() {
        var invoked = false

        Result.Success("ok").onFailure { invoked = true }

        assertTrue(!invoked)
    }
}
