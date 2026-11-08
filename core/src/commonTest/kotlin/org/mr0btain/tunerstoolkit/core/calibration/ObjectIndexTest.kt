package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.a2l.A2lResolver
import org.mr0btain.tunerstoolkit.core.a2l.EpkCheck
import org.mr0btain.tunerstoolkit.core.a2l.EpkResult
import org.mr0btain.tunerstoolkit.core.a2l.readA2l
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.testing.DemoEcu
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ObjectIndexTest {
    private fun obj(name: String) = CalibrationObject(name, "", ObjectKind.VALUE, 0, Storage(emptyList(), ByteOrder.LITTLE_ENDIAN), Conversion.Identical)

    private fun part(name: String, start: Long, end: Long, role: PartRole = PartRole.VALUES) =
        IndexedPart(obj(name), ObjectPart(AddressRange(start, end), role))

    @Test
    fun findsEveryPartOverlappingARange() {
        val index = ObjectIndex.of(
            listOf(part("Long", 0, 1000), part("A", 10, 20), part("B", 15, 30), part("C", 40, 50), part("D", 2000, 2010))
        )
        assertEquals(listOf("Long", "A", "B"), index.overlapping(AddressRange(18, 19)).map { it.obj.name })
        assertEquals(listOf("Long", "C"), index.at(45).map { it.obj.name })
        assertEquals(listOf("Long"), index.at(30).map { it.obj.name }, "end is exclusive")
        assertEquals(emptyList(), index.at(1500).map { it.obj.name })
        assertEquals(listOf("D"), index.overlapping(AddressRange(2005, 3000)).map { it.obj.name })
        assertEquals(listOf(AddressRange(0, 1000), AddressRange(2000, 2010)), index.coverage)
    }

    @Test
    fun matchesABruteForceSearch() {
        val random = Random(7)
        val parts = List(500) { i ->
            val start = random.nextLong(0, 10_000)
            part("O$i", start, start + random.nextLong(1, if (i % 50 == 0) 3000 else 40))
        }
        val index = ObjectIndex.of(parts)
        repeat(300) {
            val start = random.nextLong(0, 11_000)
            val range = AddressRange(start, start + random.nextLong(1, 64))
            val expected = parts.filter { it.range.overlaps(range) }.map { it.obj.name }.toSet()
            assertEquals(expected, index.overlapping(range).map { it.obj.name }.toSet(), "$range")
        }
    }

    @Test
    fun overlapsBetweenDifferentObjects() {
        val index = ObjectIndex.of(listOf(part("A", 0, 10), part("A", 5, 8, PartRole.RECORD), part("B", 8, 12), part("C", 20, 30)))
        val overlaps = index.overlaps()
        assertEquals(1, overlaps.size)
        assertEquals(Triple("A", "B", AddressRange(8, 10)), overlaps.single().let { Triple(it.first, it.second, it.range) })
    }

    @Test
    fun theDemoIndexesEveryObjectWithItsParts() {
        val demo = DemoEcu.build()
        val set = A2lResolver(readA2l(demo.a2l).value.modules.single()).resolve()
        val build = ObjectIndex.build(set, demo.image)
        assertEquals(set.objects.size, build.decoded)
        assertEquals(emptyList(), build.problems.map { it.toString() })
        assertEquals(emptyList(), build.index.overlaps().map { "${it.first} ${it.second}" })
        val ignition = build.index.parts("IgnitionBase")
        assertEquals(listOf(PartRole.RECORD, PartRole.RECORD, PartRole.AXIS_POINTS, PartRole.AXIS_POINTS, PartRole.VALUES), ignition.map { it.role })
        assertEquals(listOf(0, 1), ignition.filter { it.role == PartRole.AXIS_POINTS }.map { it.axis })
        val valuesStart = ignition.last().range.start
        assertEquals(listOf("IgnitionBase"), build.index.at(valuesStart).map { it.obj.name })
        // A map on shared axes only holds its values; the axes are their own objects
        assertEquals(listOf(PartRole.VALUES), build.index.parts("BoostTarget").map { it.role })
        assertEquals(listOf(PartRole.RECORD, PartRole.VALUES), build.index.parts("RpmAxis16").map { it.role })
    }

    @Test
    fun objectsThatCantBeReadAreReported() {
        val demo = DemoEcu.build()
        val set = A2lResolver(readA2l(demo.a2l).value.modules.single()).resolve()
        val build = ObjectIndex.build(set, MemoryImage.of(DemoEcu.CAL_START to ByteArray(4)))
        assertTrue(build.decoded < set.objects.size)
        assertTrue(build.problems.all { it.severity == Severity.ERROR && it.objectName != null })
        assertEquals(set.objects.size - build.decoded, build.problems.size)
    }

    @Test
    fun epkCheck() {
        val demo = DemoEcu.build()
        val module = readA2l(demo.a2l).value.modules.single()
        assertEquals(EpkResult.Match(DemoEcu.EPK_ADDRESS, DemoEcu.EPK), EpkCheck.check(module, demo.image))

        val other = demo.image.snapshot().also { it.write(DemoEcu.EPK_ADDRESS, "XX".encodeToByteArray()) }
        val mismatch = assertIs<EpkResult.Mismatch>(EpkCheck.check(module, other))
        assertTrue(mismatch.found.startsWith("XXDEMO1"))

        assertIs<EpkResult.NotInImage>(EpkCheck.check(module, MemoryImage.of(0L to ByteArray(4))))
        val noEpk = readA2l("/begin PROJECT P \"\" /begin MODULE M \"\" /end MODULE /end PROJECT").value.modules.single()
        assertEquals(EpkResult.NotDescribed, EpkCheck.check(noEpk, demo.image))
    }
}
