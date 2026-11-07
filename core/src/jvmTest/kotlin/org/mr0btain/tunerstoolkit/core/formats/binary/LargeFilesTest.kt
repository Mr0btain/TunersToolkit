package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

/**
 * Sizes like real ECU files, and real files when they're in testdata/local (never committed, see docs/testing.md).
 * Times are printed, not asserted tightly, so a slow CI machine doesn't fail them
 */
class LargeFilesTest {
    @Test
    fun aFourMegabyteImageRoundTrips() {
        val random = Random(4)
        val image = MemoryImage.Builder().apply {
            put(0x8000_0000L, random.nextBytes(2 * 1024 * 1024))
            put(0x8030_0000L, random.nextBytes(2 * 1024 * 1024))
        }.build().image

        val (hex, writeTime) = measureTimedValue { (IntelHex.write(image, 32) as TkResult.Success).data.encodeToByteArray() }
        val (back, readTime) = measureTimedValue { (IntelHex.read(hex) as TkResult.Success).data }
        println("Intel HEX, 4 MB: wrote ${hex.size / 1024} KB in $writeTime, read it in $readTime")
        assertTrue(back.value.contentEquals(image))
        assertTrue(readTime.inWholeSeconds < 10, "Reading took $readTime")

        val srec = (SRecord.write(image) as TkResult.Success).data.encodeToByteArray()
        val (srecBack, srecTime) = measureTimedValue { (SRecord.read(srec) as TkResult.Success).data }
        println("S-record, 4 MB: read in $srecTime")
        assertTrue(srecBack.value.contentEquals(image))
    }

    @Test
    fun realFilesReadCleanly() {
        val folder = listOf("../testdata/local", "testdata/local").map(::File).firstOrNull { it.isDirectory }
        val files = folder?.walkTopDown()
            ?.filter { it.isFile && it.extension.lowercase() in setOf("hex", "s19", "s28", "s37", "srec", "mot") }
            ?.toList()
            .orEmpty()
        if (files.isEmpty()) {
            println("No real files in testdata/local, skipped")
            return
        }

        for (file in files) {
            val bytes = file.readBytes()
            val (result, time) = measureTimedValue { BinaryImport.read(bytes, file.name) }
            val imported = (result as? TkResult.Success)?.data ?: error("${file.name} didn't read: $result")
            val image = imported.value
            println("${file.name}: ${image.ranges.size} segments, ${image.size} bytes in $time; ${imported.diagnostics.size} diagnostics")
            imported.diagnostics.take(10).forEach { println("  $it") }
            assertTrue(imported.diagnostics.none { it.severity == Severity.ERROR }, "${file.name} has errors")

            // Writing it back and reading that gives the same bytes
            val again = when (BinaryFormat.detect(bytes)) {
                BinaryFormat.S_RECORD -> SRecord.read((SRecord.write(image) as TkResult.Success).data.encodeToByteArray())
                else -> IntelHex.read((IntelHex.write(image, 32) as TkResult.Success).data.encodeToByteArray())
            }
            assertTrue((again as TkResult.Success).data.value.contentEquals(image), "${file.name} round trip")
        }
    }
}
