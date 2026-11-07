package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getBitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getCompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getFormat
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ConversionType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.IndexIncrement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.IndexMode
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.compute
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.computeTabVerb
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValue
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataType
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.FormattedValue

fun List<Number>.positionValues(
    a2lData: A2lData,
    axisXDim: Int,
    axisYDim: Int,
    characteristic: Characteristic,
    indexMode: IndexMode,
    numOfAxisPts: Int,
    hexData: List<String>,
    format: String
): MutableList<FormattedValue> {
    return when (numOfAxisPts) {
        1 -> {
            positionMapValues(indexMode, axisXDim, hexData, format)
        }

        2 -> {
            positionValuesInMap(a2lData, axisXDim, axisYDim, characteristic, indexMode, hexData)
        }

        else -> {
            mutableListOf()
        }
    }
}

fun List<Any>.positionMapValues(
    indexMode:
    IndexMode,
    numberOfValues: Int,
    hexData: List<String>,
    format: String
): MutableList<FormattedValue> {
    val result = mutableListOf<FormattedValue>()
    // Column position counter
    var column = 0

    // Format the data positions according to IndexMode
    if (indexMode == IndexMode.COLUMN_DIR) {
        // Position the Values
        this.formatColumnOrRowDir(numberOfValues, false).forEachIndexed { index, list ->
            // The Index from this will be used as their Row Counter
            list.forEach {
                result.add(
                    FormattedValue(
                        originalValue = it.toString(),
                        formattedValue = if (it is Number) { it.toString().formatLength(format) } else it.toString(),
                        column = column,
                        row = index,
                        hexData = hexData.joinToString("")
                    )
                )
                // Advance the colum counter with each item
                column += 1
            }
            // Reset column counter for new Row
            column = 0
        }
    } else {
        // Position the Values
        this.formatColumnOrRowDir(numberOfValues, true).forEachIndexed { index, list ->
            // The Index from this will be used as their Row Counter
            list.forEach {
                result.add(
                    FormattedValue(
                        originalValue = it.toString(),
                        formattedValue = if (it is Number) { it.toString().formatLength(format) } else it.toString(),
                        column = column,
                        row = index,
                        hexData = hexData.joinToString("")
                    )
                )
                // Advance the colum counter with each item
                column += 1
            }
            // Reset column counter for new Row
            column = 0
        }
    }

    return result
}

fun List<Number>.positionValuesInMap(
    a2lData: A2lData,
    axisXDim: Int,
    axisYDim: Int,
    characteristic: Characteristic,
    indexMode: IndexMode,
    hexData: List<String>
): MutableList<FormattedValue> {
    val result = mutableListOf<FormattedValue>()
    val compuMethod = characteristic.getCompuMethod(a2lData)
    val format = characteristic.getFormat()
    val bitMask = characteristic.getBitMask()

    if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        var row = 0
        var column: Int
        this.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }

                column = index % axisYDim
                val physValue = compuMethod.compute(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physValue.toString(),
                        formattedValue = physValue.toString().formatLength(format),
                        column = column,
                        row = row,
                        hexData = hexData.joinToString("")
                    )
                )
            } else {
                if (index != 0 && index % axisXDim == 0) {
                    row += 1
                }

                column = index % axisXDim
                val physValue = compuMethod.compute(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physValue.toString(),
                        formattedValue = physValue.toString().formatLength(format),
                        column = row,
                        row = column,
                        hexData = hexData.joinToString("")
                    )
                )
            }
        }
    } else {
        val charDataBitmask = mutableListOf<Number>()

        if (bitMask != ULong.MIN_VALUE) {
            this.forEach {
                charDataBitmask.add(it.applyBitMask(bitMask))
            }
        } else {
            this.forEach {
                charDataBitmask.add(it)
            }
        }

        var row = 0
        var column: Int

        charDataBitmask.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }

                column = index % axisYDim
                val physUnit = compuMethod.computeTabVerb(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physUnit,
                        formattedValue = physUnit,
                        column = column,
                        row = row,
                        hexData = hexData.joinToString("")
                    )
                )
            } else {
                if (index != 0 && index % axisXDim == 0) {
                    row += 1
                }

                column = index % axisXDim
                val physUnit = compuMethod.computeTabVerb(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physUnit,
                        formattedValue = physUnit,
                        column = row,
                        row = column,
                        hexData = hexData.joinToString("")
                    )
                )
            }
        }
    }

    return result
}

fun IndexIncrement.sortAxisPosition(
    input: Any,
    index: Int,
    axisType: AxisDataType,
    numOfAxisPts: Int,
    format: String
): AxisDataValue {
    return if (this == IndexIncrement.INDEX_INCR) {
        AxisDataValue(
            originalValue = input.toString(),
            formattedValue = if (input is Number) { input.toString().formatLength(format) } else input.toString(),
            position = index,
            axis = axisType
        )
    } else {
        AxisDataValue(
            originalValue = input.toString(),
            formattedValue = if (input is Number) { input.toString().formatLength(format) } else input.toString(),
            position = (numOfAxisPts) - index,
            axis = axisType
        )
    }
}