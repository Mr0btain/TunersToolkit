package org.mr0btain.tunerstoolkit.core.formats.values

import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.exactNumber
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlElement
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlParser
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlWriter

/**
 * ASAM CDF 2.0 (.cdfx): calibration values as XML, one SW-INSTANCE per object with its physical values
 * (SW-VALUES-PHYS: V numbers, VT texts, VG groups) and its axes (SW-AXIS-CONT; a shared one by SW-INSTANCE-REF).
 *
 * Maps are written as one VG per x point holding that column's y values; cubes nest a VG per point of their outer
 * axes around that. Reading also takes values as one flat list in that order, and PaCo files, which keep their values
 * in the same SW-INSTANCE structure
 */
object Cdfx {
    /** More values than any real object has, so a file's sizes can't ask for a huge array */
    const val MAX_VALUES = 4_000_000

    fun looksLike(bytes: ByteArray): Boolean {
        val start = bytes.copyOf(minOf(bytes.size, 8192)).decodeToString()
        return "<MSRSW" in start || "SW-INSTANCE" in start
    }

    // ------------------------------------------------ Writing

    fun write(records: List<DataRecord>, system: String, title: String = system): Pair<String, List<Diagnostic>> {
        val skipped = mutableListOf<Diagnostic>()
        val xml = XmlWriter()
        xml.raw("<!DOCTYPE MSRSW PUBLIC \"-//ASAM//DTD CALIBRATION DATA FORMAT:V2.0.0:LAI:IAI:XML:CDF200.XSD//EN\" \"cdf_v2.0.0.sl.dtd\">")
        xml.element("MSRSW") {
            leaf("SHORT-NAME", title)
            leaf("CATEGORY", "CDF20")
            element("SW-SYSTEMS") {
                element("SW-SYSTEM") {
                    leaf("SHORT-NAME", system)
                    element("SW-INSTANCE-SPEC") {
                        element("SW-INSTANCE-TREE") {
                            leaf("SHORT-NAME", "Values")
                            leaf("CATEGORY", "NO_VCD")
                            for (record in records) {
                                val category = category(record) ?: run {
                                    skipped += Diagnostic(Severity.WARNING, "Not in the CDFX: there's no CDF category for a ${record.kind.name.lowercase()}", objectName = record.name)
                                    null
                                } ?: continue
                                instance(this, record, category)
                            }
                        }
                    }
                }
            }
        }
        return xml.toString() to skipped
    }

    private fun category(record: DataRecord): String? = when (record.kind) {
        ObjectKind.VALUE, ObjectKind.CURVE, ObjectKind.MAP, ObjectKind.CUBOID, ObjectKind.CUBE_4, ObjectKind.CUBE_5, ObjectKind.VAL_BLK, ObjectKind.ASCII -> record.kind.name
        ObjectKind.AXIS -> "COM_AXIS"
    }

    private fun instance(xml: XmlWriter, record: DataRecord, category: String) = xml.element("SW-INSTANCE") {
        leaf("SHORT-NAME", record.name)
        if (record.description.isNotBlank()) leaf("LONG-NAME", record.description)
        leaf("CATEGORY", category)
        element("SW-VALUE-CONT") {
            if (record.unit.isNotBlank()) leaf("UNIT-DISPLAY-NAME", record.unit)
            if (record.kind == ObjectKind.VAL_BLK) element("SW-ARRAYSIZE") { record.dimensions.forEach { leaf("V", it.toString()) } }
            element("SW-VALUES-PHYS") {
                when {
                    record.kind == ObjectKind.ASCII -> leaf("VT", record.text.orEmpty())
                    record.dimensions.size >= 2 -> groups(this, record, record.dimensions.size - 1, IntArray(record.dimensions.size))
                    record.texts != null -> record.texts.forEach { leaf("VT", it.orEmpty()) }
                    else -> record.values.forEach { leaf("V", exactNumber(it)) }
                }
            }
        }
        if (record.axes.isNotEmpty()) {
            element("SW-AXIS-CONTS") {
                record.axes.forEach { axis ->
                    element("SW-AXIS-CONT") {
                        leaf("CATEGORY", when {
                            axis.shared != null -> "COM_AXIS"
                            axis.fixed -> "FIX_AXIS"
                            else -> "STD_AXIS"
                        })
                        if (axis.unit.isNotBlank()) leaf("UNIT-DISPLAY-NAME", axis.unit)
                        if (axis.shared != null) leaf("SW-INSTANCE-REF", axis.shared)
                        element("SW-VALUES-PHYS") {
                            if (axis.texts != null) axis.texts.forEach { leaf("VT", it) } else axis.values.forEach { leaf("V", exactNumber(it)) }
                        }
                    }
                }
            }
        }
    }

    /** VG per point of dimension [d] (outermost first), down to y's values inside each x */
    private fun groups(xml: XmlWriter, record: DataRecord, d: Int, at: IntArray) {
        val dims = record.dimensions
        if (d == 1) {
            // Innermost: a VG per x holding the y values
            for (x in 0 until dims[0]) {
                at[0] = x
                xml.element("VG") {
                    for (y in 0 until dims[1]) {
                        at[1] = y
                        val index = indexOf(at, dims)
                        val text = record.texts?.getOrNull(index)
                        if (text != null) leaf("VT", text) else leaf("V", exactNumber(record.values[index]))
                    }
                }
            }
            return
        }
        for (p in 0 until dims[d]) {
            at[d] = p
            xml.element("VG") { groups(this, record, d - 1, at) }
        }
    }

    /** x first: x + nx * (y + ny * (z + …)) */
    private fun indexOf(at: IntArray, dims: List<Int>): Int {
        var index = 0
        var stride = 1
        for (d in dims.indices) {
            index += at[d] * stride
            stride *= dims[d]
        }
        return index
    }

    // ------------------------------------------------ Reading

    fun read(bytes: ByteArray, name: String = ""): TkResult<Imported<List<DataRecord>>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val (root, xmlProblems) = XmlParser.parse(bytes.decodeToString(), name)
        root ?: return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        val problems = xmlProblems.toMutableList()
        val instances = root.walk().filter { it.name.equals("SW-INSTANCE", ignoreCase = true) }.toList()
        if (instances.isEmpty()) return TkResult.Failure(ImportError.NO_DATA)
        val records = instances.mapNotNull { instance ->
            try {
                record(instance, name, problems)
            } catch (e: Exception) {
                problems += Diagnostic(Severity.ERROR, e.message ?: "Can't be read", SourceLocation(name, instance.line), instance.childText("SHORT-NAME"))
                null
            }
        }
        return TkResult.Success(Imported(records, problems))
    }

    private fun record(instance: XmlElement, file: String, problems: MutableList<Diagnostic>): DataRecord? {
        val name = instance.childText("SHORT-NAME") ?: return null
        val location = SourceLocation(file, instance.line)
        val category = instance.childText("CATEGORY")?.uppercase().orEmpty()
        val content = instance.child("SW-VALUE-CONT")
        val physical = content?.child("SW-VALUES-PHYS")
        val axes = instance.child("SW-AXIS-CONTS")?.children("SW-AXIS-CONT").orEmpty().map { axis ->
            val leaves = leaves(axis.child("SW-VALUES-PHYS"))
            val texts = leaves.takeIf { list -> list.isNotEmpty() && list.all { it.first } }?.map { it.second }
            DataAxis(
                values = if (texts != null) texts.indices.map { it.toDouble() } else leaves.map { number(it.second) },
                texts = texts,
                unit = axis.childText("UNIT-DISPLAY-NAME").orEmpty(),
                shared = axis.childText("SW-INSTANCE-REF"),
                fixed = axis.childText("CATEGORY").equals("FIX_AXIS", ignoreCase = true)
            )
        }
        val unit = content?.childText("UNIT-DISPLAY-NAME").orEmpty()
        val description = instance.childText("LONG-NAME").orEmpty()
        val kind = when (category) {
            "VALUE", "BOOLEAN", "DEPENDENT_VALUE" -> ObjectKind.VALUE
            "ASCII" -> ObjectKind.ASCII
            "VAL_BLK" -> ObjectKind.VAL_BLK
            "CURVE" -> ObjectKind.CURVE
            "MAP" -> ObjectKind.MAP
            "CUBOID" -> ObjectKind.CUBOID
            "CUBE_4" -> ObjectKind.CUBE_4
            "CUBE_5" -> ObjectKind.CUBE_5
            "COM_AXIS", "RES_AXIS", "CURVE_AXIS" -> ObjectKind.AXIS
            else -> {
                problems += Diagnostic(Severity.WARNING, "Category $category isn't a kind of value, skipped", location, name)
                return null
            }
        }
        if (kind == ObjectKind.ASCII) {
            return DataRecord(name, kind, emptyList(), emptyList(), text = leaves(physical).joinToString("") { it.second }, description = description, location = location)
        }
        val groups = physical?.let(::groupSizes).orEmpty()
        val leaves = leaves(physical)
        val axisCount = kind.axisCount
        val dims: List<Int> = when {
            kind == ObjectKind.VALUE -> emptyList()
            kind == ObjectKind.AXIS -> listOf(leaves.size)
            kind == ObjectKind.VAL_BLK -> content?.child("SW-ARRAYSIZE")?.let { size -> leaves(size).map { number(it.second).toInt() } }?.takeIf { it.isNotEmpty() }
                ?: groups.takeIf { it.size >= 2 } ?: listOf(leaves.size)
            axisCount >= 2 && groups.size >= 2 -> groups
            axes.size >= axisCount -> axes.take(axisCount).map { it.values.size }
            else -> listOf(leaves.size)
        }
        require(dims.all { it >= 1 }) { "$name has a size of ${dims.joinToString(" × ")}" }
        var product = 1L
        for (size in dims) {
            product *= size
            require(product <= MAX_VALUES) { "$name says it has more than $MAX_VALUES values" }
        }
        require(leaves.size.toLong() == product) { "$name has ${leaves.size} values but its size is ${dims.joinToString(" × ")}" }
        // Read as nested: outer axes first, x, then y innermost; stored here x first
        val order = if (dims.size >= 2) nestedToXFirst(dims) else IntArray(leaves.size) { it }
        val inOrder = arrayOfNulls<Pair<Boolean, String>>(leaves.size)
        leaves.forEachIndexed { i, leaf -> inOrder[order[i]] = leaf }
        val allText = inOrder.all { it!!.first }
        return DataRecord(
            name = name,
            kind = kind,
            dimensions = dims,
            values = if (allText) emptyList() else inOrder.map { number(it!!.second) },
            texts = if (allText) inOrder.map { it!!.second } else null,
            axes = axes,
            unit = unit,
            description = description,
            location = location
        )
    }

    /** For values listed nested (last dimension outermost, then x, then y innermost), each one's x first index */
    private fun nestedToXFirst(dims: List<Int>): IntArray {
        val total = dims.fold(1) { a, b -> a * b }
        val result = IntArray(total)
        val at = IntArray(dims.size)
        // The walk order: dimensions from last to 2, then x (0), then y (1)
        val walk = (dims.size - 1 downTo 2).toList() + listOf(0, 1)
        for (n in 0 until total) {
            var rest = n
            for (d in walk.asReversed()) {
                at[d] = rest % dims[d]
                rest /= dims[d]
            }
            result[n] = indexOf(at, dims)
        }
        return result
    }

    /** The sizes of nested VGs (outermost first, as x then y for a map), or empty when the values aren't grouped */
    private fun groupSizes(element: XmlElement): List<Int> {
        val groups = element.children("VG")
        if (groups.isEmpty()) return emptyList()
        val inner = groupSizes(groups.first())
        val leavesInFirst = groups.first().children.count { it.name.equals("V", true) || it.name.equals("VT", true) }
        val nested = if (inner.isEmpty()) listOf(groups.size, leavesInFirst) else listOf(groups.size) + inner
        // Back to x first: [outer…, x, y] becomes [x, y, …outer reversed]
        return if (nested.size <= 2) nested else nested.takeLast(2) + nested.dropLast(2).asReversed()
    }

    /** Every V (false) and VT (true) in document order, through any VGs */
    private fun leaves(element: XmlElement?): List<Pair<Boolean, String>> {
        element ?: return emptyList()
        return element.walk().drop(1).mapNotNull {
            when {
                it.name.equals("V", ignoreCase = true) -> false to it.text.trim()
                it.name.equals("VT", ignoreCase = true) -> true to it.text
                else -> null
            }
        }.toList()
    }

    private fun number(text: String): Double = text.trim().replace(',', '.').toDoubleOrNull() ?: throw IllegalArgumentException("\"$text\" isn't a number")
}
