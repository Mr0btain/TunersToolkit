package components.calibrationdata.helpers

import components.a2l.classes.data.Coeffs

/**
 * Applys a RatFunc Computation Method
 */
fun Number.applyRatFunc(coeffs: Coeffs): Double {
    return if ((coeffs.a + coeffs.c + coeffs.d + coeffs.e).toInt() == 0) {
        (this.toDouble() * coeffs.f) / coeffs.b
    } else if ((coeffs.a + coeffs.d + coeffs.e).toInt() == 0) {
        ((this.toDouble() * coeffs.f) - coeffs.c) / coeffs.b
    } else if ((coeffs.a + coeffs.b + coeffs.d + coeffs.f).toInt() == 0) {
        coeffs.c / (this.toDouble() * coeffs.e)
    } else this.toDouble()
}