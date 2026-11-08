package org.mr0btain.tunerstoolkit.core.calibration

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Turns the raw number stored in the ECU into the physical value people edit, and back. Raw values are doubles so
 * every data type fits; the codec rounds them to the type when writing
 */
sealed interface Conversion {
    fun toPhysical(raw: Double): Double

    /**
     * The raw value for [physical], not yet rounded to a type, or NaN when there isn't one
     * @param range The raw values the data type can hold, for conversions that have to search for the answer
     */
    fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>? = null): Double

    /** The text shown for [raw], for verbal conversions (and status values); null to show the number */
    fun text(raw: Double): String? = null

    /** The raw value with this [text], for verbal conversions */
    fun rawForText(text: String): Double? = null

    /** Raw is physical */
    data object Identical : Conversion {
        override fun toPhysical(raw: Double) = raw
        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?) = physical
    }

    /** physical = [factor] * raw + [offset] */
    data class Linear(val factor: Double, val offset: Double) : Conversion {
        override fun toPhysical(raw: Double) = factor * raw + offset
        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?) =
            if (factor == 0.0) Double.NaN else (physical - offset) / factor
    }

    /**
     * A2L's RAT_FUNC, which goes the other way: raw = (a p² + b p + c) / (d p² + e p + f) for physical p. Turning raw
     * into physical solves that for p; when it's quadratic and both roots work, [physicalRange] picks one
     */
    data class Rational(
        val a: Double, val b: Double, val c: Double, val d: Double, val e: Double, val f: Double,
        val physicalRange: ClosedFloatingPointRange<Double>? = null
    ) : Conversion {
        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?): Double {
            val denominator = (d * physical + e) * physical + f
            return if (denominator == 0.0) Double.NaN else ((a * physical + b) * physical + c) / denominator
        }

        override fun toPhysical(raw: Double): Double {
            // (a - raw d) p² + (b - raw e) p + (c - raw f) = 0
            val qa = a - raw * d
            val qb = b - raw * e
            val qc = c - raw * f
            if (abs(qa) < EPSILON) return if (qb == 0.0) Double.NaN else -qc / qb
            val discriminant = qb * qb - 4 * qa * qc
            if (discriminant < 0) return Double.NaN
            val root = sqrt(discriminant)
            val first = (-qb + root) / (2 * qa)
            val second = (-qb - root) / (2 * qa)
            val range = physicalRange ?: return first
            return if (first !in range && second in range) second else first
        }
    }

    /**
     * A table of raw to physical pairs (TAB_INTP, TAB_NOINTP). With [interpolate], values between entries are
     * interpolated and values outside are held at the ends. Without, an exact match is needed: anything else is
     * [default], or the nearest entry when there's no default
     */
    class Table(pairs: List<Pair<Double, Double>>, val interpolate: Boolean, val default: Double? = null) : Conversion {
        val pairs: List<Pair<Double, Double>> = pairs.sortedBy { it.first }
        private val byPhysical = pairs.map { it.second to it.first }.sortedBy { it.first }

        override fun toPhysical(raw: Double) = look(pairs, raw, default)
        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?) = look(byPhysical, physical, null)

        private fun look(table: List<Pair<Double, Double>>, x: Double, fallback: Double?): Double {
            if (table.isEmpty()) return fallback ?: Double.NaN
            if (!interpolate) {
                table.firstOrNull { it.first == x }?.let { return it.second }
                return fallback ?: table.minBy { abs(it.first - x) }.second
            }
            if (x <= table.first().first) return table.first().second
            if (x >= table.last().first) return table.last().second
            val upper = table.indexOfFirst { it.first >= x }
            val (x0, y0) = table[upper - 1]
            val (x1, y1) = table[upper]
            return if (x1 == x0) y0 else y0 + (y1 - y0) * (x - x0) / (x1 - x0)
        }
    }

    /**
     * Raw values shown as text (TAB_VERB): [entries] match one raw value, [ranges] a span of them (both ends
     * included). The physical value stays the raw number
     */
    class Verbal(val entries: List<Pair<Double, String>>, val ranges: List<VerbalRange> = emptyList(), val default: String? = null) : Conversion {
        override fun toPhysical(raw: Double) = raw
        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?) = physical

        override fun text(raw: Double): String? =
            entries.firstOrNull { it.first == raw }?.second ?: ranges.firstOrNull { raw >= it.min && raw <= it.max }?.text ?: default

        override fun rawForText(text: String): Double? =
            entries.firstOrNull { it.second == text }?.first ?: ranges.firstOrNull { it.text == text }?.min
    }

    class VerbalRange(val min: Double, val max: Double, val text: String)

    /**
     * A formula from raw (X1) to physical. Going back uses [inverse] when there is one, or else searches the raw
     * range for the value, which works when the formula only rises or only falls over it
     */
    class Formula(val formula: Expression, val inverse: Expression? = null) : Conversion {
        override fun toPhysical(raw: Double) = formula.evaluate(raw)

        override fun toRaw(physical: Double, range: ClosedFloatingPointRange<Double>?): Double {
            inverse?.let { return it.evaluate(physical) }
            return solve(range ?: return Double.NaN) { formula.evaluate(it) - physical }
        }
    }

    /**
     * A numeric conversion with some raw values that mean a status instead ("not available", say), from A2L's
     * STATUS_STRING_REF
     */
    class WithStatus(val base: Conversion, val status: Verbal) : Conversion by base {
        override fun text(raw: Double): String? = status.text(raw)
        override fun rawForText(text: String): Double? = status.rawForText(text)
    }

    companion object {
        private const val EPSILON = 1e-300
    }
}

/**
 * The x in [range] where [f] is 0, by bisection; NaN when [f] has the same sign at both ends. Ranges past what a double
 * walks well are trimmed to ±1e15
 */
internal fun solve(range: ClosedFloatingPointRange<Double>, f: (Double) -> Double): Double {
    var low = range.start.coerceAtLeast(-1e15)
    var high = range.endInclusive.coerceAtMost(1e15)
    var fLow = f(low)
    val fHigh = f(high)
    if (fLow == 0.0) return low
    if (fHigh == 0.0) return high
    if (fLow.isNaN() || fHigh.isNaN() || (fLow > 0) == (fHigh > 0)) return Double.NaN
    repeat(200) {
        val middle = (low + high) / 2
        val fMiddle = f(middle)
        if (fMiddle == 0.0 || high - low <= abs(middle) * 1e-15) return middle
        if ((fMiddle > 0) == (fLow > 0)) {
            low = middle
            fLow = fMiddle
        } else {
            high = middle
        }
    }
    return (low + high) / 2
}
