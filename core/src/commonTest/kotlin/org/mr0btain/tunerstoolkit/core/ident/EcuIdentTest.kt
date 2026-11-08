package org.mr0btain.tunerstoolkit.core.ident

import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.project.ProjectInfo
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcuIdentTest {
    /** Random bytes (with no long printable runs of their own) with [texts] written at their offsets */
    private fun image(vararg texts: Pair<Int, String>): MemoryImage {
        val random = Random(5)
        val bytes = ByteArray(64 * 1024) { (random.nextInt(128) + 128).toByte() }
        for ((at, text) in texts) text.encodeToByteArray().copyInto(bytes, at)
        return MemoryImage.of(0x80000L to bytes)
    }

    private fun List<IdentFinding>.of(kind: IdentKind) = filter { it.kind == kind }.map { it.value }

    @Test
    fun boschNumbersAndTheFamily() {
        val found = EcuIdent.scan(image(
            0x100 to "0261S02526 1037393126  MED17.5.2",
            0x2000 to "40/1/MED17.5.2/3/P671//06J906027BP/",
            0x3000 to "1037393126"
        ))
        assertEquals(listOf("0261S02526"), found.of(IdentKind.HARDWARE_NUMBER))
        assertEquals(listOf("1037393126"), found.of(IdentKind.SOFTWARE_NUMBER))
        assertEquals(2, found.first { it.value == "1037393126" }.count)
        assertEquals(0x80100L, found.first { it.value == "0261S02526" }.address)
        assertEquals(listOf("MED17.5.2"), found.of(IdentKind.ECU))
        assertEquals(listOf("06J906027BP"), found.of(IdentKind.PART_NUMBER))
        val calibration = found.first { it.kind == IdentKind.CALIBRATION }
        assertTrue(calibration.value.startsWith("40/1/MED17.5.2/3/P671"))
        assertEquals(85, calibration.confidence, "it names an ECU family")
        assertEquals(85, found.first().confidence, "the surest first")
    }

    @Test
    fun otherMakersAndAVin() {
        val found = EcuIdent.scan(image(
            0x100 to "5WP45015 A2C53147561",
            0x200 to "89661-0Z860 MB275700-4280",
            0x300 to "SIMOS 18.1  EDC16C34  WVWZZZ1KZ6W123456"
        ))
        assertEquals(listOf("5WP45015", "MB275700-4280"), found.of(IdentKind.HARDWARE_NUMBER).sorted())
        assertEquals(listOf("89661-0Z860", "A2C53147561"), found.of(IdentKind.PART_NUMBER).sorted())
        assertEquals(listOf("EDC16C34", "SIMOS 18.1"), found.of(IdentKind.ECU).sorted())
        assertEquals(listOf("WVWZZZ1KZ6W123456"), found.of(IdentKind.VIN))
    }

    @Test
    fun nearMissesArentTaken() {
        val found = EcuIdent.scan(image(
            0x100 to "02612 1037 0261S025260 ABCDEFGHIJKLMNOPQ 12345678901234567",
            0x300 to "a/b/c"
        ))
        assertEquals(emptyList(), found.map { "${it.kind} ${it.value}" })
    }

    @Test
    fun suggestionsFillOnlyWhatsEmpty() {
        val found = EcuIdent.scan(image(0x100 to "0261208288 1037366536 ME7.5"))
        assertEquals(ProjectInfo(ecu = "ME7.5 0261208288", softwareNumber = "1037366536"), EcuIdent.suggest(found))
        assertEquals(ProjectInfo(ecu = "Mine", softwareNumber = "1037366536"), EcuIdent.suggest(found, ProjectInfo(ecu = "Mine")))
    }

    @Test
    fun theDemoSaysWhatItIs() {
        val found = EcuIdent.scan(DemoEcu.build().image)
        val calibration = found.first { it.kind == IdentKind.CALIBRATION }
        assertEquals(DemoEcu.EPK, calibration.value)
        assertEquals(DemoEcu.EPK_ADDRESS, calibration.address)
        assertEquals(DemoEcu.EPK, EcuIdent.suggest(found).softwareNumber)
    }
}
