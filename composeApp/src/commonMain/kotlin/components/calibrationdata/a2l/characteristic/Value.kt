package components.calibrationdata.a2l.characteristic

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.applyBitMask
import components.parsers.a2l.characteristic.getBitMask
import components.parsers.a2l.characteristic.getByteOrder
import components.parsers.a2l.characteristic.getCompuMethod
import components.parsers.a2l.characteristic.getFormat
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.enums.CharacteristicType
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
import components.parsers.hex.HexSegmentData

fun readValueCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    val byteOrder = characteristic.getByteOrder()
    val bitMask = characteristic.getBitMask()
    val compuMethod = characteristic.getCompuMethod(a2lData)
    val format = characteristic.getFormat()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val dataType = recordLayout.getFncValues().dataType
    val converted: Any

    // Read Characteristic values from byte segment data
    val charHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = 1,
        recordLayout = recordLayout
    )

    // Format the Characteristic data
     if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        // If bitmask exists, apply it
        val value = if (bitMask != ULong.MIN_VALUE && bitMask != 0UL) {
            characteristic.applyBitMask(charHexData.formattedValues.first())
        } else charHexData.formattedValues.first()

        // Compute final value
        converted = compuMethod.compute(value, a2lData)
    } else {
        // If bitmask exists, apply it
        val value = if (bitMask != ULong.MIN_VALUE && bitMask != 0UL) {
            characteristic.applyBitMask(charHexData.formattedValues.first())
        } else charHexData.formattedValues.first()

        // Compute Tab Verb
        converted = compuMethod.computeTabVerb(value, a2lData)
    }

    // Create the data container for the value
    val charDataValue = CharDataValues(
        byteSegmentId = charHexData.segmentId,
        actualStartPosition = charHexData.startPosition,
        actualEndPosition = charHexData.endPosition,
        dataType = dataType,
        values = mutableListOf(FormattedValue(
            originalValue = converted.toString(),
            formattedValue = if (converted is Number) { converted.toString().formatLength(format) } else converted.toString(),
            column = 0,
            row = 0,
            hexData = charHexData.hexData.joinToString(""))
        ),
        format = format,
        baseOffset = charHexData.baseOffset
    )

    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.VALUE,
        charValues = charDataValue
    )
}