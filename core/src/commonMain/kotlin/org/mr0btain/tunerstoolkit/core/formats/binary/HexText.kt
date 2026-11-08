package org.mr0btain.tunerstoolkit.core.formats.binary

/** Hex digits and line splitting shared by the Intel HEX and S-record readers and writers */
internal object HexText {
    private val DIGITS = "0123456789ABCDEF".toCharArray()

    /** 0 to 15 for a hex digit's character code, -1 for anything else */
    fun digit(code: Int): Int = when (code) {
        in 0x30..0x39 -> code - 0x30
        in 0x41..0x46 -> code - 0x41 + 10
        in 0x61..0x66 -> code - 0x61 + 10
        else -> -1
    }

    /** The byte written as two hex digits at [index] in [text], or -1 if they aren't hex */
    fun byteAt(text: ByteArray, index: Int): Int {
        if (index + 1 >= text.size) return -1
        val high = digit(text[index].toInt())
        val low = digit(text[index + 1].toInt())
        return if (high < 0 || low < 0) -1 else (high shl 4) or low
    }

    fun appendByte(builder: StringBuilder, value: Int) {
        builder.append(DIGITS[(value shr 4) and 0xF]).append(DIGITS[value and 0xF])
    }

    /**
     * Calls [onLine] with each line's start and end (exclusive, without the line break) and its number from 1.
     * Takes \n, \r\n and \r line breaks, and skips a UTF-8 byte order mark at the start
     */
    inline fun forEachLine(text: ByteArray, onLine: (start: Int, end: Int, number: Int) -> Unit) {
        var start = bomLength(text)
        var number = 1
        var i = start
        while (i < text.size) {
            val c = text[i].toInt()
            if (c == '\n'.code || c == '\r'.code) {
                onLine(start, i, number)
                if (c == '\r'.code && i + 1 < text.size && text[i + 1].toInt() == '\n'.code) i++
                number++
                start = i + 1
            }
            i++
        }
        if (start < text.size) onLine(start, text.size, number)
    }

    /** [start] moved past spaces and tabs */
    fun skipBlanks(text: ByteArray, start: Int, end: Int): Int {
        var i = start
        while (i < end && (text[i].toInt() == ' '.code || text[i].toInt() == '\t'.code)) i++
        return i
    }

    /** [end] moved back past spaces and tabs */
    fun trimEnd(text: ByteArray, start: Int, end: Int): Int {
        var i = end
        while (i > start && (text[i - 1].toInt() == ' '.code || text[i - 1].toInt() == '\t'.code)) i--
        return i
    }

    /** 3 when [bytes] start with a UTF-8 byte order mark, else 0 */
    fun bomLength(bytes: ByteArray): Int =
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0

    /** The first byte that isn't whitespace or a UTF-8 byte order mark, or -1 */
    fun firstContentIndex(bytes: ByteArray): Int {
        var i = bomLength(bytes)
        while (i < bytes.size) {
            val c = bytes[i].toInt()
            if (c != ' '.code && c != '\t'.code && c != '\r'.code && c != '\n'.code) return i
            i++
        }
        return -1
    }
}
