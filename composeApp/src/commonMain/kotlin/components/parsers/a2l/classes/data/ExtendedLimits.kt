package components.parsers.a2l.classes.data

import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * Used to specify an extended
 * range of values. In the measurement and calibration
 * system, for example, when leaving the standard range
 * of values (lower limit...upper limit) a warning could be
 * generated (extended limits enabled only for "power
 * user").
 *
 */

@Serializable
data class ExtendedLimits(
    val lowerLimit: Double = Double.NaN,
    val upperLimit: Double = Double.NaN
): A2lOptional

/**
 * Parses a string and returns ExtendedLimits if values are there, else returns an
 * ExtendedLimits with Min Values
 */

fun String.parseExtendedLimits(): ExtendedLimits {
    val limits = this.replace("EXTENDED_LIMITS", "").trim().split(" ")
    return ExtendedLimits(
        limits[0].toDouble(),
        limits[1].toDouble()
    )
}