package org.mr0btain.tunerstoolkit.core.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImageSearchTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    // Two segments: 0x100 holds 1 2 3 4 1 2, 0x200 holds 3 4 1 2
    private val image = MemoryImage.of(0x100L to bytes(1, 2, 3, 4, 1, 2), 0x200L to bytes(3, 4, 1, 2))
    private val oneTwo = BytePattern(bytes(1, 2))

    @Test
    fun forwards() {
        assertEquals(0x100, image.find(oneTwo, 0))
        assertEquals(0x104, image.find(oneTwo, 0x101))
        assertEquals(0x202, image.find(oneTwo, 0x105))
        assertNull(image.find(oneTwo, 0x203))
    }

    @Test
    fun backwards() {
        assertEquals(0x202, image.find(oneTwo, 0xFFFF, backwards = true))
        assertEquals(0x104, image.find(oneTwo, 0x201, backwards = true))
        assertEquals(0x100, image.find(oneTwo, 0x103, backwards = true))
        assertNull(image.find(oneTwo, 0xFF, backwards = true))
    }

    @Test
    fun wrapsAround() {
        assertEquals(0x100, image.find(oneTwo, 0x203, wrap = true))
        assertEquals(0x202, image.find(oneTwo, 0xFF, backwards = true, wrap = true))
    }

    @Test
    fun doesNotMatchAcrossAGap() {
        // 1 2 at the end of the first segment, 3 4 at the start of the second
        assertNull(image.find(BytePattern(bytes(1, 2, 3, 4)), 0x101))
    }

    @Test
    fun wildcards() {
        val pattern = assertNotNull(BytePattern.parseHex("03 ?? 01"))
        assertEquals(0x102, image.find(pattern, 0))
        assertEquals(0x200, image.find(pattern, 0x103))
    }

    @Test
    fun parsingPatterns() {
        assertEquals(3, BytePattern.parseHex("0x12,34 ab")?.size)
        assertNull(BytePattern.parseHex("123"))
        assertNull(BytePattern.parseHex("zz"))
        assertNull(BytePattern.parseHex("?? ??"))
        assertNull(BytePattern.parseHex(""))
        val text = MemoryImage.of(0L to "xxTTDEMO1".encodeToByteArray())
        assertEquals(2, text.find(BytePattern.ofText("TTDEMO")!!, 0))
    }

    @Test
    fun parsingAddresses() {
        assertEquals(0x8000_1234L, parseAddress("0x80001234"))
        assertEquals(0x8000_1234L, parseAddress(" 8000_1234 "))
        assertEquals(0x1234L, parseAddress("#4660"))
        assertNull(parseAddress("0x1_0000_0000"))
        assertNull(parseAddress("nope"))
        assertNull(parseAddress(""))
    }
}
