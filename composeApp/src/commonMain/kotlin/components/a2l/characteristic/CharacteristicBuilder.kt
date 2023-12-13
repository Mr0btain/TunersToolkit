package components.a2l.characteristic

import components.a2l.axis.AxisDescrBuilder
import components.a2l.classes.data.AnnotList
import components.a2l.ifdata.IfData
import components.a2l.ifdata.components.parseIfDataSection
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.BitMask
import components.a2l.classes.data.CharNum
import components.a2l.classes.data.ComparisonQuantity
import components.a2l.classes.data.DependantCharacteristic
import components.a2l.classes.data.DisplayIdentifier
import components.a2l.classes.data.EcuAddressExtension
import components.a2l.classes.data.Format
import components.a2l.classes.data.FunctionList
import components.a2l.classes.data.IfDataList
import components.a2l.classes.data.MapList
import components.a2l.classes.data.MatrixDim
import components.a2l.classes.data.MaxRefresh
import components.a2l.classes.data.MemorySegmentReference
import components.a2l.classes.data.PhysUnit
import components.a2l.classes.data.StepSize
import components.a2l.classes.data.SymbolLink
import components.a2l.classes.data.VirtCharacteristic
import components.a2l.classes.data.parseAnnotation
import components.a2l.classes.data.parseExtendedLimits
import components.a2l.classes.enums.Discrete
import components.a2l.classes.enums.GuardRails
import components.a2l.classes.enums.ReadOnly
import components.a2l.classes.enums.parseByteOrder
import components.a2l.classes.enums.parseCalibrationAccess
import components.a2l.classes.enums.parseCharacteristicType
import components.a2l.classes.enums.parseDeposit
import components.a2l.objects.A2lOptional

/**
 * Parses a list of strings and builds a Characteristic from the available
 * options it contains
 */

class CharacteristicBuilder {
    fun build(input: List<String>): Characteristic {
        return buildObject(input)
    }

    private fun buildObject(input: List<String>): Characteristic {
        // Check if size it big enough else we get index out of bounds
        if (input.size >= 9) {
            val mAnnotation = mutableListOf<Annotation>()
            var mAxisIncrement = 0
            var mName = ""
            val mIfData = mutableListOf<IfData>()
            val mOptionalParams = mutableMapOf<CharOpt, A2lOptional>()

            // Parse the optional params
            try {
                input.forEachIndexed { index, line ->
                    when {
                        line.startsWith("BIT_MASK") -> {
                            mOptionalParams[CharOpt.BitMask] = BitMask(
                                line.replace("BIT_MASK", "").trim()
                                    .replace("0x", "").toULongOrNull(16) ?: ULong.MIN_VALUE
                            )
                        }

                        line.startsWith("BYTE_ORDER") -> {
                            mOptionalParams[CharOpt.ByteOrder] = line
                                .replace("BYTE_ORDER", "").trim().parseByteOrder()
                        }

                        line.startsWith("CALIBRATION_ACCESS") -> {
                            mOptionalParams[CharOpt.CalibrationAccess] = line.parseCalibrationAccess()
                        }

                        line.startsWith("COMPARISON_QUANTITY") -> {
                            mOptionalParams[CharOpt.ComparisonQuantity] = ComparisonQuantity(
                                line.replace("COMPARISON_QUANTITY", "").trim()
                            )
                        }

                        line.startsWith("DISCRETE") -> {
                            mOptionalParams[CharOpt.Discrete] = Discrete.True
                        }

                        line.startsWith("DISPLAY_IDENTIFIER") -> {
                            mOptionalParams[CharOpt.DisplayIdentifier] = DisplayIdentifier(
                                line.replace("DISPLAY_IDENTIFIER", "").trim()
                            )
                        }

                        line.startsWith("DEPOSIT") -> {
                            mOptionalParams[CharOpt.Deposit] = line
                                .replace("DEPOSIT", "").trim().parseDeposit()
                        }

                        line.startsWith("FORMAT") -> {
                            if (!mOptionalParams.containsKey(CharOpt.Format)) {
                                mOptionalParams[CharOpt.Format] = Format(line.replace("FORMAT", "").trim())
                            }
                        }

                        line.startsWith("EXTENDED_LIMITS") -> {
                            mOptionalParams[CharOpt.ExtendedLimits] = line.parseExtendedLimits()
                        }

                        line.startsWith("ECU_ADDRESS_EXTENSION") -> {
                            mOptionalParams[CharOpt.EcuAddressExtension] = EcuAddressExtension(
                                line.replace("ECU_ADDRESS_EXTENSION", "").trim()
                                    .replace("0x", "")
                            )
                        }

                        line.startsWith("NUMBER") -> {
                            mOptionalParams[CharOpt.Number] =  CharNum(
                                line.replace("NUMBER", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                            )
                        }

                        line.startsWith("MATRIX_DIM") -> {
                            // TODO: Test Matrix Dim
                            val result = line.replace("MATRIX_DIM", "").trim().split(" ")
                            if (result.size >= 3) {
                                mOptionalParams[CharOpt.MatrixDim] = MatrixDim(
                                    xDim = result[0].toIntOrNull() ?: Int.MIN_VALUE,
                                    yDim = result[1].toIntOrNull() ?: Int.MIN_VALUE,
                                    zDim = result[2].toIntOrNull() ?: Int.MIN_VALUE
                                )
                            } else if (result.size == 2) {
                                mOptionalParams[CharOpt.MatrixDim] = MatrixDim(
                                    xDim = result[0].toIntOrNull() ?: Int.MIN_VALUE,
                                    yDim = result[1].toIntOrNull() ?: Int.MIN_VALUE,
                                    zDim = Int.MIN_VALUE
                                )
                            } else {
                                mOptionalParams[CharOpt.MatrixDim] = MatrixDim(
                                    xDim = result[0].toIntOrNull() ?: Int.MIN_VALUE,
                                    yDim = Int.MIN_VALUE,
                                    zDim = Int.MIN_VALUE
                                )
                            }
                        }

                        line.startsWith("MAX_REFRESH") -> {
                            // TODO: Test max refresh
                            val result = line.replace("MAX_REFRESH", "").trim().split(" ")
                            val scalingUnit = result[0].toUIntOrNull()
                            val rate = result[1].toULongOrNull()
                            if (scalingUnit != null && rate != null) {
                                mOptionalParams[CharOpt.MaxRefresh] = MaxRefresh(
                                    scalingUnit = scalingUnit,
                                    rate = rate
                                )
                            }
                        }

                        line.startsWith("READ_ONLY") -> {
                            mOptionalParams[CharOpt.ReadOnly] = ReadOnly.True
                        }

                        line.startsWith("GUARD_RAILS") -> {
                            mOptionalParams[CharOpt.GuardRails] = GuardRails.True
                        }

                        line.startsWith("PHYS_UNIT") -> {
                            mOptionalParams[CharOpt.PhysUnit] = PhysUnit(
                                line.replace("PHYS_UNIT", "").trim()
                            )
                        }

                        line.startsWith("STEP_SIZE") -> {
                            mOptionalParams[CharOpt.StepSize] = StepSize(
                                line.replace("STEP_SIZE", "").trim().toFloatOrNull() ?: Float.NaN
                            )
                        }

                        line.startsWith("SYMBOL_LINK") -> {
                            // TODO: Test Symbol link
                            val result = line.replace("SYMBOL_LINK", "").trim().split(" ")
                            if (result.size >= 2) {
                                mOptionalParams[CharOpt.SymbolLink] = SymbolLink(
                                    symbolName = result[0],
                                    offset = result[1].toULong()
                                )
                            }
                        }

                        line.startsWith("REF_MEMORY_SEGMENT") -> {
                            mOptionalParams[CharOpt.RefMemorySegment] = MemorySegmentReference(
                                line.replace("REF_MEMORY_SEGMENT", "").trim()
                            )
                        }

                        line.contains("/begin") -> {
                            when {
                                line.contains("AXIS_DESCR") -> {
                                    val subL = input.subList(index, input.size)
                                    var found = false

                                    subL.forEachIndexed { ind, lin ->
                                        when {
                                            !found && lin.contains("/end AXIS_DESCR") -> {
                                                val result = AxisDescrBuilder().build(subL.subList(0, ind + 1))
                                                val axis = mAxisIncrement.getCharAxisDescType()
                                                found = true
                                                mAxisIncrement++
                                                mOptionalParams[axis] = result
                                            }
                                        }
                                    }
                                }

                                line.contains("IF_DATA") -> {
                                    mIfData.add(input.subList(index, input.size).parseIfDataSection())
                                }

                                line.contains("FUNCTION_LIST") -> {
                                    val subL = input.subList(index, input.size)
                                    var found = false
                                    val mFunctionList = mutableListOf<String>()
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

                                    mOptionalParams[CharOpt.FunctionList] = FunctionList(mFunctionList)
                                }

                                line.contains("VIRTUAL_CHARACTERISTIC") -> {
                                    val subL = input.subList(index, input.size)
                                    var found = false
                                    val mVirtualCharacteristic = mutableListOf<String>()

                                    subL.forEachIndexed { ind, lin ->
                                        when {
                                            !found && lin.contains("/end VIRTUAL_CHARACTERISTIC") -> {
                                                val result = subL.subList(0, ind).joinToString(" ").trim()
                                                    .replace("/begin VIRTUAL_CHARACTERISTIC", "")
                                                    .replace("/end VIRTUAL_CHARACTERISTIC", "")
                                                    .split(" ")
                                                found = true
                                                result.forEach {
                                                    mVirtualCharacteristic.add(it)
                                                }
                                            }
                                        }
                                    }

                                    mOptionalParams[CharOpt.VirtualCharacteristic] = VirtCharacteristic(mVirtualCharacteristic)
                                }

                                line.contains("DEPENDENT_CHARACTERISTIC") -> {
                                    // TODO: Test Dependant Characteristic
                                    val subL = input.subList(index, input.size)
                                    var found = false
                                    val mDependantCharacteristic = mutableListOf<String>()

                                    subL.forEachIndexed { ind, lin ->
                                        when {
                                            !found && lin.contains("/end DEPENDENT_CHARACTERISTIC") -> {
                                                val result = subL.subList(0, ind).joinToString(" ").trim()
                                                    .replace("/begin DEPENDENT_CHARACTERISTIC", "")
                                                    .replace("/end DEPENDENT_CHARACTERISTIC", "")
                                                    .split(" ")
                                                found = true
                                                result.forEach {
                                                    mDependantCharacteristic.add(it)
                                                }
                                            }
                                        }
                                    }

                                    mOptionalParams[CharOpt.DependantCharacteristic] = DependantCharacteristic(mDependantCharacteristic)
                                }

                                line.contains("MAP_LIST") -> {
                                    // TODO: Test Map List
                                    val subL = input.subList(index, input.size)
                                    var found = false
                                    val mMapList = mutableListOf<String>()

                                    subL.forEachIndexed { ind, lin ->
                                        when {
                                            !found && lin.contains("/end MAP_LIST") -> {
                                                val result = subL.subList(0, ind).joinToString(" ").trim()
                                                    .replace("/begin MAP_LIST", "")
                                                    .replace("/end MAP_LIST", "")
                                                    .split(" ")
                                                found = true
                                                result.forEach {
                                                    mMapList.add(it)
                                                }
                                            }
                                        }
                                    }

                                    mOptionalParams[CharOpt.MapList] = MapList(mMapList)
                                }

                                line.contains("ANNOTATION") -> {
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

                                line.contains("CHARACTERISTIC") -> {
                                    val result = line.replace("/begin CHARACTERISTIC", "").trim()
                                    if (result.isNotBlank()) {
                                        mName = result
                                    }
                                }
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
                    mOptionalParams[CharOpt.Annotations] = AnnotList(mAnnotation)
                }

                // Add IfData
                mIfData.isNotEmpty() -> {
                    mOptionalParams[CharOpt.IfData] = IfDataList(mIfData)
                }
            }

            return if (mName.isNotBlank()) {
                Characteristic(
                    name = mName,
                    identifier = input[1].replace("\"", "").trim(),
                    type = input[2].parseCharacteristicType(),
                    address = input[3].replace("0x", "").trim().toULongOrNull(16) ?: ULong.MIN_VALUE,
                    refRecordLayout = input[4].trim(),
                    maxDiff = (input[5].trim().toDoubleOrNull() ?: Double.NaN),
                    conversion = input[6].trim(),
                    lowerLimit = (input[7].toDoubleOrNull() ?: Double.NaN),
                    upperLimit = (input[8].toDoubleOrNull() ?: Double.NaN),
                    optionalParameters = mOptionalParams
                )
            } else {
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
                    optionalParameters = mOptionalParams
                )
            }
        } else return Characteristic()
    }
}