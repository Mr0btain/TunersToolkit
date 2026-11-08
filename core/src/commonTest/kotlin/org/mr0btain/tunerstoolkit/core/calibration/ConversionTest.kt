package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionTest {
    @Test
    fun linear() {
        val conversion = Conversion.Linear(0.75, -48.0)
        assertEquals(27.0, conversion.toPhysical(100.0))
        assertEquals(100.0, conversion.toRaw(27.0))
        assertTrue(Conversion.Linear(0.0, 1.0).toRaw(1.0).isNaN())
    }

    @Test
    fun rationalQuadraticPicksTheRootInRange() {
        // raw = p²: both p and -p give it, the range decides
        val positive = Conversion.Rational(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, physicalRange = 0.0..100.0)
        val negative = Conversion.Rational(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, physicalRange = -100.0..0.0)
        assertEquals(4.0, positive.toPhysical(16.0))
        assertEquals(-4.0, negative.toPhysical(16.0))
        assertEquals(16.0, positive.toRaw(4.0))
        assertTrue(positive.toPhysical(-1.0).isNaN(), "no real root")
    }

    @Test
    fun rationalWithAPhysicalDenominator() {
        // raw = (2p + 1) / (p + 1)
        val conversion = Conversion.Rational(0.0, 2.0, 1.0, 0.0, 1.0, 1.0)
        val raw = conversion.toRaw(3.0)
        assertEquals(7.0 / 4, raw)
        assertEquals(3.0, conversion.toPhysical(raw), 1e-12)
    }

    @Test
    fun tableWithInterpolation() {
        val table = Conversion.Table(listOf(10.0 to 100.0, 0.0 to 0.0, 20.0 to 150.0), interpolate = true)
        assertEquals(50.0, table.toPhysical(5.0))
        assertEquals(125.0, table.toPhysical(15.0))
        assertEquals(0.0, table.toPhysical(-3.0), "held at the first entry")
        assertEquals(150.0, table.toPhysical(99.0), "held at the last entry")
        assertEquals(15.0, table.toRaw(125.0))
    }

    @Test
    fun tableWithoutInterpolation() {
        val withDefault = Conversion.Table(listOf(0.0 to 0.0, 10.0 to 100.0), interpolate = false, default = -1.0)
        assertEquals(100.0, withDefault.toPhysical(10.0))
        assertEquals(-1.0, withDefault.toPhysical(4.0))
        val nearest = Conversion.Table(listOf(0.0 to 0.0, 10.0 to 100.0), interpolate = false)
        assertEquals(100.0, nearest.toPhysical(7.0))
        assertEquals(10.0, nearest.toRaw(100.0))
    }

    @Test
    fun verbalWithValuesRangesAndADefault() {
        val verbal = Conversion.Verbal(
            listOf(0.0 to "Off", 1.0 to "On"),
            listOf(Conversion.VerbalRange(2.0, 9.0, "Auto")),
            default = "Invalid"
        )
        assertEquals("On", verbal.text(1.0))
        assertEquals("Auto", verbal.text(9.0), "both ends of a range are in it")
        assertEquals("Invalid", verbal.text(10.0))
        assertEquals(1.0, verbal.rawForText("On"))
        assertEquals(2.0, verbal.rawForText("Auto"))
        assertNull(verbal.rawForText("Sideways"))
        assertEquals(5.0, verbal.toPhysical(5.0))
    }

    @Test
    fun statusTextOnANumericConversion() {
        val conversion = Conversion.WithStatus(Conversion.Linear(0.1, 0.0), Conversion.Verbal(listOf(65535.0 to "SNA")))
        assertEquals(1.5, conversion.toPhysical(15.0))
        assertEquals("SNA", conversion.text(65535.0))
        assertNull(conversion.text(15.0))
        assertEquals(65535.0, conversion.rawForText("SNA"))
    }

    @Test
    fun formulaWithAndWithoutAnInverse() {
        val forward = (Expression.parse("X1 * 0.5 - 10") as TkResult.Success).data
        val inverse = (Expression.parse("(X1 + 10) * 2") as TkResult.Success).data
        assertEquals(40.0, Conversion.Formula(forward, inverse).toRaw(10.0))
        // Without one, the raw range is searched
        val searched = Conversion.Formula(forward).toRaw(10.0, DataType.UBYTE.range)
        assertEquals(40.0, searched, 1e-9)
        assertTrue(Conversion.Formula(forward).toRaw(10_000.0, DataType.UBYTE.range).isNaN(), "out of the type's reach")
        assertTrue(Conversion.Formula(forward).toRaw(10.0).isNaN(), "no range to search")
    }

    @Test
    fun dataTypeRanges() {
        assertEquals(0.0..255.0, DataType.UBYTE.range)
        assertEquals(-128.0..127.0, DataType.SBYTE.range)
        assertEquals(-32768.0..32767.0, DataType.SWORD.range)
        assertEquals(4294967295.0, DataType.ULONG.max)
        assertEquals(-2147483648.0, DataType.SLONG.min)
        assertEquals(65504.0, DataType.FLOAT16_IEEE.max)
        assertEquals(3.0, DataType.UWORD.stored(2.6))
    }

    @Test
    fun halfFloats() {
        for (value in listOf(0.0f, 1.0f, -2.5f, 0.000061035156f, 65504f, 5.9604645E-8f, 0.33325195f)) {
            assertEquals(value, halfToFloat(floatToHalf(value)), "$value")
        }
        assertEquals(0x3C00, floatToHalf(1.0f))
        assertEquals(0xC100, floatToHalf(-2.5f))
        assertEquals(0x7C00, floatToHalf(70000f), "too big is infinity")
        assertEquals(0x3555, floatToHalf(1f / 3), "rounded to the nearest")
        assertTrue(halfToFloat(0x7E00).isNaN())
    }

    @Test
    fun numbersInAnImage() {
        val image = MemoryImage.of(0x100L to ByteArray(16))
        val order = ByteOrder.BIG_ENDIAN
        assertTrue(image.writeNumber(0x100, DataType.SWORD, order, -2.0))
        assertEquals(listOf(0xFF, 0xFE), listOf(image.byteAt(0x100), image.byteAt(0x101)))
        assertEquals(-2.0, image.readNumber(0x100, DataType.SWORD, order))
        assertEquals(65534.0, image.readNumber(0x100, DataType.UWORD, order))
        image.writeNumber(0x102, DataType.FLOAT16_IEEE, order, 1.5)
        assertEquals(1.5, image.readNumber(0x102, DataType.FLOAT16_IEEE, order))
        image.writeNumber(0x104, DataType.FLOAT32_IEEE, order, 0.25)
        assertEquals(0.25, image.readNumber(0x104, DataType.FLOAT32_IEEE, order))
        image.writeNumber(0x108, DataType.A_UINT64, order, 1e18)
        assertEquals(1e18, image.readNumber(0x108, DataType.A_UINT64, order))
        assertNull(image.readNumber(0x10E, DataType.ULONG, order), "runs off the end")
    }
}
