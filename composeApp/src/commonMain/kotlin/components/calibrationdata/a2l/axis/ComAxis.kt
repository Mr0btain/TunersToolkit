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
    val mAxisHexData: HexDataResult
    var mAxisPts = RecordLayoutAxisPoint()
    val mAxisPtsRef = axisDescr.getRefAxisPoint(a2lData)
    val mResult = mutableListOf<AxisDataValue>()
    var mByteOrder = axisDescr.getByteOrder()
    val mCompuMethod = axisDescr.getCompuMethod(a2lData)
    var mDataType = DataType.NOT_SET
    val mFormat = axisDescr.getFormat()
    var mIndexIncr = IndexIncrement.NOT_SET
    var mNumOfAxisPts = axisDescr.maxAxisPoints.toInt()
    val mNoAxisPts: NoAxisPts
    var mPosition = ULong.MIN_VALUE
    val mRecordLayout = mAxisPtsRef.getRecordLayout(a2lData)
    var mReserved = mRecordLayout.getReserved()
    // var mSrcAddress = SrcAddress()

    // Check Axis point reference
    if (mAxisPtsRef != AxisPts()) {
        // Get the address of the axis point data from reference axis point
        if (mAxisPtsRef.address != ULong.MIN_VALUE) {
            mPosition = mAxisPtsRef.address
        }

        // Get Byte Order
        mByteOrder = mAxisPtsRef.getByteOrder()

        // Get RecordLayout Values for AXIS_PTS
        if (mRecordLayout != RecordLayout()) {
            // Get Reserved
            mReserved = mRecordLayout.getReserved()

            // Get SrcAddress
            // mSrcAddress = mRecordLayout.getSrcAddrX()

            val mRecordLayoutAxisPts = mRecordLayout.getAxisPtsX()

            if (mRecordLayoutAxisPts != RecordLayoutAxisPoint()) {
                mAxisPts = mRecordLayoutAxisPts
            }
        }

        // If number of axis points hasn't been found, use it from reference axis point
        if (mNumOfAxisPts == 0 && mAxisPtsRef.maxAxisPoints != UInt.MIN_VALUE) {
            mNumOfAxisPts = mAxisPtsRef.maxAxisPoints.toInt()
        }
    }

    // Check record layout for Values
    if (mAxisPts != RecordLayoutAxisPoint()) {

        // Set index order
        val mTempAxisPts = mRecordLayout.getAxisPtsX()

        if (mAxisPts.indexIncrement != IndexIncrement.NOT_SET) {
            mIndexIncr = mAxisPts.indexIncrement
        } else if (mTempAxisPts != RecordLayoutAxisPoint()) {
            if (mTempAxisPts.indexIncrement != IndexIncrement.NOT_SET) {
                mIndexIncr =
                    mTempAxisPts.indexIncrement
            }
        }

        // Set Data type
        mDataType = mAxisPts.dataType

        // Get NO_AXIS_PTS
        mNoAxisPts = mRecordLayout.getNoAxisPtsX()

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

    } else {
        // Get Rescale Axis
        val mAxisRescale = mRecordLayout.getAxisRescaleX()

        if (mAxisRescale != RecordLayoutAxisRescale()) {
            mIndexIncr = mAxisRescale.indexIncrement

            // Get Rescale Data
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

            // Set Data Type
            mDataType = mAxisRescale.dataType
        }
    }

    // Get Values from Data
    mAxisHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = mPosition,
        dataType = mDataType,
        byteOrder = mByteOrder,
        numberOfValues = mNumOfAxisPts,
        recordLayout = mRecordLayout
    )

    // Convert values and add to array
    if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
        mAxisHexData.formattedValues.forEachIndexed { index, result ->
            val mPhysUnit = mCompuMethod.compute(result, a2lData)

            mResult.add(
                mIndexIncr.sortAxisPosition(
                    input = mPhysUnit,
                    index = index,
                    axisType = axisDataType,
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
                    axisType = axisDataType,
                    numOfAxisPts = mNumOfAxisPts,
                    format = mFormat
                )
            )
        }
    }

    return Pair(
        mPosition,
        AxisDataValues(
            axisDataType = axisDataType,
            byteSegmentId = mAxisHexData.segmentId,
            startPosition = mAxisHexData.startPosition,
            endPosition = mAxisHexData.endPosition,
            dataType = mDataType,
            values = mResult,
            format = mFormat,
            upperLimit = axisDescr.upperLimit,
            lowerLimit = axisDescr.lowerLimit,
            unit = mCompuMethod.unit
        )
    )
}
