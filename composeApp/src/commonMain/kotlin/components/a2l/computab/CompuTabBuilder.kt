package components.a2l.computab

import components.a2l.classes.data.NumberValuePairs
import components.a2l.classes.enums.parseConversionType

/**
 * Parses a list of strings and build a CompuTab from the parameters if they exist
 */

class CompuTabBuilder {
    fun build(input: List<String>): CompuTab {
        return buildObject(input)
    }

    private fun buildObject(input: List<String>): CompuTab {
        // Check if size it big enough else we get index out of bounds
        if (input.size >= 3) {
            var mName = ""
            var mNumberValuePairs = NumberValuePairs()
            var mDefaultValue = ""
            var mDefaultValueNumeric = Double.NaN

            input.forEach { line ->
                when {
                    line.startsWith("/begin COMPU_TAB") -> {
                        val result = line.replace("/begin COMPU_TAB", "").trim()
                        if (result.isNotBlank()) {
                            mName = result
                        }
                    }

                    line.startsWith("DEFAULT_VALUE") -> {
                        mDefaultValue = line.replace("DEFAULT_VALUE", "").trim()
                    }

                    line.startsWith("DEFAULT_VALUE_NUMERIC") -> {
                        val result = line.replace("DEFAULT_VALUE_NUMERIC", "").trim().toDoubleOrNull()
                        if (result != null) {
                            mDefaultValueNumeric = result
                        }
                    }
                }
            }

            if (mName.isNotBlank()) {
                val mTotal = input[3].toIntOrNull()
                val mValuePairs = mutableMapOf<Long, Float>()

                // TODO: Needs changed up!

                /*
                7 /* number_value_pairs */
                1 4.3 THEN 2 4.7 THEN 3 5.8 4 14.2 5 16.8  <-- numbers state position!
                6 17.2 7 19.4 /* value pairs */
                 */

                if (mTotal != null) {
                    input.subList(3, input.size).forEach {
                        val item = it.trim().split(" ")
                        if (item.size == 2) {
                            val first = item[0].toLongOrNull()
                            val second = item[1].toFloatOrNull()
                            if (first != null && second != null) {
                                mValuePairs[first] = second
                            }
                        }
                    }

                    mNumberValuePairs = NumberValuePairs(
                        total = mTotal,
                        pairs = mValuePairs
                    )
                }
            } else {
                val mTotal = input[4].toIntOrNull()
                val mValuePairs = mutableMapOf<Long, Float>()

                if (mTotal != null) {
                    input.subList(4, input.size).forEach {
                        val item = it.trim().split(" ")
                        if (item.size == 2) {
                            val first = item[0].toLongOrNull()
                            val second = item[1].toFloatOrNull()
                            if (first != null && second != null) {
                                mValuePairs[first] = second
                            }
                        }
                    }

                    mNumberValuePairs = NumberValuePairs(
                        total = mTotal,
                        pairs = mValuePairs
                    )
                }
            }

            return if (mName.isNotBlank()) {
                CompuTab(
                    name = mName,
                    longIdentifier = input[1].trim(),
                    conversionType = input[2].parseConversionType(),
                    defaultValueNumeric = mDefaultValueNumeric,
                    defaultValue = mDefaultValue,
                    numberValuePairs = mNumberValuePairs
                )
            } else {
                CompuTab(
                    name = input[1].trim(),
                    longIdentifier = input[2].trim(),
                    conversionType = input[3].parseConversionType(),
                    numberValuePairs = mNumberValuePairs,
                    defaultValueNumeric = mDefaultValueNumeric,
                    defaultValue = mDefaultValue
                )
            }
        } else return CompuTab()
    }
}