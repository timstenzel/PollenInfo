package ch.stenzel.tim.polleninfo.feature.example.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.FakeExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.pollenSnapshot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetPollenSnapshotUseCaseTest {

    @Test
    fun `passes the supplied coordinates through to the repository`() = runTest {
        val repository = FakeExampleRepository()
        val useCase = GetPollenSnapshotUseCase(repository)

        useCase(latitude = 46.9503, longitude = 7.4447)

        assertEquals(listOf(46.9503 to 7.4447), repository.calls)
    }

    @Test
    fun `defaults to Zurich when no coordinates are supplied`() = runTest {
        val repository = FakeExampleRepository()
        val useCase = GetPollenSnapshotUseCase(repository)

        useCase()

        assertEquals(listOf(47.3769 to 8.5417), repository.calls)
    }

    @Test
    fun `returns the repository result unchanged on success`() = runTest {
        val snapshot = pollenSnapshot(location = "Bern")
        val useCase = GetPollenSnapshotUseCase(
            FakeExampleRepository(Result.Success(snapshot)),
        )

        val result = useCase()

        assertIs<Result.Success<*>>(result)
        assertEquals(snapshot, result.data)
    }

    @Test
    fun `propagates failures from the repository`() = runTest {
        val cause = RuntimeException("upstream down")
        val useCase = GetPollenSnapshotUseCase(
            FakeExampleRepository(Result.Failure(cause)),
        )

        val result = useCase()

        assertIs<Result.Failure>(result)
        assertEquals(cause, result.exception)
    }
}
