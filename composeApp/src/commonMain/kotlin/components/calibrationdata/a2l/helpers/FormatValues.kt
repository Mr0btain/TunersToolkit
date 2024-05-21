package components.calibrationdata.a2l.helpers

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.applyBitMask
import components.parsers.a2l.characteristic.getBitMask
import components.parsers.a2l.compumethod.CompuMethod
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.DataType
import components.calibrationdata.classes.HexDataResult
import components.parsers.a2l.classes.enums.ByteOrder
import components.parsers.a2l.compumethod.compute

/**
 * Formats a String using the format spec %5.1
 */
fun String.formatLength(format: String): String {
    val actualFormat = format.replace("\"", "").replace("%", "").split(".")
    val input = this.split(".")

    when (input.size) {
        1 -> {
            if (actualFormat.size > 1) {
                val beforeLength = actualFormat[0].replace(".", "").toInt()
                val afterLength = actualFormat[1].replace(".", "").toInt()
                val inputBefore = input[0]
                val result = StringBuilder()

                when {
                    inputBefore.length == beforeLength -> {
                        result.append(inputBefore)
                    }

                    inputBefore.length < beforeLength -> {
                        for (i in 0..(beforeLength - inputBefore.length)) {
                            result.append("0")
                        }

                        result.append(inputBefore)
                    }

                    else -> {
                        result.append(inputBefore.subSequence(0, beforeLength))
                    }
                }

                if (afterLength != 0) { result.append(".") }

                for (i in 0.. afterLength) {
                    result.append("0")
                }

                return result.toString()
            } else {
                val beforeLength = actualFormat[0].replace(".", "").toInt()
                val inputBefore = input[0]
                val result = StringBuilder()

                when {
                    inputBefore.length == beforeLength -> {
                        result.append(inputBefore)
                    }

                    inputBefore.length < beforeLength -> {
                        for (i in 0 until (beforeLength - inputBefore.length)) {
                            result.append("0")
                        }

                        result.append(inputBefore)
                    }

                    else -> {
                        result.append(inputBefore.subSequence(0, beforeLength))
                    }
                }

                return result.toString()
            }
        }

        2 -> {
            if (actualFormat.size > 1) {
                val beforeLength = actualFormat[0].replace(".", "").toInt()
                val afterLength = actualFormat[1].replace(".", "").toInt()
                val inputBefore = input[0]
                val inputAfter = input[1]
                val result = StringBuilder()

                when {
                    inputBefore.length == beforeLength -> {
                        result.append(inputBefore)
                    }

                    inputBefore.length < beforeLength -> {
                        for (i in 0 until (beforeLength - inputBefore.length)) {
                            result.append("0")
                        }

                        result.append(inputBefore)
                    }

                    else -> {
                        result.append(inputBefore.subSequence(0, beforeLength))
                    }
                }

                if (afterLength != 0) { result.append(".") }

                when {
                    inputAfter.length == afterLength -> { result.append(inputAfter) }

                    inputAfter.length < afterLength -> {
                        result.append(inputAfter)

                        for (i in 0 until (afterLength - inputAfter.length)) {
                            result.append("0")
                        }
                    }

                    inputAfter.length > afterLength -> {
                        result.append(inputAfter.subSequence(0, afterLength))
                    }
                }

                return result.toString()
            } else {
                val beforeLength = actualFormat[0].replace(".", "").toInt()
                val inputBefore = input[0]
                val result = StringBuilder()

                when {
                    inputBefore.length == beforeLength -> {
                        result.append(inputBefore)
                    }

                    inputBefore.length < beforeLength -> {
                        for (i in 0 until (beforeLength - inputBefore.length)) {
                            result.append("0")
                        }

                        result.append(inputBefore)
                    }

                    else -> {
                        result.append(inputBefore.subSequence(0, beforeLength))
                    }
                }

                return result.toString()
            }
        }
    }

    return this
}

/**
 * Formats the Input based on the data type
 */

fun String.formatValue(dataType: DataType): Number {
    // Format the value based on it DataType
    return when (dataType) {
        DataType.UBYTE -> {
            formatUBYTE(this)
        }

        DataType.SBYTE -> {
            formatSBYTE(this)
        }

        DataType.UWORD -> {
            formatUWORD(this)
        }

        DataType.SWORD -> {
            formatSWORD(this)
        }

        DataType.ULONG -> {
            formatULONG(this)
        }

        DataType.SLONG -> {
            formatSLONG(this)
        }

        DataType.Float32Ieee -> {
            formatFLOAT32IEEE(this)
        }

        DataType.Float64Ieee -> {
            formatFLOAT64IEEE(this)
        }

        DataType.Default -> {
            formatUBYTE(this)
        }

        else -> {
            this.toIntOrNull(16) ?: 0
        }
    }
}

/**
 * Formats RawByteDataResult values typically for Map
 */

fun HexDataResult.formatMapValues(
    characteristic: Characteristic,
    compuMethod: CompuMethod,
    a2lData: A2lData,
    format: String
): List<Any> {
    val result = mutableListOf<Any>()

    if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        this.formattedValues.forEach {
            result.add(compuMethod.compute(it, a2lData))
        }
    } else if (characteristic.getBitMask() != ULong.MIN_VALUE) {
        // Else if bitmask is not empty apply bitmask if its not blank
        this.formattedValues.forEach {
            result.add(characteristic.applyBitMask(it))
        }
    } else {
        // Else just convert the value straight to a double
        this.formattedValues.forEach {
            result.add(it.toDouble())
        }
    }

    return result
}

/**
 * Formats a list of values into their correct positions depending on Colum or Row Dir
 */

fun List<Any>.formatColumnOrRowDir(itemsPer: Int, isRowDir: Boolean): List<List<Any>> {
    // TODO: Re Test
    return if (isRowDir) {
        this.chunked(itemsPer)
    } else {
        val numGroups = (this.size + itemsPer - 1) / itemsPer

        (0 until numGroups).map { index ->
            this.slice(index * itemsPer until minOf((index + 1) * itemsPer, this.size))
        }
    }
}

/**
 * Formats a number to a String as specified EX: %8.2 where 8 digits before decidable 2 after,
 * preserving trailing zeros
 */

fun Number.formatToString(format: String): String {
    return this.toString().formatLength(format)
}

/**
 * Formats a hexadecimal string of data type UBYTE
 */
fun formatUBYTE(hexString: String): Short {
    return hexString.toUByte(16).toShort()
}

/**
 * Formats a hexadecimal string of data type SBYTE
 */
fun formatSBYTE(hexString: String): Byte {
    return hexString.toUByte(16).toByte()
}

/**
 * Formats a hexadecimal string of data type ULONG
 */
fun formatULONG(hexString: String): Long {
    return hexString.toULong(16).toLong()
}

/**
 * Formats a hexadecimal string of data type SLONG
 */
fun formatSLONG(hexString: String): Int {
    return hexString.toLong(16).toInt()
}

/**
 * Formats a hexadecimal string of data type UWORD
 */
fun formatUWORD(hexString: String): Int {
    return hexString.toUShort(16).toInt()
}

/**
 * Formats a hexadecimal string of data type SWORD
 */
fun formatSWORD(hexString: String): Short {
    return hexString.toInt(16).toShort()
}

/**
 * Formats a hexadecimal string of data type FLOAT32
 */
fun formatFLOAT32IEEE(hexString: String): Float {
    val intValue = hexString.toUInt(16).toInt()
    return Float.fromBits(intValue)
}

/**
 * Formats a hexadecimal string of data type FLOAT64
 */
fun formatFLOAT64IEEE(hexString: String): Double {
    val longValue = hexString.toULong(16).toLong()
    return Double.fromBits(longValue)
}

fun String.applyByteOrder(byteOrder: ByteOrder): String {
    return when (byteOrder) {
        // No change needed for big-endian order
        ByteOrder.BIG_ENDIAN -> this

        ByteOrder.LITTLE_ENDIAN -> {
            // Split the hex string into pairs of characters (bytes)
            val bytePairs = this.chunked(2)

            // Reverse the order of the byte pairs to convert to little-endian
            val reversedHex = bytePairs.reversed().joinToString("")

            reversedHex
        }
    }
}
