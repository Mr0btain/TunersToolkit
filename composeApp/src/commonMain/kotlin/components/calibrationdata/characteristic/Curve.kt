package components.calibrationdata.characteristic

import components.a2l.axis.AxisDescr
import components.a2l.classes.enums.AxisType
import components.a2l.characteristic.Characteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.classes.data.A2lData
import components.calibrationdata.axis.computeFixAxis
import components.calibrationdata.axis.readComAxisValues
import components.calibrationdata.axis.readCurveAxisValues
import components.calibrationdata.axis.readResAxisValues
import components.calibrationdata.axis.readStdAxisValues
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.helpers.getCharData
import components.calibrationdata.helpers.getFixCharData
import components.hex.HexSegmentData

/**
 * Gets data for a CURVE type Characteristic
 * @return Pair of ByteArray First being AxisData, Second being Characteristic data
 */

fun readCurveCharacteristicData(
    a2lData: A2lData,
    segmentData: Map<String, HexSegmentData>,
    characteristic: Characteristic
): MapDataValues {
    // Get Axis Description
    val mAxisDescription = characteristic.getAxisDescrX()

    // Depending on Axis type, get the values
    if (mAxisDescription != AxisDescr()) {
        // Check the Axis Type and read the data using the method for them
        return when (mAxisDescription.type) {
            // FIX or Fixed axis Point
            AxisType.FIX_AXIS -> {
                // Compute Axis values
                val mAxisResult = mAxisDescription.computeFixAxis(AxisDataType.X)

                // Read Characteristic values from byte segment data
                val mCharResult = mAxisResult.getFixCharData(a2lData, segmentData, characteristic)

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = mCharResult,
                    axisValues = listOf(mAxisResult)
                )
            }

            // Standard Axis
            AxisType.STD_AXIS -> {
                // Read Axis values from byte segment data and format them
                val mAxisResult = readStdAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = mAxisDescription,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val mCharResult = mAxisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = mAxisResult.first,
                    characteristic = characteristic,
                    axisType = AxisType.STD_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = mCharResult,
                    axisValues = listOf(mAxisResult.second)
                )
            }

            // COM or Common Axis Point shared by multiple AXIS_DESC
            AxisType.COM_AXIS -> {
                // Read Axis values from byte segment data and format them
                val mAxisResult =  readComAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = mAxisDescription,
                    axisDataType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val mCharResult = mAxisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.COM_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = mCharResult,
                    axisValues = listOf(mAxisResult.second)
                )
            }

            AxisType.RES_AXIS -> {
                // TODO: Test
                // Read Axis values from byte segment data and format them
                val mAxisResult =  readResAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = mAxisDescription,
                    axisType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val mCharResult = mAxisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.RES_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = mCharResult,
                    axisValues = listOf(mAxisResult.second)
                )
            }

            // Curve
            AxisType.CURVE_AXIS -> {
                // TODO: Test
                // Get Curve Axis data from its reference Characteristic
                val mAxisResult = readCurveAxisValues(a2lData, segmentData, mAxisDescription)

                // Read Characteristic values from byte segment data
                val mCharResult = mAxisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.CURVE_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = mCharResult,
                    axisValues = listOf(mAxisResult.second)
                )
            }

            else -> { MapDataValues() }
        }
    }

    return MapDataValues()
}