package org.mr0btain.tunerstoolkit.core.calibration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValueFormatTest {
    @Test
    fun followsTheA2lFormat() {
        assertEquals("21.75", formatValue(21.75, "%5.2"))
        assertEquals("21.8", formatValue(21.75, "%5.1"))
        assertEquals("22", formatValue(21.75, "%5.0"))
        assertEquals("-0.50", formatValue(-0.5, "%6.2"))
        assertEquals("0.00", formatValue(-0.001, "%6.2"), "no minus on a zero")
        assertEquals("1.00012", formatValue(1.000122, "%7.5"))
        assertEquals("820", formatValue(820.0, "%6"))
    }

    @Test
    fun withoutAFormat() {
        assertEquals("1.5", formatValue(1.5))
        assertEquals("1000", formatValue(1000.0))
        assertEquals("0.3333", formatValue(1.0 / 3))
        assertEquals("NaN", formatValue(Double.NaN))
        assertNull(decimalsOf("%s"))
        assertNull(decimalsOf(null))
    }
}
