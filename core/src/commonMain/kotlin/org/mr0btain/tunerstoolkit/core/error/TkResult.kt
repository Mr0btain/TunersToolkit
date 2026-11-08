package org.mr0btain.tunerstoolkit.core.error

import org.mr0btain.applogger.LogMessage

/**
 * The error half of a [TkResult]. [message] should be readable enough to show the user, since import and export
 * problems end up in the import report and the log screen. Being a [LogMessage], it can be logged as it is:
 * `AppLogger.e(TAG, error)`
 */
interface Error : LogMessage

/**
 * Either the value a call produced or why it couldn't. Used instead of throwing, so callers have to decide what a
 * failure means for them
 */
sealed interface TkResult<out D, out E : Error> {
    data class Success<out D>(val data: D) : TkResult<D, Nothing>
    data class Failure<out E : Error>(val error: E) : TkResult<Nothing, E>
}

typealias EmptyResult<E> = TkResult<Unit, E>

inline fun <T, E : Error, R> TkResult<T, E>.map(transform: (T) -> R): TkResult<R, E> = when (this) {
    is TkResult.Failure -> this
    is TkResult.Success -> TkResult.Success(transform(data))
}

inline fun <T, E : Error, R> TkResult<T, E>.flatMap(transform: (T) -> TkResult<R, E>): TkResult<R, E> = when (this) {
    is TkResult.Failure -> this
    is TkResult.Success -> transform(data)
}

inline fun <T, E : Error> TkResult<T, E>.onSuccess(action: (T) -> Unit): TkResult<T, E> {
    if (this is TkResult.Success) action(data)
    return this
}

inline fun <T, E : Error> TkResult<T, E>.onFailure(action: (E) -> Unit): TkResult<T, E> {
    if (this is TkResult.Failure) action(error)
    return this
}

fun <T, E : Error> TkResult<T, E>.asEmptyResult(): EmptyResult<E> = map { }

/** The value, or null if it failed */
fun <T, E : Error> TkResult<T, E>.getOrNull(): T? = (this as? TkResult.Success)?.data

/** The error, or null if it worked */
fun <T, E : Error> TkResult<T, E>.errorOrNull(): E? = (this as? TkResult.Failure)?.error

/** The value, or what [onFailure] gives instead. It can also leave, like `return@load` */
inline fun <T, E : Error> TkResult<T, E>.valueOr(onFailure: (E) -> T): T = when (this) {
    is TkResult.Success -> data
    is TkResult.Failure -> onFailure(error)
}
