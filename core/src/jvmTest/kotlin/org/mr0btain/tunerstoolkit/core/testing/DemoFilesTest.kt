package org.mr0btain.tunerstoolkit.core.testing

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.binary.IntelHex
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Keeps testdata/demo (the TT-DEMO-1 files tests and the app's demo project use) the same as [DemoEcu] makes them.
 * After changing DemoEcu, rewrite them with
 * `./gradlew :core:jvmTest --tests "*DemoFilesTest" -DregenerateDemo=true`
 */
class DemoFilesTest {
    private val folder = File("../testdata/demo")
    private val hexFile = File(folder, "tt-demo-1.hex")
    private val a2lFile = File(folder, "tt-demo-1.a2l")
    private val demo = DemoEcu.build()
    private val hex = (IntelHex.write(demo.image, bytesPerRecord = 32) as TkResult.Success).data

    @Test
    fun committedFilesMatchTheGenerator() {
        if (System.getProperty("regenerateDemo") == "true") {
            folder.mkdirs()
            hexFile.writeText(hex)
            a2lFile.writeText(demo.a2l)
            println("Wrote ${hexFile.path} and ${a2lFile.path}")
            return
        }

        assertTrue(hexFile.isFile && a2lFile.isFile, "testdata/demo is missing, make it with -DregenerateDemo=true")
        assertEquals(hex, hexFile.readText(), "tt-demo-1.hex is out of date, rewrite it with -DregenerateDemo=true")
        assertEquals(demo.a2l, a2lFile.readText(), "tt-demo-1.a2l is out of date, rewrite it with -DregenerateDemo=true")
    }

    @Test
    fun theImageHoldsWhatTheA2lSays() {
        val image = (IntelHex.read(hex.encodeToByteArray()) as TkResult.Success).data.value
        val order = ByteOrder.LITTLE_ENDIAN
        fun at(name: String) = demo.placed.getValue(name).address

        assertEquals(6800UL, image.readUnsigned(at("RevLimit"), 2, order))
        assertEquals(-35, image.readSigned(at("InjDeadTimeOffset"), 2, order))
        assertEquals(20261107UL, image.readUnsigned(at("SerialNumber"), 4, order))
        assertEquals(0UL, image.readUnsigned(at("LaunchControl"), 1, order))
        assertEquals("TTDEMO1-0001", image.read(at("SoftwareId"), 12).decodeToString())
        assertEquals(DemoEcu.EPK, image.read(DemoEcu.EPK_ADDRESS, DemoEcu.EPK.length).decodeToString())

        // A shared axis: its count, then its points
        val rpm = at("RpmAxis16")
        assertEquals(16UL, image.readUnsigned(rpm, 2, order))
        assertEquals(DemoEcu.rpmAxis.map { it.toLong().toULong() }, (0 until 16).map { image.readUnsigned(rpm + 2 + it * 2, 2, order) })

        // A STD_AXIS map: counts, X points, Y points, then values column by column
        val ignition = at("IgnitionBase")
        assertEquals(16UL, image.readUnsigned(ignition, 2, order))
        assertEquals(12UL, image.readUnsigned(ignition + 2, 2, order))
        val values = ignition + 4 + 16 * 2 + 12 * 2
        val expected = demo.placed.getValue("IgnitionBase").values
        // (x = 3, y = 5) is at index 3 * 12 + 5; raw = (deg + 35.25) / 0.375
        val raw = image.readUnsigned(values + 3 * 12 + 5, 1, order).toDouble()
        assertEquals(expected[3 * 12 + 5], raw * 0.375 - 35.25, 0.375 / 2)

        assertTrue(image.ranges.size == 2, "Flash and boot")
        assertEquals(DemoEcu.BOOT_START, image.startAddress)
    }
}
