package components.parsers.a2l.compumethod

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.CompuTabRef
import components.parsers.a2l.classes.data.Formula
import components.parsers.a2l.classes.data.RefUnit
import components.parsers.a2l.classes.data.StatusStringRef
import components.parsers.a2l.classes.data.parseCoeffs
import components.parsers.a2l.classes.data.parseCoeffsLinear
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.parseConversionType
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses and builds a Compu Method from a list of strings if it contains one
 */

fun buildCompuMethod(input: List<String>): Pair<CompuMethod, List<LogDataEntry>> {
    val process = "BuildCompuMethod"
    val errors = mutableListOf<LogDataEntry>()

    // Check if size it big enough else we get index out of bounds
    var name = ""
    val mOptionalParams = mutableMapOf<CompuMethodOpt, A2lOptional>()

    safeCall {
        input.forEach { line ->
            when {
                line.startsWith("/begin") -> {
                    val result = line.replace("/begin COMPU_METHOD", "").trim()

                    if (result.isNotBlank()) {
                        name = result
                    }
                }

                line.startsWith("COEFFS") -> {
                    safeCall {
                        mOptionalParams[CompuMethodOpt.Coeffs] = line.parseCoeffs()
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set Coeffs!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }

                }

                line.startsWith("COMPU_TAB_REF") -> {
                    mOptionalParams[CompuMethodOpt.CompuTabRef] = CompuTabRef(
                        line.replace("COMPU_TAB_REF", "").trim()
                    )
                }

                line.startsWith("COEFFS_LINEAR") -> {
                    safeCall {
                        mOptionalParams[CompuMethodOpt.CoeffsLinear] = line.parseCoeffsLinear()
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to set CoeffsLinear!; ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }

                line.startsWith("FORMULA") -> {
                    mOptionalParams[CompuMethodOpt.Formula] = Formula(
                        line.replace("FORMULA", "").trim()
                    )
                }

                line.startsWith("REF_UNIT") -> {
                    mOptionalParams[CompuMethodOpt.RefUnit] = RefUnit(
                        line.replace("REF_UNIT", "").trim()
                    )
                }

                line.startsWith("STATUS_STRING_REF") -> {
                    mOptionalParams[CompuMethodOpt.StatusStringRef] = StatusStringRef(
                        line.replace("STATUS_STRING_REF", "").trim()
                    )
                }
            }
        }

        if (name.isNotBlank()) {
            val conversionType = input[2].parseConversionType()

            return if (conversionType != ConversionType.NOT_SET) {
                Pair(
                    CompuMethod(
                        name = name,
                        longIdentifier = input[1].trim(),
                        conversionType = conversionType,
                        format = input[3].replace("\"", "").replace("%", "").trim(),
                        unit = input[4].replace("\"", "").trim(),
                        optionalParameters = mOptionalParams
                    ),
                    errors
                )
            } else {
                errors.add(
                    LogDataEntry(
                        process = process,
                        message = "Failure to Set Conversion Method!",
                        error = true
                    )
                )

                Pair(CompuMethod(), errors)
            }
        } else {
            val conversionType = input[3].parseConversionType()

            return if (conversionType != ConversionType.NOT_SET) {
                Pair(
                    CompuMethod(
                        name = input[1].trim(),
                        longIdentifier = input[2].trim(),
                        conversionType = conversionType,
                        format = input[4].replace("\"", "").replace("%", "").trim(),
                        unit = input[5].replace("\"", "").trim(),
                        optionalParameters = mOptionalParams
                    ),
                    errors
                )
            } else {
                errors.add(
                    LogDataEntry(
                        process = process,
                        message = "Failure to Set Conversion Method!",
                        error = true
                    )
                )

                Pair(CompuMethod(), errors)
            }
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to Parse CompuMethod!; ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(CompuMethod(), errors)
}