package org.mr0btain.tunerstoolkit.core.checksum

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ChecksumTest {
    private val nine = "123456789".encodeToByteArray()

    @Test
    fun crcsGiveTheirCatalogueCheckValues() {
        // The check values of "123456789" from the CRC catalogue
        val expected = mapOf(
            "CRC-8/SMBUS" to 0xF4uL, "CRC-8/SAE-J1850" to 0x4BuL,
            "CRC-16/ARC" to 0xBB3DuL, "CRC-16/MODBUS" to 0x4B37uL, "CRC-16/IBM-3740" to 0x29B1uL,
            "CRC-16/XMODEM" to 0x31C3uL, "CRC-16/KERMIT" to 0x2189uL,
            "CRC-32/ISO-HDLC" to 0xCBF43926uL, "CRC-32/BZIP2" to 0xFC891918uL, "CRC-32/MPEG-2" to 0x0376E6E7uL,
            "CRC-32/ISCSI" to 0xE3069283uL
        )
        assertEquals(expected.keys, ChecksumAlgorithm.CRCS.map { it.name }.toSet())
        for ((name, value) in expected) assertEquals(value, ChecksumAlgorithm.crc(name)!!.compute(nine), name)
        // Fed in pieces, the same
        assertEquals(0xCBF43926uL, ChecksumAlgorithm.crc("CRC-32")!!.compute("1234".encodeToByteArray(), "56789".encodeToByteArray()))
    }

    @Test
    fun sumsXorsAndAdler() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0x05)
        assertEquals(0x10AuL, ChecksumAlgorithm.Sum(1, 16).compute(bytes))
        assertEquals(0x0AuL, ChecksumAlgorithm.Sum(1, 8).compute(bytes), "wraps at its width")
        // 0x0102 + 0x03FF + 0x0500 (the last word padded)
        assertEquals(0x0A01uL, ChecksumAlgorithm.Sum(2, 16, ByteOrder.BIG_ENDIAN).compute(bytes))
        assertEquals(0x0A01uL, ChecksumAlgorithm.Sum(2, 16, ByteOrder.BIG_ENDIAN).compute(byteArrayOf(0x01), byteArrayOf(0x02, 0x03, 0xFF.toByte(), 0x05)), "words across pieces")
        assertEquals((0x0201 + 0xFF03 + 0x0005).toULong() and 0xFFFFu, ChecksumAlgorithm.Sum(2, 16, ByteOrder.LITTLE_ENDIAN).compute(bytes))
        assertEquals(0xFAuL, ChecksumAlgorithm.Xor().compute(bytes))
        assertEquals(0x11E60398uL, ChecksumAlgorithm.Adler32.compute("Wikipedia".encodeToByteArray()))
    }

    @Test
    fun theDemoImageHasRightChecksums() {
        val demo = DemoEcu.build()
        val module = ChecksumModules.detect(demo.image).single().module
        assertEquals("tt-demo-1", module.id)
        val checks = (module.checks(demo.image) as TkResult.Success).data
        assertEquals(listOf(true, true, true), Checksums.verify(demo.image, checks).map { it.ok })
    }

    @Test
    fun editsBreakThemAndFixingMendsThemInOneStep() {
        val image = DemoEcu.build().image.snapshot()
        val checks = ChecksumModules.demo.checkList
        image.write(0x804010, byteArrayOf(0x42)) // calibration
        image.write(0x10, byteArrayOf(0x42)) // boot
        image.write(0x800010, byteArrayOf(0x42)) // code
        assertEquals(listOf(false, false, false), Checksums.verify(image, checks).map { it.ok })
        val report = Checksums.fix(image, checks)
        assertTrue(report.allOk)
        assertEquals(3, report.fixed)
        assertEquals(0x42, image.byteAt(0x800010), "the edit stays; only the correction word moves")
        image.undo()
        assertEquals(listOf(false, false, false), Checksums.verify(image, checks).map { it.ok }, "one undo step")
    }

    @Test
    fun aCrcCanBeForcedToAnyTarget() {
        val random = Random(1)
        val image = MemoryImage.of(0x1000L to ByteArray(4096) { random.nextInt(256).toByte() })
        for (target in listOf(0x35015001uL, 0uL, 0xFFFFFFFFuL)) {
            val check = ChecksumCheck(
                "Forced", ChecksumAlgorithm.Crc(32, 0x04C11DB7u, 0xFADECAFEu, true, true, 0u), listOf(AddressRange(0x1000, 0x2000)),
                ChecksumStore(0x1FFC, 4, form = StoreForm.FORCE, target = target)
            )
            assertTrue(Checksums.fixOne(image, check))
            assertTrue(Checksums.verify(image, check).ok, "0x${target.toString(16)}")
        }
        // A 16-bit CRC forced through a 2 byte word in the middle
        val crc16 = ChecksumCheck("16", ChecksumAlgorithm.crc("CRC-16/XMODEM")!!, listOf(AddressRange(0x1000, 0x1800)), ChecksumStore(0x1400, 2, form = StoreForm.FORCE, target = 0x1234u))
        assertTrue(Checksums.fixOne(image, crc16))
        assertTrue(Checksums.verify(image, crc16).ok)
    }

    @Test
    fun problemsAreReportedNotThrown() {
        val image = MemoryImage.of(0x0L to ByteArray(16))
        val outside = ChecksumCheck("Outside", ChecksumAlgorithm.Sum(), listOf(AddressRange(4, 32)), ChecksumStore(0, 2))
        assertTrue(Checksums.verify(image, outside).problem!!.contains("isn't all in the image"))
        val overlapping = ChecksumCheck("Self", ChecksumAlgorithm.Sum(), listOf(AddressRange(0, 16)), ChecksumStore(14, 2))
        assertTrue(Checksums.verify(image, overlapping).problem!!.contains("its own checksum"))
        val huge = ChecksumCheck("Huge", ChecksumAlgorithm.Sum(), listOf(AddressRange(4, 1L shl 40)), ChecksumStore(0, 2))
        assertTrue(Checksums.verify(image, huge).problem!!.contains("more than"))
        // Adler can't be forced; a byte sum can't be forced through one byte
        assertFalse(Checksums.fixOne(image, ChecksumCheck("A", ChecksumAlgorithm.Adler32, listOf(AddressRange(0, 16)), ChecksumStore(12, 4, form = StoreForm.FORCE))))
        assertFalse(Checksums.fixOne(image, ChecksumCheck("B", ChecksumAlgorithm.Sum(1, 16), listOf(AddressRange(0, 16)), ChecksumStore(12, 1, form = StoreForm.FORCE))))
    }

    @Test
    fun modulesRoundTripAsTtsum() {
        val module = ChecksumModules.demo
        val text = TtSum.write(module)
        assertTrue(TtSum.looksLike(text.encodeToByteArray()))
        val back = (TtSum.read(text.encodeToByteArray()) as TkResult.Success).data
        assertEquals(emptyList(), back.diagnostics)
        assertEquals(module, back.value)
        // A custom CRC by its parameters, and broken entries left out with a reason
        val custom = """
            {"format": "ttsum", "formatVersion": 1, "id": "x", "name": "X",
             "detect": [{"bytes": "CAFE", "address": "0x0"}, {"nothing": true}],
             "checks": [
               {"name": "Mine", "algorithm": {"crc": "MY-CRC", "width": 16, "poly": "0x1021", "init": "0xFFFF"}, "ranges": ["0x2-0xF"], "store": {"address": "0x10", "size": 2}},
               {"name": "Bad range", "algorithm": {"adler32": true}, "ranges": ["0xF-0x2"], "store": {"address": "0x10", "size": 4}},
               {"name": "No algorithm", "algorithm": {}, "ranges": ["0x0-0x1"], "store": {"address": "0x10", "size": 4}}
             ]}
        """.trimIndent()
        val read = (TtSum.read(custom.encodeToByteArray()) as TkResult.Success).data
        assertEquals(listOf("Mine"), read.value.checkList.map { it.name })
        assertEquals(ModuleStatus.UNVERIFIED, read.value.status, "unverified unless it says otherwise")
        assertEquals(0x29B1uL, read.value.checkList.single().algorithm.compute(nine), "the same as CRC-16/IBM-3740")
        assertEquals(listOf("Bad range", "No algorithm", null), read.diagnostics.map { it.objectName })
        val image = MemoryImage.of(0x0L to byteArrayOf(0xCA.toByte(), 0xFE.toByte()))
        assertNotNull(read.value.detect(image))
        if (read.value.detect(MemoryImage.of(0x0L to ByteArray(2))) != null) fail("it shouldn't match other bytes")
    }

    @Test
    fun gf2Solving() {
        // x0 ^ x1 = 1, x1 = 1  →  x0 = 0, x1 = 1
        assertEquals(0b10uL, Checksums.solveGf2(ulongArrayOf(0b01uL, 0b11uL), 0b11uL, 2))
        // No solution: two equal columns can't give a bit they don't touch
        assertEquals(null, Checksums.solveGf2(ulongArrayOf(0b01uL, 0b01uL), 0b10uL, 2))
    }
}
