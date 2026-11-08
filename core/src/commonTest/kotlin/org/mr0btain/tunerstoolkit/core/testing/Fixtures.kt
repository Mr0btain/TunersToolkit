package org.mr0btain.tunerstoolkit.core.testing

import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.random.Random

/**
 * Made up ECU images for tests, the same every run for a seed. Real ECU files are OEM copyrighted and stay out of
 * git (docs/testing.md), so tests build their own
 */
object Fixtures {
    /**
     * Segments of random bytes at random addresses, with gaps between them (so they stay separate segments when
     * read back)
     * @param maxAddress Highest address any byte may be at, plus one
     */
    fun randomImage(
        seed: Int,
        segments: Int = 4,
        maxSegmentSize: Int = 4096,
        maxAddress: Long = 0x1_0000_0000L
    ): MemoryImage {
        val random = Random(seed)
        val sizes = List(segments) { 1 + random.nextInt(maxSegmentSize) }
        val needed = sizes.sumOf { it.toLong() + 1 }
        require(needed < maxAddress) { "Segments don't fit under the address limit" }

        // Spread the free space randomly between the segments, at least one byte apart
        val free = maxAddress - needed
        val cuts = List(segments) { random.nextLong(0, free + 1) }.sorted()
        var previousCut = 0L
        var address = 0L
        val pieces = sizes.mapIndexed { index, size ->
            address += cuts[index] - previousCut
            previousCut = cuts[index]
            val bytes = random.nextBytes(size)
            (address to bytes).also { address += size + 1 }
        }
        return MemoryImage.of(*pieces.toTypedArray())
    }

    /**
     * Laid out like a typical 32 bit ECU: a boot block, code, and a calibration area at high addresses, one of them
     * crossing a 64K boundary
     */
    fun ecuLikeImage(seed: Int = 7): MemoryImage {
        val random = Random(seed)
        fun block(size: Int, filler: Int): ByteArray =
            ByteArray(size) { if (random.nextInt(4) == 0) filler.toByte() else random.nextInt(256).toByte() }

        return MemoryImage.Builder().apply {
            put(0x8000_0000L, block(0x4000, 0xFF))
            put(0x8001_F000L, block(0x3000, 0x00))   // crosses 0x80020000
            put(0xA00F_FFF0L, block(0x200, 0xFF))    // crosses 0xA0100000
            startAddress = 0x8000_0000L
        }.build().image
    }

    /** One small segment with known bytes, for checking exact output */
    fun counting(address: Long, size: Int): MemoryImage = MemoryImage.of(address to ByteArray(size) { it.toByte() })
}
