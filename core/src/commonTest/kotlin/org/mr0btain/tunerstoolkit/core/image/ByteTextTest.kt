package org.mr0btain.tunerstoolkit.core.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ByteTextTest {
    private val bytes = byteArrayOf(0x1A, 0x00, 0x7F, 0xFF.toByte(), 0x41)

    @Test
    fun toHexAndAscii() {
        assertEquals("1A 00 7F FF 41", ByteText.toHex(bytes))
        assertEquals("1A007FFF41", ByteText.toHex(bytes, ""))
        assertEquals("....A", ByteText.toAscii(bytes).replace('\u007F', '.'))
        assertEquals("", ByteText.toHex(ByteArray(0)))
    }

    @Test
    fun parsesHexHoweverItsWritten() {
        listOf("1A 00 7F FF 41", "1a007fff41", "0x1A, 0x00, 0x7F, 0xFF, 0x41", "1A-00-7F\nFF:41", " 1A 00 7f ff 41 \n").forEach {
            assertContentEquals(bytes, ByteText.parseHex(it), it)
        }
        assertContentEquals(bytes, ByteText.parseHex(ByteText.toHex(bytes)))
    }

    @Test
    fun refusesWhatIsntHex() {
        listOf("", "  ", "1A 0", "hello", "1A ZZ", "1A??").forEach { assertNull(ByteText.parseHex(it), it) }
    }

    @Test
    fun textIsOneBytePerCharacter() {
        assertContentEquals(byteArrayOf(0x48, 0x69, 0xE9.toByte(), '?'.code.toByte()), ByteText.fromText("Hié€"))
    }
}
