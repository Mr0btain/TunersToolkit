package org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.axis
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisDescr
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getFixAxisPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getFixAxisParDist
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getFixAxisParList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.getFormat
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixAxisPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FixAxisParDist
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValue
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataType
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.AxisDataValues
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.helpers.formatLength

fun AxisDescr.computeFixAxis(axisType: AxisDataType): AxisDataValues {
    val result = mutableListOf<AxisDataValue>()
    val fixAxisPar = getFixAxisPar()
    val fixAxisParDist = getFixAxisParDist()
    val fixAxisParList = getFixAxisParList()

    when {
        // Check if FIX_AXIS_PAR is not empty
        fixAxisPar != FixAxisPar() -> {
            // Process values and add to result
            for (i in 0 until fixAxisPar.numberApo.toInt()) {
                val value = fixAxisPar.offset.toDouble() + (i shl fixAxisPar.shift.toInt())

                result.add(
                    AxisDataValue(
                        originalValue = value.toString(),
                        formattedValue = value.toString().formatLength(getFormat()),
                        position = i,
                        axis = axisType
                    )
                )
            }
        }

        // Check if FIX_AXIS_PAR_DIST is not empty
        fixAxisParDist != FixAxisParDist() -> {
            // Process values and add to result
            for (i in 0 until fixAxisParDist.numberApo.toInt()) {
                val value = fixAxisParDist.offset.toDouble() + (i * fixAxisParDist.distance)

                result.add(
                    AxisDataValue(
                        originalValue = value.toString(),
                        formattedValue = value.toString().formatLength(getFormat()),
                        position = i,
                        axis = axisType
                    )
                )
            }
        }

        // Check if FIX_AXIS_PAR_LIST is not empty
        fixAxisParList.isNotEmpty() -> {
            val list = mutableListOf<Double>()

            // Process values and add to result
            for (element in fixAxisParList) {
                val res = element.toDoubleOrNull()
                if (res != null) {
                    list.add(res)
                }
            }

            if (list.size == fixAxisParList.size) {
                var i = 0

                list.forEach {
                    result.add(
                        AxisDataValue(
                            originalValue = it.toString(),
                            formattedValue = it.toString().formatLength(getFormat()),
                            position = i,
                            axis = axisType
                        )
                    )
                    i++
                }
            }
        }
    }

    // Return the result
    return AxisDataValues(
        axisDataType = axisType,
        values = result,
        format = this.getFormat(),
        upperLimit = this.upperLimit,
        lowerLimit = this.lowerLimit,
        unit = "Fixed Axis"
    )
}