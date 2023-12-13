package components.parsers.a2l.modcommon

import components.parsers.a2l.classes.enums.ByteOrder
import components.parsers.a2l.classes.enums.DataSize
import components.parsers.a2l.classes.enums.Deposit
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseDataSize
import components.parsers.a2l.classes.enums.parseDeposit

/**
 * Parses a list of strings and returns a ModCommon with parameters if they exist
 */

fun buildModCommon(input: List<String>): ModCommon {
    var mAlignmentByte = 1
    var mAlignmentLong = 4
    var mAlignmentFloat16Ieee = 4
    var mAlignmentFloat32Ieee = 4
    var mAlignmentFloat64Ieee = 4
    var mAlignmentInt64 = 8
    var mAlignmentWord = 2
    var mByteOrder = ByteOrder.LITTLE_ENDIAN
    var mDataSize = DataSize.NOT_SET
    var mDeposit = Deposit.NOT_SET

    try {
        input.forEach { line ->
            when {
                line.startsWith("ALIGNMENT_BYTE") -> {
                    val result = line
                        .replace("ALIGNMENT_BYTE", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentByte = result
                    }
                }

                line.startsWith("ALIGNMENT_LONG") -> {
                    val result = line
                        .replace("ALIGNMENT_LONG", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentLong = result
                    }
                }

                line.startsWith("ALIGNMENT_FLOAT16_IEEE") -> {
                    val result = line
                        .replace("ALIGNMENT_FLOAT16_IEEE", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentFloat16Ieee = result
                    }
                }

                line.startsWith("ALIGNMENT_FLOAT32_IEEE") -> {
                    val result = line
                        .replace("ALIGNMENT_FLOAT32_IEEE", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentFloat32Ieee = result
                    }
                }

                line.startsWith("ALIGNMENT_FLOAT64_IEEE") -> {
                    val result = line
                        .replace("ALIGNMENT_FLOAT64_IEEE", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentFloat64Ieee = result
                    }
                }

                line.startsWith("ALIGNMENT_INT64") -> {
                    val result = line
                        .replace("ALIGNMENT_INT64", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentInt64 = result
                    }
                }

                line.startsWith("ALIGNMENT_WORD") -> {
                    val result = line
                        .replace("ALIGNMENT_WORD", "")
                        .replace("\"", "")
                        .trim().toIntOrNull()
                    if (result != null) {
                        mAlignmentWord = result
                    }
                }

                line.startsWith("BYTE_ORDER") -> {
                    mByteOrder = line.parseByteOrder()
                }

                line.startsWith("DATA_SIZE") -> {
                    mDataSize = line.parseDataSize()
                }

                line.startsWith("DEPOSIT") -> {
                    mDeposit = line.parseDeposit()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ModCommon(
        alignmentByte = mAlignmentByte,
        alignmentLong = mAlignmentLong,
        alignmentFloat16Ieee = mAlignmentFloat16Ieee,
        alignmentFloat32Ieee = mAlignmentFloat32Ieee,
        alignmentFloat64Ieee = mAlignmentFloat64Ieee,
        alignmentInt64 = mAlignmentInt64,
        alignmentWord = mAlignmentWord,
        byteOrder = mByteOrder,
        dataSize = mDataSize,
        deposit = mDeposit
    )
}