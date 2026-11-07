package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
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
): A2lOptional
fun CoeffsLinear.compute(input: Double): Double { return (a * input) - b }

fun String.parseCoeffsLinear(): CoeffsLinear {
    val result = this.replace("COEFFS_LINEAR", "").trim().split(" ")

    return CoeffsLinear(
        result[0].toDouble(),
        result[1].toDouble()
    )
}