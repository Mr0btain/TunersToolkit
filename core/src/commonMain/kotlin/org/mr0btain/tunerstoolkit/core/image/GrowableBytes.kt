package org.mr0btain.tunerstoolkit.core.image

/** A byte array that grows as bytes are added, for collecting a segment while a file is read */
internal class GrowableBytes(initialCapacity: Int = 4096) {
    private var bytes = ByteArray(initialCapacity.coerceAtLeast(16))

    var size: Int = 0
        private set

    fun add(source: ByteArray, offset: Int = 0, length: Int = source.size - offset) {
        ensureSize(size + length)
        source.copyInto(bytes, size, offset, offset + length)
        size += length
    }

    /** Writes [source] at [position], growing to fit. Bytes between the old end and [position] are 0 */
    fun set(position: Int, source: ByteArray, offset: Int = 0, length: Int = source.size - offset) {
        val end = position + length
        if (end > size) {
            ensureCapacity(end)
            size = end
        }
        source.copyInto(bytes, position, offset, offset + length)
    }

    fun toByteArray(): ByteArray = bytes.copyOf(size)

    private fun ensureSize(newSize: Int) = ensureCapacity(newSize)

    private fun ensureCapacity(capacity: Int) {
        require(capacity >= 0) { "A segment can't be over ${Int.MAX_VALUE} bytes" }
        if (capacity <= bytes.size) return
        var newCapacity = bytes.size.toLong() * 2
        while (newCapacity < capacity) newCapacity *= 2
        bytes = bytes.copyOf(newCapacity.coerceAtMost(Int.MAX_VALUE.toLong() - 8).toInt().coerceAtLeast(capacity))
    }
}
