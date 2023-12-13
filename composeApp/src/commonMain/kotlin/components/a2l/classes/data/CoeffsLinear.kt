package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * This keyword is used to specify the coefficients a and b
 * of the linear function of the following type:
 * f(x)=ax + b
 *
 */

@Serializable
data class CoeffsLinear(
    val a: Double = Double.NaN,
    val b: Double = Double.NaN
): A2lOptional {
    fun isNotBlank(): Boolean {
        return !(a.isNaN() || b.isNaN())
    }

    fun compute(input: Double): Double { return (a * input) - b }
}

fun String.parseCoeffsLinear(): CoeffsLinear {
    val result = this.replace("COEFFS_LINEAR", "").trim().split(" ")
    var a = Double.NaN
    var b = Double.NaN

    if (result.size >= 2) {
        a = result[0].toDoubleOrNull() ?: Double.NaN
        b = result[1].toDoubleOrNull() ?: Double.NaN
    }

    return CoeffsLinear(a, b)
}