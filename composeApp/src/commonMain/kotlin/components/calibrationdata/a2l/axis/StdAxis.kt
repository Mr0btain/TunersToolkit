package components.calibrationdata.a2l.axis

import components.calibrationdata.classes.HexDataResult
import components.calibrationdata.a2l.helpers.alignA2lAddress
import components.calibrationdata.a2l.helpers.getValueData
import components.parsers.hex.HexSegmentData
import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.axis.getByteOrder
import components.parsers.a2l.axis.getCompuMethod
import components.parsers.a2l.axis.getDeposit
import components.parsers.a2l.axis.getFormat
import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getRecordLayout
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.data.SrcAddress
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.DataType
import components.parsers.a2l.classes.enums.Deposit
import components.parsers.a2l.classes.enums.IndexIncrement
import components.parsers.a2l.compumethod.compute
import components.parsers.a2l.compumethod.computeTabVerb
import components.parsers.a2l.recordlayout.getAxisPts
import components.parsers.a2l.recordlayout.getNoAxisPts
import components.parsers.a2l.recordlayout.getSrcAddr
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.a2l.helpers.formatValue
import components.calibrationdata.a2l.helpers.getDataFromHexSegment
import components.calibrationdata.a2l.helpers.sortAxisPosition
import components.parsers.hex.getSegmentId

fun readStdAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr,
    position: ULong,
    characteristic: Characteristic,
    axisType: AxisDataType
): Pair<ULong, AxisDataValues> {
    // TODO: Deposit
    val axisHexData: HexDataResult
    val byteOrder = axisDescr.getByteOrder()
    val compuMethod = axisDescr.getCompuMethod(a2lData)
    val dataType: DataType
    val deposit = axisDescr.getDeposit()
    val format = axisDescr.getFormat()
    val indexIncr: IndexIncrement
    val numOfAxisPts: Int
    var position1 = position
    val recordLayout = characteristic.getRecordLayout(a2lData)
    val result = mutableListOf<AxisDataValue>()
    val srcAddress = recordLayout.getSrcAddr(axisType)
    val axisPts = recordLayout.getAxisPts(axisType)
    val noAxisPts = recordLayout.getNoAxisPts(axisType)

    if (characteristic.type == CharacteristicType.MAP) {
        dataType = if (noAxisPts.dataType != DataType.Default) {
            noAxisPts.dataType
        } else { DataType.Default }

        // Check for SRC_ADDR
        if (srcAddress != SrcAddress()) {
            position1 += srcAddress.dataType.size.toULong()
        }

        // Get the ID of the byte segment that contains our data
        // TODO: All our data *should* be in the same segment maybe add some logic to make sure?
        val segmentId = getSegmentId(
            segments = segmentData,
            dataType = dataType,
            position = position1
        )

        position1 = position1.alignA2lAddress(recordLayout, dataType, a2lData)

        numOfAxisPts = getDataFromHexSegment(
            byteOrder,
            segmentData[segmentId] ?: HexSegmentData(),
            position1,
            dataType
        ).hexData.formatValue(dataType).toInt()

        position1 += (dataType.size).toULong()

        // Get Axis values from Byte data
        axisHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            position = position1,
            dataType = dataType,
            byteOrder = byteOrder,
            numberOfValues = numOfAxisPts,
            recordLayout = recordLayout
        )

        indexIncr = axisPts.indexIncrement

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
    } else {
        // Set Data Type
        dataType = if (axisPts.dataType != DataType.Default) {
             axisPts.dataType
        } else DataType.Default

        // Set Index Increment
        indexIncr = axisPts.indexIncrement

        // Check for SRC_ADDR
        if (srcAddress != SrcAddress()) {
            position1 += srcAddress.dataType.size.toULong()
        }

        // Get the ID of the byte segment that contains our data
        // TODO: All our data *should* be in the same segment maybe add some logic to make sure?
        val segmentId = getSegmentId(
            segments = segmentData,
            dataType = dataType,
            position = position1
        )

        position1 = position1.alignA2lAddress(recordLayout, dataType, a2lData)

        numOfAxisPts = getDataFromHexSegment(
            byteOrder,
            segmentData[segmentId] ?: HexSegmentData(),
            position1,
            dataType
        ).hexData.formatValue(dataType).toInt()

        position1 += (dataType.size).toULong()

        // Get Axis values from Byte data
        axisHexData = getValueData(
            a2lData = a2lData,
            segmentList = segmentData,
            position = position1,
            dataType = dataType,
            byteOrder = byteOrder,
            numberOfValues = numOfAxisPts,
            recordLayout = recordLayout
        )

        // Convert Axis values
        if (compuMethod.conversionType != ConversionType.TAB_VERB) {
            var physValue = 0.0
            var predValue = 0.0

            axisHexData.formattedValues.forEachIndexed { index, formattedValue ->
                if (deposit != Deposit.DIFFERENCE) {
                    physValue = compuMethod.compute(formattedValue, a2lData)
                } else {
                    if (index == 0) {
                        physValue = compuMethod.compute(formattedValue, a2lData)
                        predValue = physValue
                    }
                    if (index > 0 && index < numOfAxisPts - 1) {
                        physValue = compuMethod.compute(formattedValue, a2lData) + predValue
                        predValue = physValue
                    }
                    if (index == numOfAxisPts - 1) {
                        physValue = compuMethod.compute(formattedValue, a2lData) + predValue
                        predValue = physValue
                    }
                }

                result.add(
                    indexIncr.sortAxisPosition(
                        input = physValue,
                        index = index,
                        axisType = axisType,
                        numOfAxisPts = numOfAxisPts,
                        format = format
                    )
                )
            }
        } else {
            axisHexData.formattedValues.forEachIndexed { index, formattedValue ->
                val physValue = compuMethod.computeTabVerb(formattedValue, a2lData)

                result.add(
                    indexIncr.sortAxisPosition(
                        input = physValue,
                        index = index,
                        axisType = axisType,
                        numOfAxisPts = numOfAxisPts,
                        format = format
                    )
                )
            }
        }
    }

    return Pair(
        position1,
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