package components.calibrationdata.a2l.axis

import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.axis.AxisPts
import components.parsers.a2l.axis.getByteOrder
import components.parsers.a2l.axis.getCompuMethod
import components.parsers.a2l.axis.getFormat
import components.parsers.a2l.axis.getRecordLayout
import components.parsers.a2l.axis.getRefAxisPoint
import components.parsers.a2l.classes.RecordLayoutAxisPoint
import components.parsers.a2l.classes.RecordLayoutAxisRescale
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.data.NoAxisPts
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.classes.HexDataResult
import components.parsers.hex.HexSegmentData
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.DataType
import components.parsers.a2l.classes.enums.IndexIncrement
import components.parsers.a2l.compumethod.compute
import components.parsers.a2l.compumethod.computeTabVerb
import components.parsers.a2l.recordlayout.RecordLayout
import components.parsers.a2l.recordlayout.getAxisPtsX
import components.parsers.a2l.recordlayout.getAxisRescaleX
import components.parsers.a2l.recordlayout.getNoAxisPtsX
import components.parsers.a2l.recordlayout.getReserved
import components.calibrationdata.a2l.helpers.getAxisRescaleData
import components.calibrationdata.a2l.helpers.getNoAxisPtsData
import components.calibrationdata.a2l.helpers.getValueData
import components.calibrationdata.a2l.helpers.sortAxisPosition

/**
 * Gets a COM axis' data from the ByteArray segment it is contained in
 */

fun readComAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr,
    axisDataType: AxisDataType,
): Pair<ULong, AxisDataValues> {
    val axisHexData: HexDataResult
    var axisPts = RecordLayoutAxisPoint()
    val refAxisPts = axisDescr.getRefAxisPoint(a2lData)
    val result = mutableListOf<AxisDataValue>()
    var byteOrder = axisDescr.getByteOrder()
    val compuMethod = axisDescr.getCompuMethod(a2lData)
    var dataType = DataType.NOT_SET
    val format = axisDescr.getFormat()
    var indexIncr = IndexIncrement.NOT_SET
    var numOfAxisPts = axisDescr.maxAxisPoints.toInt()
    val noAxisPts: NoAxisPts
    var position = ULong.MIN_VALUE
    val recordLayout = refAxisPts.getRecordLayout(a2lData)
    var reserved = recordLayout.getReserved()
    // var mSrcAddress = SrcAddress()

    // Check Axis point reference
    if (refAxisPts != AxisPts()) {
        // Get the address of the axis point data from reference axis point
        if (refAxisPts.address != ULong.MIN_VALUE) {
            position = refAxisPts.address
        }

        // Get Byte Order
        byteOrder = refAxisPts.getByteOrder()

        // Get RecordLayout Values for AXIS_PTS
        if (recordLayout != RecordLayout()) {
            // Get Reserved
            reserved = recordLayout.getReserved()

            // Get SrcAddress
            // mSrcAddress = mRecordLayout.getSrcAddrX()

            val recordLayoutAxisPts = recordLayout.getAxisPtsX()

            if (recordLayoutAxisPts != RecordLayoutAxisPoint()) {
                axisPts = recordLayoutAxisPts
            }
        }

        // If number of axis points hasn't been found, use it from reference axis point
        if (numOfAxisPts == 0 && refAxisPts.maxAxisPoints != UInt.MIN_VALUE) {
            numOfAxisPts = refAxisPts.maxAxisPoints.toInt()
        }
    }

    // Check if AXIS_PTS is not empty
    if (axisPts != RecordLayoutAxisPoint()) {

        // Set index order
        val tempAxisPts = recordLayout.getAxisPtsX()

        if (axisPts.indexIncrement != IndexIncrement.NOT_SET) {
            indexIncr = axisPts.indexIncrement
        } else if (tempAxisPts != RecordLayoutAxisPoint()) {
            if (tempAxisPts.indexIncrement != IndexIncrement.NOT_SET) {
                indexIncr =
                    tempAxisPts.indexIncrement
            }
        }

        // Set Data type
        dataType = axisPts.dataType

        // Get NO_AXIS_PTS
        noAxisPts = recordLayout.getNoAxisPtsX()

        // Check no axis pts
        if (noAxisPts != NoAxisPts()) {
            val dataResult = recordLayout.getNoAxisPtsData(
                byteOrder = byteOrder,
                position = position,
                dataType = noAxisPts.dataType,
                a2lData = a2lData,
                segmentData = segmentData
            )

            // Update position
            position = dataResult.first

            // Update number of axis points if it's not set
            if (numOfAxisPts == Int.MIN_VALUE) {
                numOfAxisPts = dataResult.second
            }
        }

    } else {
        // Get Rescale Axis
        val axisRescale = recordLayout.getAxisRescaleX()

        if (axisRescale != RecordLayoutAxisRescale()) {
            indexIncr = axisRescale.indexIncrement

            // Get Rescale Data
            val dataResult = axisRescale.getAxisRescaleData(
                position = position,
                segmentData = segmentData,
                reserved = reserved,
                byteOrder = byteOrder
            )

            // Set position
            position = dataResult.first

            // Set number of axis points if it hasn't been already
            if (numOfAxisPts == Int.MIN_VALUE) {
                numOfAxisPts = dataResult.second
            }

            // Set Data Type
            dataType = axisRescale.dataType
        }
    }

    // Get Values from Data
    axisHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = position,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = numOfAxisPts,
        recordLayout = recordLayout
    )

    // Convert values and add to array
    if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        axisHexData.formattedValues.forEachIndexed { index, formattedValue ->
            val physUnit = compuMethod.compute(formattedValue, a2lData)

            result.add(
                indexIncr.sortAxisPosition(
                    input = physUnit,
                    index = index,
                    axisType = axisDataType,
                    numOfAxisPts = numOfAxisPts,
                    format = format
                )
            )
        }
    } else {
        axisHexData.formattedValues.forEachIndexed { index, formattedValue ->
            val physUnit = compuMethod.computeTabVerb(formattedValue, a2lData)

            result.add(
                indexIncr.sortAxisPosition(
                    input = physUnit,
                    index = index,
                    axisType = axisDataType,
                    numOfAxisPts = numOfAxisPts,
                    format = format
                )
            )
        }
    }

    return Pair(
        position,
        AxisDataValues(
            axisDataType = axisDataType,
            byteSegmentId = axisHexData.segmentId,
            startPosition = axisHexData.startPosition,
            endPosition = axisHexData.endPosition,
            dataType = dataType,
            values = result,
            format = format,
            upperLimit = axisDescr.upperLimit,
            lowerLimit = axisDescr.lowerLimit,
            unit = compuMethod.unit
        )
    )
}
