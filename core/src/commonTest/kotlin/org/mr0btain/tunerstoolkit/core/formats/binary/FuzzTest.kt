package org.mr0btain.tunerstoolkit.core.formats.binary

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.testing.Fixtures
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Files are untrusted, so no input may make a reader throw: broken input becomes diagnostics or a failed result
 */
class FuzzTest {
    private fun readAll(bytes: ByteArray) {
        IntelHex.read(bytes)
        SRecord.read(bytes)
        BinaryImport.read(bytes, "fuzz")
    }

    @Test
    fun randomBytes() {
        val random = Random(1)
        repeat(300) {
            readAll(random.nextBytes(random.nextInt(0, 600)))
        }
    }

    @Test
    fun randomRecordShapedText() {
        val random = Random(2)
        val alphabet = ":S0123456789ABCDEFabcdefxz \r\n"
        repeat(300) {
            val text = buildString { repeat(random.nextInt(0, 400)) { append(alphabet[random.nextInt(alphabet.length)]) } }
            readAll(text.encodeToByteArray())
        }
    }

    @Test
    fun mutatedValidFiles() {
        val random = Random(3)
        val image = Fixtures.ecuLikeImage()
        val hex = (IntelHex.write(image, 32) as TkResult.Success).data.encodeToByteArray()
        val srec = (SRecord.write(image) as TkResult.Success).data.encodeToByteArray()

        for (original in listOf(hex, srec)) {
            repeat(150) {
                val bytes = original.copyOf()
                repeat(1 + random.nextInt(20)) {
                    val at = random.nextInt(bytes.size)
                    bytes[at] = when (random.nextInt(4)) {
                        0 -> random.nextInt(256).toByte()
                        1 -> '\n'.code.toByte()
                        2 -> ':'.code.toByte()
                        else -> "0123456789ABCDEF"[random.nextInt(16)].code.toByte()
                    }
                }
                val cut = bytes.copyOf(random.nextInt(bytes.size + 1))
                readAll(cut)
            }
        }
    }

    @Test
    fun aSingleFlippedDigitIsCaughtByTheChecksum() {
        val image = Fixtures.counting(0x1000, 64)
        val hex = (IntelHex.write(image) as TkResult.Success).data.toCharArray()
        // The first data digit of the second line
        val at = hex.indexOf('\n') + 1 + 9
        hex[at] = if (hex[at] == '0') '1' else '0'
        val result = IntelHex.read(hex.concatToString().encodeToByteArray()) as TkResult.Success
        assertTrue(result.data.errorCount == 1, result.data.diagnostics.joinToString())
    }
}
