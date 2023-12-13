package components.calibrationdata.helpers

import components.a2l.characteristic.Characteristic
import components.a2l.compumethod.CompuMethod
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ConversionType
import components.a2l.classes.enums.DataType
import components.calibrationdata.classes.HexDataResult
import components.a2l.classes.enums.ByteOrder

/**
 * Formats a String using the format spec %5.1
 */
fun String.formatLength(format: String): String {
    val mFormat = format.replace("\"", "").replace("%", "").split(".")
    val mInput = this.split(".")

    when (mInput.size) {
        1 -> {
            if (mFormat.size > 1) {
                val mBeforeLength = mFormat[0].replace(".", "").toInt()
                val mAfterLength = mFormat[1].replace(".", "").toInt()
                val mInputBefore = mInput[0]
                val mResult = StringBuilder()

                when {
                    mInputBefore.length == mBeforeLength -> {
                        mResult.append(mInputBefore)
                    }

                    mInputBefore.length < mBeforeLength -> {
                        for (i in 0..(mBeforeLength - mInputBefore.length)) {
                            mResult.append("0")
                        }

                        mResult.append(mInputBefore)
                    }

                    else -> {
                        mResult.append(mInputBefore.subSequence(0, mBeforeLength))
                    }
                }

                if (mAfterLength != 0) { mResult.append(".") }

                for (i in 0.. mAfterLength) {
                    mResult.append("0")
                }

                return mResult.toString()
            } else {
                val mBeforeLength = mFormat[0].replace(".", "").toInt()
                val mInputBefore = mInput[0]
                val mResult = StringBuilder()

                when {
                    mInputBefore.length == mBeforeLength -> {
                        mResult.append(mInputBefore)
                    }

                    mInputBefore.length < mBeforeLength -> {
                        for (i in 0 until (mBeforeLength - mInputBefore.length)) {
                            mResult.append("0")
                        }

                        mResult.append(mInputBefore)
                    }

                    else -> {
                        mResult.append(mInputBefore.subSequence(0, mBeforeLength))
                    }
                }

                return mResult.toString()
            }
        }

        2 -> {
            if (mFormat.size > 1) {
                val mBeforeLength = mFormat[0].replace(".", "").toInt()
                val mAfterLength = mFormat[1].replace(".", "").toInt()
                val mInputBefore = mInput[0]
                val mInputAfter = mInput[1]
                val mResult = StringBuilder()

                when {
                    mInputBefore.length == mBeforeLength -> {
                        mResult.append(mInputBefore)
                    }

                    mInputBefore.length < mBeforeLength -> {
                        for (i in 0 until (mBeforeLength - mInputBefore.length)) {
                            mResult.append("0")
                        }

                        mResult.append(mInputBefore)
                    }

                    else -> {
                        mResult.append(mInputBefore.subSequence(0, mBeforeLength))
                    }
                }

                if (mAfterLength != 0) { mResult.append(".") }

                when {
                    mInputAfter.length == mAfterLength -> { mResult.append(mInputAfter) }

                    mInputAfter.length < mAfterLength -> {
                        mResult.append(mInputAfter)

                        for (i in 0 until (mAfterLength - mInputAfter.length)) {
                            mResult.append("0")
                        }
                    }

                    mInputAfter.length > mAfterLength -> {
                        mResult.append(mInputAfter.subSequence(0, mAfterLength))
                    }
                }

                return mResult.toString()
            } else {
                val mBeforeLength = mFormat[0].replace(".", "").toInt()
                val mInputBefore = mInput[0]
                val mResult = StringBuilder()

                when {
                    mInputBefore.length == mBeforeLength -> {
                        mResult.append(mInputBefore)
                    }

                    mInputBefore.length < mBeforeLength -> {
                        for (i in 0 until (mBeforeLength - mInputBefore.length)) {
                            mResult.append("0")
                        }

                        mResult.append(mInputBefore)
                    }

                    else -> {
                        mResult.append(mInputBefore.subSequence(0, mBeforeLength))
                    }
                }

                return mResult.toString()
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
    val mResult = mutableListOf<Any>()

    if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        this.formattedValues.forEach {
            mResult.add(compuMethod.compute(it, a2lData, format))
        }
    } else if (characteristic.getBitMask() != ULong.MIN_VALUE) {
        // Else if bitmask is not empty apply bitmask if its not blank
        this.formattedValues.forEach {
            mResult.add(characteristic.applyBitMask(it))
        }
    } else {
        // Else just convert the value straight to a double
        this.formattedValues.forEach {
            mResult.add(it.toDouble())
        }
    }

    return mResult
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
 * Formats a Number to a Double, doesn't always keep trailing zeros, formatToString is much more reliable
 */

fun Number.formatValue(format: String): Double {
    val formatString = "${format.replace("\"", "")}f"
    val formattedString = this.toString().formatLength(format)
    return formattedString.toDouble()
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
