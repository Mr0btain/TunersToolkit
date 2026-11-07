package org.mr0btain.tunerstoolkit.core.logging

// Only compiled on a Mac (GitHub CI's iOS job)
internal actual fun platformLog(entry: LogEntry) {
    println("${entry.level}: [${entry.tag}] ${entry.message}${entry.throwable?.let { "\n$it" }.orEmpty()}")
}
