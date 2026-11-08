package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.Fixtures
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class IntelHexTest {
    private fun read(text: String): Imported<MemoryImage> = when (val result = IntelHex.read(text.encodeToByteArray(), "test.hex")) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail("Expected it to read, got ${result.error}")
    }

    private fun bytes(hex: String) = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // The classic example from the Intel HEX specification's Wikipedia page
    private val wikipediaSample = """
        :10010000214601360121470136007EFE09D2190140
        :100110002146017E17C20001FF5F16002148011928
        :10012000194E79234623965778239EDA3F01B2CAA7
        :100130003F0156702B5E712B722B732146013421C7
        :00000001FF
    """.trimIndent()

    @Test
    fun readsTheSpecExample() {
        val imported = read(wikipediaSample)
        assertTrue(imported.diagnostics.isEmpty(), imported.diagnostics.joinToString())
        assertEquals(listOf(AddressRange(0x100, 0x140)), imported.value.ranges)
        assertContentEquals(bytes("214601360121470136007EFE09D21901"), imported.value.read(0x100, 16))
        assertContentEquals(bytes("3F0156702B5E712B722B732146013421"), imported.value.read(0x130, 16))
    }

    @Test
    fun extendedLinearAndSegmentAddresses() {
        val imported = read(
            """
            :020000040800F2
            :0400000001020304F2
            :020000021200EA
            :02001000AABB89
            :0400000508000100EE
            :00000001FF
            """.trimIndent()
        )
        assertTrue(imported.diagnostics.isEmpty(), imported.diagnostics.joinToString())
        val image = imported.value
        assertContentEquals(bytes("01020304"), image.read(0x0800_0000L, 4))
        assertContentEquals(bytes("AABB"), image.read(0x12000L + 0x10, 2), "Segment base is the value times 16")
        assertEquals(0x0800_0100L, image.startAddress)
    }

    @Test
    fun startSegmentAddress() {
        // CS 0x1234, IP 0x0010: 0x12340 + 0x10
        assertEquals(0x12350L, read(":0100000000FF\n:0400000312340010A3\n:00000001FF").value.startAddress)
    }

    @Test
    fun brokenRecordsAreSkippedAndReported() {
        val imported = read(
            """
            :0400000001020304F2
            :0400040005060708FF
            :04000800090A0BZZ00
            :0400100011121314
            just some text
            :03001400151617A7
            :00000001FF
            """.trimIndent()
        )
        val image = imported.value
        assertContentEquals(bytes("01020304"), image.read(0, 4))
        assertNull(image.byteAtOrNull(4), "The record with the wrong checksum is left out")
        assertContentEquals(bytes("151617"), image.read(0x14, 3))

        val problems = imported.diagnostics
        assertEquals(listOf(2, 3, 4, 5), problems.map { it.location?.line })
        assertTrue(problems[0].message.contains("checksum"))
        assertTrue(problems[1].message.contains("hex digit"))
        assertEquals(Severity.WARNING, problems[3].severity, "Text that isn't a record only warns")
        assertEquals(3, imported.errorCount)
    }

    @Test
    fun missingEndAndTrailingLinesWarn() {
        assertTrue(read(":0100000000FF").diagnostics.any { it.message.contains("no end of file") })
        val trailing = read(":0100000000FF\n:00000001FF\n:0100010001FD\n")
        assertTrue(trailing.diagnostics.any { it.message.contains("after the end of file") })
        assertNull(trailing.value.byteAtOrNull(1))
    }

    @Test
    fun overlappingDataWarns() {
        val imported = read(":0200000001FFFE\n:0100010002FC\n:00000001FF")
        assertEquals(2, imported.value.byteAt(1))
        assertTrue(imported.diagnostics.single().message.contains("more than once"))
    }

    @Test
    fun emptyAndDatalessFilesFail() {
        assertEquals(TkResult.Failure(ImportError.EMPTY_FILE), IntelHex.read("  \r\n".encodeToByteArray()))
        assertEquals(TkResult.Failure(ImportError.NO_DATA), IntelHex.read(":00000001FF".encodeToByteArray()))
    }

    @Test
    fun takesLowerCaseCrlfAndBlanks() {
        val imported = read("﻿  :0400000001020304f2  \r\n\r\n:00000001ff\r\n")
        assertContentEquals(bytes("01020304"), imported.value.read(0, 4))
    }

    @Test
    fun writesExactRecords() {
        val image = MemoryImage.Builder().apply {
            put(0x0800_FFFEL, bytes("0102030405"))
            startAddress = 0x0800_0100L
        }.build().image

        val text = (IntelHex.write(image, bytesPerRecord = 16, lineEnding = "\n") as TkResult.Success).data
        assertEquals(
            listOf(
                ":020000040800F2",
                ":02FFFE000102FE",  // stops at the 64K boundary
                ":020000040801F1",
                ":03000000030405F1",
                ":0400000508000100EE",
                ":00000001FF",
                ""
            ),
            text.split("\n")
        )
    }

    @Test
    fun roundTrips() {
        val images = (1..20).map { Fixtures.randomImage(seed = it, segments = 1 + it % 5) } + Fixtures.ecuLikeImage()
        for (image in images) {
            for (bytesPerRecord in listOf(1, 16, 32, 255)) {
                val text = (IntelHex.write(image, bytesPerRecord) as TkResult.Success).data
                val back = read(text)
                assertTrue(back.diagnostics.isEmpty(), back.diagnostics.joinToString())
                assertTrue(back.value.contentEquals(image), "$image at $bytesPerRecord bytes a record")
                assertEquals(image.startAddress, back.value.startAddress)
            }
        }
    }

    @Test
    fun addressesPast32BitsCantBeWritten() {
        val image = MemoryImage.of(0xFFFF_FFFFL to bytes("01"), 0x1_0000_0000L + 4 to bytes("02"))
        assertEquals(TkResult.Failure(ExportError.ADDRESS_TOO_HIGH), IntelHex.write(image))
        assertEquals(TkResult.Failure(ExportError.EMPTY_IMAGE), IntelHex.write(MemoryImage.empty()))
    }

    @Test
    fun theLastAddressOf32BitsWorks() {
        val image = MemoryImage.of(0xFFFF_FFF0L to ByteArray(16) { it.toByte() })
        val back = IntelHex.read((IntelHex.write(image) as TkResult.Success).data.encodeToByteArray()).getOrNull()
        assertTrue(back!!.value.contentEquals(image))
    }
}
