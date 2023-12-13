package components.a2l.classes.data

import components.a2l.objects.A2lOptional
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
): A2lOptional {
    fun isNotBlank(): Boolean {
        return when {
            a.isNaN() -> false
            b.isNaN() -> false
            c.isNaN() -> false
            d.isNaN() -> false
            e.isNaN() -> false
            f.isNaN() -> false
            else -> false
        }
    }

    fun isNotIncomplete(): Boolean {
        return when {
            this.toString().contains("NaN") -> false
            else -> true
        }
    }
}

fun String.parseCoeffs(): Coeffs {
    val result = this.replace("COEFFS", "").trim().split(" ")
    var a: Double = Double.NaN
    var b: Double = Double.NaN
    var c: Double = Double.NaN
    var d: Double = Double.NaN
    var e: Double = Double.NaN
    var f: Double = Double.NaN

    if (result.size >= 6) {
        a = result[0].toDoubleOrNull() ?: Double.NaN
        b = result[1].toDoubleOrNull() ?: Double.NaN
        c = result[2].toDoubleOrNull() ?: Double.NaN
        d = result[3].toDoubleOrNull() ?: Double.NaN
        e = result[4].toDoubleOrNull() ?: Double.NaN
        f = result[5].toDoubleOrNull() ?: Double.NaN
    }

    return Coeffs(a, b, c, d, e, f)
}