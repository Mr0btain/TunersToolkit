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
    val mCharacteristic = axisDescr.getReferenceCurveAxis(a2lData)

    if (mCharacteristic != Characteristic()) {
        val mRefAxisDescr = mCharacteristic.getAxisDescrX()

        if (mRefAxisDescr != AxisDescr()) {
            when (mRefAxisDescr.type) {
                AxisType.STD_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readStdAxisValues(
                        a2lData,
                        segmentData,
                        mRefAxisDescr,
                        mCharacteristic.address,
                        mCharacteristic,
                        AxisDataType.X
                    )
                }

                AxisType.COM_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readComAxisValues(
                        a2lData,
                        segmentData,
                        mRefAxisDescr,
                        AxisDataType.X
                    )
                }

                AxisType.FIX_AXIS -> {
                    // Compute Axis values
                    return Pair(ULong.MIN_VALUE, mRefAxisDescr.computeFixAxis(AxisDataType.X))
                }

                AxisType.CURVE_AXIS -> {
                    // Read Axis values using ref axis
                    return readCurveAxisValues(a2lData, segmentData, mRefAxisDescr)
                }

                AxisType.RES_AXIS -> {
                    // Read Axis values from byte segment data and format them
                    return readResAxisValues(
                        a2lData,
                        segmentData,
                        mRefAxisDescr,
                        AxisDataType.X
                    )
                }

                AxisType.NOT_SET -> {
                    return Pair(ULong.MIN_VALUE, AxisDataValues())
                }
            }
        }
    }

    return Pair(ULong.MIN_VALUE, AxisDataValues())
}