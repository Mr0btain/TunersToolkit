package components.calibrationdata.a2l.axis

import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.axis.getReferenceCurveAxis
import components.parsers.a2l.classes.enums.AxisType
import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getAxisDescrX
import components.parsers.a2l.classes.data.A2lData
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValues
import components.parsers.hex.HexSegmentData

fun readCurveAxisValues(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    axisDescr: AxisDescr
): Pair<ULong, AxisDataValues> {
    val characteristic = axisDescr.getReferenceCurveAxis(a2lData)

    // Check if CHARACTERISTIC is not empty
    if (characteristic != Characteristic()) {
        val refAxisDescr = characteristic.getAxisDescrX()

        // Check if the reference AXIS_DESC is not empty
        if (refAxisDescr != AxisDescr()) {
            // Get values dependent on type
            when (refAxisDescr.type) {
                AxisType.STD_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readStdAxisValues(
                        a2lData,
                        segmentData,
                        refAxisDescr,
                        characteristic.address,
                        characteristic,
                        AxisDataType.X
                    )
                }

                AxisType.COM_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readComAxisValues(
                        a2lData,
                        segmentData,
                        refAxisDescr,
                        AxisDataType.X
                    )
                }

                AxisType.FIX_AXIS -> {
                    // Compute Axis values
                    return Pair(ULong.MIN_VALUE, refAxisDescr.computeFixAxis(AxisDataType.X))
                }

                AxisType.CURVE_AXIS -> {
                    // Read Axis values using ref axis
                    return readCurveAxisValues(a2lData, segmentData, refAxisDescr)
                }

                AxisType.RES_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readResAxisValues(
                        a2lData,
                        segmentData,
                        refAxisDescr,
                        AxisDataType.X
                    )
                }

                // If not type is set, return empty value
                AxisType.NOT_SET -> {
                    return Pair(ULong.MIN_VALUE, AxisDataValues())
                }
            }
        }
    }

    return Pair(ULong.MIN_VALUE, AxisDataValues())
}