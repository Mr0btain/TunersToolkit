package org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.buildAxisDescription
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AnnotList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Annotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.BitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.CharNum
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ComparisonQuantity
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DependantCharacteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DisplayIdentifier
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.EcuAddressExtension
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Format
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FunctionList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.IfDataList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MapList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MaxRefresh
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MemorySegmentReference
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.PhysUnit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.StepSize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SymbolLink
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.VirtCharacteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseAnnotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseExtendedLimits
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseMatrixDim
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CalibrationAccess
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Deposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.Discrete
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.GuardRails
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ReadOnly
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseCalibrationAccess
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseCharacteristicType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.parseDeposit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers.parseItemsFromList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.parseIfDataSection
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of strings and builds a Characteristic from the available
 * options it contains
 */

fun buildCharacteristic(input: List<String>): Pair<Characteristic, List<LogDataEntry>> {
    val process = "BuildCharacteristic"
    val errors = mutableListOf<LogDataEntry>()

    safeCall {
        val annotations = mutableListOf<Annotation>()
        var axisIncrement = 0
        var name = ""
        val ifData = mutableListOf<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData>()
        val optionalParams = mutableMapOf<CharOpt, A2lOptional>()

        // Parse the optional params
        input.forEachIndexed { index, line ->
            when {
                line.startsWith("BIT_MASK") -> {
                    safeCall {
                        optionalParams[CharOpt.BitMask] = BitMask(
                            line.replace("BIT_MASK", "").trim()
                                .replace("0x", "").toULong()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set BitMask!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("BYTE_ORDER") -> {
                    optionalParams[CharOpt.ByteOrder] = line
                        .replace("BYTE_ORDER", "").trim().parseByteOrder()
                }

                line.startsWith("CALIBRATION_ACCESS") -> {
                    val calibration = line.parseCalibrationAccess()

                    if (calibration != CalibrationAccess.NOT_SET) {
                        optionalParams[CharOpt.CalibrationAccess] = calibration
                    } else {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set CalibrationAcees!",
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("COMPARISON_QUANTITY") -> {
                    optionalParams[CharOpt.ComparisonQuantity] = ComparisonQuantity(
                        line.replace("COMPARISON_QUANTITY", "").trim()
                    )
                }

                line.startsWith("DISCRETE") -> {
                    optionalParams[CharOpt.Discrete] = Discrete.True
                }

                line.startsWith("DISPLAY_IDENTIFIER") -> {
                    optionalParams[CharOpt.DisplayIdentifier] = DisplayIdentifier(
                        line.replace("DISPLAY_IDENTIFIER", "").trim()
                    )
                }

                line.startsWith("DEPOSIT") -> {
                    val deposit = line.replace("DEPOSIT", "").trim().parseDeposit()

                    if (deposit != Deposit.NOT_SET) {
                        optionalParams[CharOpt.Deposit] = deposit
                    } else {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set Deposit!",
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("FORMAT") -> {
                    if (!optionalParams.containsKey(CharOpt.Format)) {
                        optionalParams[CharOpt.Format] = Format(
                            line.replace("FORMAT", "").trim()
                        )
                    }
                }

                line.startsWith("EXTENDED_LIMITS") -> {
                    safeCall {
                        optionalParams[CharOpt.ExtendedLimits] = line.parseExtendedLimits()
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to parse ExtendedLimits!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                    optionalParams[CharOpt.EcuAddressExtension] = EcuAddressExtension(
                        line.replace("ECU_ADDRESS_EXTENSION", "").trim()
                            .replace("0x", "")
                    )
                }

                line.startsWith("NUMBER") -> {
                    safeCall {
                        optionalParams[CharOpt.Number] =  CharNum(
                            line.replace("NUMBER", "").trim().toInt()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set Number!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("MATRIX_DIM") -> {
                    safeCall {
                        optionalParams[CharOpt.MatrixDim] = parseMatrixDim(line)
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set MatrixDim!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("MAX_REFRESH") -> {
                    safeCall {
                        val result = line.replace("MAX_REFRESH", "")
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }

                        optionalParams[CharOpt.MaxRefresh] = MaxRefresh(
                            scalingUnit = result[0].toUInt(),
                            rate = result[1].toULong()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set MaxRefresh!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("READ_ONLY") -> {
                    optionalParams[CharOpt.ReadOnly] = ReadOnly.True
                }

                line.startsWith("GUARD_RAILS") -> {
                    optionalParams[CharOpt.GuardRails] = GuardRails.True
                }

                line.startsWith("PHYS_UNIT") -> {
                    optionalParams[CharOpt.PhysUnit] = PhysUnit(
                        line.replace("PHYS_UNIT", "").trim()
                    )
                }

                line.startsWith("STEP_SIZE") -> {
                    safeCall {
                        optionalParams[CharOpt.StepSize] = StepSize(
                            line.replace("STEP_SIZE", "")
                                .trim()
                                .toFloat()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set StepSize!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("SYMBOL_LINK") -> {
                    safeCall {
                        val result = line.replace("SYMBOL_LINK", "")
                            .trim()
                            .split(" ")

                        optionalParams[CharOpt.SymbolLink] = SymbolLink(
                            symbolName = result[0],
                            offset = result[1].toULong()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set SymbolLink!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("REF_MEMORY_SEGMENT") -> {
                    optionalParams[CharOpt.RefMemorySegment] = MemorySegmentReference(
                        line.replace("REF_MEMORY_SEGMENT", "").trim()
                    )
                }

                line.contains("/begin") -> {
                    when {
                        line.contains("AXIS_DESCR") -> {
                            val subList = input.subList(index, input.size).takeWhile { !it.contains("/end AXIS_DESCR") }

                            val (result, error) = buildAxisDescription(subList)
                            val axis = axisIncrement.getCharAxisDescType()
                            axisIncrement++

                            if (error.isEmpty()) {
                                optionalParams[axis] = result
                            } else {
                                errors.addAll(error)
                                optionalParams[axis] = result
                            }
                        }

                        line.contains("IF_DATA") -> {
                            safeCall {
                                ifData.add(input.subList(index, input.size).parseIfDataSection())
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to parse IfData!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("FUNCTION_LIST") -> {
                            safeCall {
                                optionalParams[CharOpt.FunctionList] = FunctionList(
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
                                        message = "Failure to parse FunctionList!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("VIRTUAL_CHARACTERISTIC") -> {
                            safeCall {
                                optionalParams[CharOpt.VirtualCharacteristic] = VirtCharacteristic(
                                    parseItemsFromList(
                                        input = input.subList(index, input.size),
                                        startingId = "/begin VIRTUAL_CHARACTERISTIC",
                                        endId = "/end VIRTUAL_CHARACTERISTIC"
                                    )
                                )
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to parse VirtualCharacteristic!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("DEPENDENT_CHARACTERISTIC") -> {
                            safeCall {
                                optionalParams[CharOpt.DependantCharacteristic] = DependantCharacteristic(
                                    parseItemsFromList(
                                        input = input.subList(index, input.size),
                                        startingId = "/begin DEPENDENT_CHARACTERISTIC",
                                        endId = "/end DEPENDENT_CHARACTERISTIC"
                                    )
                                )
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to parse DependentCharacteristic!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("MAP_LIST") -> {
                            safeCall {
                                optionalParams[CharOpt.MapList] = MapList(
                                    parseItemsFromList(
                                        input = input.subList(index, input.size),
                                        startingId = "/begin MAP_LIST",
                                        endId = "/end MAP_LIST"
                                    )
                                )
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to parse MapList!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("ANNOTATION") -> {
                            safeCall {
                                annotations.add(parseAnnotation(input.subList(index, input.size)))
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to parse Annotations!; ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }

                        line.contains("CHARACTERISTIC") -> {
                            val result = line.replace("/begin CHARACTERISTIC", "").trim()
                            if (result.isNotBlank()) {
                                name = result
                            }
                        }
                    }
                }
            }
        }

        when {
            // Add Annotations
            annotations.isNotEmpty() -> {
                optionalParams[CharOpt.Annotations] = AnnotList(annotations)
            }

            // Add IfData
            ifData.isNotEmpty() -> {
                optionalParams[CharOpt.IfData] = IfDataList(ifData)
            }
        }

        return if (name.isNotBlank()) {
            Pair(
                Characteristic(
                    name = name,
                    identifier = input[1].replace("\"", "").trim(),
                    type = input[2].parseCharacteristicType(),
                    address = input[3].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                    refRecordLayout = input[4].trim(),
                    maxDiff = (input[5].trim().toDoubleOrNull() ?: Double.NaN),
                    conversion = input[6].trim(),
                    lowerLimit = (input[7].toDoubleOrNull() ?: Double.NaN),
                    upperLimit = (input[8].toDoubleOrNull() ?: Double.NaN),
                    optionalParameters = optionalParams
                ),
                errors
            )
        } else {
            Pair(
                Characteristic(
                    name = input[1].trim(),
                    identifier = input[2].replace("\"", "").trim(),
                    type = input[3].parseCharacteristicType(),
                    address = input[4].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                    refRecordLayout = input[5].trim(),
                    maxDiff = (input[6].trim().toDoubleOrNull() ?: Double.NaN),
                    conversion = input[7].trim(),
                    lowerLimit = (input[8].toDoubleOrNull() ?: Double.NaN),
                    upperLimit = (input[9].toDoubleOrNull() ?: Double.NaN),
                    optionalParameters = optionalParams
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to Build Characteristic!; ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(Characteristic(), errors)
}