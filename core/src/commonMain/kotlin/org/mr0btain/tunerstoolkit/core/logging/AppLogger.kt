package org.mr0btain.tunerstoolkit.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * One logged line, kept in [AppLogger.entries] for the log screen and bug reports
 * @param timeMillis Milliseconds since the epoch
 */
data class LogEntry(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
)

/**
 * The app's logger. Each class declares its tag as a constant and adds the function it's in, like
 * `AppLogger.e("$TAG:ParseRecordLayout", "...")`.
 *
 * Every line goes to the platform's log ([platformLog]: Logcat, the console, stderr) and into a ring buffer of the
 * last [capacity] lines, which the log screen shows and "copy for bug report" reads. File contents never go in here.
 */
object AppLogger {
    /** Whether [d] writes anything. Off until the app turns it on for debug builds */
    var debug: Boolean = false

    /** How many lines [entries] keeps */
    var capacity: Int = 2_000

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    /** Something failed */
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    /** Something looked wrong but we carried on */
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)

    /** Worth knowing, like a file opened or a project saved */
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message, null)

    /** Only written when [debug] is on */
    fun d(tag: String, message: String) {
        if (debug) log(LogLevel.DEBUG, tag, message, null)
    }

    fun clear() = _entries.update { emptyList() }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(nowMillis(), level, tag, message, throwable?.stackTraceToString())
        _entries.update { (it + entry).takeLast(capacity) }
        platformLog(entry)
    }
}

/** Writes [entry] to the platform's own log */
internal expect fun platformLog(entry: LogEntry)

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
