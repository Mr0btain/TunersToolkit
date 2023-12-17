package components.parsers.a2l.computab

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.NumberValuePairs
import components.parsers.a2l.classes.data.parseNumberValuePairs
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.parseConversionType

/**
 * Parses a list of strings and build a CompuTab from the parameters if they exist
 */

fun buildCompuTab(input: List<String>): Pair<CompuTab, List<LogDataEntry>> {
    val process = "BuildCompuTab"
    val errors = mutableListOf<LogDataEntry>()
    var name = ""
    var defaultValue = ""
    var defaultValueNumeric = Double.NaN
    var numberValuePairs = NumberValuePairs()

    input.forEach { line ->
        when {
            line.startsWith("/begin COMPU_TAB") -> {
                val result = line.replace("/begin COMPU_TAB", "").trim()
                if (result.isNotBlank()) {
                    name = result
                }
            }

            line.startsWith("DEFAULT_VALUE") -> {
                defaultValue = line.replace("DEFAULT_VALUE", "").trim()
            }

            line.startsWith("DEFAULT_VALUE_NUMERIC") -> {
                val result = line.replace("DEFAULT_VALUE_NUMERIC", "").trim().toDoubleOrNull()
                if (result != null) {
                    defaultValueNumeric = result
                }
            }
        }
    }

    safeCall {
        numberValuePairs = parseNumberValuePairs(input, name)
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to add NumberValuePairs! ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    if (name.isNotBlank()) {
        val conversionType = input[2].parseConversionType()

        return if (conversionType != ConversionType.NOT_SET) {
            Pair(
                CompuTab(
                    name = name,
                    longIdentifier = input[1].trim(),
                    conversionType = input[2].parseConversionType(),
                    defaultValueNumeric = defaultValueNumeric,
                    defaultValue = defaultValue,
                    numberValuePairs = numberValuePairs
                ),
                errors
            )
        } else {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set Conversion Type!",
                    error = true
                )
            )

            Pair(CompuTab(), errors)
        }
    } else {
        val conversionType = input[3].parseConversionType()

        return if (conversionType != ConversionType.NOT_SET) {
            Pair(
                CompuTab(
                    name = input[1].trim(),
                    longIdentifier = input[2].trim(),
                    conversionType = conversionType,
                    numberValuePairs = numberValuePairs,
                    defaultValueNumeric = defaultValueNumeric,
                    defaultValue = defaultValue
                ),
                errors
            )
        } else {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set Conversion Type!",
                    error = true
                )
            )

            Pair(CompuTab(), errors)
        }
    }
}