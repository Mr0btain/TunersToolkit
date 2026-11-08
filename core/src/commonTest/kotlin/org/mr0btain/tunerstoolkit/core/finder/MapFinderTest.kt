package org.mr0btain.tunerstoolkit.core.finder

import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ObjectIndex
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.decode
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapFinderTest {
    private val demo = DemoEcu.build()
    private val set = A2lResolver(readA2l(demo.a2l).value.modules.single()).resolve()

    @Test
    fun findsTheDemosMapsThatCarryTheirOwnAxes() {
        val found = MapFinder.find(demo.image)
        val byAddress = found.associateBy { it.record.start }

        val ignition = byAddress.getValue(demo.placed.getValue("IgnitionBase").address)
        assertEquals(ObjectKind.MAP, ignition.kind)
        assertEquals(16 to 12, ignition.columns to ignition.rows)
        assertEquals(ValueOrder.Y_FIRST, ignition.order, "the demo stores maps column by column")
        assertEquals(DataType.UBYTE, ignition.valueType)
        assertEquals(DataType.UWORD, ignition.x.type)
        assertEquals(ByteOrder.LITTLE_ENDIAN, ignition.byteOrder)
        assertTrue(ignition.counted)
        assertTrue(ignition.confidence >= 90, "${ignition.confidence}")

        val efficiency = byAddress.getValue(demo.placed.getValue("VolumetricEfficiency").address)
        assertEquals(DataType.UWORD, efficiency.valueType)

        val warmup = byAddress.getValue(demo.placed.getValue("WarmupEnrichment").address)
        assertEquals(ObjectKind.CURVE, warmup.kind)
        assertEquals(9, warmup.columns)

        // Shared axes back to back aren't taken for a curve
        assertTrue(found.none { it.record.start == demo.placed.getValue("RpmAxis16").address }, found.joinToString { it.label })
        // Even when the next axis's count is another type (LoadAxis12's are words, PedalAxis10's bytes)
        assertTrue(found.none { it.record.start == demo.placed.getValue("LoadAxis12").address }, found.joinToString { it.label })
    }

    @Test
    fun aConfirmedSuggestionReadsTheSameAsTheA2l() {
        val found = MapFinder.find(demo.image).first { it.record.start == demo.placed.getValue("IgnitionBase").address }
        val made = CalibrationSet(found.toManualMap("Mine").objects())
        val mine = made.decode(demo.image, made["Mine"]!!).getOrNull()!!
        val a2l = set.decode(demo.image, set["IgnitionBase"]!!).getOrNull()!!
        assertEquals(a2l.raw.toList(), mine.raw.toList())
        assertEquals(a2l.axes.map { it.raw.toList() }, mine.axes.map { it.raw.toList() })
        assertEquals(found.values.toList(), mine.raw.toList())
    }

    @Test
    fun whatTheDefinitionCoversIsSkipped() {
        val index = ObjectIndex.build(set, demo.image).index
        val found = MapFinder.find(demo.image, FinderOptions(skip = index.coverage))
        assertTrue(found.none { map -> index.coverage.any { it.overlaps(map.record) } })
        assertTrue(found.isEmpty(), found.joinToString { it.label })
    }

    @Test
    fun randomBytesGiveNothing() {
        val random = Random(3)
        val noise = MemoryImage.of(0L to ByteArray(512 * 1024) { random.nextInt(256).toByte() })
        assertEquals(emptyList(), MapFinder.find(noise).map { "${it.confidence} ${it.label}" })
        // Erased flash neither
        assertEquals(emptyList(), MapFinder.find(MemoryImage.of(0L to ByteArray(64 * 1024) { 0xFF.toByte() })))
    }

    @Test
    fun aBigEndianMapWithBareAxesAmongNoise() {
        // Noise, then x points (8, UWORD MSB first), y points (6), and a smooth 8 × 6 SWORD map stored row by row
        val random = Random(9)
        val bytes = ByteArray(4096) { random.nextInt(256).toByte() }
        fun put16(at: Int, value: Int) {
            bytes[at] = (value shr 8).toByte()
            bytes[at + 1] = value.toByte()
        }
        var at = 0x400
        val xs = listOf(500, 1000, 1500, 2000, 3000, 4000, 5000, 6500)
        val ys = listOf(10, 25, 40, 60, 80, 100)
        xs.forEach { put16(at, it); at += 2 }
        ys.forEach { put16(at, it); at += 2 }
        val valuesAt = at
        for (y in ys.indices) for (x in xs.indices) { put16(at, (300 * sin(x / 3.0) + y * 40 - 200).toInt()); at += 2 }
        val image = MemoryImage.of(0x10000L to bytes)
        val found = MapFinder.find(image, FinderOptions(minConfidence = 40)).first { it.address == 0x10000L + valuesAt }
        assertEquals(8 to 6, found.columns to found.rows)
        assertEquals(DataType.SWORD, found.valueType)
        assertEquals(ByteOrder.BIG_ENDIAN, found.byteOrder)
        assertEquals(ValueOrder.X_FIRST, found.order)
        assertTrue(!found.counted)
        assertEquals(xs.map(Int::toDouble), found.x.points.toList())
    }

    @Test
    fun itStopsWhenAsked() {
        var calls = 0
        val found = MapFinder.find(demo.image, cancelled = { calls++ > 2 })
        assertTrue(found.size < MapFinder.find(demo.image).size)
        var last = 0.0
        MapFinder.find(demo.image, progress = { last = it })
        assertEquals(1.0, last)
        assertTrue(MapFinder.find(demo.image, FinderOptions(ranges = listOf(AddressRange(0, 0x100)))).isEmpty())
    }
}
