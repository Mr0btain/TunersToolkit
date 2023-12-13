package components.parsers.a2l.compuvtab

import components.parsers.a2l.classes.enums.parseConversionType

/**
 * Builds the list of computation tabs
 */

fun buildCompuVTab(input: List<String>): CompuVTab {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 3) {
        var mName = ""
        var mNumberOfValuePairs = Int.MIN_VALUE
        var mDefaultValue = ""
        input.forEach { line ->
            when {
                line.startsWith("/begin COMPU_VTAB") -> {
                    val result = line.replace("/begin COMPU_VTAB", "").trim()
                    if (result.isNotBlank()) {
                        mName = result
                    }
                }

                line.startsWith("DEFAULT_VALUE") -> {
                    mDefaultValue = line.replace("DEFAULT_VALUE", "").trim()
                }
            }
        }

        if (mName.isNotBlank()) {
            mNumberOfValuePairs = input[3].trim().toIntOrNull() ?: Int.MIN_VALUE
        } else {
            input[4].trim().toIntOrNull() ?: Int.MIN_VALUE
        }

        // TODO: Test COMPU_VTAB Parse works correctly
        return if (mName.isNotBlank()) {
            CompuVTab(
                name = mName,
                longIdentifier = input[1].replace("\"", "").trim(),
                type = input[2].parseConversionType(),
                numberOfValuePairs = mNumberOfValuePairs,
                valuePairs = input.subList(4, input.size - 1).parseListOfValuePairs(),
                defaultValue = mDefaultValue
            )
        } else {
            CompuVTab(
                name = input[1].trim(),
                longIdentifier = input[2].replace("\"", "").trim(),
                type = input[3].parseConversionType(),
                numberOfValuePairs = mNumberOfValuePairs,
                valuePairs = input.subList(5, input.size - 1).parseListOfValuePairs(),
                defaultValue = mDefaultValue
            )
        }
    } else return CompuVTab()
}

private fun List<String>.parseListOfValuePairs(): Map<Int, String> {
    val valuePairList = mutableMapOf<Int, String>()

    this.forEach {line ->
        val data = line.replace("\"", "").trim().split(" ")

        if (data.size == 2) {
            val a = data[0].toIntOrNull()
            val b = data[1]

            if (a != null && b.isNotBlank()) {
                valuePairList[a] = b
            }
        }
    }

    return valuePairList
}