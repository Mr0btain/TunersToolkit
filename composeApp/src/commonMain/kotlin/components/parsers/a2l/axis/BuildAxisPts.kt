package components.parsers.a2l.axis

import components.parsers.a2l.classes.data.DisplayIdentifier
import components.parsers.a2l.classes.data.EcuAddressExtension
import components.parsers.a2l.classes.data.Format
import components.parsers.a2l.classes.data.FunctionList
import components.parsers.a2l.classes.data.IfDataList
import components.parsers.a2l.classes.data.MemorySegmentReference
import components.parsers.a2l.classes.data.PhysUnit
import components.parsers.a2l.classes.data.StepSize
import components.parsers.a2l.classes.data.SymbolLink
import components.a2l.ifdata.components.parseIfDataSection
import components.parsers.a2l.classes.data.parseExtendedLimits
import components.parsers.a2l.classes.enums.GuardRails
import components.parsers.a2l.classes.enums.ReadOnly
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseCalibrationAccess
import components.parsers.a2l.classes.enums.parseConversionType
import components.parsers.a2l.classes.enums.parseDeposit
import components.parsers.a2l.classes.enums.parseMonotony
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of strings and returns an AxisPts item with parameters that it contains
 */

fun buildAxisPts(input: List<String>): AxisPts {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 10) {
        var mName = ""
        val mIfData = mutableListOf<components.parsers.a2l.ifdata.IfData>()
        val mOptionalParams = mutableMapOf<AxisPtsOpt, A2lOptional>()

        try {
            input.forEachIndexed { index, line ->
                when {
                    line.startsWith("DISPLAY_IDENTIFIER") -> {
                        mOptionalParams[AxisPtsOpt.DisplayIdentifier] = DisplayIdentifier(line.replace("DISPLAY_IDENTIFIER", "").trim())
                    }

                    line.startsWith("FORMAT") -> {
                        mOptionalParams[AxisPtsOpt.Format] = Format(line.replace("FORMAT", "")
                            .replace("\"", "").trim())
                    }

                    line.startsWith("DEPOSIT") -> {
                        mOptionalParams[AxisPtsOpt.Deposit] = line.parseDeposit()
                    }

                    line.startsWith("BYTE_ORDER") -> {
                        mOptionalParams[AxisPtsOpt.ByteOrder] = line.parseByteOrder()
                    }

                    line.startsWith("/begin") -> {
                        when {
                            line.contains("AXIS_PTS") -> {
                                val result = line.replace("/begin AXIS_PTS", "").trim()
                                if (result.isNotBlank()) {
                                    mName = result
                                }
                            }

                            line.contains("FUNCTION_LIST") -> {
                                // TODO: Test Function list
                                val mFunctionList = mutableListOf<String>()
                                val subL = input.subList(index, input.size)
                                var found = false
                                subL.forEachIndexed { ind, lin ->
                                    when {
                                        !found && lin.contains("/end FUNCTION_LIST") -> {
                                            val result = subL.subList(0, ind).joinToString(" ").trim()
                                                .replace("/begin FUNCTION_LIST", "")
                                                .replace("/end FUNCTION_LIST", "").split(" ")
                                            found = true
                                            result.forEach {
                                                mFunctionList.add(it)
                                            }
                                        }
                                    }
                                }

                                mOptionalParams[AxisPtsOpt.FunctionList] = FunctionList(mFunctionList)
                            }

                            line.contains("IF_DATA") -> {
                                // TODO: Test Function list
                                mIfData.add(input.subList(index, input.size).parseIfDataSection())
                            }
                        }
                    }

                    line.startsWith("REF_MEMORY_SEGMENT") -> {
                        mOptionalParams[AxisPtsOpt.RefMemorySegment] = MemorySegmentReference(
                            line.replace("REF_MEMORY_SEGMENT", "").trim()
                        )
                    }

                    line.startsWith("GUARD_RAILS") -> {
                        mOptionalParams[AxisPtsOpt.GuardRails] = GuardRails.True
                    }

                    line.startsWith("EXTENDED_LIMITS") -> {
                        mOptionalParams[AxisPtsOpt.ExtendedLimits] = line.parseExtendedLimits()
                    }

                    line.startsWith("CALIBRATION_ACCESS") -> {
                        mOptionalParams[AxisPtsOpt.CalibrationAccess] = line.parseCalibrationAccess()
                    }

                    line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                        mOptionalParams[AxisPtsOpt.EcuAddressExtension] = EcuAddressExtension(
                            line.replace("ECU_ADDRESS_EXTENSION", "").trim()
                        )
                    }

                    line.startsWith("MONOTONY") -> {
                        mOptionalParams[AxisPtsOpt.Monotony] = line.parseMonotony()
                    }

                    line.startsWith("PHYS_UNIT") -> {
                        mOptionalParams[AxisPtsOpt.PhysUnit] = PhysUnit(line.replace("PHYS_UNIT", "").trim())
                    }

                    line.startsWith("READ_ONLY") -> {
                        mOptionalParams[AxisPtsOpt.ReadOnly] = ReadOnly.True
                    }

                    line.startsWith("STEP_SIZE") -> {
                        mOptionalParams[AxisPtsOpt.StepSize] = StepSize(
                            line.replace("STEP_SIZE", "").trim().toFloatOrNull() ?: Float.NaN
                        )
                    }

                    line.startsWith("SYMBOL_LINK") -> {
                        val data = line.replace("SYMBOL_LINK", "").trim().split(" ")
                        if (data.size >= 2) {
                            val mOffset = data[1].toULongOrNull() ?: ULong.MIN_VALUE
                            mOptionalParams[AxisPtsOpt.SymbolLink] = SymbolLink(data.first(), mOffset)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mOptionalParams[AxisPtsOpt.IfData] = IfDataList(mIfData)

        return if (mName.isNotBlank()) {
            AxisPts(
                name = mName,
                longIdentifier = input[1].replace("\"", "").trim(),
                address = input[2].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                inputQuantity = input[3].trim(),
                recordLayout = input[4].trim(),
                maxDiff = input[5].trim().toDoubleOrNull() ?: Double.NaN,
                conversion = input[6].parseConversionType(),
                maxAxisPoints = input[7].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                lowerLimit = input[8].trim().toDoubleOrNull() ?: Double.NaN,
                upperLimit = input[9].trim().toDoubleOrNull() ?: Double.NaN,
                optionalParameters = mOptionalParams
            )
        } else {
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
                optionalParameters = mOptionalParams
            )
        }
    } else return AxisPts()
}