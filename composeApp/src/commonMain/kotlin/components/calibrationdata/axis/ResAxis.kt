package components.calibrationdata.axis

import components.a2l.axis.AxisDescr
import components.a2l.axis.AxisPts
import components.a2l.classes.RecordLayoutAxisPoint
import components.a2l.classes.RecordLayoutAxisRescale
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.NoAxisPts
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValues
import components.hex.HexSegmentData
import components.a2l.classes.enums.ConversionType
import components.a2l.classes.enums.DataType
import components.a2l.classes.enums.IndexIncrement
import components.a2l.recordlayout.RecordLayout
import components.calibrationdata.helpers.getAxisRescaleData
import components.calibrationdata.helpers.getNoAxisPtsData
import components.calibrationdata.helpers.getValueData
import components.calibrationdata.helpers.sortAxisPosition

fun readResAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr,
    axisType: AxisDataType
): Pair<ULong, AxisDataValues> {
    val mResult = mutableListOf<AxisDataValue>()
    val mAxisPtsRef = axisDescr.getRefAxisPoint(a2lData)
    var mByteOrder = axisDescr.getByteOrder()
    val mCompuMethod = axisDescr.getCompuMethod(a2lData)
    var mDataType = DataType.NOT_SET
    val mFormat = axisDescr.getFormat()
    var mIndexIncr = IndexIncrement.NOT_SET
    var mNumOfAxisPts = axisDescr.maxAxisPoints.toInt()
    var mPosition = ULong.MIN_VALUE
    val mRecordLayout = mAxisPtsRef.getRecordLayout(a2lData)
    var mReserved = mRecordLayout.getReserved()

    // Check Axis point reference
    if (mAxisPtsRef != AxisPts()) {

        // Set Byte Order
        mByteOrder = mAxisPtsRef.getByteOrder()

        // Get the address of the axis point data from reference axis point
        if (mAxisPtsRef.address != ULong.MIN_VALUE) {
            mPosition = mAxisPtsRef.address
        }

        // Get RecordLayout Values for AXIS_PTS
        if (mRecordLayout != RecordLayout()) {
            mDataType = mRecordLayout.getFncValues().dataType
            mReserved = mRecordLayout.getReserved()
        }


        // If number of axis points hasn't been found, use it from reference axis point
        if (mNumOfAxisPts == 0 && mAxisPtsRef.maxAxisPoints != UInt.MIN_VALUE) {
            mNumOfAxisPts = mAxisPtsRef.maxAxisPoints.toInt()
        }
    }

    // Get NO_AXIS_PTS
    val mNoAxisPts: NoAxisPts = mRecordLayout.getNoAxisPts(axisType)

    val mAxisPts = mRecordLayout.getAxisPts(axisType)
    val mAxisPtsX = mRecordLayout.getAxisPtsX()

    // Check record layout for FncValues
    if (mAxisPts != RecordLayoutAxisPoint()) {
        // Set index order
        if (mAxisPts.indexIncrement != IndexIncrement.NOT_SET) {
            mIndexIncr = mAxisPts.indexIncrement
        } else if (mAxisPtsX != RecordLayoutAxisPoint()) {
            if (mAxisPtsX.indexIncrement != IndexIncrement.NOT_SET
            ) {
                mIndexIncr =
                    mAxisPtsX.indexIncrement
            }
        }

        // Check no axis pts
        if (mNoAxisPts != NoAxisPts()) {
            val result = mRecordLayout.getNoAxisPtsData(
                byteOrder = mByteOrder,
                position = mPosition,
                dataType = mNoAxisPts.dataType,
                a2lData = a2lData,
                segmentData = segmentData
            )

            // Update position
            mPosition = result.first

            // Update number of axis points if it's not set
            if (mNumOfAxisPts == Int.MIN_VALUE) {
                mNumOfAxisPts = result.second
            }
        }

        // Set Data Type
        mDataType = mAxisPts.dataType

    } else {
        val mAxisRescale = mRecordLayout.getAxisRescale(axisType)

        if (mAxisRescale != RecordLayoutAxisRescale()) {
            mIndexIncr = mAxisRescale.indexIncrement
            val result = mAxisRescale.getAxisRescaleData(
                position = mPosition,
                segmentData = segmentData,
                reserved = mReserved,
                byteOrder = mByteOrder
            )

            // Set position
            mPosition = result.first

            // Set number of axis points if it hasn't been already
            if (mNumOfAxisPts == Int.MIN_VALUE) {
                mNumOfAxisPts = result.second
            }
        }
    }

    val mAxisHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = mPosition,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = mNumOfAxisPts,
        recordLayout = mRecordLayout
    )

    if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
        mAxisHexData.formattedValues.forEachIndexed { index, result ->
            val mPhysUnit = mCompuMethod.compute(result, a2lData)

            mResult.add(
                mIndexIncr.sortAxisPosition(
                    input = mPhysUnit,
                    index = index,
                    axisType = axisType,
                    numOfAxisPts = mNumOfAxisPts,
                    format = mFormat
                )
            )
        }
    } else {
        mAxisHexData.formattedValues.forEachIndexed { index, result ->
            val mPhysUnit = mCompuMethod.computeTabVerb(result, a2lData)

            mResult.add(
                mIndexIncr.sortAxisPosition(
                    input = mPhysUnit,
                    index = index,
                    axisType = axisType,
                    numOfAxisPts = mNumOfAxisPts,
                    format = mFormat
                )
            )
        }
    }

    return Pair(
        mPosition,
        AxisDataValues(
            axisDataType = axisType,
            byteSegmentId = mAxisHexData.segmentId,
            startPosition = mAxisHexData.startPosition,
            endPosition = mAxisHexData.endPosition,
            values = mResult,
            dataType = mDataType,
            format = mFormat,
            upperLimit = axisDescr.upperLimit,
            lowerLimit = axisDescr.lowerLimit,
            unit = mCompuMethod.unit
        )
    )
}