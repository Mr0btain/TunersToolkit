package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

/**
 * A2Ls the size of real ones, and real ones when they're in testdata/local (never committed, see docs/testing.md).
 * The plan's target is under 2 s for 8.6 MB on desktop; times are printed, and only a generous limit is asserted so a
 * slow CI machine doesn't fail
 */
class LargeA2lTest {
    /** About as many objects, and as much text, as a big engine controller's A2L */
    private fun bigA2l(characteristics: Int): String = buildString {
        appendLine("ASAP2_VERSION 1 61")
        appendLine("/begin PROJECT Big \"\"\n/begin MODULE M \"\"")
        appendLine("/begin MOD_COMMON \"\" BYTE_ORDER MSB_FIRST ALIGNMENT_BYTE 1 /end MOD_COMMON")
        appendLine("/begin RECORD_LAYOUT L FNC_VALUES 1 UWORD ROW_DIR DIRECT /end RECORD_LAYOUT")
        appendLine("/begin COMPU_METHOD c \"\" RAT_FUNC \"%5.2\" \"Nm\" COEFFS 0 1 0 0 0 0.25 /end COMPU_METHOD")
        for (index in 0 until characteristics) {
            val address = 0x80000000L + index * 64
            appendLine(
                """
                /* Map $index, made up */
                /begin CHARACTERISTIC map_$index "A made up map number $index with a long description like real ones"
                  MAP 0x${address.toString(16)} L 0 c 0 6553.5
                  /begin AXIS_DESCR STD_AXIS rpm_$index c 16 0 8000 FORMAT "%5.0" /end AXIS_DESCR
                  /begin AXIS_DESCR STD_AXIS load_$index c 12 0 100 /end AXIS_DESCR
                  FORMAT "%6.2"
                  EXTENDED_LIMITS -100 7000
                  /begin IF_DATA CANAPE_EXT 100 LINK_MAP "map_$index" 0x${address.toString(16)} 0 0 0 1 0x8F 0 DISPLAY 0 0 100 /end IF_DATA
                /end CHARACTERISTIC
                """.trimIndent()
            )
            if (index % 4 == 0) {
                appendLine("/begin MEASUREMENT meas_$index \"\" UWORD c 1 0 0 65535 ECU_ADDRESS 0xD000${(index % 0x10000).toString(16).padStart(4, '0')} /end MEASUREMENT")
            }
        }
        appendLine("/end MODULE\n/end PROJECT")
    }

    @Test
    fun anEightMegabyteA2lParsesQuickly() {
        val text = bigA2l(characteristics = 18_000)
        val bytes = text.encodeToByteArray()
        // Warm the JIT up once, as a running app would be
        A2lImport.read(bytes.copyOf(200_000).let { it.decodeToString().substringBeforeLast("/begin CHARACTERISTIC") + "/end MODULE /end PROJECT" }.encodeToByteArray())

        val (result, time) = measureTimedValue { A2lImport.read(bytes, "big.a2l") }
        val imported = (result as TkResult.Success).data
        println("A2L, ${bytes.size / 1024} KB: ${A2lImport.summary(imported)} in $time")
        assertTrue(bytes.size > 8 * 1024 * 1024, "Only ${bytes.size} bytes")
        assertEquals(18_000, imported.value.modules.single().characteristics.size)
        assertEquals(0, imported.diagnostics.count { it.severity == Severity.ERROR })
        assertTrue(time.inWholeSeconds < 15, "Parsing took $time")
    }

    @Test
    fun realA2lsReadWithoutCrashing() {
        val folder = listOf("../testdata/local", "testdata/local").map(::File).firstOrNull { it.isDirectory } ?: return
        folder.walkTopDown().filter { it.isFile && it.extension.lowercase() == "a2l" }.forEach { file ->
            val (result, time) = measureTimedValue { A2lImport.read(file.readBytes(), file.name) { path, _ -> File(file.parentFile, path).takeIf { it.isFile }?.readBytes() } }
            when (result) {
                is TkResult.Success -> {
                    println("${file.name}: ${A2lImport.summary(result.data)} in $time")
                    result.data.diagnostics.take(20).forEach { println("  $it") }
                }
                is TkResult.Failure -> println("${file.name}: ${result.error.message}")
            }
        }
    }
}
