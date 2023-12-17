package components.calibrationdata.a2l.helpers

import components.parsers.a2l.classes.data.Coeffs
import components.parsers.a2l.classes.data.validate

/**
 * Reverses a RatFunc Computation Method
 */
fun Number.reverseRatFunc(coeffs: Coeffs): Double {
    val mInputNumber = this.toDouble()

    val mResult: Double = try {
        if (coeffs.validate()) {
            if ((coeffs.a + coeffs.c + coeffs.d + coeffs.e).toInt() == 0) {
                (mInputNumber * coeffs.b ) / coeffs.f
            }else if ((coeffs.a + coeffs.d + coeffs.e).toInt() == 0) {
                (mInputNumber * coeffs.b) / coeffs.f + coeffs.c
            } else if ((coeffs.a + coeffs.b + coeffs.d + coeffs.f).toInt() == 0) {
                (mInputNumber * coeffs.c) / coeffs.e
            } else this.toDouble()
        }else {
            return this.toDouble()
        }
    } catch (e: Exception) {
        mInputNumber
    }

    return mResult
}