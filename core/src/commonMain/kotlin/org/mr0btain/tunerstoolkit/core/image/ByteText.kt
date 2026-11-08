package org.mr0btain.tunerstoolkit.core.image

/** Bytes as text and back, for copying and pasting */
object ByteText {
    private const val DIGITS = "0123456789ABCDEF"

    /** "1A 2B 3C" */
    fun toHex(bytes: ByteArray, separator: String = " "): String = buildString(bytes.size * (2 + separator.length)) {
        bytes.forEachIndexed { index, byte ->
            if (index > 0) append(separator)
            val value = byte.toInt() and 0xFF
            append(DIGITS[value shr 4])
            append(DIGITS[value and 0x0F])
        }
    }

    /** Printable ASCII as itself, everything else as '.', the same as the hex view's text column */
    fun toAscii(bytes: ByteArray): String = buildString(bytes.size) {
        bytes.forEach { val value = it.toInt() and 0xFF; append(if (value in 0x20..0x7E) value.toChar() else '.') }
    }

    /**
     * Hex bytes from pasted text, however it was written: "1A 2B", "1a2b", "0x1A, 0x2B", "1A-2B" or across lines.
     * Null when it isn't hex, or has an odd number of digits
     */
    fun parseHex(text: String): ByteArray? {
        val digits = StringBuilder()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '0' && index + 1 < text.length && (text[index + 1] == 'x' || text[index + 1] == 'X') -> index++
                char.digitToIntOrNull(16) != null -> digits.append(char)
                char.isWhitespace() || char == ',' || char == '-' || char == ':' || char == ';' -> {}
                else -> return null
            }
            index++
        }
        if (digits.isEmpty() || digits.length % 2 != 0) return null
        return ByteArray(digits.length / 2) { ((digits[it * 2].digitToInt(16) shl 4) or digits[it * 2 + 1].digitToInt(16)).toByte() }
    }

    /** Text as one byte per character; characters past Latin-1 become '?' */
    fun fromText(text: String): ByteArray = ByteArray(text.length) { text[it].code.let { code -> if (code <= 0xFF) code else '?'.code }.toByte() }
}
