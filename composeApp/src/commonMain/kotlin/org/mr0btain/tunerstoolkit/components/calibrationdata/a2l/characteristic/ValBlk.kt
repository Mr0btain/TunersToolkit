package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.applyBitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getBitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getCompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getDimArray
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getFormat
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getRecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CharacteristicType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.CompuMethod
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
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.positionValues
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData

fun readValBlkCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    // TODO: Needs Major testing, and add ability to handle more than 2 axis
    val byteOrder = characteristic.getByteOrder()
    val compuMethod = characteristic.getCompuMethod(a2lData)
    val charDataFormattedValues = mutableListOf<FormattedValue>()
    val dimArray = characteristic.getDimArray()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val fncValues = recordLayout.getFncValues()
    val format = characteristic.getFormat()
    val indexMode = fncValues.indexMode
    val position = characteristic.address

    if (dimArray.size < 2 || dimArray[1] == 1) {
        // Get Data from Byte Segments
        val charHexDataValues = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = byteOrder,
            dataType = fncValues.dataType,
            position = position,
            numberOfValues = dimArray[0],
            recordLayout = recordLayout
        )

        charHexDataValues.formattedValues.forEachIndexed { index, it ->
            // Check Computation Method
            if (compuMethod != CompuMethod()) {
                // Check conversion type
                if (compuMethod.conversionType != ConversionType.TAB_VERB) {
                    val physUnit = compuMethod.compute(it, a2lData)

                    charDataFormattedValues.add(
                        FormattedValue(
                            originalValue = physUnit.toString(),
                            formattedValue = physUnit.toString().formatLength(format),
                            column = index,
                            row = 0,
                            hexData = charHexDataValues.hexData.joinToString("")
                        )
                    )
                } else {
                    // Apply bitmask if its not blank
                    val charDataTabVerb = if (characteristic.getBitMask() != ULong.MIN_VALUE) {
                        characteristic.applyBitMask(it)
                    } else it

                    // Compute Tab Verb and add it to our result
                    val physUnit = compuMethod.computeTabVerb(charDataTabVerb, a2lData)

                    charDataFormattedValues.add(
                        FormattedValue(
                            originalValue = physUnit,
                            formattedValue = physUnit,
                            column = index,
                            row = 0,
                            hexData = charHexDataValues.hexData.joinToString("")
                        )
                    )
                }
            }
        }

        // Set Data class for Characteristic Values
        val charDataValues = CharDataValues(
            byteSegmentId = charHexDataValues.segmentId,
            actualStartPosition = charHexDataValues.startPosition,
            actualEndPosition = charHexDataValues.endPosition,
            dataType = fncValues.dataType,
            values = charDataFormattedValues,
            format = format,
            baseOffset = charHexDataValues.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = charDataValues
        )
    } else {
        // Get Data from byte segments
        val charHexDataValues = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            byteOrder = byteOrder,
            dataType = fncValues.dataType,
            position = position,
            numberOfValues = dimArray[0] * dimArray[1],
            recordLayout = recordLayout
        )

        // Position the values and format them
        val charDataFinalized = charHexDataValues.formattedValues.positionValues(
            a2lData = a2lData,
            axisXDim = dimArray[0],
            axisYDim = dimArray[1],
            characteristic = characteristic,
            indexMode = indexMode,
            numOfAxisPts = 1,
            hexData = charHexDataValues.hexData,
            format = format
        )

        // Set Data class for Characteristic Values
        val charResult = CharDataValues(
            byteSegmentId = charHexDataValues.segmentId,
            actualStartPosition = charHexDataValues.startPosition,
            actualEndPosition = charHexDataValues.endPosition,
            dataType = fncValues.dataType,
            values = charDataFinalized,
            format = format,
            baseOffset = charHexDataValues.baseOffset
        )

        return MapDataValues(
            name = characteristic.name,
            mapType = CharacteristicType.VAL_BLK,
            charValues = charResult
        )
    }
}