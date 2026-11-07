package org.mr0btain.tunerstoolkit.core.logging

/** Errors and warnings to stderr, everything else to stdout */
internal actual fun platformLog(entry: LogEntry) {
    val line = "${entry.level}: [${entry.tag}] ${entry.message}"
    val stream = if (entry.level >= LogLevel.WARN) System.err else System.out
    stream.println(line)
    entry.throwable?.let { stream.println(it) }
}
