package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * Verbal range based conversion table unit
 * @param inValMin lower limit as float value, needs to be integer ECU
 * @param inValMax upper limit as float value, needs to be integer ECU value when assigned
 * to ―non-float‖ definitions.
 * @param outVal display string for the value range
 */

@Serializable
data class ValueTriples(
    val inValMin: Double = Double.NaN,
    val inValMax: Double = Double.NaN,
    val outVal: String = ""
): A2lOptional