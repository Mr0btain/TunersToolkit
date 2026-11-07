package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * This keyword is used to specify coefficients a, b, c, d,
 * e, f for the fractional rational function of the following
 * type:
 * f(x)=(axx + bx + c) / (dxx + ex + f)
 */

@Serializable
data class Coeffs(
    val a: Double = Double.NaN,
    val b: Double = Double.NaN,
    val c: Double = Double.NaN,
    val d: Double = Double.NaN,
    val e: Double = Double.NaN,
    val f: Double = Double.NaN,
): A2lOptional

fun Coeffs.validate(): Boolean {
    return when {
        a.isNaN() -> false
        b.isNaN() -> false
        c.isNaN() -> false
        d.isNaN() -> false
        e.isNaN() -> false
        f.isNaN() -> false
        else -> true
    }
}

fun String.parseCoeffs(): Coeffs {
    val result = this.replace("COEFFS", "").trim().split(" ")

    return Coeffs(
        result[0].toDouble(),
        result[1].toDouble(),
        result[2].toDouble(),
        result[3].toDouble(),
        result[4].toDouble(),
        result[5].toDouble()
    )
}