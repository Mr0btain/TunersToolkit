package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisDescr
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getCompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getFormat
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getRecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getRefAxisPoint
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutAxisPoint
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.RecordLayoutAxisRescale
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.NoAxisPts
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValue
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataType
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValues
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ConversionType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.IndexIncrement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.compute
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.computeTabVerb
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.RecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getAxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getAxisPtsX
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getAxisRescale
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getFncValues
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getNoAxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.getReserved
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.getAxisRescaleData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.getNoAxisPtsData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.getValueData
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.sortAxisPosition

fun readResAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr,
    axisType: AxisDataType
): Pair<ULong, AxisDataValues> {
    val result = mutableListOf<AxisDataValue>()
    val refAxisPts = axisDescr.getRefAxisPoint(a2lData)
    var byteOrder = axisDescr.getByteOrder()
    val compuMethod = axisDescr.getCompuMethod(a2lData)
    var dataType = DataType.NOT_SET
    val format = axisDescr.getFormat()
    var indexIncr = IndexIncrement.NOT_SET
    var numOfAxisPts = axisDescr.maxAxisPoints.toInt()
    var position = ULong.MIN_VALUE
    val recordLayout = refAxisPts.getRecordLayout(a2lData)
    var reserved = recordLayout.getReserved()

    // Check if REF_AXIS_PTS is not empty
    if (refAxisPts != AxisPts()) {

        // Set Byte Order
        byteOrder = refAxisPts.getByteOrder()

        // Get the address of the axis point data from reference axis point
        if (refAxisPts.address != ULong.MIN_VALUE) {
            position = refAxisPts.address
        }

        // Get RecordLayout Values for AXIS_PTS
        if (recordLayout != RecordLayout()) {
            dataType = recordLayout.getFncValues().dataType
            reserved = recordLayout.getReserved()
        }


        // If number of axis points hasn't been found, use it from reference axis point
        if (numOfAxisPts == 0 && refAxisPts.maxAxisPoints != UInt.MIN_VALUE) {
            numOfAxisPts = refAxisPts.maxAxisPoints.toInt()
        }
    }

    // Get NO_AXIS_PTS
    val recordLayoutNoAxisPts: NoAxisPts = recordLayout.getNoAxisPts(axisType)

    // Get AXIS_PTS and AXIS_PTS_X
    val recordLayoutAxisPts = recordLayout.getAxisPts(axisType)
    val recordLayoutAxisPtsX = recordLayout.getAxisPtsX()

    // Check if RECORD_LAYOUT_AXIS_PTS is not empty
    if (recordLayoutAxisPts != RecordLayoutAxisPoint()) {
        // Set index order
        if (recordLayoutAxisPts.indexIncrement != IndexIncrement.NOT_SET) {
            indexIncr = recordLayoutAxisPts.indexIncrement
        } else if (recordLayoutAxisPtsX != RecordLayoutAxisPoint()) {
            if (recordLayoutAxisPtsX.indexIncrement != IndexIncrement.NOT_SET
            ) {
                indexIncr =
                    recordLayoutAxisPtsX.indexIncrement
            }
        }

        // Check if RECORD_LAYOUT_NO_AXIS_PTS is not empty
        if (recordLayoutNoAxisPts != NoAxisPts()) {
            val data = recordLayout.getNoAxisPtsData(
                byteOrder = byteOrder,
                position = position,
                dataType = recordLayoutNoAxisPts.dataType,
                a2lData = a2lData,
                segmentData = segmentData
            )

            // Update position
            position = data.first

            // Update number of axis points if it's not set
            if (numOfAxisPts == Int.MIN_VALUE) {
                numOfAxisPts = data.second
            }
        }

        // Set Data Type
        dataType = recordLayoutAxisPts.dataType

    } else {
        // If RECORD_LAYOUT_AXIS_PTS is empty, then continue and check rescale
        val recordLayoutAxisRescale = recordLayout.getAxisRescale(axisType)

        if (recordLayoutAxisRescale != RecordLayoutAxisRescale()) {
            indexIncr = recordLayoutAxisRescale.indexIncrement
            val axisRescaleData = recordLayoutAxisRescale.getAxisRescaleData(
                position = position,
                segmentData = segmentData,
                reserved = reserved,
                byteOrder = byteOrder
            )

            // Set position
            position = axisRescaleData.first

            // Set number of axis points if it hasn't been already
            if (numOfAxisPts == Int.MIN_VALUE) {
                numOfAxisPts = axisRescaleData.second
            }
        }
    }

    // Pull the Hex data
    val axisHexData = getValueData(
        a2lData = a2lData,
        segmentList = segmentData,
        position = position,
        dataType = dataType,
        byteOrder = byteOrder,
        numberOfValues = numOfAxisPts,
        recordLayout = recordLayout
    )

    // Check conversion type, compute, and sort values
    if (compuMethod.conversionType != ConversionType.TAB_VERB) {
        axisHexData.formattedValues.forEachIndexed { index, formattedValue ->
            val physUnit = compuMethod.compute(formattedValue, a2lData)

            result.add(
                indexIncr.sortAxisPosition(
                    input = physUnit,
                    index = index,
                    axisType = axisType,
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
                    axisType = axisType,
                    numOfAxisPts = numOfAxisPts,
                    format = format
                )
            )
        }
    }

    // Return the result
    return Pair(
        position,
        AxisDataValues(
            axisDataType = axisType,
            byteSegmentId = axisHexData.segmentId,
            startPosition = axisHexData.startPosition,
            endPosition = axisHexData.endPosition,
            values = result,
            dataType = dataType,
            format = format,
            upperLimit = axisDescr.upperLimit,
            lowerLimit = axisDescr.lowerLimit,
            unit = compuMethod.unit
        )
    )
}