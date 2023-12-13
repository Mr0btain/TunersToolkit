package components.calibrationdata.characteristic

import components.a2l.characteristic.Characteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.compumethod.CompuMethod
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ConversionType
import components.calibrationdata.classes.FormattedValue
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.helpers.formatLength
import components.calibrationdata.helpers.getValueData
import components.calibrationdata.helpers.positionValues
import components.hex.HexSegmentData

fun readValBlkCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    // TODO: Needs Major testing, and add ability to handle more than 2 axis
    val mByteOrder = characteristic.getByteOrder()
    val mCompuMethod = characteristic.getCompuMethod(a2lData)
    val mCharDataFormatted = mutableListOf<FormattedValue>()
    val mDimArray = characteristic.getDimArray()
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mFncValues = mRecordLayout.getFncValues()
    val mFormat = characteristic.getFormat()
    val mIndexMode = mFncValues.indexMode
    val mPosition = characteristic.address

    if (mDimArray.size < 2 || mDimArray[1] == 1) {
        // Get Data from Byte Segments
        val mCharHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = mByteOrder,
            dataType = mFncValues.dataType,
            position = mPosition,
            numberOfValues = mDimArray[0],
            recordLayout = mRecordLayout
        )

        mCharHexData.formattedValues.forEachIndexed { index, it ->
            // Check Computation Method
            if (mCompuMethod != CompuMethod()) {
                // Check conversion type
                if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
                    val mPhysUnit = mCompuMethod.compute(it, a2lData)

                    mCharDataFormatted.add(
                        FormattedValue(
                            originalValue = mPhysUnit.toString(),
                            formattedValue = mPhysUnit.toString().formatLength(mFormat),
                            column = index,
                            row = 0,
                            hexData = mCharHexData.hexData.joinToString("")
                        )
                    )
                } else {
                    // Apply bitmask if its not blank
                    val mCharDataTabVerb = if (characteristic.getBitMask() != ULong.MIN_VALUE) {
                        characteristic.applyBitMask(it)
                    } else it

                    // Compute Tab Verb and add it to our result
                    val mPhysUnit = mCompuMethod.computeTabVerb(mCharDataTabVerb, a2lData)

                    mCharDataFormatted.add(
                        FormattedValue(
                            originalValue = mPhysUnit,
                            formattedValue = mPhysUnit,
                            column = index,
                            row = 0,
                            hexData = mCharHexData.hexData.joinToString("")
                        )
                    )
                }
            }
        }

        // Set Data class for Characteristic Values
        val mCharResult = CharDataValues(
            byteSegmentId = mCharHexData.segmentId,
            actualStartPosition = mCharHexData.startPosition,
            actualEndPosition = mCharHexData.endPosition,
            dataType = mFncValues.dataType,
            values = mCharDataFormatted,
            format = mFormat,
            baseOffset = mCharHexData.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = mCharResult
        )
    } else {
        // Get Data from byte segments
        val mCharHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = mByteOrder,
            dataType = mFncValues.dataType,
            position = mPosition,
            numberOfValues = mDimArray[0] * mDimArray[1],
            recordLayout = mRecordLayout
        )

        // Position the values and format them
        val mCharDataFinal = mCharHexData.formattedValues.positionValues(
            a2lData = a2lData,
            axisXDim = mDimArray[0],
            axisYDim = mDimArray[1],
            characteristic = characteristic,
            indexMode = mIndexMode,
            numOfAxisPts = 1,
            hexData = mCharHexData.hexData,
            format = mFormat
        )

        // Set Data class for Characteristic Values
        val mCharResult = CharDataValues(
            byteSegmentId = mCharHexData.segmentId,
            actualStartPosition = mCharHexData.startPosition,
            actualEndPosition = mCharHexData.endPosition,
            dataType = mFncValues.dataType,
            values = mCharDataFinal,
            format = mFormat,
            baseOffset = mCharHexData.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = mCharResult
        )
    }
}