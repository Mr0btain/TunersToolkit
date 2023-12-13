package components.parsers.a2l.computab

import components.parsers.a2l.classes.data.NumberValuePairs
import components.parsers.a2l.classes.enums.ConversionType
import components.parsers.a2l.helpers.interpolateLinear
import kotlinx.serialization.Serializable

/**
 * Conversion table for conversions that cannot be represented as a function
 * @param name The identification of the CompuTab
 * @param longIdentifier Description of the CompuTab
 * @param conversionType possible Types: TAB_INTP table with interpolation
 * TAB_NOINTP table without interpolation
 * Note: This parameter is redundant information
 * because the record defined with COMPU_METHOD also contains it.
 * @param defaultValue string used as OutVal for display when the ECU value is out of
 * any declared range.
 * This string is not selectable for calibration (when writing to the ECU).
 * @param defaultValueNumeric Float value used as OutVal for display when the
 * ECU value is out of any declared range.
 * This value is not selectable for calibration (when writing to the ECU)
 */

@Serializable
data class CompuTab(
    val name: String = "",
    val longIdentifier: String = "",
    val conversionType: ConversionType = ConversionType.NOT_SET,
    val numberValuePairs: NumberValuePairs = NumberValuePairs(),
    val defaultValue: String = "",
    val defaultValueNumeric: Double = Double.NaN
)

fun CompuTab.validate(): Boolean {
    return when {
        name.isBlank() -> false
        conversionType == ConversionType.NOT_SET -> false
        else -> true
    }
}

fun CompuTab.isValuePairsNotBlank(): Boolean {
    return numberValuePairs.pairs.isNotEmpty()
}

fun CompuTab.applyTabNoIntP(input: Long): Double {
    return numberValuePairs.pairs[input]?.toDouble() ?: Double.NaN
}

fun CompuTab.applyTabIntP(input: Long): Double {
    val mNumberValuePairs = numberValuePairs.pairs

    val mNumberValue = if (mNumberValuePairs.containsKey(input)) {
        mNumberValuePairs[input] ?: Float.NaN
    } else Float.NaN

    return if (mNumberValue.isNaN()) {
        val keys = mNumberValuePairs.keys.toTypedArray()

        var cnt = 0
        var x1 = 0L
        var x2 = 0L
        var y1 = 0f
        var y2 = 0f

        // TODO: Make this more elegant

        for (entryKey in keys) {
            if (entryKey > input) {
                x1 = if (cnt == 0) {
                    keys[0]
                } else keys[cnt - 1]

                x2 = if (cnt == 0) {
                    keys[1]
                } else keys[cnt]

                val y1Test = if (mNumberValuePairs.containsKey(x1)) {
                    mNumberValuePairs[x1] ?: Float.NaN
                } else Float.NaN

                val y2Test = if (mNumberValuePairs.containsKey(x2)) {
                    mNumberValuePairs[x2] ?: Float.NaN
                } else Float.NaN

                if (!y1Test.isNaN() && !y2Test.isNaN()) {
                    y1 = y1Test
                    y2 = y2Test
                }
                break
            }

            cnt++
        }

        interpolateLinear(x1, x2, y1, y2, input)
    } else mNumberValue.toDouble()
}