package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.error.TkResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ExpressionTest {
    private fun parse(text: String, constants: Map<String, String> = emptyMap()): Expression =
        when (val result = Expression.parse(text, constants)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> fail(result.error.message)
        }

    private fun eval(text: String, vararg x: Double) = parse(text).evaluate(*x)

    private fun error(text: String): String = when (val result = Expression.parse(text)) {
        is TkResult.Success -> fail("\"$text\" was read")
        is TkResult.Failure -> result.error.message
    }

    @Test
    fun arithmeticFollowsCPrecedence() {
        assertEquals(14.0, eval("2 + 3 * 4"))
        assertEquals(20.0, eval("(2 + 3) * 4"))
        assertEquals(1.0, eval("10 - 4 - 5"))
        assertEquals(2.5, eval("10 / 4"))
        assertEquals(1.0, eval("10 % 3"))
        assertEquals(-6.0, eval("-2 * 3"))
        assertEquals(1.5e3, eval("1.5e3"))
        assertEquals(255.0, eval("0xFF"))
    }

    @Test
    fun inputs() {
        assertEquals(21.0, eval("X1 * 2 + 1", 10.0))
        assertEquals(21.0, eval("x * 2 + 1", 10.0))
        assertEquals(7.0, eval("X1 + X2", 3.0, 4.0))
        assertEquals(2, parse("X1 + X2").inputCount)
        assertEquals(0.0, eval("X3", 1.0), "a missing input is 0")
    }

    @Test
    fun bitAndLogicOperators() {
        assertEquals(0x0F.toDouble(), eval("X1 & 0x0F", 0xAF.toDouble()))
        assertEquals(0xF0.toDouble(), eval("X1 | 0xF0", 0.0))
        assertEquals(0x0F.toDouble(), eval("0xFF ^ 0xF0"), "^ is xor")
        assertEquals(16.0, eval("1 << 4"))
        assertEquals(4.0, eval("64 >> 4"))
        assertEquals((0.inv()).toDouble(), eval("~0"))
        assertEquals(1.0, eval("3 > 2 && 2 >= 2"))
        assertEquals(0.0, eval("3 < 2 || 2 != 2"))
        assertEquals(1.0, eval("!0"))
        assertEquals(1.0, eval("1 << 2 == 4"), "shift binds tighter than ==")
        assertEquals(1.0, eval("1 & 3 == 3"), "== binds tighter than &, as in C: 1 & (3 == 3), not (1 & 3) == 3")
    }

    @Test
    fun functions() {
        assertEquals(3.0, eval("sqrt(9)"))
        assertEquals(8.0, eval("pow(2, 3)"))
        assertEquals(5.0, eval("abs(-5)"))
        assertEquals(1.0, eval("sin(X1 / 2)", PI), 1e-12)
        assertEquals(2.0, eval("log10(100)"))
        assertEquals(1.0, eval("log(exp(1))"))
        assertEquals(3.0, eval("floor(3.7)"))
        assertEquals(4.0, eval("ceil(3.2)"))
        assertEquals(2.0, eval("min(2, 3)"))
    }

    @Test
    fun systemConstants() {
        val expression = parse("X1 * sysc(CYLINDERS) + sysc(\"OFFSET\")", mapOf("CYLINDERS" to "4", "OFFSET" to " 0.5 "))
        assertEquals(8.5, expression.evaluate(2.0))
        assertTrue("SYSTEM_CONSTANT NOPE" in error("sysc(NOPE)"))
    }

    @Test
    fun badFormulasAreReported() {
        assertTrue("unknown function" in error("foo(1)"))
        assertTrue("unknown name" in error("Y + 1"))
        assertTrue("ends" in error("1 +"))
        assertTrue("')' was expected" in error("(1 + 2"))
        assertTrue("unexpected" in error("1 + 2)"))
        assertTrue("unexpected" in error("1 $ 2"))
    }

    @Test
    fun hugeFormulasAreRefusedInsteadOfOverflowing() {
        assertTrue("nested" in error("(".repeat(100_000) + "1" + ")".repeat(100_000)))
        assertTrue("nested" in error("-".repeat(100_000) + "1"))
        assertTrue("operators" in error((1..100_000).joinToString("+")))
    }
}
