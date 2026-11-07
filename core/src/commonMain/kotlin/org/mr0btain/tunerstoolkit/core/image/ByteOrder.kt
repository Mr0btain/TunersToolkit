package org.mr0btain.tunerstoolkit.core.image

/**
 * How a value wider than a byte is laid out in memory. A2L calls these MSB_FIRST (big endian, Motorola) and
 * MSB_LAST (little endian, Intel)
 */
enum class ByteOrder {
    BIG_ENDIAN,
    LITTLE_ENDIAN;

    /** The unsigned value of [size] bytes of [bytes] from [offset] */
    fun decode(bytes: ByteArray, offset: Int = 0, size: Int = bytes.size - offset): ULong {
        require(size in 1..8) { "Values are 1 to 8 bytes, not $size" }
        var value = 0UL
        for (i in 0 until size) {
            val index = if (this == BIG_ENDIAN) offset + i else offset + size - 1 - i
            value = (value shl 8) or (bytes[index].toULong() and 0xFFUL)
        }
        return value
    }

    /** [value]'s low [size] bytes, in this order */
    fun encode(value: ULong, size: Int): ByteArray {
        require(size in 1..8) { "Values are 1 to 8 bytes, not $size" }
        val bytes = ByteArray(size)
        var rest = value
        for (i in 0 until size) {
            val index = if (this == BIG_ENDIAN) size - 1 - i else i
            bytes[index] = (rest and 0xFFUL).toByte()
            rest = rest shr 8
        }
        return bytes
    }
}
