package ch.stenzel.tim.polleninfo.core.result

/**
 * Outcome of an operation that can fail: either [Success] with data or [Failure] with the exception.
 *
 * **This intentionally shadows `kotlin.Result`**, which the compiler imports by default. An explicit
 * `import ch.stenzel.tim.polleninfo.core.result.Result` wins over that default import, so always
 * import this type — a file that forgets to will silently bind to `kotlin.Result` instead and fail
 * to compile against [Success] / [Failure]. Prefer [safeCall] over `runCatching`, which returns the
 * stdlib type.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Failure) action(exception)
    return this
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
}

inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Failure(e)
}
