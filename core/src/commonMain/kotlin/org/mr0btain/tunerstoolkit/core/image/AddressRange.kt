package org.mr0btain.tunerstoolkit.core.image

/**
 * Addresses from [start] up to but not including [endExclusive]. Addresses are unsigned 32 bit values held in a
 * [Long], up to 0xFFFFFFFF for the HEX formats
 */
data class AddressRange(val start: Long, val endExclusive: Long) : Comparable<AddressRange> {
    init {
        require(start >= 0) { "Address ${start.toHexAddress()} is negative" }
        require(endExclusive >= start) { "Range ends (${endExclusive.toHexAddress()}) before it starts (${start.toHexAddress()})" }
    }

    val size: Long get() = endExclusive - start
    val isEmpty: Boolean get() = size == 0L

    /** The last address in the range. Only meaningful when it isn't empty */
    val last: Long get() = endExclusive - 1

    operator fun contains(address: Long): Boolean = address in start until endExclusive

    fun contains(other: AddressRange): Boolean = other.start >= start && other.endExclusive <= endExclusive

    fun overlaps(other: AddressRange): Boolean = start < other.endExclusive && other.start < endExclusive

    override fun compareTo(other: AddressRange): Int =
        compareValuesBy(this, other, AddressRange::start, AddressRange::endExclusive)

    /** `0x80000000-0x8000FFFF`, both ends included */
    override fun toString(): String =
        if (isEmpty) "${start.toHexAddress()} (empty)" else "${start.toHexAddress()}-${last.toHexAddress()}"

    companion object {
        fun ofSize(start: Long, size: Long): AddressRange = AddressRange(start, start + size)
    }
}

/** `0x0000FFFF`: eight hex digits, or more when the address needs them */
fun Long.toHexAddress(): String = "0x" + toString(16).uppercase().padStart(8, '0')

/**
 * Joins ranges that touch or overlap, sorted by address
 */
fun List<AddressRange>.merged(): List<AddressRange> {
    if (size < 2) return sorted()
    val result = mutableListOf<AddressRange>()
    for (range in sorted()) {
        val last = result.lastOrNull()
        if (last != null && range.start <= last.endExclusive) {
            result[result.lastIndex] = AddressRange(last.start, maxOf(last.endExclusive, range.endExclusive))
        } else {
            result += range
        }
    }
    return result
}
