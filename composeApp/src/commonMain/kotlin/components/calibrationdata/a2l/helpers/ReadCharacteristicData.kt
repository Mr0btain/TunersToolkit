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
    val mByteOrder = getByteOrder()
    val mRecordLayout = getRecordLayout(a2lData)
    val mFncValues = mRecordLayout.getFncValues()
    // val mAddrType = mFncValues.addressType
    val mIndexMode = mFncValues.indexMode
    val mDataType = mFncValues.dataType
    val mFormat = getFormat()
    // val mCompuMethod = characteristic.getCompuMethod(a2lData)

    // Get Axis Points
    val mNumOfAxisPts = if (axisYDim != Int.MIN_VALUE && axisYDim != 0) {
        axisXDim * axisYDim
    } else axisXDim

    // Get Characteristic values from Byte data
    val mCharData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = position,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = mNumOfAxisPts,
        recordLayout = mRecordLayout
    )

    // Position the values and format them
    val mCharDataFinal = mCharData.formattedValues.positionValues(
        a2lData = a2lData,
        axisXDim = axisXDim,
        axisYDim = axisYDim,
        characteristic = this,
        indexMode = mIndexMode,
        numOfAxisPts = if (axisYDim != 0) 2 else 1,
        hexData = mCharData.hexData,
        format = mFormat
    )

    return CharDataValues(
        byteSegmentId = mCharData.segmentId,
        actualStartPosition = mCharData.startPosition,
        actualEndPosition = mCharData.endPosition,
        dataType = mDataType,
        values = mCharDataFinal,
        format = mFormat,
        baseOffset = mCharData.baseOffset
    )
}


fun readFixCharacteristicData(
    mNumOfAxisPts: Int,
    characteristic: Characteristic,
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>
): CharDataValues {
    val mByteOrder = characteristic.getByteOrder()
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mFncValues = mRecordLayout.getFncValues()
    // val mAddrType = mFncValues.addressType
    val mIndexMode = mFncValues.indexMode
    val mCharData: HexDataResult
    val mDataType = mFncValues.dataType
    val mFormat = characteristic.getFormat()
    val mCompuMethod = characteristic.getCompuMethod(a2lData)

    // Get Characteristic values from Byte data
    mCharData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = characteristic.address,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = mNumOfAxisPts,
        recordLayout = mRecordLayout
    )

    // Format the values
    val mCharDataResult = mCharData.formatMapValues(characteristic, mCompuMethod, a2lData, mFormat)

    return CharDataValues(
        byteSegmentId = mCharData.segmentId,
        actualStartPosition = mCharData.startPosition,
        actualEndPosition = mCharData.endPosition,
        dataType = mDataType,
        values = mCharDataResult.positionMapValues(mIndexMode, mNumOfAxisPts, mCharData.hexData, mFormat).toMutableList(),
        format = mFormat,
        baseOffset = mCharData.baseOffset
    )
}