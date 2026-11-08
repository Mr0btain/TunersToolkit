package org.mr0btain.tunerstoolkit.core.a2l

import kotlin.test.Test
import kotlin.test.assertEquals

class A2lTextTest {
    private val text = "/begin PROJECT P \"Grüße\" /end PROJECT"

    @Test
    fun utf8WithAndWithoutAMark() {
        val bytes = text.encodeToByteArray()
        assertEquals(A2lText.Encoding.UTF_8, A2lText.detect(bytes))
        assertEquals(text, A2lText.decode(bytes))
        val marked = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + bytes
        assertEquals(text, A2lText.decode(marked))
    }

    @Test
    fun utf16BothWays() {
        val little = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.flatMap { listOf((it.code and 0xFF).toByte(), (it.code shr 8).toByte()) }
        val big = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + text.flatMap { listOf((it.code shr 8).toByte(), (it.code and 0xFF).toByte()) }
        assertEquals(text, A2lText.decode(little))
        assertEquals(text, A2lText.decode(big))
        // Without a mark, the zero bytes give it away
        assertEquals(text, A2lText.decode(little.copyOfRange(2, little.size)))
        assertEquals(text, A2lText.decode(big.copyOfRange(2, big.size)))
    }

    @Test
    fun latin1WhenItIsntUtf8() {
        val bytes = text.map { it.code.toByte() }.toByteArray()
        assertEquals(A2lText.Encoding.LATIN_1, A2lText.detect(bytes))
        assertEquals(text, A2lText.decode(bytes))
    }

    @Test
    fun theWholeImportReadsLatin1() {
        val bytes = "ASAP2_VERSION 1 61 /begin PROJECT P \"Grüße\" /end PROJECT".map { it.code.toByte() }.toByteArray()
        val result = A2lImport.read(bytes) as org.mr0btain.tunerstoolkit.core.error.TkResult.Success
        assertEquals("Grüße", result.data.value.project?.longId)
    }
}
