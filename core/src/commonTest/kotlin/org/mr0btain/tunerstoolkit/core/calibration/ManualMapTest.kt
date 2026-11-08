package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ManualMapTest {
    @Test
    fun aMapWithAnAxisInMemoryAndOneEvenlySpaced() {
        // x points (UWORD, LSB first, factor 10) at 0x100; values (y first) at 0x110
        val image = MemoryImage.of(0x100L to byteArrayOf(10, 0, 20, 0, 30, 0) + ByteArray(10) + byteArrayOf(1, 2, 3, 4, 5, 6))
        val manual = ManualMap(
            name = "Boost", kind = ObjectKind.MAP, address = 0x110, columns = 3, rows = 2, order = ValueOrder.Y_FIRST,
            factor = 0.5, offset = 1.0, unit = "bar",
            x = ManualAxis(ManualAxis.Source.MEMORY, address = 0x100, type = DataType.UWORD, byteOrder = ByteOrder.LITTLE_ENDIAN, factor = 10.0),
            y = ManualAxis(ManualAxis.Source.EVEN, from = 20.0, to = 80.0)
        )
        assertEquals(null, manual.problem())
        val objects = manual.objects()
        assertEquals(listOf("Boost", "Boost X axis"), objects.map { it.name })
        val set = CalibrationSet(objects)
        val values = assertNotNull(set.decode(image, set["Boost"]!!).getOrNull())
        assertEquals(listOf(100.0, 200.0, 300.0), values.axes[0].physical.toList())
        assertEquals(listOf(20.0, 80.0), values.axes[1].physical.toList())
        assertEquals(1.0 + 0.5 * 4, values[1, 1], "y first: (1, 1) is the 4th stored value")

        // And the form can be filled in from it again
        assertEquals(manual, ManualMap.of(set["Boost"]!!, set))
    }

    @Test
    fun problems() {
        assertEquals("It needs a name", ManualMap(name = " ", address = 0).problem())
        assertEquals("The factor can't be 0", ManualMap(name = "A", address = 0, factor = 0.0).problem())
        assertEquals("Up to 1024 points along each axis", ManualMap(name = "A", address = 0, columns = 5000).problem())
        val block = ManualMap(name = "B", kind = ObjectKind.VAL_BLK, address = 0, columns = 4, rows = 2).objects().single()
        assertEquals(listOf(4, 2), block.dimensions)
    }
}
