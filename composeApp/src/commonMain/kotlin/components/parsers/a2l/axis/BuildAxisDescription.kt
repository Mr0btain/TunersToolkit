@file:Suppress("SpellCheckingInspection")

package components.parsers.a2l.axis

import components.parsers.a2l.classes.data.AxisPointReference
import components.parsers.a2l.classes.data.CurveReferenceAxis
import components.parsers.a2l.classes.data.FixAxisPar
import components.parsers.a2l.classes.data.FixAxisParDist
import components.parsers.a2l.classes.data.FixAxisParList
import components.parsers.a2l.classes.data.Format
import components.parsers.a2l.classes.data.MaxGradient
import components.parsers.a2l.classes.data.PhysUnit
import components.parsers.a2l.classes.data.StepSize
import components.parsers.a2l.classes.enums.ReadOnly
import components.parsers.a2l.classes.data.parseAnnotation
import components.parsers.a2l.classes.data.parseExtendedLimits
import components.parsers.a2l.classes.enums.AxisType
import components.parsers.a2l.classes.enums.parseAxisType
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseDeposit
import components.parsers.a2l.classes.enums.parseMonotony
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of Strings and builds an AxisDescr from the options in the list
 * if it contains them
 */

fun buildAxisDescription(input: List<String>): AxisDescr {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 6) {
        var mType = AxisType.NOT_SET
        val mOptionalParameters = mutableMapOf<AxisDescrOpt, A2lOptional>()

        // Parse the input and set the values
        try {
            // Parse the separated lower end
            input.forEachIndexed { index, line ->
                when {
                    line.startsWith("/begin AXIS_DESC") -> {
                        val result = line.replace("/begin AXIS_DESCR", "").trim()
                        if (result.isNotBlank()) {
                            mType = result.parseAxisType()
                        }
                    }

                    line.startsWith("FORMAT") -> {
                        mOptionalParameters[AxisDescrOpt.Format] = Format(line.replace("FORMAT", "")
                            .replace("\"", "")
                            .trim())
                    }

                    line.startsWith("AXIS_PTS_REF") -> {
                        mOptionalParameters[AxisDescrOpt.ReferenceAxisPoint] = AxisPointReference(line.replace("AXIS_PTS_REF", "").trim())
                    }

                    line.startsWith("FIX_AXIS_PAR") -> {
                        val result = line.replace("FIX_AXIS_PAR", "")
                            .trim().split(" ").filter { it.isNotBlank() }
                        if (result.size == 3) {
                            mOptionalParameters[AxisDescrOpt.FixAxisPar] = FixAxisPar(
                                offset = result[0].toFloatOrNull() ?: Float.NaN,
                                shift = result[1].toFloatOrNull() ?: Float.NaN,
                                numberApo = result[2].toUIntOrNull() ?: UInt.MIN_VALUE
                            )
                        }
                    }

                    line.startsWith("FIX_AXIS_PAR_DIST") -> {
                        val result = line.replace("FIX_AXIS_PAR_DIST", "").trim().split(" ")
                        if (result.size >= 3) {

                            mOptionalParameters[AxisDescrOpt.FixAxisParDist] = FixAxisParDist(
                                offset = result[0].toFloatOrNull() ?: Float.NaN,
                                distance = result[1].toFloatOrNull() ?: Float.NaN,
                                numberApo = result[2].toUIntOrNull() ?: UInt.MIN_VALUE
                            )
                        }
                    }

                    line.startsWith("/begin FIX_AXIS_PAR_LIST") -> {
                        // fix_axis_par_list_block : BEGIN FIX_AXIS_PAR_LIST AxisPts_Values+=value* END FIX_AXIS_PAR_LIST;
                        // TODO: test fixaxisparlist
                        val subList = input.subList(index, input.size)
                        val mFixAxisParList = mutableListOf<String>()
                        subList.forEachIndexed { ind, lin ->
                            if (lin.startsWith("/end FIX_AXIS_PAR_LIST")) {
                                subList.subList(0, ind).joinToString(" ")
                                    .replace("/begin FIX_AXIS_PAR_LIST", "")
                                    .replace("/end FIX_AXIS_PAR_LIST", "").split(" ")
                                    .forEach {
                                        mFixAxisParList.add(it)
                                    }
                            }
                        }
                        mOptionalParameters[AxisDescrOpt.FixAxisParList] = FixAxisParList(mFixAxisParList)
                    }

                    line.startsWith("READ_ONLY") -> {
                        mOptionalParameters[AxisDescrOpt.ReadOnly] = ReadOnly.True
                    }

                    line.startsWith("MAX_GRAD") -> {
                        mOptionalParameters[AxisDescrOpt.MaxGradient] = MaxGradient(line
                            .replace("MAX_GRAD", "").trim()
                            .toDoubleOrNull() ?: Double.NaN)
                    }

                    line.startsWith("MONOTONY") -> {
                        mOptionalParameters[AxisDescrOpt.Monotony] = line
                            .replace("MONOTONY", "")
                            .trim().parseMonotony()
                    }

                    line.startsWith("BYTE_ORDER") -> {
                        mOptionalParameters[AxisDescrOpt.ByteOrder] = line
                            .replace("BYTE_ORDER", "")
                            .trim().parseByteOrder()
                    }

                    line.startsWith("EXTENDED_LIMITS") -> {
                        mOptionalParameters[AxisDescrOpt.ExtendedLimits] = line.parseExtendedLimits()
                    }

                    line.startsWith("CURVE_AXIS_REF") -> {
                        mOptionalParameters[AxisDescrOpt.ReferenceCurveAxis] = CurveReferenceAxis(line.replace("CURVE_AXIS_REF", "").trim())
                    }

                    line.startsWith("DEPOSIT") -> {
                        mOptionalParameters[AxisDescrOpt.Deposit] = line.replace("DEPOSIT", "").parseDeposit()
                    }

                    line.startsWith("PHYS_UNIT") -> {
                        mOptionalParameters[AxisDescrOpt.PhysUnit] = PhysUnit(line.replace("PHYS_UNIT", "").trim())
                    }

                    line.startsWith("STEP_SIZE") -> {
                        mOptionalParameters[AxisDescrOpt.StepSize] = StepSize(line
                            .replace("STEP_SIZE", "").trim()
                            .toFloatOrNull() ?: Float.NaN)
                    }

                    line.startsWith("/begin ANNOTATION") -> {
                        // TODO: Test Annotations
                        val subL = input.subList(index, input.size)
                        var annotList = listOf("")
                        var found = false
                        subL.forEachIndexed { ind, lin ->
                            when {
                                !found && lin.contains("/end ANNOTATION") -> {
                                    annotList = subL.subList(0, ind)
                                    found = true
                                }
                            }
                        }

                        mOptionalParameters[AxisDescrOpt.Annotations] = annotList.parseAnnotation()
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (mType != AxisType.NOT_SET) {
            AxisDescr(
                type = mType,
                inputQuantity = input[1].trim(),
                conversion = input[2].trim(),
                maxAxisPoints = (input[3].toUIntOrNull() ?: UInt.MIN_VALUE),
                lowerLimit = (input[4].toDoubleOrNull() ?: Double.NaN),
                upperLimit = (input[5].toDoubleOrNull() ?: Double.NaN),
                optionalParameters = mOptionalParameters
            )
        } else {
            AxisDescr(
                type = input[1].parseAxisType(),
                inputQuantity = input[2].trim(),
                conversion = input[3].trim(),
                maxAxisPoints = (input[4].toUIntOrNull() ?: UInt.MIN_VALUE),
                lowerLimit = (input[5].toDoubleOrNull() ?: Double.NaN),
                upperLimit = (input[6].toDoubleOrNull() ?: Double.NaN),
                optionalParameters = mOptionalParameters
            )
        }
    } else return AxisDescr()
}