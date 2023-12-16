package components.errorstuff

inline fun <R> safeCall(call: () -> R): Result<R> {
    return runCatching { call() }
}