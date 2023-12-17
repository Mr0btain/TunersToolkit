package components.parsers.a2l.compuvtabrange

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.ValueTriples

/**
 * Parses a list of strings and builds a CompuVtabRange from its parameters if they exist
 */

fun buildCompuVTabRange(input: List<String>): Pair<CompuVTabRange, List<LogDataEntry>> {
    val process = "BuildCompuVTabRange"
    val errors = mutableListOf<LogDataEntry>()
    var name = ""
    val valueTriples = mutableListOf<ValueTriples>()
    var defaultValue = ""

    input.forEach { line ->
        when {
            line.startsWith("/begin COMPU_VTAB_RANGE") -> {
                val result = line.replace("/begin COMPU_VTAB_RANGE", "").trim()

                if (result.isNotBlank()) {
                    name = result
                }
            }

            line.startsWith("DEFAULT_VALUE") -> {
                defaultValue = line.replace("DEFAULT_VALUE", "").trim()
            }

            line.split(" ").size >= 3 -> {
                val result = line.split(" ")
                val inValMin = result[0].trim().toDoubleOrNull()
                val inValMax = result[1].trim().toDoubleOrNull()
                val outVal = result[2].trim()

                if (inValMin != null && inValMax != null) {
                    valueTriples.add(
                        ValueTriples(
                            inValMin = inValMin,
                            inValMax = inValMax,
                            outVal = outVal
                        )
                    )
                }
            }
        }
    }

    if (name.isNotBlank()) {
        safeCall {
            return Pair(
                CompuVTabRange(
                    name = name,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    numberOfValueTriples = input[2].trim().toUInt(),
                    valueTriples = valueTriples,
                    defaultValue = defaultValue
                ),
                errors
            )
        }.onFailure {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set NumberOfValueTriples! ${it.message}",
                    cause = it.cause.toString(),
                    error = true
                )
            )

            return Pair(CompuVTabRange(), errors)
        }
    } else {
        safeCall {
            return Pair(
                CompuVTabRange(
                    name = input[1].trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    numberOfValueTriples = input[3].trim().toUInt(),
                    valueTriples = valueTriples,
                    defaultValue = defaultValue
                ),
                errors
            )
        }.onFailure {
            errors.add(
                LogDataEntry(
                    process = process,
                    message = "Failure to set NumberOfValueTriples! ${it.message}",
                    cause = it.cause.toString(),
                    error = true
                )
            )

            return Pair(CompuVTabRange(), errors)
        }
    }

    errors.add(
        LogDataEntry(
            process = process,
            message = "Failure to build CompuVTabRange!",
            cause = "UNKNOWN",
            error = true
        )
    )

    return Pair(CompuVTabRange(), errors)
}