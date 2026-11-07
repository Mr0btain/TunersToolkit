package org.mr0btain.tunerstoolkit.core.logging

import android.util.Log

internal actual fun platformLog(entry: LogEntry) {
    val message = entry.throwable?.let { "${entry.message}\n$it" } ?: entry.message
    when (entry.level) {
        LogLevel.DEBUG -> Log.d(entry.tag, message)
        LogLevel.INFO -> Log.i(entry.tag, message)
        LogLevel.WARN -> Log.w(entry.tag, message)
        LogLevel.ERROR -> Log.e(entry.tag, message)
    }
}
