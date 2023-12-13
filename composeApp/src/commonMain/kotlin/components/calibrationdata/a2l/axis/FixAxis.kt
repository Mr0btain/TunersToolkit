package components.calibrationdata.a2l.axis

import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.axis.getFixAxisPar
import components.parsers.a2l.axis.getFixAxisParDist
import components.parsers.a2l.axis.getFixAxisParList
import components.parsers.a2l.axis.getFormat
import components.parsers.a2l.classes.data.FixAxisPar
import components.parsers.a2l.classes.data.FixAxisParDist
import components.calibrationdata.classes.AxisDataValue
import components.calibrationdata.classes.AxisDataType
import components.calibrationdata.classes.AxisDataValues
import components.calibrationdata.a2l.helpers.formatLength

fun AxisDescr.computeFixAxis(axisType: AxisDataType): AxisDataValues {
    val mResult = mutableListOf<AxisDataValue>()
    val mFixAxisPar = getFixAxisPar()
    val mFixAxisParDist = getFixAxisParDist()
    val mFixAxisParList = getFixAxisParList()

    when {
        // Fix Axis Par
        mFixAxisPar != FixAxisPar() -> {
            for (i in 0 until mFixAxisPar.numberApo.toInt()) {
                val result = mFixAxisPar.offset.toDouble() + (i shl mFixAxisPar.shift.toInt())

                mResult.add(
                    AxisDataValue(
                        originalValue = result.toString(),
                        formattedValue = result.toString().formatLength(getFormat()),
                        position = i,
                        axis = axisType
                    )
                )
            }
        }

        // Fix Axis Par Dist
        mFixAxisParDist != FixAxisParDist() -> {
            for (i in 0 until mFixAxisParDist.numberApo.toInt()) {
                val result = mFixAxisParDist.offset.toDouble() + (i * mFixAxisParDist.distance)

                mResult.add(
                    AxisDataValue(
                        originalValue = result.toString(),
                        formattedValue = result.toString().formatLength(getFormat()),
                        position = i,
                        axis = axisType
                    )
                )
            }
        }

        // Fix Axis Par List
        mFixAxisParList.isNotEmpty() -> {
            val list = mutableListOf<Double>()

            for (element in mFixAxisParList) {
                val res = element.toDoubleOrNull()
                if (res != null) {
                    list.add(res)
                }
            }

            if (list.size == mFixAxisParList.size) {
                var i = 0

                list.forEach {
                    mResult.add(
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

    return AxisDataValues(
        axisDataType = axisType,
        values = mResult,
        format = this.getFormat(),
        upperLimit = this.upperLimit,
        lowerLimit = this.lowerLimit,
        unit = "Fixed Axis"
    )
}