package components.calibrationdata.a2l.helpers

import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getByteOrder
import components.parsers.a2l.characteristic.getCompuMethod
import components.parsers.a2l.characteristic.getFormat
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.recordlayout.getFncValues
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.HexDataResult
import components.parsers.hex.HexSegmentData

fun Characteristic.readDataValues(
    axisXDim: Int,
    axisYDim: Int,
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    position: ULong
): CharDataValues {
    val byteOrder = getByteOrder()
    val recordLayout = getRecordLayout(a2lData)
    val fncValues = recordLayout.getFncValues()
    // val mAddrType = mFncValues.addressType
    val indexMode = fncValues.indexMode
    val dataType = fncValues.dataType
    val format = getFormat()
    // val mCompuMethod = characteristic.getCompuMethod(a2lData)

    // Get Axis Points
    val numOfAxisPts = if (axisYDim != Int.MIN_VALUE && axisYDim != 0) {
        axisXDim * axisYDim
    } else axisXDim

    // Get Characteristic values from Byte data
    val charData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = position,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = numOfAxisPts,
        recordLayout = recordLayout
    )

    // Position the values and format them
    val formattedValues = charData.formattedValues.positionValues(
        a2lData = a2lData,
        axisXDim = axisXDim,
        axisYDim = axisYDim,
        characteristic = this,
        indexMode = indexMode,
        numOfAxisPts = if (axisYDim != 0) 2 else 1,
        hexData = charData.hexData,
        format = format
    )

    return CharDataValues(
        byteSegmentId = charData.segmentId,
        actualStartPosition = charData.startPosition,
        actualEndPosition = charData.endPosition,
        dataType = dataType,
        values = formattedValues,
        format = format,
        baseOffset = charData.baseOffset
    )
}


fun readFixCharacteristicData(
    mNumOfAxisPts: Int,
    characteristic: Characteristic,
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>
): CharDataValues {
    val byteOrder = characteristic.getByteOrder()
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val fncValues = recordLayout.getFncValues()
    // val mAddrType = mFncValues.addressType
    val indexMode = fncValues.indexMode
    val charData: HexDataResult
    val dataType = fncValues.dataType
    val format = characteristic.getFormat()
    val compuMethod = characteristic.getCompuMethod(a2lData)

    // Get Characteristic values from Byte data
    charData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = mNumOfAxisPts,
        recordLayout = recordLayout
    )

    // Format the values
    val charDataResult = charData.formatMapValues(characteristic, compuMethod, a2lData, format)

    return CharDataValues(
        byteSegmentId = charData.segmentId,
        actualStartPosition = charData.startPosition,
        actualEndPosition = charData.endPosition,
        dataType = dataType,
        values = charDataResult.positionMapValues(indexMode, mNumOfAxisPts, charData.hexData, format).toMutableList(),
        format = format,
        baseOffset = charData.baseOffset
    )
}