package components.parsers.hex

import kotlin.random.Random

/*
fun readFileAsAscii(inputStream: InputStream): List<Char> {
    val result = StringBuilder()
    BufferedReader(InputStreamReader(inputStream, Charset.forName("US-ASCII"))).use { reader ->
        reader.forEachLine {
            result.append(it)
        }
    }
    return result.toList()
}
 */

fun String.hexToAscii(): String {
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

fun String.asciiToHex(): String {
    val hexChars = "0123456789ABCDEF"
    val result = StringBuilder(this.length * 2)

    for (char in this) {
        val asciiValue = char.code
        val firstHexDigit = hexChars[asciiValue ushr 4]
        val secondHexDigit = hexChars[asciiValue and 0x0F]
        result.append(firstHexDigit)
        result.append(secondHexDigit)
    }

    return result.toString()
    /*
    val stringBuilder = StringBuilder()
    for (c in this.toCharArray()) {
        stringBuilder.append(c.code.toHexString(HexFormat.UpperCase))
    }
    return stringBuilder.toString()
     */
}

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.toAsciiString(): String {
    return this.toHexString(HexFormat.Default).hexToAscii()
}

fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(this.length / 2)
    for (i in this.indices step 2) {
        val byteValue = this.substring(i, i + 2).toInt(16)
        result[i / 2] = byteValue.toByte()
    }
    return result
}