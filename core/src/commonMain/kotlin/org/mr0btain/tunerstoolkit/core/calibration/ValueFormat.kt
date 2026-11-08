package org.mr0btain.tunerstoolkit.core.calibration

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A physical value as text the way an A2L FORMAT asks: `"%5.2"` is two decimals (the width is ignored). Without a
 * format, up to four decimals with trailing zeros dropped. Works the same on every platform, which String.format
 * doesn't
 */
fun formatValue(value: Double, format: String? = null): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val decimals = decimalsOf(format)
    return if (decimals != null) fixed(value, decimals) else fixed(value, 4).trimZeros()
}

/** The decimals an A2L FORMAT asks for, null when it doesn't say */
fun decimalsOf(format: String?): Int? {
    val text = format?.trim()?.removePrefix("%") ?: return null
    val point = text.indexOf('.')
    if (point < 0) return if (text.isNotEmpty() && text.all { it.isDigit() }) 0 else null
    return text.substring(point + 1).takeWhile { it.isDigit() }.toIntOrNull()?.coerceIn(0, 12)
}

private fun fixed(value: Double, decimals: Int): String {
    var scale = 1.0
    repeat(decimals) { scale *= 10 }
    val scaled = abs(value) * scale
    if (scaled >= 9e15) return value.toString()
    val units = scaled.roundToLong()
    val whole = units / scale.toLong()
    val fraction = units % scale.toLong()
    val sign = if (value < 0 && units != 0L) "-" else ""
    return if (decimals == 0) "$sign$whole" else "$sign$whole.${fraction.toString().padStart(decimals, '0')}"
}

private fun String.trimZeros(): String = if ('.' in this) trimEnd('0').trimEnd('.') else this
