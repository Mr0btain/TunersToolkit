package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * Similar to FixAxisPar
 */
@Serializable
data class FixAxisParDist(
    val offset: Float = Float.NaN,
    val distance: Float = Float.NaN,
    val numberApo: UInt = UInt.MIN_VALUE
): A2lOptional