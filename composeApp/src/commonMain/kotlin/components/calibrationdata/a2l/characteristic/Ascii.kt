package components.calibrationdata.a2l.characteristic

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getByteOrder
import components.parsers.a2l.characteristic.getNumber
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.recordlayout.getFncValues
import components.calibrationdata.classes.FormattedValue
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.a2l.helpers.getValueData
import components.parsers.hex.HexSegmentData

fun readAsciiCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    val mByteOrder = characteristic.getByteOrder()
    val mNumber = characteristic.getNumber()
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mDataType = mRecordLayout.getFncValues().dataType
    val mAsciiResult = mutableListOf<FormattedValue>()

    val mResult = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = mNumber,
        recordLayout = mRecordLayout
    )

    mResult.formattedValues.forEachIndexed { index, result ->
        mAsciiResult.add(
            FormattedValue(
                originalValue = result.toChar().toString(),
                formattedValue = result.toChar().toString(),
                column = index,
                row = 0,
                hexData = mResult.hexData.joinToString("")
            )
        )
    }

    val mCharResult = CharDataValues(
        byteSegmentId = mResult.segmentId,
        actualStartPosition = mResult.startPosition,
        actualEndPosition = mResult.endPosition,
        dataType = mDataType,
        values = mAsciiResult,
        format = "",
        baseOffset = mResult.baseOffset
    )

    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.ASCII,
        charValues = mCharResult
    )
}