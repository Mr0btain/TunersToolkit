package components.parsers.a2l.measurement

import components.parsers.a2l.classes.data.AnnotList
import components.a2l.ifdata.components.parseIfDataSection
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
import components.parsers.a2l.classes.enums.Discrete
import components.parsers.a2l.classes.enums.ReadOnly
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseDataTypeEnum
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of strings and returns a Measurement with parameters if they exist
 */

fun buildMeasurement(input: List<String>): Measurement {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 8) {
        var mName = ""
        val mAnnotation = mutableListOf<Annotation>()
        val mOptionalParams = mutableMapOf<MeasOpt, A2lOptional>()
        val mIfData = mutableListOf<components.parsers.a2l.ifdata.IfData>()

        try {
            input.subList(7, input.size).forEachIndexed { index, line ->
                when {
                    line.startsWith("BIT_MASK") -> {
                        mOptionalParams[MeasOpt.BitMask] = BitMask(
                            line.replace("BIT_MASK", "").trim()
                                .replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE
                        )
                    }

                    line.startsWith("BYTE_ORDER") -> {
                        mOptionalParams[MeasOpt.ByteOrder] = line
                            .replace("BYTE_ORDER", "").trim().parseByteOrder()
                    }

                    line.startsWith("FORMAT") -> {
                        mOptionalParams[MeasOpt.Format] = Format(
                            line.replace("FORMAT", "")
                                .replace("\"", "").trim()
                        )
                    }

                    line.startsWith("ECU_ADDRESS") -> {
                        mOptionalParams[MeasOpt.EcuAddress] = EcuAddress(
                            line.replace("ECU_ADDRESS", "").trim()
                                .replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE
                        )
                    }

                    line.startsWith("SYMBOL_LINK") -> {
                        val data = line.replace("SYMBOL_LINK", "").trim().split("\\s+".toRegex())
                        if (data.size >= 2) {
                            val mOffset = data[1].trim().toULongOrNull()
                            if (mOffset != null) {
                                mOptionalParams[MeasOpt.SymbolLink] = SymbolLink(
                                    symbolName = data[0],
                                    offset = mOffset
                                )
                            }
                        }
                    }

                    line.startsWith("LAYOUT") -> {
                        when (line.replace("LAYOUT", "").trim()) {
                            "ROW_DIR" -> {
                                mOptionalParams[MeasOpt.Layout] = IndexMode.ROW_DIR
                            }

                            "COLUMN_DIR" -> {
                                mOptionalParams[MeasOpt.Layout] = IndexMode.COLUMN_DIR
                            }

                            "ALTERNATE_WITH_X" -> {
                                mOptionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_WITH_X
                            }

                            "ALTERNATE_WITH_Y" -> {
                                mOptionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_WITH_Y
                            }

                            "ALTERNATE_CURVES" -> {
                                mOptionalParams[MeasOpt.Layout] = IndexMode.ALTERNATE_CURVES
                            }
                        }
                    }

                    line.startsWith("DISPLAY_IDENTIFIER") -> {
                        mOptionalParams[MeasOpt.DisplayIdentifier] = DisplayIdentifier(
                            line.replace("DISPLAY_IDENTIFIER", "").trim()
                        )
                    }

                    line.startsWith("READ_WRITE") -> {
                        mOptionalParams[MeasOpt.ReadWrite] = ReadOnly.True
                    }

                    line.startsWith("ARRAY_SIZE") -> {
                        mOptionalParams[MeasOpt.ArraySize] = ArraySize(
                            line.replace("ARRAY_SIZE", "").trim().toUIntOrNull() ?: UInt.MIN_VALUE
                        )
                    }

                    line.startsWith("/begin BIT_OPERATION") -> {
                        val mData = input.subList(index, input.size)
                        mOptionalParams[MeasOpt.BitOperations] = mData.parseBitOperation()
                    }

                    line.startsWith("MAX_REFRESH") -> {
                        val data = line.replace("MAX_REFRESH", "").trim().split(" ")
                        if (data.size >= 2) {
                            val sU = data[0].toUIntOrNull()
                            val rate = data[1].toULongOrNull()
                            if (sU != null && rate != null) {
                                mOptionalParams[MeasOpt.MaxRefresh] = MaxRefresh(
                                    scalingUnit = sU,
                                    rate = rate
                                )
                            }
                        }
                    }

                    line.startsWith("/begin VIRTUAL") -> {
                        val mVirtual = mutableListOf<String>()

                        for (l in input.subList(index + 7, input.size)) {
                            when {
                                l.contains("/end VIRTUAL") -> {
                                    l.replace("/end VIRTUAL", "")
                                        .trim().split(" ").forEach {
                                            mVirtual.add(it)
                                        }
                                    break
                                }
                                else -> {
                                    l.trim().split(" ").forEach {
                                        mVirtual.add(it)
                                    }
                                }
                            }
                        }

                        mOptionalParams[MeasOpt.Virtual] = VirtualList(mVirtual)
                    }

                    line.startsWith("ERROR_MASK") -> {
                        mOptionalParams[MeasOpt.ErrorMask] = ErrorMask(
                            line.replace("ERROR_MASK", "").trim()
                                .toULongOrNull() ?: ULong.MIN_VALUE
                        )
                    }

                    line.startsWith("/begin FUNCTION_LIST") -> {
                        val mFunctionList = mutableListOf<String>()

                        for (l in input.subList(index + 7, input.size)) {
                            when {
                                l.startsWith("/end FUNCTION_LIST") -> {
                                    l.replace("/end FUNCTION_LIST", "")
                                        .trim().split(" ").forEach {
                                            mFunctionList.add(it)
                                        }
                                    break
                                }
                                else -> {
                                    l.trim().split(" ").forEach {
                                        mFunctionList.add(it)
                                    }
                                }
                            }
                        }

                        mOptionalParams[MeasOpt.FunctionList] = FunctionList(mFunctionList)
                    }

                    line.startsWith("REF_MEMORY_SEGMENT") -> {
                        mOptionalParams[MeasOpt.RefMemorySegment] = MemorySegmentReference(
                            line.replace("REF_MEMORY_SEGMENT", "").trim()
                        )
                    }

                    line.startsWith("/begin ANNOTATION") -> {
                        val subL = input.subList(index, input.size)
                        var annotList = listOf("")
                        var found = false
                        subL.forEachIndexed { ind, lin ->
                            when {
                                !found && lin.contains("/end ANNOTATION") -> {
                                    annotList = subL.subList(0, ind)
                                    found = true
                                }
                            }
                        }

                        mAnnotation.add(annotList.parseAnnotation())
                    }

                    line.startsWith("MATRIX_DIM") -> {
                        val result = line.replace("MATRIX_DIM", "")
                            .trim().split(" ")
                        if (result.size >= 3) {
                            val x = result[0].toIntOrNull()
                            val y = result[1].toIntOrNull()
                            val z = result[2].toIntOrNull()
                            if (x != null && y != null && z != null) {
                                mOptionalParams[MeasOpt.MatrixDim] = MatrixDim(
                                    xDim = x,
                                    yDim = y,
                                    zDim = z
                                )
                            }
                        } else if (result.size >= 2) {
                            val x = result[0].toIntOrNull()
                            val y = result[1].toIntOrNull()
                            if (x != null && y != null) {
                                mOptionalParams[MeasOpt.MatrixDim] = MatrixDim(
                                    xDim = x,
                                    yDim = y
                                )
                            }
                        } else {
                            val x = result[0].toIntOrNull()
                            if (x != null) {
                                mOptionalParams[MeasOpt.MatrixDim] = MatrixDim(x)
                            }
                        }
                    }

                    line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                        mOptionalParams[MeasOpt.EcuAddress] = EcuAddress(
                            line.replace("ECU_ADDRESS_EXTENSION", "").trim().toULongOrNull() ?: ULong.MIN_VALUE
                        )
                    }

                    line.startsWith("DISCRETE") -> {
                        mOptionalParams[MeasOpt.Discrete] = Discrete.True
                    }

                    line.startsWith("PHYS_UNIT") -> {
                        mOptionalParams[MeasOpt.PhysUnit] = PhysUnit(
                            line.replace("PHYS_UNIT", "").trim()
                        )
                    }

                    line.startsWith("/begin IF_DATA") -> {
                        mIfData.add(input.subList(index, input.size).parseIfDataSection())
                    }

                    line.startsWith("/begin MEASUREMENT") -> {
                        val result = line.replace("/begin MEASUREMENT", "").trim()
                        if (result.isNotBlank()) {
                            mName = result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        when {
            // Add Annotations
            mAnnotation.isNotEmpty() -> {
                mOptionalParams[MeasOpt.Annotation] = AnnotList(mAnnotation)
            }

            // Add IfData
            mIfData.isNotEmpty() -> {
                mOptionalParams[MeasOpt.IfData] = IfDataList(mIfData)
            }
        }

        return if (mName.isNotBlank()) {
            Measurement(
                name = mName,
                longIdentifier = input[1].replace("\"", "").trim(),
                dataType = input[2].parseDataTypeEnum(),
                conversion = input[3].trim(),
                resolution = input[4].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                accuracy = input[5].trim().toDoubleOrNull() ?: Double.NaN,
                lowerLimit = input[6].trim().toDoubleOrNull() ?: Double.NaN,
                upperLimit = input[7].trim().toDoubleOrNull() ?: Double.NaN,
                optionalParameters = mOptionalParams
            )
        } else {
            Measurement(
                name = input[1].trim(),
                longIdentifier = input[2].replace("\"", "").trim(),
                dataType = input[3].parseDataTypeEnum(),
                conversion = input[4].trim(),
                resolution = input[5].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                accuracy = input[6].trim().toDoubleOrNull() ?: Double.NaN,
                lowerLimit = input[7].trim().toDoubleOrNull() ?: Double.NaN,
                upperLimit = input[8].trim().toDoubleOrNull() ?: Double.NaN,
                optionalParameters = mOptionalParams
            )
        }
    } else return Measurement()
}

