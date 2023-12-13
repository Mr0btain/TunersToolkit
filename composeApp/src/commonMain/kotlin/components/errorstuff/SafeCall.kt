package components.errorstuff

inline fun <R> safeCall(call: () -> R): Result<R> {
    return try {
        Result.success(call())
    } catch (e: Exception) {
        Result.failure(e)
    }
}