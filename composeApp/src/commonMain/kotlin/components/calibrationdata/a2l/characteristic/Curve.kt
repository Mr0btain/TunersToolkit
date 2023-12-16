package components.calibrationdata.a2l.characteristic

import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.classes.enums.AxisType
import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.getAxisDescrX
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.data.A2lData
import components.calibrationdata.a2l.axis.computeFixAxis
import components.calibrationdata.a2l.axis.readComAxisValues
import components.calibrationdata.a2l.axis.readCurveAxisValues
import components.calibrationdata.a2l.axis.readResAxisValues
import components.calibrationdata.a2l.axis.readStdAxisValues
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.a2l.helpers.getCharData
import components.calibrationdata.a2l.helpers.getFixCharData
import components.parsers.hex.HexSegmentData

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
    val axisDescription = characteristic.getAxisDescrX()

    // Depending on Axis type, get the values
    if (axisDescription != AxisDescr()) {
        // Check the Axis Type and read the data using the method for them
        return when (axisDescription.type) {
            // FIX or Fixed axis Point
            AxisType.FIX_AXIS -> {
                // Compute Axis values
                val mAxisResult = axisDescription.computeFixAxis(AxisDataType.X)

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
                val axisResult = readStdAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = axisDescription,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val charResult = axisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = axisResult.first,
                    characteristic = characteristic,
                    axisType = AxisType.STD_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = charResult,
                    axisValues = listOf(axisResult.second)
                )
            }

            // COM or Common Axis Point shared by multiple AXIS_DESC
            AxisType.COM_AXIS -> {
                // Read Axis values from byte segment data and format them
                val axisResult =  readComAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = axisDescription,
                    axisDataType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val charResult = axisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.COM_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = charResult,
                    axisValues = listOf(axisResult.second)
                )
            }

            AxisType.RES_AXIS -> {
                // TODO: Test
                // Read Axis values from byte segment data and format them
                val axisResult = readResAxisValues(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    axisDescr = axisDescription,
                    axisType = AxisDataType.X
                )

                // Read Characteristic values from byte segment data
                val charResult = axisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.RES_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = charResult,
                    axisValues = listOf(axisResult.second)
                )
            }

            // Curve
            AxisType.CURVE_AXIS -> {
                // TODO: Test
                // Get Curve Axis data from its reference Characteristic
                val axisResult = readCurveAxisValues(a2lData, segmentData, axisDescription)

                // Read Characteristic values from byte segment data
                val charResult = axisResult.second.getCharData(
                    a2lData = a2lData,
                    segmentData = segmentData,
                    position = characteristic.address,
                    characteristic = characteristic,
                    axisType = AxisType.CURVE_AXIS
                )

                MapDataValues(
                    name = characteristic.name,
                    mapType = CharacteristicType.CURVE,
                    charValues = charResult,
                    axisValues = listOf(axisResult.second)
                )
            }

            else -> { MapDataValues() }
        }
    }

    return MapDataValues()
}