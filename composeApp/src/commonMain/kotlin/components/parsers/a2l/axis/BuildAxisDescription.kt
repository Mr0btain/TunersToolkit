@file:Suppress("SpellCheckingInspection")

package components.parsers.a2l.axis

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.AxisPointReference
import components.parsers.a2l.classes.data.CurveReferenceAxis
import components.parsers.a2l.classes.data.FixAxisPar
import components.parsers.a2l.classes.data.FixAxisParDist
import components.parsers.a2l.classes.data.Format
import components.parsers.a2l.classes.data.MaxGradient
import components.parsers.a2l.classes.data.PhysUnit
import components.parsers.a2l.classes.data.StepSize
import components.parsers.a2l.classes.data.parseAnnotation
import components.parsers.a2l.classes.data.parseExtendedLimits
import components.parsers.a2l.classes.enums.AxisType
import components.parsers.a2l.classes.enums.Monotony
import components.parsers.a2l.classes.enums.ReadOnly
import components.parsers.a2l.classes.enums.parseAxisType
import components.parsers.a2l.classes.enums.parseByteOrder
import components.parsers.a2l.classes.enums.parseDeposit
import components.parsers.a2l.classes.enums.parseMonotony
import components.parsers.a2l.helpers.parseItemsFromList
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses a list of Strings and builds an AxisDescr from the options in the list
 * if it contains them
 */

fun buildAxisDescription(input: List<String>): Pair<AxisDescr, List<LogDataEntry>> {
    val errors = mutableListOf<LogDataEntry>()
    val process = "BuildAxisDescription"

    safeCall {
        var type = AxisType.NOT_SET
        val optionalParameters = mutableMapOf<AxisDescrOpt, A2lOptional>()

        // Parse the separated lower end
        input.forEachIndexed { index, line ->
            when {
                line.startsWith("/begin AXIS_DESC") -> {
                    val result = line.replace("/begin AXIS_DESCR", "").trim()
                    if (result.isNotBlank()) {
                        type = result.parseAxisType()
                    }
                }

                line.startsWith("FORMAT") -> {
                    optionalParameters[AxisDescrOpt.Format] = Format(line.replace("FORMAT", "")
                        .replace("\"", "")
                        .trim())
                }

                line.startsWith("AXIS_PTS_REF") -> {
                    optionalParameters[AxisDescrOpt.ReferenceAxisPoint] = AxisPointReference(
                        line.replace("AXIS_PTS_REF", "").trim()
                    )
                }

                line.startsWith("FIX_AXIS_PAR") -> {
                    safeCall {
                        val result = line.replace("FIX_AXIS_PAR", "")
                            .trim().split(" ").filter { it.isNotBlank() }

                        optionalParameters[AxisDescrOpt.FixAxisPar] = FixAxisPar(
                            offset = result[0].toFloat(),
                            shift = result[1].toFloat(),
                            numberApo = result[2].toUInt()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error at FixAxisPar! ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("FIX_AXIS_PAR_DIST") -> {
                    safeCall {
                        val result = line.replace("FIX_AXIS_PAR_DIST", "").trim().split(" ")

                        optionalParameters[AxisDescrOpt.FixAxisParDist] = FixAxisParDist(
                            offset = result[0].toFloat(),
                            distance = result[1].toFloat(),
                            numberApo = result[2].toUInt()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error building FixAxisParDist ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("/begin FIX_AXIS_PAR_LIST") -> {
                    // fix_axis_par_list_block : BEGIN FIX_AXIS_PAR_LIST AxisPts_Values+=value* END FIX_AXIS_PAR_LIST;
                    // TODO: test fixaxisparlist
                    safeCall {
                        parseItemsFromList(
                            input = input.subList(index, input.size),
                            startingId = "/begin FIX_AXIS_PAR_LIST",
                            endId = "/end FIX_AXIS_PAR_LIST"
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error at FixAxisParList ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("READ_ONLY") -> {
                    optionalParameters[AxisDescrOpt.ReadOnly] = ReadOnly.True
                }

                line.startsWith("MAX_GRAD") -> {
                    safeCall {
                        optionalParameters[AxisDescrOpt.MaxGradient] = MaxGradient(
                            line.replace("MAX_GRAD", "")
                                .trim()
                                .toDouble()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error at MaxGrad ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("MONOTONY") -> {
                    val mon = line
                        .replace("MONOTONY", "")
                        .trim()
                        .parseMonotony()

                    if (mon == Monotony.NOT_SET) {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error parsing Monotony!",
                                error = true
                            )
                        )
                    } else {
                        optionalParameters[AxisDescrOpt.Monotony] = mon
                    }
                }

                line.startsWith("BYTE_ORDER") -> {
                    optionalParameters[AxisDescrOpt.ByteOrder] = line
                        .replace("BYTE_ORDER", "")
                        .trim()
                        .parseByteOrder()
                }

                line.startsWith("EXTENDED_LIMITS") -> {
                    safeCall {
                        optionalParameters[AxisDescrOpt.ExtendedLimits] = line.parseExtendedLimits()
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error parsing ExtendedLimits!: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("CURVE_AXIS_REF") -> {
                    optionalParameters[AxisDescrOpt.ReferenceCurveAxis] = CurveReferenceAxis(
                        line.replace("CURVE_AXIS_REF", "").trim()
                    )
                }

                line.startsWith("DEPOSIT") -> {
                    optionalParameters[AxisDescrOpt.Deposit] = line.replace("DEPOSIT", "").parseDeposit()
                }

                line.startsWith("PHYS_UNIT") -> {
                    optionalParameters[AxisDescrOpt.PhysUnit] = PhysUnit(
                        line.replace("PHYS_UNIT", "").trim()
                    )
                }

                line.startsWith("STEP_SIZE") -> {
                    safeCall {
                        optionalParameters[AxisDescrOpt.StepSize] = StepSize(
                            line.replace("STEP_SIZE", "")
                                .trim()
                                .toFloat()
                        )
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error setting StepSize!: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("/begin ANNOTATION") -> {
                    safeCall {
                        val subList = input.subList(index, input.size)
                        optionalParameters[AxisDescrOpt.Annotations] = parseAnnotation(subList)
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Error Parsing Annotation!: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }
            }
        }

        return if (type != AxisType.NOT_SET) {
            Pair(
                AxisDescr(
                    type = type,
                    inputQuantity = input[1].trim(),
                    conversion = input[2].trim(),
                    maxAxisPoints = (input[3].toUIntOrNull() ?: UInt.MIN_VALUE),
                    lowerLimit = (input[4].toDoubleOrNull() ?: Double.NaN),
                    upperLimit = (input[5].toDoubleOrNull() ?: Double.NaN),
                    optionalParameters = optionalParameters
                ),
                errors
            )
        } else {
            Pair(
                AxisDescr(
                    type = input[1].parseAxisType(),
                    inputQuantity = input[2].trim(),
                    conversion = input[3].trim(),
                    maxAxisPoints = (input[4].toUIntOrNull() ?: UInt.MIN_VALUE),
                    lowerLimit = (input[5].toDoubleOrNull() ?: Double.NaN),
                    upperLimit = (input[6].toDoubleOrNull() ?: Double.NaN),
                    optionalParameters = optionalParameters
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to build AxisDescription!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(AxisDescr(), errors)
}