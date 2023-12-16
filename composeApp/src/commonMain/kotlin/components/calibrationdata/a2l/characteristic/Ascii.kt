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
    val byteOrder = characteristic.getByteOrder()
    val number = characteristic.getNumber()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val dataType = recordLayout.getFncValues().dataType
    val asciiResult = mutableListOf<FormattedValue>()

    val hexDataResult = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = number,
        recordLayout = recordLayout
    )

    hexDataResult.formattedValues.forEachIndexed { index, formattedValue ->
        asciiResult.add(
            FormattedValue(
                originalValue = formattedValue.toChar().toString(),
                formattedValue = formattedValue.toChar().toString(),
                column = index,
                row = 0,
                hexData = hexDataResult.hexData.joinToString("")
            )
        )
    }

    val result = CharDataValues(
        byteSegmentId = hexDataResult.segmentId,
        actualStartPosition = hexDataResult.startPosition,
        actualEndPosition = hexDataResult.endPosition,
        dataType = dataType,
        values = asciiResult,
        format = "",
        baseOffset = hexDataResult.baseOffset
    )

    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.ASCII,
        charValues = result
    )
}