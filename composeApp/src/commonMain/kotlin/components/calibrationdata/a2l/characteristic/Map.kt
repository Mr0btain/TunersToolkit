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
    val mByteOrder = characteristic.getByteOrder()
    val mCharHexData: HexDataResult
    val mAxisDescriptions = characteristic.getAxisDescrMap()
    val mAxisData = mutableListOf<Pair<ULong, AxisDataValues>>()
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mFncValues = mRecordLayout.getFncValues()
    val mFormat = characteristic.getFormat()
    val mIndexMode = mFncValues.indexMode
    var mPosition = characteristic.address
    var mAxisXDim = 0
    var mAxisYDim = 0
    var mAxisCount = 0

    // Get data for Axis Descriptions
    mAxisDescriptions.forEach { (type, descr) ->
        val mResult = descr.getHexValues(
            a2lData = a2lData,
            segmentData = byteSegmentData,
            characteristic = characteristic,
            position = mPosition,
            axisDataType = type
        )

        // If it contains an STD_AXIS then characteristic data begins after the Axis data,
        // so we set mPosition to the ending position of the axis data
        if (descr.type == AxisType.STD_AXIS) {
            mPosition = mResult.first
        }

        // Add to our list
        mAxisData.add(mResult)

        mAxisCount++
    }

    // Get the dimension of our map, by multiplying the axis sizes by each other
    mAxisData.forEach { axisValue ->
        if (mAxisXDim == 0) {
            mAxisXDim = axisValue.second.values.size
        } else if (mAxisYDim == 0) {
            mAxisYDim = axisValue.second.values.size
        }
    }

    // Get the Raw data from the byte segment
    mCharHexData = getValueData(
        a2lData = a2lData,
        segmentList = byteSegmentData,
        byteOrder = mByteOrder,
        dataType = mFncValues.dataType,
        position = mPosition,
        numberOfValues = mAxisXDim * mAxisYDim,
        recordLayout = mRecordLayout
    )

    // Position the values and format them
    val mCharDataFinal = mCharHexData.formattedValues.positionValues(
        a2lData = a2lData,
        axisXDim = mAxisXDim,
        axisYDim = mAxisYDim,
        characteristic = characteristic,
        indexMode = mIndexMode,
        numOfAxisPts = mAxisCount,
        hexData = mCharHexData.hexData,
        format = mFormat
    )

    // Create the Data class to store all the characteristic values
    val mCharDataResult = CharDataValues(
        byteSegmentId = mCharHexData.segmentId,
        actualStartPosition = mCharHexData.startPosition,
        actualEndPosition = mCharHexData.endPosition,
        values = mCharDataFinal,
        dataType = mFncValues.dataType,
        format = mFormat,
        baseOffset = mCharHexData.baseOffset
    )

    // Drop the first element in the pairs in the list of axis data
    val mAxisDataFinal = mAxisData.map { it.second }.toMutableList()

    // Return the results
    return MapDataValues(
        name = characteristic.name,
        mapType = CharacteristicType.MAP,
        charValues = mCharDataResult,
        axisValues = mAxisDataFinal,
    )
}