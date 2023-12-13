package components.parsers.a2l.compumethod

import components.parsers.a2l.classes.data.CompuTabRef
import components.parsers.a2l.classes.data.Formula
import components.parsers.a2l.classes.data.RefUnit
import components.parsers.a2l.classes.data.StatusStringRef
import components.parsers.a2l.classes.data.parseCoeffs
import components.parsers.a2l.classes.data.parseCoeffsLinear
import components.parsers.a2l.classes.enums.parseConversionType
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses and builds a Compu Method from a list of strings if it contains one
 */

fun buildCompuMethod(input: List<String>): CompuMethod {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 5) {
        var mName = ""
        val mOptionalParams = mutableMapOf<CompuMethodOpt, A2lOptional>()

        input.forEach { line ->
            when {
                line.startsWith("/begin") -> {
                    val result = line.replace("/begin COMPU_METHOD", "").trim()
                    if (result.isNotBlank()) {
                        mName = result
                    }
                }

                line.startsWith("COEFFS") -> {
                    mOptionalParams[CompuMethodOpt.Coeffs] = line.parseCoeffs()
                }

                line.startsWith("COMPU_TAB_REF") -> {
                    mOptionalParams[CompuMethodOpt.CompuTabRef] = CompuTabRef(
                        line.replace("COMPU_TAB_REF", "").trim()
                    )
                }

                line.startsWith("COEFFS_LINEAR") -> {
                    mOptionalParams[CompuMethodOpt.CoeffsLinear] = line.parseCoeffsLinear()
                }

                line.startsWith("FORMULA") -> {
                    mOptionalParams[CompuMethodOpt.Formula] = Formula(line.replace("FORMULA", "").trim())
                }

                line.startsWith("REF_UNIT") -> {
                    mOptionalParams[CompuMethodOpt.RefUnit] = RefUnit(line.replace("REF_UNIT", "").trim())
                }

                line.startsWith("STATUS_STRING_REF") -> {
                    mOptionalParams[CompuMethodOpt.StatusStringRef] = StatusStringRef(line.replace("STATUS_STRING_REF", "").trim())
                }
            }
        }

        return if (mName.isNotBlank()) {
            CompuMethod(
                name = mName,
                longIdentifier = input[1].trim(),
                conversionType = input[2].parseConversionType(),
                format = input[3].replace("\"", "").replace("%", "").trim(),
                unit = input[4].replace("\"", "").trim(),
                optionalParameters = mOptionalParams
            )
        } else {
            CompuMethod(
                name = input[1].trim(),
                longIdentifier = input[2].trim(),
                conversionType = input[3].parseConversionType(),
                format = input[4].replace("\"", "").replace("%", "").trim(),
                unit = input[5].replace("\"", "").trim(),
                optionalParameters = mOptionalParams
            )
        }
    } else return CompuMethod()
}