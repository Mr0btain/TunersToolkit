package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.a2l.A2lFolders
import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapEditTest {
    private val demo = DemoEcu.build()
    private val module = readA2l(demo.a2l).value.modules.single()
    private val set = A2lResolver(module).resolve()

    private fun read(image: MemoryImage, name: String) = (set.decode(image, set[name]!!) as TkResult.Success).data

    /** A 3 x 2 block of UBYTEs, values (x, y) = 10 y + x, with a 0.5 factor */
    private fun block(): Pair<MemoryImage, CalibrationValues> {
        val image = MemoryImage.of(0x100L to byteArrayOf(0, 2, 4, 20, 22, 24))
        val obj = CalibrationObject(
            "B", "", ObjectKind.VAL_BLK, 0x100, Storage(listOf(RecordField.Values(DataType.UBYTE, ValueOrder.X_FIRST)), ByteOrder.LITTLE_ENDIAN),
            Conversion.Linear(0.5, 0.0), dimensions = listOf(3, 2), lowerLimit = 0.0, upperLimit = 100.0
        )
        return image to (CalibrationCodec.decode(image, obj) as TkResult.Success).data
    }

    @Test
    fun selectionsCoverTheirCells() {
        val (_, values) = block()
        assertEquals(3, values.columns)
        assertEquals(2, values.rows)
        val selection = CellSelection.between(2 to 1, 1 to 0)
        assertEquals(listOf(1, 2, 4, 5), selection.indices(values))
        assertEquals(6, CellSelection.all(values).indices(values).size)
    }

    @Test
    fun arithmetic() {
        val (_, values) = block()
        val all = CellSelection.all(values)
        assertEquals(mapOf(0 to 7.0, 1 to 7.0), MapEdit.apply(values, CellSelection(0..1, 0..0), MapOperation.SET, 7.0))
        // Value 4 is (1, 1): 11
        assertEquals(12.0, MapEdit.apply(values, all, MapOperation.ADD, 1.0)[4])
        assertEquals(22.0, MapEdit.apply(values, all, MapOperation.MULTIPLY, 2.0)[4])
        assertEquals(12.1, MapEdit.apply(values, all, MapOperation.ADD_PERCENT, 10.0)[4]!!, 1e-9)
    }

    @Test
    fun interpolateFollowsTheAxisPoints() {
        // IgnitionBase's speed axis isn't evenly spaced, so the middle of a row lands by speed, not by position
        val values = read(demo.image, "IgnitionBase")
        val row = CellSelection(0..2, 0..0)
        val result = MapEdit.apply(values, row, MapOperation.INTERPOLATE)
        val speeds = values.axes[0].physical
        val first = values[0, 0]
        val last = values[2, 0]
        val expected = first + (last - first) * (speeds[1] - speeds[0]) / (speeds[2] - speeds[0])
        assertEquals(expected, result[values.indexAt(1, 0)]!!, 1e-9)
        assertEquals(first, result[values.indexAt(0, 0)])
        assertEquals(last, result[values.indexAt(2, 0)])
    }

    @Test
    fun interpolateASurfaceFromItsCorners() {
        val (_, values) = block()
        // Corners 0, 2 (top) and 10, 12 (bottom) along positions, since a block has no axes
        val result = MapEdit.apply(values, CellSelection.all(values), MapOperation.INTERPOLATE)
        assertEquals(1.0, result[1])
        assertEquals(11.0, result[4])
    }

    @Test
    fun smoothAveragesNeighbours() {
        val (_, values) = block()
        val result = MapEdit.apply(values, CellSelection(0..0, 0..0), MapOperation.SMOOTH)
        // (0, 0) with (1, 0), (0, 1), (1, 1): 0, 1, 10, 11
        assertEquals(5.5, result.values.single())
    }

    @Test
    fun pasteFromASpreadsheet() {
        val (_, values) = block()
        val grid = Tsv.parse("1\t2\t3\t4\r\n5\t\t7\n")
        val result = MapEdit.paste(values, CellSelection(1..1, 0..0), grid)
        assertEquals(mapOf(1 to 1.0, 2 to 2.0, 4 to 5.0), result, "cut off at the edge, blanks left alone")
        assertEquals(6, MapEdit.paste(values, CellSelection.all(values), listOf(listOf(9.0))).size, "one value fills the selection")
        assertEquals(listOf(listOf("1", "2"), listOf("11", "12")), MapEdit.copy(values, CellSelection(1..2, 0..1)))
    }

    @Test
    fun tsv() {
        assertEquals(listOf(listOf(1.5, 2.0), listOf(null, -3.25)), Tsv.parse("1,5\t2\nabc\t-3.25"))
        assertEquals(listOf(listOf(1234.5)), Tsv.parse("1.234,5"))
        assertEquals(emptyList(), Tsv.parse(""))
        assertEquals("1\t2\n3\t4", Tsv.format(listOf(listOf("1", "2"), listOf("3", "4"))))
        assertEquals("0.333333", plainNumber(1.0 / 3))
        assertEquals("-2", plainNumber(-2.0))
    }

    @Test
    fun writeAllIsOneStepAndClamps() {
        val (image, values) = block()
        val summary = CalibrationCodec.writeAll(image, values, mapOf(0 to 3.0, 1 to 500.0, 2 to Double.NaN, 9 to 1.0))
        assertEquals(2, summary.written)
        assertEquals(1, summary.clamped)
        assertEquals(setOf(2, 9), summary.failed.keys)
        assertEquals(6, image.byteAt(0x100))
        assertEquals(200, image.byteAt(0x101), "500 clamped to the upper limit 100, raw 200")
        image.undo()
        assertEquals(listOf(0, 2), listOf(image.byteAt(0x100), image.byteAt(0x101)), "undone together")
    }

    @Test
    fun stepIsOneRawUnit() {
        val (_, values) = block()
        assertEquals(0.5, values.step(0))
        val q15 = read(demo.image, "InjectorTrim")
        assertTrue(abs(q15.step(0) - 1.0 / 32768) < 1e-12)
    }

    @Test
    fun functionsAndSearchInTheMapList() {
        val folders = A2lFolders.byFunction(module, set)
        assertEquals(listOf("Ignition", "Fueling", "Boost", "Driveability", "Limits", "Identification"), folders.map { it.name })
        val closed = ObjectTree.rows(folders, set, expanded = emptySet())
        assertTrue(closed.all { it is TreeRow.Folder && !it.expanded })
        assertEquals(3, (closed.first() as TreeRow.Folder).count)

        val open = ObjectTree.rows(folders, set, expanded = setOf("Fueling"))
        assertEquals(
            listOf("VolumetricEfficiency", "LambdaTarget", "WarmupEnrichment", "InjectorTrim", "InjDeadTimeOffset"),
            open.filterIsInstance<TreeRow.Object>().map { it.obj.name }
        )

        val found = ObjectTree.rows(folders, set, expanded = emptySet(), query = "boost")
        assertEquals(listOf("BoostTarget", "MaxBoost"), found.filterIsInstance<TreeRow.Object>().map { it.obj.name })
        assertEquals(listOf("Boost"), found.filterIsInstance<TreeRow.Folder>().map { it.folder.name })
        // By description too, any case
        assertEquals(listOf("RevLimit"), ObjectTree.rows(folders, set, emptySet(), "SPEED LIMIT").filterIsInstance<TreeRow.Object>().map { it.obj.name })
    }

    @Test
    fun objectsInNoFolderAndCyclesStillShow() {
        val text = demo.a2l.replace("/end MODULE", "/begin FUNCTION Loop \"\" /begin SUB_FUNCTION Loop /end SUB_FUNCTION /end FUNCTION\n/begin GROUP G \"\" ROOT /begin REF_CHARACTERISTIC RevLimit /end REF_CHARACTERISTIC /end GROUP\n/end MODULE")
        val looped = readA2l(text).value.modules.single()
        val folders = A2lFolders.byFunction(looped, set)
        ObjectTree.rows(folders, set, expanded = folders.map { it.name }.toSet())
        val groups = A2lFolders.byGroup(looped, set)
        assertEquals(listOf("G", "Not in a group"), groups.map { it.name })
        assertEquals(set.objects.size - 1, groups.last().objects.size)
    }

    @Test
    fun compareNamesTheObjectsThatChanged() {
        val image = demo.image.snapshot()
        val index = ObjectIndex.build(set, image).index
        val boost = read(image, "BoostTarget")
        CalibrationCodec.write(image, boost, 5, 1500.0)
        image.write(0x0010, byteArrayOf(0x55))
        val comparison = Compare.images(image, demo.image, index)
        assertEquals(listOf("BoostTarget"), comparison.changedObjects.map { it.name })
        assertEquals(listOf(AddressRange(0x10, 0x11)), comparison.outsideObjects.map { it.range })

        // Copying the map back from the original undoes the change
        val copied = Compare.copyObject(set["BoostTarget"]!!, index, demo.image, image)
        assertEquals(boost.size * 2, copied)
        assertEquals(emptyList(), Compare.images(image, demo.image, index).changedObjects)
    }
}
