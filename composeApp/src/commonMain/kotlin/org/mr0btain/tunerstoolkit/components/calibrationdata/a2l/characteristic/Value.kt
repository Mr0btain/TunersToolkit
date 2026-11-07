package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.applyBitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getBitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getCompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getFormat
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getRecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CharacteristicType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ConversionType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.compute
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.computeTabVerb
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getFncValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.FormattedValue
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.CharDataValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.formatLength
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.getValueData
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData

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
    val charHexDataValues = getValueData(
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
            characteristic.applyBitMask(charHexDataValues.formattedValues.first())
        } else charHexDataValues.formattedValues.first()

        // Compute final value
        converted = compuMethod.compute(value, a2lData)
    } else {
        // If bitmask exists, apply it
        val value = if (bitMask != ULong.MIN_VALUE && bitMask != 0UL) {
            characteristic.applyBitMask(charHexDataValues.formattedValues.first())
        } else charHexDataValues.formattedValues.first()

        // Compute Tab Verb
        converted = compuMethod.computeTabVerb(value, a2lData)
    }

    // Create the data container for the value
    val charDataValues = CharDataValues(
        byteSegmentId = charHexDataValues.segmentId,
        actualStartPosition = charHexDataValues.startPosition,
        actualEndPosition = charHexDataValues.endPosition,
        dataType = dataType,
        values = mutableListOf(FormattedValue(
            originalValue = converted.toString(),
            formattedValue = if (converted is Number) { converted.toString().formatLength(format) } else converted.toString(),
            column = 0,
            row = 0,
            hexData = charHexDataValues.hexData.joinToString(""))
        ),
        format = format,
        baseOffset = charHexDataValues.baseOffset
    )

    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.VALUE,
        charValues = charDataValues
    )
}