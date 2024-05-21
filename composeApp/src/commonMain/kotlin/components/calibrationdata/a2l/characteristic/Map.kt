package components.calibrationdata.a2l.characteristic

import components.parsers.a2l.classes.enums.AxisType
import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getAxisDescrMap
import components.parsers.a2l.characteristic.getByteOrder
import components.parsers.a2l.characteristic.getFormat
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.recordlayout.getFncValues
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.classes.HexDataResult
import components.calibrationdata.a2l.helpers.getHexValues
import components.calibrationdata.a2l.helpers.getValueData
import components.calibrationdata.a2l.helpers.positionValues
import components.parsers.hex.HexSegmentData

fun readMapCharacteristicData(
    a2lData: A2lData,
    byteSegmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    // TODO
    val byteOrder = characteristic.getByteOrder()
    val charHexDataValues: HexDataResult
    val axisDescriptions = characteristic.getAxisDescrMap()
    val axisDataValuesResult = mutableListOf<Pair<ULong, AxisDataValues>>()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val fncValues = recordLayout.getFncValues()
    val format = characteristic.getFormat()
    val indexMode = fncValues.indexMode
    var position = characteristic.address
    var axisXDim = 0
    var axisYDim = 0
    var axisPointCount = 0

    // Get data for Axis Descriptions
    axisDescriptions.forEach { (type, descr) ->
        val axisDataValues = descr.getHexValues(
            a2lData = a2lData,
            segmentData = byteSegmentData,
            characteristic = characteristic,
            position = position,
            axisDataType = type
        )

        // If it contains an STD_AXIS then characteristic data begins after the Axis data,
        // so we set mPosition to the ending position of the axis data
        if (descr.type == AxisType.STD_AXIS) {
            position = axisDataValues.first
        }

        // Add to our list
        axisDataValuesResult.add(axisDataValues)

        axisPointCount++
    }

    // Get the dimension of our map, by multiplying the axis sizes by each other
    axisDataValuesResult.forEach { axisValue ->
        if (axisXDim == 0) {
            axisXDim = axisValue.second.values.size
        } else if (axisYDim == 0) {
            axisYDim = axisValue.second.values.size
        }
    }

    // Get the Raw data from the byte segment
    charHexDataValues = getValueData(
        a2lData = a2lData,
        segmentList = byteSegmentData,
        byteOrder = byteOrder,
        dataType = fncValues.dataType,
        position = position,
        numberOfValues = axisXDim * axisYDim,
        recordLayout = recordLayout
    )

    // Position the values and format them
    val formattedValues = charHexDataValues.formattedValues.positionValues(
        a2lData = a2lData,
        axisXDim = axisXDim,
        axisYDim = axisYDim,
        characteristic = characteristic,
        indexMode = indexMode,
        numOfAxisPts = axisPointCount,
        hexData = charHexDataValues.hexData,
        format = format
    )

    // Create the Data class to store all the characteristic values
    val charDataValues = CharDataValues(
        byteSegmentId = charHexDataValues.segmentId,
        actualStartPosition = charHexDataValues.startPosition,
        actualEndPosition = charHexDataValues.endPosition,
        values = formattedValues,
        dataType = fncValues.dataType,
        format = format,
        baseOffset = charHexDataValues.baseOffset
    )

    // Drop the first element in the pairs in the list of axis data
    val axisDataValues = axisDataValuesResult.map { it.second }.toMutableList()

    // Return the results
    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.MAP,
        charValues = charDataValues,
        axisValues = axisDataValues,
    )
}