package org.mr0btain.tunerstoolkit.core.image

/**
 * Bytes to look for, where [mask] false at an index matches any byte there. Made from hex with ?? for any byte
 * ([parseHex]), or from text ([ofText])
 */
class BytePattern(bytes: ByteArray, mask: BooleanArray? = null) {
    private val values = bytes.copyOf()
    private val care = mask?.copyOf() ?: BooleanArray(bytes.size) { true }

    init {
        require(values.isNotEmpty()) { "A pattern needs at least one byte" }
        require(care.size == values.size) { "The mask must be as long as the pattern" }
        require(care.any { it }) { "A pattern of only wildcards matches everything" }
    }

    val size: Int get() = values.size

    /** Whether the pattern matches [bytes] from [offset] */
    fun matchesAt(bytes: ByteArray, offset: Int): Boolean {
        if (offset < 0 || offset + values.size > bytes.size) return false
        for (i in values.indices) if (care[i] && bytes[offset + i] != values[i]) return false
        return true
    }

    companion object {
        /**
         * `12 34 ?? AB` or `1234??AB`: pairs of hex digits, ?? for any byte. Null when it isn't valid
         */
        fun parseHex(text: String): BytePattern? {
            val digits = text.filterNot { it.isWhitespace() || it == ',' }.removePrefix("0x").removePrefix("0X")
            if (digits.isEmpty() || digits.length % 2 != 0) return null
            val bytes = ByteArray(digits.length / 2)
            val mask = BooleanArray(bytes.size)
            for (i in bytes.indices) {
                val pair = digits.substring(i * 2, i * 2 + 2)
                if (pair == "??") continue
                val value = pair.toIntOrNull(16) ?: return null
                bytes[i] = value.toByte()
                mask[i] = true
            }
            return if (mask.any { it }) BytePattern(bytes, mask) else null
        }

        /** [text] as UTF-8 bytes */
        fun ofText(text: String): BytePattern? = text.encodeToByteArray().takeIf { it.isNotEmpty() }?.let { BytePattern(it) }
    }
}

/**
 * Where [pattern] next appears at or after [from] (or at or before it, [backwards]), or null. Matches don't run
 * across gaps between segments. With [wrap], carries on from the other end of the image
 */
fun MemoryImage.find(pattern: BytePattern, from: Long, backwards: Boolean = false, wrap: Boolean = false): Long? {
    val ranges = ranges
    if (ranges.isEmpty()) return null

    fun scan(index: Int, startOffset: Int?, endOffset: Int?): Long? {
        val bytes = segmentBytes(index)
        val last = bytes.size - pattern.size
        if (last < 0) return null
        if (!backwards) {
            for (offset in (startOffset ?: 0).coerceAtLeast(0)..minOf(last, endOffset ?: last)) {
                if (pattern.matchesAt(bytes, offset)) return ranges[index].start + offset
            }
        } else {
            for (offset in minOf(last, startOffset ?: last) downTo (endOffset ?: 0).coerceAtLeast(0)) {
                if (pattern.matchesAt(bytes, offset)) return ranges[index].start + offset
            }
        }
        return null
    }

    val order = if (backwards) ranges.indices.reversed() else ranges.indices
    for (index in order) {
        val range = ranges[index]
        val found = when {
            !backwards && range.endExclusive <= from -> null
            backwards && range.start > from -> null
            else -> {
                val offset = (from - range.start).coerceIn(0, range.size).toInt()
                if (!backwards) scan(index, if (from > range.start) offset else 0, null)
                else scan(index, if (from < range.endExclusive) offset else null, null)
            }
        }
        if (found != null) return found
    }

    if (!wrap) return null
    // From the other end, up to where the search started
    for (index in order) {
        val range = ranges[index]
        val found = if (!backwards) {
            if (range.start > from) null else scan(index, 0, (from - range.start).coerceAtMost(range.size).toInt())
        } else {
            if (range.endExclusive <= from) null else scan(index, null, (from - range.start).coerceAtLeast(0).toInt())
        }
        if (found != null) return found
    }
    return null
}

/**
 * An address typed by someone: `0x80001234`, `80001234` or `8000_1234` are hex, `#1234` is decimal. Null when it
 * isn't one
 */
fun parseAddress(text: String): Long? {
    val trimmed = text.trim().replace("_", "").replace(" ", "")
    if (trimmed.isEmpty()) return null
    val value = when {
        trimmed.startsWith("#") -> trimmed.drop(1).toLongOrNull()
        trimmed.startsWith("0x", ignoreCase = true) -> trimmed.drop(2).toLongOrNull(16)
        else -> trimmed.toLongOrNull(16)
    }
    return value?.takeIf { it in 0..0xFFFF_FFFFL }
}
