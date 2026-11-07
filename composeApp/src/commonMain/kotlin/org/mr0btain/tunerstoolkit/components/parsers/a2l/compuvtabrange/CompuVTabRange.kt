package org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtabrange
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ValueTriples
import kotlinx.serialization.Serializable

/**
 * Conversion table for the assignment of display strings to a value range. In particular
 * this is useful for ASAM MCD-2 MC definitions with the data type ‗floating point
 * (referred as FLOAT definitions).
 * For FLOAT definitions, the declared string is displayed for
 * InValMin <= ECU value < InValMax, with InValMin, InValMax as floating point values.
 * For non-FLOAT definitions, the declared string is displayed for
 * InValMin <= ECU value <= InValMax, with InValMin, InVal as integer values.
 * Note: InValMin and InValMax can have the same value to express an assignment of
 * one ECU value to a string (as in COMPU_VTAB); this is not realistic for
 * floating point (and therefore not supported).
 * Note: Overlapping ranges may not be declared. The ASAM MCD-2 MC file is invalid
 * in case of overlapping ranges within COMPU_VTAB_RANGE. But still, the
 * Format of the description file
 * 84 ASAM MCD-2 MC Data Model for ECU Measurement and Calibration
 * Version 1.6.1
 * upper limit of one range may be the same FLOAT value than the lower limit of
 * the following range in case of a FLOAT definition (see display rules).
 * Note: When a COMPU_METHOD with COMPU_VTAB_RANGE is used for
 * calibration (writing of values to ECU), the InValMin is used when the assigned
 * STRING (OutVal) is selected in the user interface.
 * Note: If the optional DEFAULT_VALUE is declared, this string is displayed when the
 * ECU value is out of any declared range. This string shall not be selectable for
 * calibration.
 *
 * @param name Identification of the Compu_Vtab_Range
 * @param longIdentifier Description of the item
 * @param numberOfValueTriples Number of successive value triples for this verbal range
 * based conversion table
 * @param valueTriples List of all the ValueTriples
 * @param defaultValue Used as OutVal for display when the ECU value
 * is out of any declared range. This string shall not be
 * selectable for calibration (when writing to the ECU).
 */

@Serializable
data class CompuVTabRange(
    val name: String = "",
    val longIdentifier: String = "",
    val numberOfValueTriples: UInt = UInt.MIN_VALUE,
    val valueTriples: List<ValueTriples> = listOf(),
    val defaultValue: String = ""
)

fun CompuVTabRange.validate(): Boolean {
    return when {
        name.isBlank() -> false
        numberOfValueTriples == UInt.MIN_VALUE && valueTriples.isEmpty() -> false
        else -> true
    }
}