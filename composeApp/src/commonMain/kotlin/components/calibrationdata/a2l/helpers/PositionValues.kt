package components.calibrationdata.a2l.helpers

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getBitMask
import components.parsers.a2l.characteristic.getCompuMethod
import components.parsers.a2l.characteristic.getFormat
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.IndexIncrement
import components.parsers.a2l.classes.enums.IndexMode
import components.parsers.a2l.compumethod.compute
import components.parsers.a2l.compumethod.computeTabVerb
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.FormattedValue

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
            this.positionMapValues(indexMode, axisXDim, hexData, format)
        }

        2 -> {
            this.positionValuesInMap(a2lData, axisXDim, axisYDim, characteristic, indexMode, hexData)
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

    // Format the data positions according to IndexMode
    if (indexMode == IndexMode.COLUMN_DIR) {
        // Column position counter
        var col = 0

        // Position the Values
        this.formatColumnOrRowDir(numberOfValues, false).forEachIndexed { index, list ->
            // The Index from this will be used as their Row Counter
            list.forEach {
                result.add(
                    FormattedValue(
                        originalValue = it.toString(),
                        formattedValue = if (it is Number) { it.toString().formatLength(format) } else it.toString(),
                        column = col,
                        row = index,
                        hexData = hexData.joinToString("")
                    )
                )
                // Advance the colum counter with each item
                col += 1
            }
            // Reset column counter for new Row
            col = 0
        }
    } else {
        // Column counter
        var col = 0

        // Position the Values
        this.formatColumnOrRowDir(numberOfValues, true).forEachIndexed { index, list ->
            // The Index from this will be used as their Row Counter
            list.forEach {
                result.add(
                    FormattedValue(
                        originalValue = it.toString(),
                        formattedValue = if (it is Number) { it.toString().formatLength(format) } else it.toString(),
                        column = col,
                        row = index,
                        hexData = hexData.joinToString("")
                    )
                )
                // Advance the colum counter with each item
                col += 1
            }
            // Reset column counter for new Row
            col = 0
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
        var col: Int
        this.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }

                col = index % axisYDim

                val physValue = compuMethod.compute(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physValue.toString(),
                        formattedValue = physValue.toString().formatLength(format),
                        column = col,
                        row = row,
                        hexData = hexData.joinToString("")
                    )
                )
            } else {
                if (index != 0 && index % axisXDim == 0) {
                    row += 1
                }

                col = index % axisXDim

                val physValue = compuMethod.compute(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physValue.toString(),
                        formattedValue = physValue.toString().formatLength(format),
                        column = row,
                        row = col,
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
        var col: Int
        charDataBitmask.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }
                col = index % axisYDim

                val physUnit = compuMethod.computeTabVerb(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physUnit,
                        formattedValue = physUnit,
                        column = col,
                        row = row,
                        hexData = hexData.joinToString("")
                    )
                )
            } else {
                if (index != 0 && index % axisXDim == 0) {
                    row += 1
                }
                col = index % axisXDim

                val physUnit = compuMethod.computeTabVerb(value, a2lData)

                result.add(
                    FormattedValue(
                        originalValue = physUnit,
                        formattedValue = physUnit,
                        column = row,
                        row = col,
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