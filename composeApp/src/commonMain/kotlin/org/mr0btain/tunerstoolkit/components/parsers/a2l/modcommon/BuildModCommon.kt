package org.mr0btain.tunerstoolkit.components.parsers.a2l.modcommon
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Deposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseDataSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseDeposit

/**
 * Parses a list of strings and returns a ModCommon with parameters if they exist
 */

fun buildModCommon(input: List<String>): ModCommon {
    val process = "BuildModCommon"
    val errors = mutableListOf<LogDataEntry>()
    var alignmentByte = 1
    var alignmentLong = 4
    var alignmentFloat16Ieee = 4
    var alignmentFloat32Ieee = 4
    var alignmentFloat64Ieee = 4
    var alignmentInt64 = 8
    var alignmentWord = 2
    var byteOrder = ByteOrder.LITTLE_ENDIAN
    var dataSize = DataSize.NOT_SET
    var deposit = Deposit.NOT_SET

    input.forEach { line ->
        when {
            line.startsWith("ALIGNMENT_BYTE") -> {
                safeCall {
                    alignmentByte = line
                        .replace("ALIGNMENT_BYTE", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentByte!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_LONG") -> {
                safeCall {
                    alignmentLong = line
                        .replace("ALIGNMENT_LONG", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentLong!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT16_IEEE") -> {
                safeCall {
                    alignmentFloat16Ieee = line
                        .replace("ALIGNMENT_FLOAT16_IEEE", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat16!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT32_IEEE") -> {
                safeCall {
                    alignmentFloat32Ieee = line
                        .replace("ALIGNMENT_FLOAT32_IEEE", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat32!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT64_IEEE") -> {
                safeCall {
                    alignmentFloat64Ieee = line
                        .replace("ALIGNMENT_FLOAT64_IEEE", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat64!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }

            }

            line.startsWith("ALIGNMENT_INT64") -> {
                safeCall {
                    alignmentInt64 = line
                        .replace("ALIGNMENT_INT64", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentInt64!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_WORD") -> {
                safeCall {
                    alignmentWord = line
                        .replace("ALIGNMENT_WORD", "")
                        .replace("\"", "")
                        .trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentWord!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("BYTE_ORDER") -> {
                byteOrder = line.parseByteOrder()
            }

            line.startsWith("DATA_SIZE") -> {
                val result =  line.parseDataSize()

                if (result != DataSize.NOT_SET) {
                    dataSize = result
                } else {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse DataSize!",
                            error = true
                        )
                    )
                }
            }

            line.startsWith("DEPOSIT") -> {
                val result = line.parseDeposit()

                if (result != Deposit.NOT_SET) {
                    deposit = result
                } else {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse Deposit!",
                            error = true
                        )
                    )
                }
            }
        }
    }

    return ModCommon(
        alignmentByte = alignmentByte,
        alignmentLong = alignmentLong,
        alignmentFloat16Ieee = alignmentFloat16Ieee,
        alignmentFloat32Ieee = alignmentFloat32Ieee,
        alignmentFloat64Ieee = alignmentFloat64Ieee,
        alignmentInt64 = alignmentInt64,
        alignmentWord = alignmentWord,
        byteOrder = byteOrder,
        dataSize = dataSize,
        deposit = deposit
    )
}