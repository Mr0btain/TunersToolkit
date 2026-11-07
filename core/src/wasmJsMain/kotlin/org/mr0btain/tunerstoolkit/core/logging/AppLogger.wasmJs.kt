package org.mr0btain.tunerstoolkit.core.logging

/** Writes to the browser console, open the developer tools to read it */
internal actual fun platformLog(entry: LogEntry) {
    println("${entry.level}: [${entry.tag}] ${entry.message}${entry.throwable?.let { "\n$it" }.orEmpty()}")
}
