package org.mr0btain.tunerstoolkit.components.parsers.a2l.axis
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DisplayIdentifier
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.EcuAddressExtension
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Format
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FunctionList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.IfDataList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MemorySegmentReference
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.PhysUnit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.StepSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SymbolLink
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseExtendedLimits
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CalibrationAccess
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Deposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.GuardRails
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Monotony
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ReadOnly
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseCalibrationAccess
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseConversionType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseDeposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseMonotony
import org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers.parseItemsFromList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.parseIfDataSection
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of strings and returns an AxisPts item with parameters that it contains
 */

fun buildAxisPts(input: List<String>): Pair<AxisPts, List<LogDataEntry>> {
    val process = "BuildAxisPts"
    val errors = mutableListOf<LogDataEntry>()

    safeCall {
        var name = ""
        val ifData = mutableListOf<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData>()
        val optionalParams = mutableMapOf<AxisPtsOpt, A2lOptional>()

        input.forEachIndexed { index, line ->
            when {
                line.startsWith("DISPLAY_IDENTIFIER") -> {
                    optionalParams[AxisPtsOpt.DisplayIdentifier] = DisplayIdentifier(
                        line.replace("DISPLAY_IDENTIFIER", "").trim()
                    )
                }

                line.startsWith("FORMAT") -> {
                    optionalParams[AxisPtsOpt.Format] = Format(
                        line.replace("FORMAT", "")
                            .replace("\"", "").trim()
                    )
                }

                line.startsWith("DEPOSIT") -> {
                    val deposit = line.parseDeposit()
                    if (deposit != Deposit.NOT_SET) {
                        optionalParams[AxisPtsOpt.Deposit] = deposit
                    } else {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error parsing Deposit!",
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("BYTE_ORDER") -> {
                    optionalParams[AxisPtsOpt.ByteOrder] = line.parseByteOrder()
                }

                line.startsWith("/begin") -> {
                    when {
                        line.contains("AXIS_PTS") -> {
                            val result = line.replace("/begin AXIS_PTS", "").trim()

                            if (result.isNotBlank()) { name = result }
                        }

                        line.contains("FUNCTION_LIST") -> {
                            safeCall {
                                optionalParams[AxisPtsOpt.FunctionList] = FunctionList(
                                    parseItemsFromList(
                                        input = input.subList(index, input.size),
                                        startingId = "/begin FUNCTION_LIST",
                                        endId = "/end FUNCTION_LIST"
                                    )
                                )
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failed to parse FunctionsList: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("IF_DATA") -> {
                            // TODO: Test Function list
                            safeCall {
                                ifData.add(input.subList(index, input.size).parseIfDataSection())
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failed to parse IfData: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                line.startsWith("REF_MEMORY_SEGMENT") -> {
                    optionalParams[AxisPtsOpt.RefMemorySegment] = MemorySegmentReference(
                        line.replace("REF_MEMORY_SEGMENT", "").trim()
                    )
                }

                line.startsWith("GUARD_RAILS") -> {
                    optionalParams[AxisPtsOpt.GuardRails] = GuardRails.True
                }

                line.startsWith("EXTENDED_LIMITS") -> {
                    safeCall {
                        optionalParams[AxisPtsOpt.ExtendedLimits] = line.parseExtendedLimits()
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failed to parse ExtendedLimits!: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("CALIBRATION_ACCESS") -> {
                    val calibration = line.parseCalibrationAccess()

                    if (calibration != CalibrationAccess.NOT_SET) {
                        optionalParams[AxisPtsOpt.CalibrationAccess] = calibration
                    } else {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failed to set Calibration!",
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                    optionalParams[AxisPtsOpt.EcuAddressExtension] = EcuAddressExtension(
                        line.replace("ECU_ADDRESS_EXTENSION", "").trim()
                    )
                }

                line.startsWith("MONOTONY") -> {
                    val mon = line.parseMonotony()

                    if (mon != Monotony.NOT_SET) {
                        optionalParams[AxisPtsOpt.Monotony] = mon
                    } else {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failed to parse Monotony!",
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("PHYS_UNIT") -> {
                    optionalParams[AxisPtsOpt.PhysUnit] = PhysUnit(
                        line.replace("PHYS_UNIT", "").trim()
                    )
                }

                line.startsWith("READ_ONLY") -> {
                    optionalParams[AxisPtsOpt.ReadOnly] = ReadOnly.True
                }

                line.startsWith("STEP_SIZE") -> {
                    safeCall {
                        optionalParams[AxisPtsOpt.StepSize] = StepSize(
                            line.replace("STEP_SIZE", "").trim()
                                .toFloat()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failed to parse StepSize: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("SYMBOL_LINK") -> {
                    val data = line.replace("SYMBOL_LINK", "").trim().split(" ")

                    safeCall {
                        optionalParams[AxisPtsOpt.SymbolLink] = SymbolLink(data.first(), data[1].toULong())
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failed to parse SymbolLink!: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }
            }
        }

        optionalParams[AxisPtsOpt.IfData] = IfDataList(ifData)

        return if (name.isNotBlank()) {
            Pair(
                AxisPts(
                    name = name,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    address = input[2].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                    inputQuantity = input[3].trim(),
                    recordLayout = input[4].trim(),
                    maxDiff = input[5].trim().toDoubleOrNull() ?: Double.NaN,
                    conversion = input[6].parseConversionType(),
                    maxAxisPoints = input[7].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    lowerLimit = input[8].trim().toDoubleOrNull() ?: Double.NaN,
                    upperLimit = input[9].trim().toDoubleOrNull() ?: Double.NaN,
                    optionalParameters = optionalParams
                ),
                errors
            )
        } else {
            Pair(
                AxisPts(
                    name = input[1].replace("/begin AXIS_PTS ", "").trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    address = input[3].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                    inputQuantity = input[4].trim(),
                    recordLayout = input[5].trim(),
                    maxDiff = input[6].trim().toDoubleOrNull() ?: Double.NaN,
                    conversion = input[7].parseConversionType(),
                    maxAxisPoints = input[8].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    lowerLimit = input[9].trim().toDoubleOrNull() ?: Double.NaN,
                    upperLimit = input[10].trim().toDoubleOrNull() ?: Double.NaN,
                    optionalParameters = optionalParams
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failed to Build AxisPts! ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(AxisPts(), errors)
}