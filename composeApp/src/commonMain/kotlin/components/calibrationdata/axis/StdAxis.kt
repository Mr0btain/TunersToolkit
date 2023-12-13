package components.calibrationdata.axis

import components.calibrationdata.classes.HexDataResult
import components.calibrationdata.helpers.alignA2lAddress
import components.calibrationdata.helpers.getValueData
import components.hex.HexSegmentData
import components.a2l.axis.AxisDescr
import components.a2l.characteristic.Characteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.classes.data.SrcAddress
import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.ConversionType
import components.a2l.classes.enums.DataType
import components.a2l.classes.enums.Deposit
import components.a2l.classes.enums.IndexIncrement
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.helpers.formatValue
import components.calibrationdata.helpers.getDataFromHexSegment
import components.calibrationdata.helpers.sortAxisPosition
import components.hex.getSegmentId

fun readStdAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr,
    position: ULong,
    characteristic: Characteristic,
    axisType: AxisDataType
): Pair<ULong, AxisDataValues> {
    // TODO: Deposit
    val mAxisHexData: HexDataResult
    val mByteOrder = axisDescr.getByteOrder()
    val mCompuMethod = axisDescr.getCompuMethod(a2lData)
    val mDataType: DataType
    val mDeposit = axisDescr.getDeposit()
    val mFormat = axisDescr.getFormat()
    val mIndexIncr: IndexIncrement
    val mNumOfAxisPts: Int
    var mPosition = position
    val mRecordLayout = characteristic.getRecordLayout(a2lData)
    val mResult = mutableListOf<AxisDataValue>()
    val mSrcAddress = mRecordLayout.getSrcAddr(axisType)
    val mAxisPts = mRecordLayout.getAxisPts(axisType)
    val mNoAxisPts = mRecordLayout.getNoAxisPts(axisType)

    if (characteristic.type == CharacteristicType.MAP) {
        mDataType = if (mNoAxisPts.dataType != DataType.Default) {
            mNoAxisPts.dataType
        } else { DataType.Default }

        // Check for SRC_ADDR
        if (mSrcAddress != SrcAddress()) {
            mPosition += mSrcAddress.dataType.size.toULong()
        }

        // Get the ID of the byte segment that contains our data
        // TODO: All our data *should* be in the same segment maybe add some logic to make sure?
        val segmentId = getSegmentId(
            segments = segmentData,
            dataType = mDataType,
            position = mPosition
        )

        mPosition = mPosition.alignA2lAddress(mRecordLayout, mDataType, a2lData)

        mNumOfAxisPts = getDataFromHexSegment(
            mByteOrder,
            segmentData[segmentId] ?: HexSegmentData(),
            mPosition,
            mDataType
        ).hexData.formatValue(mDataType).toInt()

        mPosition += (mDataType.size).toULong()

        // Get Axis values from Byte data
        mAxisHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            position = mPosition,
            dataType = mDataType,
            byteOrder = mByteOrder,
            numberOfValues = mNumOfAxisPts,
            recordLayout = mRecordLayout
        )

        mIndexIncr = mAxisPts.indexIncrement

        if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
            mAxisHexData.formattedValues.forEachIndexed { index, result ->
                val mPhysUnit = mCompuMethod.compute(result, a2lData, mFormat)

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
    } else {
        // Set Data Type
        mDataType = if (mAxisPts.dataType != DataType.Default) {
             mAxisPts.dataType
        } else DataType.Default

        // Set Index Increment
        mIndexIncr = mAxisPts.indexIncrement

        // Check for SRC_ADDR
        if (mSrcAddress != SrcAddress()) {
            mPosition += mSrcAddress.dataType.size.toULong()
        }

        // Get the ID of the byte segment that contains our data
        // TODO: All our data *should* be in the same segment maybe add some logic to make sure?
        val segmentId = getSegmentId(
            segments = segmentData,
            dataType = mDataType,
            position = mPosition
        )

        mPosition = mPosition.alignA2lAddress(mRecordLayout, mDataType, a2lData)

        mNumOfAxisPts = getDataFromHexSegment(
            mByteOrder,
            segmentData[segmentId] ?: HexSegmentData(),
            mPosition,
            mDataType
        ).hexData.formatValue(mDataType).toInt()

        mPosition += (mDataType.size).toULong()

        // Get Axis values from Byte data
        mAxisHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            position = mPosition,
            dataType = mDataType,
            byteOrder = mByteOrder,
            numberOfValues = mNumOfAxisPts,
            recordLayout = mRecordLayout
        )

        // Convert Axis values
        if (mCompuMethod.conversionType != ConversionType.TAB_VERB) {
            var mPhysValue = 0.0
            var mPredValue = 0.0

            mAxisHexData.formattedValues.forEachIndexed { index, result ->
                if (mDeposit != Deposit.DIFFERENCE) {
                    mPhysValue = mCompuMethod.compute(result, a2lData, mFormat)
                } else {
                    if (index == 0) {
                        mPhysValue = mCompuMethod.compute(result, a2lData, mFormat)
                        mPredValue = mPhysValue
                    }
                    if (index > 0 && index < mNumOfAxisPts - 1) {
                        mPhysValue = mCompuMethod.compute(result, a2lData, mFormat) + mPredValue
                        mPredValue = mPhysValue
                    }
                    if (index == mNumOfAxisPts - 1) {
                        mPhysValue = mCompuMethod.compute(result, a2lData, mFormat) + mPredValue
                        mPredValue = mPhysValue
                    }
                }

                mResult.add(
                    mIndexIncr.sortAxisPosition(
                        input = mPhysValue,
                        index = index,
                        axisType = axisType,
                        numOfAxisPts = mNumOfAxisPts,
                        format = mFormat
                    )
                )
            }
        } else {
            mAxisHexData.formattedValues.forEachIndexed { index, result ->
                val mPhysValue = mCompuMethod.computeTabVerb(result, a2lData)

                mResult.add(
                    mIndexIncr.sortAxisPosition(
                        input = mPhysValue,
                        index = index,
                        axisType = axisType,
                        numOfAxisPts = mNumOfAxisPts,
                        format = mFormat
                    )
                )
            }
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