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
    val byteOrder = characteristic.getByteOrder()
    val compuMethod = characteristic.getCompuMethod(a2lData)
    val charDataFormatted = mutableListOf<FormattedValue>()
    val dimArray = characteristic.getDimArray()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val fncValues = recordLayout.getFncValues()
    val format = characteristic.getFormat()
    val indexMode = fncValues.indexMode
    val position = characteristic.address

    if (dimArray.size < 2 || dimArray[1] == 1) {
        // Get Data from Byte Segments
        val charHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = byteOrder,
            dataType = fncValues.dataType,
            position = position,
            numberOfValues = dimArray[0],
            recordLayout = recordLayout
        )

        charHexData.formattedValues.forEachIndexed { index, it ->
            // Check Computation Method
            if (compuMethod != CompuMethod()) {
                // Check conversion type
                if (compuMethod.conversionType != ConversionType.TAB_VERB) {
                    val physUnit = compuMethod.compute(it, a2lData)

                    charDataFormatted.add(
                        FormattedValue(
                            originalValue = physUnit.toString(),
                            formattedValue = physUnit.toString().formatLength(format),
                            column = index,
                            row = 0,
                            hexData = charHexData.hexData.joinToString("")
                        )
                    )
                } else {
                    // Apply bitmask if its not blank
                    val charDataTabVerb = if (characteristic.getBitMask() != ULong.MIN_VALUE) {
                        characteristic.applyBitMask(it)
                    } else it

                    // Compute Tab Verb and add it to our result
                    val physUnit = compuMethod.computeTabVerb(charDataTabVerb, a2lData)

                    charDataFormatted.add(
                        FormattedValue(
                            originalValue = physUnit,
                            formattedValue = physUnit,
                            column = index,
                            row = 0,
                            hexData = charHexData.hexData.joinToString("")
                        )
                    )
                }
            }
        }

        // Set Data class for Characteristic Values
        val charResult = CharDataValues(
            byteSegmentId = charHexData.segmentId,
            actualStartPosition = charHexData.startPosition,
            actualEndPosition = charHexData.endPosition,
            dataType = fncValues.dataType,
            values = charDataFormatted,
            format = format,
            baseOffset = charHexData.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = charResult
        )
    } else {
        // Get Data from byte segments
        val charHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = byteOrder,
            dataType = fncValues.dataType,
            position = position,
            numberOfValues = dimArray[0] * dimArray[1],
            recordLayout = recordLayout
        )

        // Position the values and format them
        val charDataFinal = charHexData.formattedValues.positionValues(
            a2lData = a2lData,
            axisXDim = dimArray[0],
            axisYDim = dimArray[1],
            characteristic = characteristic,
            indexMode = indexMode,
            numOfAxisPts = 1,
            hexData = charHexData.hexData,
            format = format
        )

        // Set Data class for Characteristic Values
        val charResult = CharDataValues(
            byteSegmentId = charHexData.segmentId,
            actualStartPosition = charHexData.startPosition,
            actualEndPosition = charHexData.endPosition,
            dataType = fncValues.dataType,
            values = charDataFinal,
            format = format,
            baseOffset = charHexData.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = charResult
        )
    }
}