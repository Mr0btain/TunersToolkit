package org.mr0btain.tunerstoolkit.ui.kit.hex

import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexDisplayTest {
    private val bytes = byteArrayOf(0x34, 0x12, 0xFE.toByte(), 0xFF.toByte(), 0x01, 0x00, 0x00, 0x80.toByte())

    @Test
    fun formats() {
        assertEquals("34", HexDisplay().format(bytes, 0))
        assertEquals("1234", HexDisplay(2, ByteOrder.LITTLE_ENDIAN).format(bytes, 0))
        assertEquals("3412", HexDisplay(2, ByteOrder.BIG_ENDIAN).format(bytes, 0))
        assertEquals("65534", HexDisplay(2, radix = HexRadix.UNSIGNED).format(bytes, 2))
        assertEquals("    -2", HexDisplay(2, radix = HexRadix.SIGNED).format(bytes, 2))
        assertEquals("  1", HexDisplay(1, radix = HexRadix.UNSIGNED).format(bytes, 4))
        assertEquals("-2147483647", HexDisplay(4, radix = HexRadix.SIGNED).format(bytes, 4))
        assertEquals(8, HexDisplay(4).cellChars)
    }

    @Test
    fun onlyPlainHexBytesAreEditable() {
        assertTrue(HexDisplay().editable)
        assertFalse(HexDisplay(2).editable)
        assertFalse(HexDisplay(radix = HexRadix.UNSIGNED).editable)
    }

    @Test
    fun selectionRunsBetweenAnchorAndCursor() {
        val state = HexViewState()
        assertNull(state.selection)
        state.moveTo(0x10)
        assertNull(state.selection, "Just a cursor")
        state.moveTo(0x13, extend = true)
        assertEquals(AddressRange(0x10, 0x14), state.selection)
        state.moveTo(0x0E, extend = true)
        assertEquals(AddressRange(0x0E, 0x11), state.selection)
        state.moveTo(0x20)
        assertNull(state.selection)
        state.select(AddressRange(0x40, 0x48))
        assertEquals(AddressRange(0x40, 0x48), state.selection)
    }
}
