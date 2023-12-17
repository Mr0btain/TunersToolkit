package components.parsers.a2l.compuvtab

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.classes.enums.parseConversionType

/**
 * Builds the list of computation tabs
 */

fun buildCompuVTab(input: List<String>): Pair<CompuVTab, List<LogDataEntry>> {
    val process = "BuildCompuVTab"
    val errors = mutableListOf<LogDataEntry>()
    var name = ""
    var numberOfValuePairs = Int.MIN_VALUE
    var defaultValue = ""

    input.forEach { line ->
        when {
            line.startsWith("/begin COMPU_VTAB") -> {
                val result = line.replace("/begin COMPU_VTAB", "").trim()
                if (result.isNotBlank()) {
                    name = result
                }
            }

            line.startsWith("DEFAULT_VALUE") -> {
                defaultValue = line.replace("DEFAULT_VALUE", "").trim()
            }
        }
    }

    if (name.isNotBlank()) {
        numberOfValuePairs = input[3].trim().toInt()
    } else {
        input[4].trim().toInt()
    }

    // TODO: Test COMPU_VTAB Parse works correctly
    if (name.isNotBlank()) {
        val conversionType = input[2].parseConversionType()

        if (conversionType != ConversionType.NOT_SET) {
            safeCall {
                val valuePairs = input.subList(4, input.size - 1).parseListOfValuePairs()

                return Pair(
                    CompuVTab(
                        name = name,
                        longIdentifier = input[1].replace("\"", "").trim(),
                        type = conversionType,
                        numberOfValuePairs = numberOfValuePairs,
                        valuePairs = valuePairs,
                        defaultValue = defaultValue
                    ),
                    errors
                )
            }.onFailure {
                errors.add(
                    LogDataEntry(
                        process = process,
                        message = "Failure to set Value Pairs!",
                        error = true
                    )
                )

                return Pair(CompuVTab(), errors)
            }
        } else {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set Conversion Type!",
                    error = true
                )
            )

            return Pair(CompuVTab(), errors)
        }
    } else {
        val conversionType = input[3].parseConversionType()

        if (conversionType != ConversionType.NOT_SET) {
            safeCall {
                val valuePairs = input.subList(5, input.size - 1).parseListOfValuePairs()

                return Pair(
                    CompuVTab(
                        name = input[1].trim(),
                        longIdentifier = input[2].replace("\"", "").trim(),
                        type = conversionType,
                        numberOfValuePairs = numberOfValuePairs,
                        valuePairs = valuePairs,
                        defaultValue = defaultValue
                    ),
                    errors
                )
            }.onFailure {
                errors.add(
                    LogDataEntry(
                        process = process,
                        message = "Failure to set Value Pairs!",
                        error = true
                    )
                )

                return Pair(CompuVTab(), errors)
            }
        } else {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set Conversion Type!",
                    error = true
                )
            )

            return Pair(CompuVTab(), errors)
        }
    }

    errors.add(
        LogDataEntry(
            process = process,
            message = "Failure to build CompuVTab!",
            cause = "UNKNOWN",
            error = true
        )
    )

    return Pair(CompuVTab(), errors)
}

private fun List<String>.parseListOfValuePairs(): Map<Int, String> {
    val valuePairList = mutableMapOf<Int, String>()

    this.forEach {line ->
        val data = line.replace("\"", "").trim().split(" ")

        valuePairList[data[0].toInt()] = data[1]
    }

    return valuePairList
}