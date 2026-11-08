package org.mr0btain.tunerstoolkit.ui.project

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** How long ago [millis] was, roughly: "just now", "5 min ago", "3 days ago" */
@OptIn(ExperimentalTime::class)
fun timeAgo(millis: Long, now: Long = Clock.System.now().toEpochMilliseconds()): String {
    val minutes = (now - millis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> plural(minutes / 60, "hour")
        minutes < 60 * 24 * 60 -> plural(minutes / (60 * 24), "day")
        minutes < 60 * 24 * 730 -> plural(minutes / (60 * 24 * 30), "month")
        else -> plural(minutes / (60 * 24 * 365), "year")
    }
}

private fun plural(count: Long, unit: String) = if (count == 1L) "1 $unit ago" else "$count ${unit}s ago"
