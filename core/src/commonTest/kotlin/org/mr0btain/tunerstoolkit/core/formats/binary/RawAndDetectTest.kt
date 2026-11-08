package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.Fixtures
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawAndDetectTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun rawReadsAtTheBaseAddress() {
        val image = RawBinary.read(bytes(1, 2, 3), baseAddress = 0x8000_0000L).getOrNull()!!.value
        assertEquals(listOf(AddressRange(0x8000_0000L, 0x8000_0003L)), image.ranges)
        assertEquals(3, image.byteAt(0x8000_0002L))
        assertEquals(TkResult.Failure(ImportError.EMPTY_FILE), RawBinary.read(ByteArray(0)))
    }

    @Test
    fun rawFillsGaps() {
        val image = MemoryImage.of(0x10L to bytes(1, 2), 0x14L to bytes(3))
        assertContentEquals(bytes(1, 2, 0xFF, 0xFF, 3), RawBinary.write(image).getOrNull())
        assertContentEquals(bytes(0, 0, 1), RawBinary.write(image, AddressRange(0x0E, 0x11), fill = 0).getOrNull())
        assertEquals(TkResult.Failure(ExportError.EMPTY_IMAGE), RawBinary.write(MemoryImage.empty()))
        assertEquals(
            TkResult.Failure(ExportError.TOO_BIG),
            RawBinary.write(MemoryImage.of(0L to bytes(1), 0xFFFF_0000L to bytes(2)))
        )
    }

    @Test
    fun rawRoundTripsASingleSegment() {
        val image = Fixtures.randomImage(seed = 3, segments = 1)
        val start = image.ranges.single().start
        val back = RawBinary.read(RawBinary.write(image).getOrNull()!!, start).getOrNull()!!.value
        assertTrue(back.contentEquals(image))
    }

    @Test
    fun detectsByContent() {
        assertEquals(BinaryFormat.INTEL_HEX, BinaryFormat.detect(":10010000214601360121470136007EFE09D2190140".encodeToByteArray()))
        assertEquals(BinaryFormat.INTEL_HEX, BinaryFormat.detect("﻿\r\n  :00000001FF".encodeToByteArray()))
        assertEquals(BinaryFormat.S_RECORD, BinaryFormat.detect("S00F000068656C6C6F202020202000003C".encodeToByteArray()))
        assertEquals(BinaryFormat.S_RECORD, BinaryFormat.detect("s1070000".encodeToByteArray()))
        assertEquals(BinaryFormat.RAW, BinaryFormat.detect(bytes(0x3A, 0x00, 0xFF, 0x12)), "A colon then binary isn't HEX")
        assertEquals(BinaryFormat.RAW, BinaryFormat.detect("Some notes".encodeToByteArray()))
        assertEquals(BinaryFormat.RAW, BinaryFormat.detect(ByteArray(0)))
    }

    @Test
    fun importPicksTheReader() {
        val hex = BinaryImport.read(":0100000042BD\n:00000001FF".encodeToByteArray(), "a.bin").getOrNull()!!
        assertEquals(0x42, hex.value.byteAt(0), "The content decides, not the file name")

        val raw = BinaryImport.read(bytes(9, 8), "a.hex", rawBaseAddress = 0x100).getOrNull()!!
        assertEquals(9, raw.value.byteAt(0x100))

        val forced = BinaryImport.read(":0100".encodeToByteArray(), "x", format = BinaryFormat.RAW).getOrNull()!!
        assertEquals(5, forced.value.size)
    }
}
