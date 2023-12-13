package components.calibrationdata.a2l.characteristic

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.applyBitMask
import components.parsers.a2l.characteristic.getBitMask
import components.parsers.a2l.characteristic.getByteOrder
import components.parsers.a2l.characteristic.getCompuMethod
import components.parsers.a2l.characteristic.getDimArray
import components.parsers.a2l.characteristic.getFormat
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.compumethod.CompuMethod
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.compumethod.compute
import components.parsers.a2l.compumethod.computeTabVerb
import components.parsers.a2l.recordlayout.getFncValues
import components.calibrationdata.classes.FormattedValue
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.a2l.helpers.formatLength
import components.calibrationdata.a2l.helpers.getValueData
import components.calibrationdata.a2l.helpers.positionValues
import components.parsers.hex.HexSegmentData

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