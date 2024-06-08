package components.hextexteditor.components

/**
 * Formats a string of Hexadecimal data into the format we see in the editor with line breaks and spaces
 */

fun String.formatHexStringForEditor(
    bitEncoding: BitEncoding,
    digitsPerLine: Int
): String {
    val actualHexCharsPerLine = bitEncoding.number() * digitsPerLine
    val result = StringBuilder()

    for (i in this.indices) {
        if (i % bitEncoding.number() == 0 && i != 0 && i % actualHexCharsPerLine != 0) {
            result.append(' ')
        }
        if (i % (actualHexCharsPerLine) == 0 && i != 0) {
            result.append('\n')
        }
        result.append(this[i])
    }

    return result.toString()
}

/**
 * Generates the offsets for each line from the text of our hex data.
 */

@OptIn(ExperimentalStdlibApi::class)
fun String.getOffsetsFromHex(digitsPer: Int, bitEncoding: BitEncoding): String {
    // Get the number of lines
    val lines = this.length / (digitsPer * bitEncoding.number())
    val byteLocations = mutableListOf<String>() // create an empty list to store byte locations
    var offset = 0 // initialize the offset to 0

    for (line in 0..lines + 1) {
        byteLocations.add(offset.toHexString(HexFormat.UpperCase).removeRange(0, 2)) //  String.format("%04X", offset) add the current offset as a string to the list
        offset += (digitsPer * bitEncoding.number()) // increment the offset by half the length of the line
        // (assuming each byte is represented by two characters)
    }

    return byteLocations.joinToString("\n") // join the byte locations into a
    // single string with line breaks
}

/**
 *  Formats the Hexadecimal data into its Ascii representation
 */

fun String.convertHexToAscii(): String {
    val stringBuilder = StringBuilder()
    var i = 0

    try {
        while (i < this.length && this.length >= i+2) {
            val hex = this.substring(i, i + 2)
            val decimal = hex.toInt(16)
            stringBuilder.append(decimal.toChar())
            i += 2
        }
    } catch (e: Exception) {
        println(this)
        e.printStackTrace()
    }

    return stringBuilder.toString()
}

/**
 * Adds the \n for each line break and the '.' for the non Ascii characters
 */

fun String.formatForAsciiEditor(digitsPerLine: Int): String {
    val outputBuilder = StringBuilder()

    for ((i, c) in this.withIndex()) {
        if (i % digitsPerLine == 0 && i < this.length && i != 0) {
            outputBuilder.append('\n')
        }

        val isAsciiLetterOrDigit = c.isLetterOrDigit() && c.code < 128
        outputBuilder.append(if (isAsciiLetterOrDigit) c else '.')
    }

    return outputBuilder.toString()
}

fun String.addLineBreaksForAsciiEditor(digitsPerLine: Int): String {
    val outputBuilder = StringBuilder()

    for ((i, c) in this.withIndex()) {
        // Append the character
        outputBuilder.append(c)

        // Add line break if were at the end of the line
        if ((i + 1) % digitsPerLine == 0 && i < this.length - 1) {
            outputBuilder.append('\n')
        }
    }

    return outputBuilder.toString()
}