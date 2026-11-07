package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisDescr
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.AxisType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.getAxisDescrX
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.CharacteristicType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.RecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutAxisRescale
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutReserved
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis.computeFixAxis
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis.readComAxisValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis.readCurveAxisValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis.readResAxisValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis.readStdAxisValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataType
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.CharDataValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.HexDataResult
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.RawDataResult
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData
import org.mr0btain.tunerstoolkit.components.parsers.hex.getSegmentId

/**
 * Returns the raw hex data, along with some other needed values at a given address
 */

fun getDataFromHexSegment(
    byteOrder: ByteOrder,
    mHexData: HexSegmentData,
    position: ULong,
    dataType: DataType
): RawDataResult {
    val length = dataType.size * 2
    val address = (position - mHexData.baseOffset).toInt() * 2
    var result = ""

    mHexData.data.entries.forEach { (start, data) ->
        val endPosition = (start + data.length)

        if (address in start..endPosition) {
            result = if (endPosition > (address + length)) {
                data.substring(address - start, (address + length) - start)
            } else {
                val temp = StringBuilder()
                temp.append(data.subSequence(address - start, data.length))
                temp.append(mHexData.data[start + data.length]?.subSequence(0, (address + length) - (start + data.length))
                    ?: "")

                temp.toString()
            }
        }
    }

    return RawDataResult(
        startPosition = address,
        hexData = result.applyByteOrder(byteOrder)
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
    return when (type) {
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
            Pair(ULong.MIN_VALUE, computeFixAxis(axisDataType))
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
    var position1 = position
    val length = numberOfValues * (dataType.size * 2)
    val hexValues = mutableListOf<String>()
    val formattedValues = mutableListOf<Number>()
    var actualStart = Int.MIN_VALUE

    // Get the ID of the byte segment that contains our data
    val segmentId = getSegmentId(
        segments = segmentList,
        dataType = dataType,
        position = position
    )

    // Get Base Offset
    val baseOffset = if (segmentList.containsKey(segmentId)) {
        segmentList[segmentId]?.baseOffset ?: 0u
    } else 0u

    for (i in 0 until numberOfValues) {
        // Get the data from the Byte Segments and add it to the list
        val rawDataValues = getDataFromHexSegment(
            byteOrder = byteOrder,
            mHexData = segmentList[segmentId] ?: HexSegmentData(),
            position = position1.alignA2lAddress(recordLayout, dataType, a2lData),
            dataType = dataType
        )

        // Set actual start if it hasn't been already
        if (actualStart == Int.MIN_VALUE) {
            actualStart = rawDataValues.startPosition
        }

        // Add the Hex value to the list
        hexValues.add(
            rawDataValues.hexData
        )

        // Advance the position to the next section of data
        position1 += dataType.size.toULong()
    }

    // Format each value to its number based on its data type
    hexValues.forEach {
        formattedValues.add(
            it.formatValue(dataType)
        )
    }

    // Return our result
    return HexDataResult(
        startPosition = actualStart,
        endPosition = actualStart + length,
        hexData = hexValues,
        formattedValues = formattedValues,
        segmentId = segmentId,
        baseOffset = baseOffset
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
    var currentPosition = position.alignA2lAddress(this, dataType, a2lData)
    var numOfAxisPts = Int.MIN_VALUE

    val segmentId = getSegmentId(
        segments = segmentData,
        dataType = dataType,
        position = currentPosition
    )

    // Get value from segment
    val numberOfAxisPoints = getDataFromHexSegment(
        byteOrder,
        segmentData[segmentId] ?: HexSegmentData(),
        currentPosition,
        dataType
    ).hexData.formatValue(dataType)

    // If we've only returned one Byte, which we should have, set it as the number of axis points
    if (numberOfAxisPoints.toInt() != 0) {
        numOfAxisPts = numberOfAxisPoints.toInt()
    }

    // Advance position size of data
    currentPosition += (dataType.size).toULong()

    return Pair(currentPosition, numOfAxisPts)
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
    var currentPosition = position
    var numOfAxisPts = Int.MIN_VALUE

    val segmentId = getSegmentId(
        segments = segmentData,
        dataType = dataType,
        position = currentPosition
    )

    val numOfAxisResult = getDataFromHexSegment(
        byteOrder,
        segmentData[segmentId] ?: HexSegmentData(),
        currentPosition,
        dataType
    ).hexData.formatValue(dataType)

    // If we've only returned one Byte, which we should have, set it as the number of axis points
    if (numOfAxisResult.toInt() != 0) {
        numOfAxisPts = numOfAxisResult.toInt()
    }

    currentPosition += (dataType.size).toULong()

    // Skip reserved
    if (reserved.dataType != org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType.NOT_SET) {
        currentPosition += (reserved.dataType.size).toULong()
    }

    return Pair(currentPosition, numOfAxisPts)
}

fun AxisDataValues.getCharData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    position: ULong,
    characteristic: Characteristic,
    axisType: AxisType
): CharDataValues {
    var currentPosition = characteristic.address

    // Check if we should use the end position in case of Standard axis
    if (axisType == AxisType.STD_AXIS) {
        currentPosition = position
    }

    // Read Characteristic values from byte segment data
    return characteristic.readDataValues(
        axisXDim = values.size,
        axisYDim = 0,
        a2lData = a2lData,
        position = currentPosition,
        segmentData = segmentData
    )
}

fun AxisDataValues.getFixCharData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): CharDataValues {
    // Read Characteristic values from byte segment data
    return if (values.isNotEmpty()) {
        readFixCharacteristicData(values.size, characteristic, a2lData, segmentData)
    } else if (characteristic.type == CharacteristicType.CURVE) {
        val size = characteristic.getAxisDescrX().maxAxisPoints
        readFixCharacteristicData(size.toInt(), characteristic, a2lData, segmentData)
    } else {
        // TODO: Add better logic for maps and values?
        val size = characteristic.getAxisDescrX().maxAxisPoints
        readFixCharacteristicData(size.toInt(), characteristic, a2lData, segmentData)
    }
}