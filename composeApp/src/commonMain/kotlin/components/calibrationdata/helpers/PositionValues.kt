package components.calibrationdata.helpers

import components.a2l.characteristic.Characteristic
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ConversionType
import components.a2l.classes.enums.IndexIncrement
import components.a2l.classes.enums.IndexMode
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
    val mResult = mutableListOf<FormattedValue>()

    // Format the data positions according to IndexMode
    if (indexMode == IndexMode.COLUMN_DIR) {
        // Column position counter
        var col = 0

        // Position the Values
        this.formatColumnOrRowDir(numberOfValues, false).forEachIndexed { index, list ->
            // The Index from this will be used as their Row Counter
            list.forEach {
                mResult.add(
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
                mResult.add(
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

    return mResult
}

fun List<Number>.positionValuesInMap(
    a2lData: A2lData,
    axisXDim: Int,
    axisYDim: Int,
    characteristic: Characteristic,
    indexMode: IndexMode,
    hexData: List<String>
): MutableList<FormattedValue> {
    val mResult = mutableListOf<FormattedValue>()
    val mCompuMethod = characteristic.getCompuMethod(a2lData)
    val mFormat = characteristic.getFormat()
    val mBitMask = characteristic.getBitMask()

    if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
        var row = 0
        var col: Int
        this.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }

                col = index % axisYDim

                val mPhysValue = mCompuMethod.compute(value, a2lData, mFormat)

                mResult.add(
                    FormattedValue(
                        originalValue = mPhysValue.toString(),
                        formattedValue = mPhysValue.toString().formatLength(mFormat),
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

                val mPhysValue = mCompuMethod.compute(value, a2lData, mFormat)

                mResult.add(
                    FormattedValue(
                        originalValue = mPhysValue.toString(),
                        formattedValue = mPhysValue.toString().formatLength(mFormat),
                        column = row,
                        row = col,
                        hexData = hexData.joinToString("")
                    )
                )
            }
        }
    } else {
        val mCharDataBitmask = mutableListOf<Number>()

        if (mBitMask != ULong.MIN_VALUE) {
            this.forEach {
                mCharDataBitmask.add(it.applyBitMask(mBitMask))
            }
        } else {
            this.forEach {
                mCharDataBitmask.add(it)
            }
        }

        var row = 0
        var col: Int
        mCharDataBitmask.forEachIndexed { index, value ->
            if (indexMode == IndexMode.COLUMN_DIR) {
                if (index != 0 && index % axisYDim == 0) {
                    row += 1
                }
                col = index % axisYDim

                val mPhysUnit = mCompuMethod.computeTabVerb(value, a2lData)

                mResult.add(
                    FormattedValue(
                        originalValue = mPhysUnit,
                        formattedValue = mPhysUnit,
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

                val mPhysUnit = mCompuMethod.computeTabVerb(value, a2lData)

                mResult.add(
                    FormattedValue(
                        originalValue = mPhysUnit,
                        formattedValue = mPhysUnit,
                        column = row,
                        row = col,
                        hexData = hexData.joinToString("")
                    )
                )
            }
        }
    }

    return mResult
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