package components.a2l.compuvtabrange

import components.a2l.classes.data.ValueTriples

/**
 * Parses a list of strings and builds a CompuVtabRange from its parameters if they exist
 */

class CompuVTabRangeBuilder {
    fun build(input: List<String>): CompuVTabRange {
        return buildObject(input)
    }

    private fun buildObject(input: List<String>): CompuVTabRange {
        // Check if size it big enough else we get index out of bounds
        if (input.size >= 3) {
            var mName = ""
            val mValueTriples = mutableListOf<ValueTriples>()
            var mDefaultValue = ""

            input.forEach { line ->
                when {
                    line.startsWith("/begin COMPU_VTAB_RANGE") -> {
                        val result = line.replace("/begin COMPU_VTAB_RANGE", "").trim()

                        if (result.isNotBlank()) {
                            mName = result
                        }
                    }

                    line.startsWith("DEFAULT_VALUE") -> {
                        mDefaultValue = line.replace("DEFAULT_VALUE", "").trim()
                    }

                    line.split(" ").size >= 3 -> {
                        val result = line.split(" ")
                        val mInValMin = result[0].trim().toDoubleOrNull()
                        val mInValMax = result[1].trim().toDoubleOrNull()
                        val mOutVal = result[2].trim()

                        if (mInValMin != null && mInValMax != null) {
                            mValueTriples.add(
                                ValueTriples(
                                    inValMin = mInValMin,
                                    inValMax = mInValMax,
                                    outVal = mOutVal
                                )
                            )
                        }
                    }
                }
            }

            return if (mName.isNotBlank()) {
                CompuVTabRange(
                    name = mName,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    numberOfValueTriples = input[2].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    valueTriples = mValueTriples,
                    defaultValue = mDefaultValue
                )
            } else {
                CompuVTabRange(
                    name = input[1].trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    numberOfValueTriples = input[3].trim().toUIntOrNull() ?: UInt.MIN_VALUE,
                    valueTriples = mValueTriples,
                    defaultValue = mDefaultValue
                )
            }
        } else return CompuVTabRange()
    }
}