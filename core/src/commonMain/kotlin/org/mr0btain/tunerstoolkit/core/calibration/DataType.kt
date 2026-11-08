package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.math.round

/**
 * How a number is stored in the ECU's memory. The names are A2L's, since every definition format maps onto these
 */
enum class DataType(val size: Int, val signed: Boolean, val float: Boolean = false) {
    UBYTE(1, false), SBYTE(1, true),
    UWORD(2, false), SWORD(2, true),
    ULONG(4, false), SLONG(4, true),
    A_UINT64(8, false), A_INT64(8, true),
    FLOAT16_IEEE(2, true, true), FLOAT32_IEEE(4, true, true), FLOAT64_IEEE(8, true, true);

    /** The smallest value this type can hold */
    val min: Double
        get() = when (this) {
            FLOAT16_IEEE -> -HALF_MAX
            FLOAT32_IEEE -> -Float.MAX_VALUE.toDouble()
            FLOAT64_IEEE -> -Double.MAX_VALUE
            else -> if (signed) -(2.0.pow(size * 8 - 1)) else 0.0
        }

    /** The largest value this type can hold */
    val max: Double
        get() = when (this) {
            FLOAT16_IEEE -> HALF_MAX
            FLOAT32_IEEE -> Float.MAX_VALUE.toDouble()
            FLOAT64_IEEE -> Double.MAX_VALUE
            else -> if (signed) 2.0.pow(size * 8 - 1) - 1 else 2.0.pow(size * 8) - 1
        }

    val range: ClosedFloatingPointRange<Double> get() = min..max

    /** [value] as this type stores it: rounded for whole number types, to float precision for floats */
    fun stored(value: Double): Double = when (this) {
        FLOAT16_IEEE -> halfToFloat(floatToHalf(value.toFloat())).toDouble()
        FLOAT32_IEEE -> value.toFloat().toDouble()
        FLOAT64_IEEE -> value
        else -> round(value)
    }

    companion object {
        /** The largest finite half precision float */
        const val HALF_MAX = 65504.0

        fun of(name: String?): DataType? = entries.firstOrNull { it.name == name }

        /** The unsigned whole number type of [size] bytes, for pointers and the like */
        fun unsigned(size: Int): DataType? = when (size) {
            1 -> UBYTE
            2 -> UWORD
            4 -> ULONG
            8 -> A_UINT64
            else -> null
        }
    }
}

private fun Double.pow(exponent: Int): Double {
    var result = 1.0
    repeat(exponent) { result *= this }
    return result
}

/** The number at [address] as [type], or null when the image doesn't hold all of its bytes */
fun MemoryImage.readNumber(address: Long, type: DataType, order: ByteOrder): Double? {
    if (!contains(address, type.size)) return null
    return when (type) {
        DataType.FLOAT16_IEEE -> halfToFloat(readUnsigned(address, 2, order).toInt()).toDouble()
        DataType.FLOAT32_IEEE -> readFloat32(address, order).toDouble()
        DataType.FLOAT64_IEEE -> readFloat64(address, order)
        DataType.A_UINT64 -> readUnsigned(address, 8, order).toDouble()
        else -> if (type.signed) readSigned(address, type.size, order).toDouble() else readUnsigned(address, type.size, order).toLong().toDouble()
    }
}

/**
 * Writes [value] at [address] as [type]. Whole number types are rounded; the caller checks the value fits
 * @return false when the image doesn't hold the bytes
 */
fun MemoryImage.writeNumber(address: Long, type: DataType, order: ByteOrder, value: Double): Boolean = when (type) {
    DataType.FLOAT16_IEEE -> writeUnsigned(address, 2, floatToHalf(value.toFloat()).toULong(), order)
    DataType.FLOAT32_IEEE -> writeFloat32(address, value.toFloat(), order)
    DataType.FLOAT64_IEEE -> writeFloat64(address, value, order)
    DataType.A_UINT64 -> writeUnsigned(address, 8, round(value).toULong(), order)
    else -> if (type.signed) writeSigned(address, type.size, round(value).toLong(), order)
    else writeUnsigned(address, type.size, round(value).toLong().toULong(), order)
}

/** An IEEE half precision float's bits as a float */
internal fun halfToFloat(bits: Int): Float {
    val sign = if (bits and 0x8000 != 0) -1f else 1f
    val exponent = (bits shr 10) and 0x1F
    val mantissa = bits and 0x3FF
    return sign * when (exponent) {
        0 -> mantissa * HALF_SUBNORMAL_STEP
        0x1F -> if (mantissa == 0) Float.POSITIVE_INFINITY else Float.NaN
        else -> Float.fromBits(((exponent - 15 + 127) shl 23) or (mantissa shl 13))
    }
}

/** [value] as IEEE half precision bits, rounded to the nearest (ties to even) */
internal fun floatToHalf(value: Float): Int {
    val bits = value.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val magnitude = bits and 0x7FFFFFFF
    return when {
        magnitude > 0x7F800000 -> sign or 0x7E00 // NaN
        magnitude >= 0x477FF000 -> sign or 0x7C00 // 65520 and up round to infinity
        magnitude < 0x38800000 -> sign or round(kotlin.math.abs(value) / HALF_SUBNORMAL_STEP).toInt() // subnormal
        else -> {
            val exponent = (magnitude ushr 23) - 127 + 15
            val mantissa = magnitude and 0x7FFFFF
            var half = (exponent shl 10) or (mantissa ushr 13)
            val rest = mantissa and 0x1FFF
            if (rest > 0x1000 || (rest == 0x1000 && half and 1 == 1)) half++
            sign or half
        }
    }
}

/** 2^-24, the step between half precision subnormals */
private const val HALF_SUBNORMAL_STEP = 5.9604645E-8f
