package org.mr0btain.tunerstoolkit.core.a2l

/**
 * Turns an A2L file's bytes into text. A2L has no encoding declaration, so it's worked out: a byte order mark says
 * UTF-8 or UTF-16; without one it's UTF-8 if the bytes are valid UTF-8, and Latin-1 (what most older tools wrote)
 * if not. Latin-1 can decode any bytes, so this never fails
 */
object A2lText {
    enum class Encoding { UTF_8, UTF_16LE, UTF_16BE, LATIN_1 }

    fun detect(bytes: ByteArray): Encoding = when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) -> Encoding.UTF_8
        bytes.startsWith(0xFF, 0xFE) -> Encoding.UTF_16LE
        bytes.startsWith(0xFE, 0xFF) -> Encoding.UTF_16BE
        // UTF-16 without a mark: ASCII text has every other byte zero
        bytes.size >= 4 && bytes[0].toInt() != 0 && bytes[1].toInt() == 0 && bytes[3].toInt() == 0 -> Encoding.UTF_16LE
        bytes.size >= 4 && bytes[0].toInt() == 0 && bytes[1].toInt() != 0 && bytes[2].toInt() == 0 -> Encoding.UTF_16BE
        isUtf8(bytes) -> Encoding.UTF_8
        else -> Encoding.LATIN_1
    }

    fun decode(bytes: ByteArray): String = decode(bytes, detect(bytes))

    fun decode(bytes: ByteArray, encoding: Encoding): String = when (encoding) {
        Encoding.UTF_8 -> {
            val start = if (bytes.startsWith(0xEF, 0xBB, 0xBF)) 3 else 0
            bytes.decodeToString(start, bytes.size)
        }
        Encoding.UTF_16LE, Encoding.UTF_16BE -> {
            val little = encoding == Encoding.UTF_16LE
            val start = if (bytes.startsWith(0xFF, 0xFE) || bytes.startsWith(0xFE, 0xFF)) 2 else 0
            val chars = CharArray((bytes.size - start) / 2) { index ->
                val a = bytes[start + index * 2].toInt() and 0xFF
                val b = bytes[start + index * 2 + 1].toInt() and 0xFF
                (if (little) a or (b shl 8) else (a shl 8) or b).toChar()
            }
            chars.concatToString()
        }
        Encoding.LATIN_1 -> CharArray(bytes.size) { (bytes[it].toInt() and 0xFF).toChar() }.concatToString()
    }

    private fun isUtf8(bytes: ByteArray): Boolean = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
        true
    } catch (_: CharacterCodingException) {
        false
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { (this[it].toInt() and 0xFF) == prefix[it] }
}
