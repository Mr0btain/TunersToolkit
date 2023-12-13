package components.calibrationdata.helpers

import components.a2l.axis.AxisDescr
import components.a2l.classes.enums.AxisType
import components.a2l.characteristic.Characteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.recordlayout.RecordLayout
import components.a2l.classes.RecordLayoutAxisRescale
import components.a2l.classes.RecordLayoutReserved
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ByteOrder
import components.a2l.classes.enums.DataType
import components.calibrationdata.axis.computeFixAxis
import components.calibrationdata.axis.readComAxisValues
import components.calibrationdata.axis.readCurveAxisValues
import components.calibrationdata.axis.readResAxisValues
import components.calibrationdata.axis.readStdAxisValues
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.classes.CharDataValues
import components.calibrationdata.classes.HexDataResult
import components.calibrationdata.classes.RawDataResult
import components.hex.HexSegmentData
import components.hex.getSegmentId

/**
 * Returns the raw hex data, along with some other needed values at a given address
 */

fun getDataFromHexSegment(
    byteOrder: ByteOrder,
    mHexData: HexSegmentData,
    position: ULong,
    dataType: DataType
): RawDataResult {
    val mLength = dataType.size * 2
    val mAddress = (position - mHexData.baseOffset).toInt() * 2
    var mResult = ""

    mHexData.data.entries.forEach { (start, data) ->
        val mEndPosition = (start + data.length)

        if (mAddress in start..mEndPosition) {
            mResult = if (mEndPosition > (mAddress + mLength)) {
                data.substring(mAddress - start, (mAddress + mLength) - start)
            } else {
                val mTemp = StringBuilder()
                mTemp.append(data.subSequence(mAddress - start, data.length))
                mTemp.append(mHexData.data[start + data.length]?.subSequence(0, (mAddress + mLength) - (start + data.length))
                    ?: "")

                mTemp.toString()
            }
        }
    }

    return RawDataResult(
        startPosition = mAddress,
        hexData = mResult.applyByteOrder(byteOrder)
        )
}

/**
 * Returns the raw data for an Axis at a given address wrapped in a ByteBuffer
 */

fun AxisDescr.getHexValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic,
    axisDataType: AxisDataType,
    position: ULong
): Pair<ULong, AxisDataValues> {
    return when (this.type) {
        AxisType.STD_AXIS -> {
            readStdAxisValues(
                a2lData = a2lData,
                segmentData = segmentData,
                axisDescr = this,
                position = position,
                characteristic = characteristic,
                axisType = axisDataType
            )
        }

        AxisType.COM_AXIS -> {
            readComAxisValues(
                a2lData = a2lData,
                segmentData = segmentData,
                axisDescr = this,
                axisDataType = axisDataType
            )
        }

        AxisType.FIX_AXIS -> {
            Pair(ULong.MIN_VALUE, this.computeFixAxis(axisDataType))
        }

        AxisType.CURVE_AXIS -> {
            readCurveAxisValues(
                a2lData = a2lData,
                segmentData = segmentData,
                axisDescr = this)
        }

        AxisType.RES_AXIS -> {
            readResAxisValues(
                a2lData = a2lData,
                segmentData = segmentData,
                axisDescr = this,
                axisType = axisDataType
            )
        }

        AxisType.NOT_SET -> {
            Pair(ULong.MIN_VALUE, AxisDataValues())
        }
    }
}

/**
 * Gets the data needed for Most use cases when getRawByteData is called
 */
fun getValueData(
    a2lData: A2lData,
    segmentList: Map<String, HexSegmentData>,
    byteOrder: ByteOrder,
    dataType: DataType,
    position: ULong,
    numberOfValues: Int,
    recordLayout: RecordLayout
): HexDataResult {
    var mPosition = position
    val mLength = numberOfValues * (dataType.size * 2)
    val mHexValues = mutableListOf<String>()
    val mFormattedValues = mutableListOf<Number>()
    var mActualStart = Int.MIN_VALUE

    // Get the ID of the byte segment that contains our data
    val mSegmentId = getSegmentId(
        segments = segmentList,
        dataType = dataType,
        position = position
    )

    // Get Base Offset
    val mBaseOffset = if (segmentList.containsKey(mSegmentId)) {
        segmentList[mSegmentId]?.baseOffset ?: 0u
    } else 0u

    for (i in 0 until numberOfValues) {
        // Get the data from the Byte Segments and add it to the list
        val mHex = getDataFromHexSegment(
            byteOrder = byteOrder,
            mHexData = segmentList[mSegmentId] ?: HexSegmentData(),
            position = mPosition.alignA2lAddress(recordLayout, dataType, a2lData),
            dataType = dataType
        )

        // Set actual start if it hasn't been already
        if (mActualStart == Int.MIN_VALUE) {
            mActualStart = mHex.startPosition
        }

        // Add the Hex value to the list
        mHexValues.add(
            mHex.hexData
        )

        // Advance the position to the next section of data
        mPosition += dataType.size.toULong()
    }

    // Format each value to its number based on its data type
    mHexValues.forEach {
        mFormattedValues.add(
            it.formatValue(dataType)
        )
    }

    // Return our result
    return HexDataResult(
        startPosition = mActualStart,
        endPosition = mActualStart + mLength,
        hexData = mHexValues,
        formattedValues = mFormattedValues,
        segmentId = mSegmentId,
        baseOffset = mBaseOffset
    )
}

/**
 * Gets the Value for NoAxisPts from the ByteSegmentData
 */

fun RecordLayout.getNoAxisPtsData(
    byteOrder: ByteOrder,
    position: ULong,
    dataType: DataType,
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>
): Pair<ULong, Int> {
    // Align the address
    var mPosition = position.alignA2lAddress(this, dataType, a2lData)
    var mNumOfAxisPts = Int.MIN_VALUE

    val mSegmentId = getSegmentId(
        segments = segmentData,
        dataType = dataType,
        position = mPosition
    )

    // Get value from segment
    val mNumberOfAxisPoints = getDataFromHexSegment(
        byteOrder,
        segmentData[mSegmentId] ?: HexSegmentData(),
        mPosition,
        dataType
    ).hexData.formatValue(dataType)

    // If we've only returned one Byte, which we should have, set it as the number of axis points
    if (mNumberOfAxisPoints.toInt() != 0) {
        mNumOfAxisPts = mNumberOfAxisPoints.toInt()
    }

    // Advance position size of data
    mPosition += (dataType.size).toULong()

    return Pair(mPosition, mNumOfAxisPts)
}

/**
 * Gets the Value for NoAxisRescale from the ByteSegmentData
 */

fun RecordLayoutAxisRescale.getAxisRescaleData(
    position: ULong,
    segmentData: Map<String, HexSegmentData>,
    reserved: RecordLayoutReserved,
    byteOrder: ByteOrder
): Pair<ULong, Int> {
    val mDataType = this.dataType
    var mPosition = position
    var mNumOfAxisPts = Int.MIN_VALUE

    val mSegmentId = getSegmentId(
        segments = segmentData,
        dataType = mDataType,
        position = mPosition
    )

    val numOfAxisResult = getDataFromHexSegment(
        byteOrder,
        segmentData[mSegmentId] ?: HexSegmentData(),
        mPosition,
        dataType
    ).hexData.formatValue(dataType)

    // If we've only returned one Byte, which we should have, set it as the number of axis points
    if (numOfAxisResult.toInt() != 0) {
        mNumOfAxisPts = numOfAxisResult.toInt()
    }

    mPosition += (this.dataType.size).toULong()

    // Skip reserved
    if (reserved.dataType != DataType.NOT_SET) {
        mPosition += (reserved.dataType.size).toULong()
    }

    return Pair(mPosition, mNumOfAxisPts)
}

fun AxisDataValues.getCharData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    position: ULong,
    characteristic: Characteristic,
    axisType: AxisType
): CharDataValues {
    var mPosition = characteristic.address

    // Check if we should use the end position in case of Standard axis
    if (axisType == AxisType.STD_AXIS) {
        mPosition = position
    }

    // Read Characteristic values from byte segment data
    return characteristic.readDataValues(
        axisXDim = this.values.size,
        axisYDim = 0,
        a2lData = a2lData,
        position = mPosition,
        segmentData = segmentData
    )
}

fun AxisDataValues.getFixCharData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): CharDataValues {
    // Read Characteristic values from byte segment data
    return if (this.values.isNotEmpty()) {
        readFixCharacteristicData(this.values.size, characteristic, a2lData, segmentData)
    } else if (characteristic.type == CharacteristicType.CURVE) {
        val mSize = characteristic.getAxisDescrX().maxAxisPoints
        readFixCharacteristicData(mSize.toInt(), characteristic, a2lData, segmentData)
    } else {
        // TODO: Add better logic for maps and values?
        val mSize = characteristic.getAxisDescrX().maxAxisPoints
        readFixCharacteristicData(mSize.toInt(), characteristic, a2lData, segmentData)
    }
}