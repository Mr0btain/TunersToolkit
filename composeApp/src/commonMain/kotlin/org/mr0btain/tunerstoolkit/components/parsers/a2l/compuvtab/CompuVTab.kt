package org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ConversionType
import kotlinx.serialization.Serializable

/**
 * Conversion table for the visualization of bit patterns
 *
 * @param name unique identifier in the program for the verbal conversion table
 * @param longIdentifier Description of the Conversion table
 * @param type Conversion table type
 * @param numberOfValuePairs Number of value pairs that are in this table
 * @param valuePairs Map of the value pairs in this table
 * Key: inVal Datatype ―float‖ is used for the input value of
 * COMPU_TAB and COMPU_VTAB. Since the accepted use case of COMPU_VTAB input
 * values are integers, all float values are rounded to the nearest integer by following the
 * formula (0.5 <= x_raw < 1.5 lead to x = 1). Only float input values used with
 * COMPU_VTAB_RANGE remain float values without truncation If you want to use float as
 * internal values (without truncation), you should use COMPU_VTAB_RANGE.
 *  and outVal (Description of the byte value)
 *  @param defaultValue string used as OutVal for display when the ECU value
 * is out of any declared range. This string shall not be selectable for calibration
 * (when writing to the ECU).
 */

@Serializable
data class CompuVTab(
    val name: String = "",
    val longIdentifier: String = "",
    val type: ConversionType = ConversionType.NOT_SET,
    val numberOfValuePairs: Int = Int.MIN_VALUE,
    val valuePairs: Map<Int, String> = mapOf(),
    val defaultValue: String = ""
)

fun CompuVTab.validate(): Boolean {
    return when {
        name.isBlank() -> false
        type == ConversionType.NOT_SET -> false
        else -> true
    }
}
