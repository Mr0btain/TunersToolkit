package org.mr0btain.tunerstoolkit.ui.kit.hex

import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HexClipboardTest {
    private val image = MemoryImage.of(
        0x100L to byteArrayOf(0x41, 0x42, 0x00, 0x7F),
        0x200L to byteArrayOf(0x10, 0x20)
    )

    @Test
    fun copiesHexOrText() {
        assertEquals("41 42 00 7F", HexClipboard.copy(image, AddressRange(0x100, 0x104), ascii = false))
        assertEquals("AB..", HexClipboard.copy(image, AddressRange(0x100, 0x104), ascii = true))
    }

    @Test
    fun copyingAcrossAGapLeavesTheGapOut() {
        assertEquals("7F 10", HexClipboard.copy(image, AddressRange(0x103, 0x201), ascii = false))
        assertNull(HexClipboard.copy(image, AddressRange(0x150, 0x160), ascii = false))
    }

    @Test
    fun pastesHexOverTheBytes() {
        val paste = assertIs<HexClipboard.Paste.Write>(HexClipboard.paste(image, 0x101, "0xAA, 0xBB", ascii = false))
        assertEquals(0x101, paste.address)
        assertContentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte()), paste.bytes)
        assertEquals(0, paste.dropped)
    }

    @Test
    fun pasteStopsAtTheEndOfTheSegment() {
        val paste = assertIs<HexClipboard.Paste.Write>(HexClipboard.paste(image, 0x102, "hello", ascii = true))
        assertContentEquals("he".encodeToByteArray(), paste.bytes)
        assertEquals(3, paste.dropped)
    }

    @Test
    fun pasteThatIsntBytes() {
        assertEquals(HexClipboard.Paste.NotHex, HexClipboard.paste(image, 0x100, "hello", ascii = false))
        assertEquals(HexClipboard.Paste.Empty, HexClipboard.paste(image, 0x100, "", ascii = false))
        assertEquals(HexClipboard.Paste.Outside, HexClipboard.paste(image, 0x150, "AA", ascii = false))
    }
}
