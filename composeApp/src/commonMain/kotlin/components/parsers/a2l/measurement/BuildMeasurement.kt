package components.parsers.a2l.measurement

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.AnnotList
import components.parsers.a2l.classes.data.Annotation
import components.parsers.a2l.classes.data.ArraySize
import components.parsers.a2l.classes.data.BitMask
import components.parsers.a2l.classes.data.DisplayIdentifier
import components.parsers.a2l.classes.data.EcuAddress
import components.parsers.a2l.classes.data.ErrorMask
import components.parsers.a2l.classes.data.Format
import components.parsers.a2l.classes.data.FunctionList
import components.parsers.a2l.classes.data.IfDataList
import components.parsers.a2l.classes.data.MatrixDim
import components.parsers.a2l.classes.data.MaxRefresh
import components.parsers.a2l.classes.data.MemorySegmentReference
import components.parsers.a2l.classes.data.PhysUnit
import components.parsers.a2l.classes.data.SymbolLink
import components.parsers.a2l.classes.data.VirtualList
import components.parsers.a2l.classes.data.parseAnnotation
import components.parsers.a2l.classes.enums.IndexMode
import components.parsers.a2l.classes.data.parseBitOperation
import components.parsers.a2l.classes.data.parseMatrixDim
import components.parsers.a2l.classes.enums.Discrete
import components.parsers.a2l.classes.enums.ReadOnly
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseDataTypeEnum
import components.parsers.a2l.ifdata.IfData
import components.parsers.a2l.ifdata.components.parseIfDataSection
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of strings and returns a Measurement with parameters if they exist
 */

fun buildMeasurement(input: List<String>): Pair<Measurement, List<LogDataEntry>> {
    val process = "BuildMeasurement"
    val errors = mutableListOf<LogDataEntry>()
    var name = ""
    val annotations = mutableListOf<Annotation>()
    val optionalParams = mutableMapOf<MeasOpt, A2lOptional>()
    val ifData = mutableListOf<IfData>()

    input.subList(7, input.size).forEachIndexed { index, line ->
        when {
            line.startsWith("BIT_MASK") -> {
                safeCall {
                    optionalParams[MeasOpt.BitMask] = BitMask(
                        line.replace("BIT_MASK", "").trim()
                            .replace("0x", "").toULong(16)
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add BitMask: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("BYTE_ORDER") -> {
                optionalParams[MeasOpt.ByteOrder] = line
                    .replace("BYTE_ORDER", "").trim().parseByteOrder()
            }

            line.startsWith("FORMAT") -> {
                optionalParams[MeasOpt.Format] = Format(
                    line.replace("FORMAT", "")
                        .replace("\"", "").trim()
                )
            }

            line.startsWith("ECU_ADDRESS") -> {
                safeCall {
                    optionalParams[MeasOpt.EcuAddress] = EcuAddress(
                        line.replace("ECU_ADDRESS", "").trim()
                            .replace("0x", "").toULong(16)
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add BEcuAddress: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("SYMBOL_LINK") -> {
                safeCall {
                    val data = line.replace("SYMBOL_LINK", "").trim().split("\\s+".toRegex())

                    optionalParams[MeasOpt.SymbolLink] = SymbolLink(
                        symbolName = data[0],
                        offset = data[1].trim().toULong()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add SymbolLink: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("LAYOUT") -> {
                when (line.replace("LAYOUT", "").trim()) {
                    "ROW_DIR" -> {
                        optionalParams[MeasOpt.Layout] = IndexMode.ROW_DIR
                    }

                    "COLUMN_DIR" -> {
                        optionalParams[MeasOpt.Layout] = IndexMode.COLUMN_DIR
                    }

                    "ALTERNATE_WITH_X" -> {
                        optionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_WITH_X
                    }

                    "ALTERNATE_WITH_Y" -> {
                        optionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_WITH_Y
                    }

                    "ALTERNATE_CURVES" -> {
                        optionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_CURVES
                    }
                }
            }

            line.startsWith("DISPLAY_IDENTIFIER") -> {
                optionalParams[MeasOpt.DisplayIdentifier] = DisplayIdentifier(
                    line.replace("DISPLAY_IDENTIFIER", "").trim()
                )
            }

            line.startsWith("READ_WRITE") -> {
                optionalParams[MeasOpt.ReadWrite] = ReadOnly.True
            }

            line.startsWith("ARRAY_SIZE") -> {
                safeCall {
                    optionalParams[MeasOpt.ArraySize] = ArraySize(
                        line.replace("ARRAY_SIZE", "").trim().toUInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add ArraySize: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin BIT_OPERATION") -> {
                safeCall {
                    optionalParams[MeasOpt.BitOperations] = input.subList(index, input.size).parseBitOperation()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add BitOperation: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("MAX_REFRESH") -> {
                safeCall {
                    val data = line.replace("MAX_REFRESH", "").trim().split(" ")

                    optionalParams[MeasOpt.MaxRefresh] = MaxRefresh(
                        scalingUnit = data[0].toUInt(),
                        rate = data[1].toULong()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add MaxRefresh: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin VIRTUAL") -> {
                // TODO: Figure out why were adding 7 to index????
                safeCall {
                    val virtual = mutableListOf<String>()

                    input.subList(index, input.size).forEach {  item ->
                        when {
                            item.contains("/end VIRTUAL") -> {
                                virtual.addAll(
                                    item.replace("/end VIRTUAL", "")
                                        .trim().split(" ")
                                )
                            }
                            else -> {
                                virtual.addAll(item.trim().split(" "))
                            }
                        }
                    }

                    optionalParams[MeasOpt.Virtual] = VirtualList(virtual)
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add Virtual: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ERROR_MASK") -> {
                safeCall {
                    optionalParams[MeasOpt.ErrorMask] = ErrorMask(
                        line.replace("ERROR_MASK", "").trim()
                            .toULong()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add ErrorMask: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin FUNCTION_LIST") -> {
                val functionList = mutableListOf<String>()

                input.subList(index, input.size).forEach { item ->
                    when {
                        item.startsWith("/end FUNCTION_LIST") -> {
                            functionList.addAll(
                                item.replace("/end FUNCTION_LIST", "")
                                    .trim().split(" ")
                            )
                        }
                        else -> {
                            functionList.addAll(item.trim().split(" "))
                        }
                    }
                }

                optionalParams[MeasOpt.FunctionList] = FunctionList(functionList)
            }

            line.startsWith("REF_MEMORY_SEGMENT") -> {
                optionalParams[MeasOpt.RefMemorySegment] = MemorySegmentReference(
                    line.replace("REF_MEMORY_SEGMENT", "").trim()
                )
            }

            line.startsWith("/begin ANNOTATION") -> {
                safeCall {
                    val subList = input.subList(index, input.size)
                    annotations.add(parseAnnotation(subList))
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse Annotation!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("MATRIX_DIM") -> {
                safeCall {
                    parseMatrixDim(line)
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add MatrixDim: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                safeCall {
                    optionalParams[MeasOpt.EcuAddress] = EcuAddress(
                        line.replace("ECU_ADDRESS_EXTENSION", "").trim().toULong()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add EcuAddress: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("DISCRETE") -> {
                optionalParams[MeasOpt.Discrete] = Discrete.True
            }

            line.startsWith("PHYS_UNIT") -> {
                optionalParams[MeasOpt.PhysUnit] = PhysUnit(
                    line.replace("PHYS_UNIT", "").trim()
                )
            }

            line.startsWith("/begin IF_DATA") -> {
                safeCall {
                    ifData.add(input.subList(index, input.size).parseIfDataSection())
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add IfData: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin MEASUREMENT") -> {
                val result = line.replace("/begin MEASUREMENT", "").trim()
                if (result.isNotBlank()) {
                    name = result
                }
            }
        }
    }

    when {
        // Add Annotations
        annotations.isNotEmpty() -> {
            optionalParams[MeasOpt.Annotation] = AnnotList(annotations)
        }

        // Add IfData
        ifData.isNotEmpty() -> {
            optionalParams[MeasOpt.IfData] = IfDataList(ifData)
        }
    }

    safeCall {
        return if (name.isNotBlank()) {
            Pair(
                Measurement(
                    name = name,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    dataType = input[2].parseDataTypeEnum(),
                    conversion = input[3].trim(),
                    resolution = input[4].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    accuracy = input[5].trim().toDoubleOrNull() ?: Double.NaN,
                    lowerLimit = input[6].trim().toDoubleOrNull() ?: Double.NaN,
                    upperLimit = input[7].trim().toDoubleOrNull() ?: Double.NaN,
                    optionalParameters = optionalParams
                ),
                errors
            )
        } else {
            Pair(
                Measurement(
                    name = input[1].trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    dataType = input[3].parseDataTypeEnum(),
                    conversion = input[4].trim(),
                    resolution = input[5].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    accuracy = input[6].trim().toDoubleOrNull() ?: Double.NaN,
                    lowerLimit = input[7].trim().toDoubleOrNull() ?: Double.NaN,
                    upperLimit = input[8].trim().toDoubleOrNull() ?: Double.NaN,
                    optionalParameters = optionalParams
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to Build Measurement!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(Measurement(), errors)
}

