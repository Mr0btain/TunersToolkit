package components.calibrationdata.characteristic

import components.a2l.characteristic.Characteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ConversionType
import components.calibrationdata.classes.FormattedValue
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.helpers.formatLength
import components.calibrationdata.helpers.getValueData
import components.hex.HexSegmentData

fun readValueCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    val mByteOrder = characteristic.getByteOrder()
    val mBitMask = characteristic.getBitMask()
    val mCompuMethod = characteristic.getCompuMethod(a2lData)
    val mFormat = characteristic.getFormat()
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mDataType = mRecordLayout.getFncValues().dataType
    val mConverted: Any

    // Read Characteristic values from byte segment data
    val mCharHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = 1,
        recordLayout = mRecordLayout
    )

    // Format the Characteristic data
     if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
        // If bitmask exists, apply it
        val mValue = if (mBitMask != ULong.MIN_VALUE && mBitMask != 0UL) {
            characteristic.applyBitMask(mCharHexData.formattedValues.first())
        } else mCharHexData.formattedValues.first()

        // Compute final value
        mConverted = mCompuMethod.compute(mValue, a2lData)
    } else {
        // If bitmask exists, apply it
        val mValue = if (mBitMask != ULong.MIN_VALUE && mBitMask != 0UL) {
            characteristic.applyBitMask(mCharHexData.formattedValues.first())
        } else mCharHexData.formattedValues.first()

        // Compute Tab Verb
        mConverted = mCompuMethod.computeTabVerb(mValue, a2lData)
    }

    // Create the data container for the value
    val mCharDataValue = CharDataValues(
        byteSegmentId = mCharHexData.segmentId,
        actualStartPosition = mCharHexData.startPosition,
        actualEndPosition = mCharHexData.endPosition,
        dataType = mDataType,
        values = mutableListOf(FormattedValue(
            originalValue = mConverted.toString(),
            formattedValue = if (mConverted is Number) { mConverted.toString().formatLength(mFormat) } else mConverted.toString(),
            column = 0,
            row = 0,
            hexData = mCharHexData.hexData.joinToString(""))
        ),
        format = mFormat,
        baseOffset = mCharHexData.baseOffset
    )

    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.VALUE,
        charValues = mCharDataValue
    )
}