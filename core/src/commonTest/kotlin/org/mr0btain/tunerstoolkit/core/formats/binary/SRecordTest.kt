package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.error.TkResult
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

class SRecordTest {
    private fun read(text: String): Imported<MemoryImage> = when (val result = SRecord.read(text.encodeToByteArray(), "test.s19")) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail("Expected it to read, got ${result.error}")
    }

    private fun write(image: MemoryImage, size: SRecord.AddressSize? = null, bytesPerRecord: Int = 32): String =
        (SRecord.write(image, size, bytesPerRecord, lineEnding = "\n") as TkResult.Success).data

    private fun bytes(hex: String) = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // The example from the S-record Wikipedia page
    private val wikipediaSample = """
        S00F000068656C6C6F202020202000003C
        S11F00007C0802A6900100049421FFF07C6C1B787C8C23783C6000003863000026
        S11F001C4BFFFFE5398000007D83637880010014382100107C0803A64E800020E9
        S111003848656C6C6F20776F726C642E0A0042
        S5030003F9
        S9030000FC
    """.trimIndent()

    @Test
    fun readsTheExample() {
        val imported = read(wikipediaSample)
        assertTrue(imported.diagnostics.isEmpty(), imported.diagnostics.joinToString())
        val image = imported.value
        assertEquals(listOf(AddressRange(0, 0x46)), image.ranges)
        assertEquals("hello", image.header)
        assertEquals(0L, image.startAddress)
        assertContentEquals("Hello world.\n".encodeToByteArray(), image.read(0x38, 13))
        assertContentEquals(bytes("7C0802A6"), image.read(0, 4))
    }

    @Test
    fun widerAddresses() {
        val image = read(
            """
            S2080100000102030AE6
            S30900AB0000AABBCCDD3D
            S70500AB00004F
            """.trimIndent()
        ).value
        assertContentEquals(bytes("0102030A"), image.read(0x010000, 4))
        assertContentEquals(bytes("AABBCCDD"), image.read(0x00AB0000, 4))
        assertEquals(0x00AB0000L, image.startAddress)
    }

    @Test
    fun brokenRecordsAreSkippedAndReported() {
        val imported = read(
            """
            S107000001020304EE
            S1070004050607080F
            S4030000FC
            S5030005F7
            S9030000FC
            """.trimIndent()
        )
        assertContentEquals(bytes("01020304"), imported.value.read(0, 4))
        assertNull(imported.value.byteAtOrNull(4))
        val messages = imported.diagnostics.map { it.message }
        assertTrue(messages[0].contains("checksum"), messages.joinToString())
        assertTrue(messages.any { it.contains("reserved") })
        assertTrue(messages.any { it.contains("says 5 data records, but 1 were read") })
    }

    @Test
    fun missingEndWarns() {
        assertTrue(read("S107000001020304EE\n").diagnostics.any { it.message.contains("end record") })
    }

    @Test
    fun emptyAndDatalessFilesFail() {
        assertEquals(TkResult.Failure(ImportError.EMPTY_FILE), SRecord.read(ByteArray(0)))
        assertEquals(TkResult.Failure(ImportError.NO_DATA), SRecord.read("S0030000FC\nS9030000FC".encodeToByteArray()))
    }

    @Test
    fun writesExactRecords() {
        assertEquals(
            listOf("S0030000FC", "S107100000010203E2", "S5030001FB", "S9030000FC", ""),
            write(Fixtures.counting(0x1000, 4)).split("\n")
        )
        assertEquals(
            listOf("S0030000FC", "S20800100000010203E1", "S5030001FB", "S804000000FB", ""),
            write(Fixtures.counting(0x1000, 4), SRecord.AddressSize.S28).split("\n")
        )
    }

    @Test
    fun picksTheSmallestAddressSizeThatFits() {
        assertTrue(write(Fixtures.counting(0xFFF0, 16)).lines()[1].startsWith("S1"))
        assertTrue(write(Fixtures.counting(0xFFF0, 17)).lines()[1].startsWith("S2"))
        assertTrue(write(Fixtures.counting(0x0100_0000L, 1)).lines()[1].startsWith("S3"))
        assertEquals(
            TkResult.Failure(ExportError.ADDRESS_TOO_HIGH),
            SRecord.write(Fixtures.counting(0x1_0000L, 1), SRecord.AddressSize.S19)
        )
    }

    @Test
    fun roundTrips() {
        val images = (1..20).map { Fixtures.randomImage(seed = 100 + it, segments = 1 + it % 5) } + Fixtures.ecuLikeImage()
        for (image in images) {
            for (bytesPerRecord in listOf(1, 16, 32, 250)) {
                val back = read(write(image, bytesPerRecord = bytesPerRecord))
                assertTrue(back.diagnostics.isEmpty(), back.diagnostics.joinToString())
                assertTrue(back.value.contentEquals(image), "$image at $bytesPerRecord bytes a record")
                assertEquals(image.startAddress ?: 0, back.value.startAddress)
            }
        }
    }

    @Test
    fun headerSurvivesARoundTrip() {
        val image = MemoryImage.Builder().apply {
            put(0, bytes("0102"))
            header = "07K906032BR"
        }.build().image
        assertEquals("07K906032BR", read(write(image)).value.header)
    }
}
