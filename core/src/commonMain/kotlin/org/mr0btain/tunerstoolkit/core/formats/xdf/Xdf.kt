package org.mr0btain.tunerstoolkit.core.formats.xdf

import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationAxis
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.Expression
import org.mr0btain.tunerstoolkit.core.calibration.ObjectFolder
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.Storage
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.plainNumber
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlElement
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlParser
import org.mr0btain.tunerstoolkit.core.formats.xml.XmlWriter
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.math.abs

/**
 * A TunerPro XDF read into calibration objects
 * @property folders Its categories, each a folder of the objects in it
 */
class XdfDefinition(val title: String, val description: String, val set: CalibrationSet, val folders: List<ObjectFolder>)

/**
 * TunerPro's XDF definition format (XML): constants, tables with their axes, and flags, at offsets into the binary.
 *
 * Addresses: an XDF counts from the start of the binary file, which is where the image's first byte is
 * ([imageStart]), plus or minus its BASEOFFSET.
 *
 * mmedtypeflags are read as TunerPro writes them: 0x01 signed, 0x02 LSB first, 0x04 column major, 0x10000 floating
 * point. MATH equations use the variable X for the stored value; `^` is a power. Equations reading other objects
 * (more than one VAR), strided data, checksums and patches aren't supported: they're reported and left out
 */
object Xdf {
    const val SIGNED = 0x01
    const val LSB_FIRST = 0x02
    const val COLUMN_MAJOR = 0x04
    const val FLOAT = 0x10000

    /** Whether [bytes] look like an XDF */
    fun looksLike(bytes: ByteArray): Boolean = bytes.copyOf(minOf(bytes.size, 4096)).decodeToString().contains("<XDFFORMAT", ignoreCase = true)

    fun read(bytes: ByteArray, name: String = "", imageStart: Long = 0): TkResult<Imported<XdfDefinition>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val (root, xmlProblems) = XmlParser.parse(bytes.decodeToString(), name)
        if (root == null || !root.name.equals("XDFFORMAT", ignoreCase = true)) return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        val reader = Reader(root, name, imageStart)
        val definition = reader.read()
        return TkResult.Success(Imported(definition, xmlProblems + reader.problems))
    }

    // ------------------------------------------------ Reading

    private class Embedded(
        val address: Long,
        val type: DataType,
        val order: ByteOrder,
        val columnMajor: Boolean,
        val rows: Int,
        val columns: Int
    )

    private class Reader(private val root: XmlElement, private val file: String, imageStart: Long) {
        val problems = mutableListOf<Diagnostic>()
        private val header = root.child("XDFHEADER")
        private val defaults = header?.child("DEFAULTS")
        private val base: Long = run {
            val offset = header?.child("BASEOFFSET")
            val value = offset?.attribute("offset")?.let(::number)?.toLong() ?: 0L
            val subtract = offset?.attribute("subtract")?.let(::number)?.toInt() == 1
            imageStart + if (subtract) -value else value
        }
        private val categories: Map<Int, String> = header?.children("CATEGORY").orEmpty().mapNotNull { category ->
            val index = category.attribute("index")?.let(::number)?.toInt() ?: return@mapNotNull null
            index to (category.attribute("name") ?: "Category $index")
        }.toMap()
        private val names = mutableSetOf<String>()
        private val objects = mutableListOf<CalibrationObject>()
        private val inCategory = linkedMapOf<String, MutableList<String>>()
        private val byId = mutableMapOf<String, XmlElement>()

        fun read(): XdfDefinition {
            root.children.forEach { element -> element.attribute("uniqueid")?.let { byId[it.lowercase()] = element } }
            for (element in root.children) {
                when (element.name.uppercase()) {
                    "XDFCONSTANT" -> constant(element)
                    "XDFTABLE" -> table(element)
                    "XDFFLAG" -> flag(element)
                    "XDFHEADER" -> Unit
                    "XDFCHECKSUM", "XDFPATCH" -> problem(Severity.INFO, element, "${element.name} \"${title(element)}\" isn't supported yet, left out")
                    else -> problem(Severity.INFO, element, "Unknown element <${element.name}>, left out")
                }
            }
            // In the order the categories are numbered
            val folders = categories.entries.sortedBy { it.key }.mapNotNull { (_, category) ->
                inCategory[category]?.let { ObjectFolder(category, objects = it) }
            }
            val rest = objects.filter { obj -> obj.kind != ObjectKind.AXIS && inCategory.values.none { obj.name in it } }.map { it.name }
            return XdfDefinition(
                title = header?.childText("deftitle").orEmpty().ifBlank { file },
                description = header?.childText("description").orEmpty(),
                set = CalibrationSet(objects, problems.toList()),
                folders = folders + if (rest.isEmpty()) emptyList() else listOf(ObjectFolder("Not in a category", objects = rest))
            )
        }

        private fun constant(element: XmlElement) {
            val name = uniqueName(element)
            val embedded = embedded(element.child("EMBEDDEDDATA"), element, name) ?: return
            val conversion = math(element, name) ?: return
            add(element, CalibrationObject(
                name = name,
                description = element.childText("description").orEmpty(),
                kind = ObjectKind.VALUE,
                address = embedded.address,
                storage = Storage(listOf(RecordField.Values(embedded.type, ValueOrder.X_FIRST)), embedded.order),
                conversion = conversion,
                unit = element.childText("units").orEmpty(),
                format = decimals(element),
                lowerLimit = element.childText("min")?.let(::number) ?: Double.NEGATIVE_INFINITY,
                upperLimit = element.childText("max")?.let(::number) ?: Double.POSITIVE_INFINITY,
                source = SourceLocation(file, element.line)
            ))
        }

        private fun flag(element: XmlElement) {
            val name = uniqueName(element)
            val embedded = embedded(element.child("EMBEDDEDDATA"), element, name) ?: return
            val mask = element.childText("mask")?.let(::number)?.toLong()?.takeIf { it != 0L } ?: return problem(Severity.ERROR, element, "Flag $name has no mask, left out", name)
            val single = mask and (mask - 1) == 0L
            add(element, CalibrationObject(
                name = name,
                description = element.childText("description").orEmpty(),
                kind = ObjectKind.VALUE,
                address = embedded.address,
                storage = Storage(listOf(RecordField.Values(embedded.type.unsigned(), ValueOrder.X_FIRST)), embedded.order),
                conversion = if (single) Conversion.Verbal(listOf(0.0 to "Off", 1.0 to "On")) else Conversion.Identical,
                bitMask = mask,
                source = SourceLocation(file, element.line)
            ))
        }

        private fun table(element: XmlElement) {
            val name = uniqueName(element)
            val axes = element.children("XDFAXIS").associateBy { it.attribute("id")?.lowercase() }
            val z = axes["z"] ?: return problem(Severity.ERROR, element, "Table $name has no z axis, left out", name)
            val data = embedded(z.child("EMBEDDEDDATA"), element, name) ?: return
            val conversion = math(z, name) ?: return
            val columns = data.columns
            val rows = data.rows
            // A table with one column runs down its y axis: it's a curve on that axis
            val alongY = columns == 1 && rows > 1
            val kind = when {
                rows <= 1 && columns <= 1 -> ObjectKind.VALUE
                rows <= 1 || alongY -> ObjectKind.CURVE
                else -> ObjectKind.MAP
            }
            val axisIds = when (kind) {
                ObjectKind.VALUE -> emptyList()
                ObjectKind.CURVE -> listOf(if (alongY) "y" else "x")
                else -> listOf("x", "y")
            }
            val calibrationAxes = axisIds.map { id ->
                val count = if (id == "x") columns else rows
                axis(axes[id], "$name ${id.uppercase()} axis", count) ?: return
            }
            add(element, CalibrationObject(
                name = name,
                description = element.childText("description").orEmpty(),
                kind = kind,
                address = data.address,
                storage = Storage(listOf(RecordField.Values(data.type, if (data.columnMajor && kind == ObjectKind.MAP) ValueOrder.Y_FIRST else ValueOrder.X_FIRST)), data.order),
                conversion = conversion,
                axes = calibrationAxes,
                unit = z.childText("units").orEmpty(),
                format = decimals(z),
                lowerLimit = z.childText("min")?.let(::number) ?: Double.NEGATIVE_INFINITY,
                upperLimit = z.childText("max")?.let(::number) ?: Double.POSITIVE_INFINITY,
                source = SourceLocation(file, element.line)
            ))
        }

        /**
         * An axis: in memory (a shared AXIS object at its address), linked to another object's data, or labels.
         * Labels that aren't numbers become texts on the positions
         */
        private fun axis(axis: XmlElement?, name: String, count: Int): CalibrationAxis? {
            if (axis == null) return CalibrationAxis(AxisSource.Fixed(List(count) { it.toDouble() }), Conversion.Identical, count)
            val units = axis.childText("units").orEmpty()
            val format = decimals(axis)
            val embedInfo = axis.child("embedinfo")
            val linked = embedInfo?.attribute("linkobjid")?.lowercase()?.let { byId[it] }
            val source = linked?.let { link -> (link.children("XDFAXIS").firstOrNull { it.attribute("id").equals("z", true) } ?: link) }
            val data = (source ?: axis).child("EMBEDDEDDATA")
            val address = data?.attribute("mmedaddress")
            if (address != null && (linked != null || embedInfo?.attribute("type") != "1")) {
                val embedded = embedded(data, axis, name, quiet = true)
                val conversion = math(source ?: axis, name)
                if (embedded != null && conversion != null) {
                    val shared = CalibrationObject(
                        name = uniqueName(name),
                        description = "",
                        kind = ObjectKind.AXIS,
                        address = embedded.address,
                        storage = Storage(listOf(RecordField.AxisPoints(0, embedded.type)), embedded.order),
                        conversion = conversion,
                        axes = listOf(CalibrationAxis(AxisSource.Record, conversion, count, unit = units, format = format, fixedCount = count)),
                        unit = units,
                        format = format,
                        source = SourceLocation(file, axis.line)
                    )
                    objects += shared
                    return CalibrationAxis(AxisSource.Shared(shared.name), conversion, count, unit = units, format = format)
                }
            }
            val labels = axis.children("LABEL").sortedBy { it.attribute("index")?.let(::number) ?: 0.0 }.map { it.attribute("value").orEmpty() }
            if (labels.isNotEmpty()) {
                val numbers = labels.map { it.trim().toDoubleOrNull() }
                if (numbers.all { it != null }) {
                    val points = (numbers.filterNotNull() + List(maxOf(0, count - labels.size)) { (labels.size + it).toDouble() }).take(count)
                    return CalibrationAxis(AxisSource.Fixed(points), Conversion.Identical, count, unit = units, format = format)
                }
                val texts = List(count) { i -> i.toDouble() to (labels.getOrNull(i) ?: i.toString()) }
                return CalibrationAxis(AxisSource.Fixed(List(count) { it.toDouble() }), Conversion.Verbal(texts), count, unit = units, format = format)
            }
            return CalibrationAxis(AxisSource.Fixed(List(count) { it.toDouble() }), Conversion.Identical, count, unit = units, format = format)
        }

        /** Where and how an item's data is stored */
        private fun embedded(data: XmlElement?, owner: XmlElement, name: String, quiet: Boolean = false): Embedded? {
            if (data == null) return null.also { if (!quiet) problem(Severity.ERROR, owner, "$name has no EMBEDDEDDATA, left out", name) }
            val address = data.attribute("mmedaddress")?.let(::number)?.toLong()
                ?: return null.also { if (!quiet) problem(Severity.ERROR, owner, "$name has no address, left out", name) }
            val flags = data.attribute("mmedtypeflags")?.let(::number)?.toInt() ?: defaultFlags()
            val bits = data.attribute("mmedelementsizebits")?.let(::number)?.toInt() ?: defaults?.attribute("datasizeinbits")?.let(::number)?.toInt() ?: 8
            val float = flags and FLOAT != 0 || defaults?.attribute("float") == "1"
            val type = dataType(bits, flags and SIGNED != 0, float)
                ?: return null.also { if (!quiet) problem(Severity.ERROR, owner, "$name is $bits bits${if (float) " floating point" else ""}, which isn't supported; left out", name) }
            val major = data.attribute("mmedmajorstridebits")?.let(::number)?.toInt() ?: 0
            val minor = data.attribute("mmedminorstridebits")?.let(::number)?.toInt() ?: 0
            if ((major != 0 && minor == 0 && major != bits) || (minor != 0 && minor != bits)) {
                if (!quiet) problem(Severity.ERROR, owner, "$name's data has gaps between its values (strides), which isn't supported yet; left out", name)
                return null
            }
            return Embedded(
                address = address + base,
                type = type,
                order = if (flags and LSB_FIRST != 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN,
                columnMajor = flags and COLUMN_MAJOR != 0,
                rows = data.attribute("mmedrowcount")?.let(::number)?.toInt()?.coerceAtLeast(1) ?: 1,
                columns = data.attribute("mmedcolcount")?.let(::number)?.toInt()?.coerceAtLeast(1) ?: 1
            )
        }

        private fun defaultFlags(): Int {
            var flags = 0
            if (defaults?.attribute("signed") == "1") flags = flags or SIGNED
            if (defaults?.attribute("lsbfirst") == "1") flags = flags or LSB_FIRST
            return flags
        }

        /** The MATH equation as a conversion; linear ones become exactly linear. Null (reported) when it can't be used */
        private fun math(element: XmlElement, name: String): Conversion? {
            val math = element.child("MATH") ?: return Conversion.Identical
            val equation = math.attribute("equation")?.trim().orEmpty().ifEmpty { return Conversion.Identical }
            val variables = math.children("VAR").mapNotNull { it.attribute("id") }.ifEmpty { listOf("X") }
            if (variables.size > 1) {
                return null.also { problem(Severity.ERROR, element, "$name's equation \"$equation\" reads other values (${variables.joinToString()}), which isn't supported yet; left out", name) }
            }
            val variable = variables.single()
            val text = if (variable.equals("X", ignoreCase = true)) equation else equation.replace(Regex("\\b${Regex.escape(variable)}\\b"), "X")
            val expression = when (val parsed = Expression.parse(text, caretIsPower = true)) {
                is TkResult.Success -> parsed.data
                is TkResult.Failure -> return null.also { problem(Severity.ERROR, element, "$name: ${parsed.error.message}; left out", name) }
            }
            return linear(expression) ?: Conversion.Formula(expression)
        }

        private fun decimals(element: XmlElement): String? = element.childText("decimalpl")?.let(::number)?.toInt()?.let { "%.$it" }

        private fun title(element: XmlElement): String = element.childText("title").orEmpty().ifBlank { element.attribute("uniqueid") ?: "untitled" }

        private fun uniqueName(element: XmlElement): String = uniqueName(title(element))

        private fun uniqueName(title: String): String {
            var name = title
            var n = 2
            while (!names.add(name)) name = "$title (${n++})"
            return name
        }

        private fun add(element: XmlElement, obj: CalibrationObject) {
            objects += obj
            for (member in element.children("CATEGORYMEM")) {
                val category = member.attribute("category")?.let(::number)?.toInt() ?: continue
                // Categories are counted from 1 in CATEGORYMEM and from 0 in CATEGORY
                val folder = categories[category - 1] ?: categories[category] ?: continue
                inCategory.getOrPut(folder) { mutableListOf() } += obj.name
            }
        }

        private fun problem(severity: Severity, element: XmlElement, message: String, objectName: String? = null) {
            problems += Diagnostic(severity, message, SourceLocation(file, element.line), objectName)
        }
    }

    /** Decimal or 0x hex */
    internal fun number(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.startsWith("0x", ignoreCase = true)) return trimmed.substring(2).toLongOrNull(16)?.toDouble()
        return trimmed.toDoubleOrNull()
    }

    private fun dataType(bits: Int, signed: Boolean, float: Boolean): DataType? = when {
        float && bits == 32 -> DataType.FLOAT32_IEEE
        float && bits == 64 -> DataType.FLOAT64_IEEE
        float -> null
        bits == 8 -> if (signed) DataType.SBYTE else DataType.UBYTE
        bits == 16 -> if (signed) DataType.SWORD else DataType.UWORD
        bits == 32 -> if (signed) DataType.SLONG else DataType.ULONG
        bits == 64 -> if (signed) DataType.A_INT64 else DataType.A_UINT64
        else -> null
    }

    private fun DataType.unsigned(): DataType = if (float) this else DataType.unsigned(size) ?: this

    /** A formula that's really factor * X + offset, as [Conversion.Linear], so going back is exact */
    internal fun linear(expression: Expression): Conversion.Linear? {
        val offset = expression.evaluate(0.0)
        val factor = expression.evaluate(1.0) - offset
        if (offset.isNaN() || factor.isNaN() || factor == 0.0) return null
        for (x in listOf(2.0, 7.0, 100.0, 255.0, 65535.0, -3.0)) {
            val expected = factor * x + offset
            if (abs(expression.evaluate(x) - expected) > 1e-9 * maxOf(1.0, abs(expected))) return null
        }
        return Conversion.Linear(factor, offset)
    }

    // ------------------------------------------------ Writing

    /** XDF text and the objects that couldn't be written, with why */
    class Written(val text: String, val skipped: List<Diagnostic>, val count: Int)

    /**
     * Writes the objects of [set] that XDF can describe, read from [image] for their sizes. Axes become embedded data
     * when their points are stored in a row, labels otherwise
     * @param folders Become categories (the top level ones; objects further down go in their top folder)
     */
    fun write(set: CalibrationSet, image: MemoryImage, title: String, folders: List<ObjectFolder> = emptyList(), imageStart: Long = image.ranges.firstOrNull()?.start ?: 0): Written {
        val skipped = mutableListOf<Diagnostic>()
        val category = mutableMapOf<String, Int>()
        fun collect(folder: ObjectFolder, index: Int, depth: Int) {
            if (depth > 32) return
            folder.objects.forEach { category.putIfAbsent(it, index) }
            folder.folders.forEach { collect(it, index, depth + 1) }
        }
        folders.forEachIndexed { i, folder -> collect(folder, i, 0) }
        var id = 0x1000
        var count = 0
        val xml = XmlWriter()
        xml.element("XDFFORMAT", "version" to "1.60") {
            element("XDFHEADER") {
                leaf("flags", "0x1")
                leaf("deftitle", title)
                leaf("description", "Written by Tuners Toolkit")
                leaf("BASEOFFSET", null, "offset" to "0", "subtract" to "0")
                leaf("DEFAULTS", null, "datasizeinbits" to "8", "sigdigits" to "2", "outputtype" to "1", "signed" to "0", "lsbfirst" to "0", "float" to "0")
                folders.forEachIndexed { i, folder -> leaf("CATEGORY", null, "index" to "0x${i.toString(16).uppercase()}", "name" to folder.name) }
            }
            for (obj in set.objects) {
                if (obj.kind == ObjectKind.AXIS) continue
                val values = when (val result = CalibrationCodec.decode(image, obj, set)) {
                    is TkResult.Success -> result.data
                    is TkResult.Failure -> {
                        skipped += Diagnostic(Severity.WARNING, "${obj.name} can't be read from the image, so its size isn't known", obj.source, obj.name)
                        continue
                    }
                }
                val why = writeObject(this, obj, values, imageStart, "0x${(id++).toString(16).uppercase()}", category[obj.name])
                if (why == null) count++ else skipped += Diagnostic(Severity.WARNING, "${obj.name} isn't in the XDF: $why", obj.source, obj.name)
            }
        }
        return Written(xml.toString(), skipped, count)
    }

    /** Writes one object; returns why it can't be written, or null when it was */
    private fun writeObject(xml: XmlWriter, obj: CalibrationObject, values: CalibrationValues, imageStart: Long, uniqueId: String, category: Int?): String? {
        val slots = values.slots ?: return "it has no stored values"
        val type = slots.type
        val flags = typeFlags(type, slots.byteOrder) ?: return "it's stored as ${type.name}"
        val bits = type.size * 8
        val start = slots.addresses.minOrNull() ?: return "it has no values"
        val equation = equation(obj.conversion) ?: return "its conversion can't be written as an XDF equation"
        if (start < imageStart) return "it's before the start of the image"
        val address = "0x${(start - imageStart).toString(16).uppercase()}"
        val decimals = org.mr0btain.tunerstoolkit.core.calibration.decimalsOf(obj.format)?.toString() ?: "2"
        fun XmlWriter.common() {
            leaf("title", obj.displayName ?: obj.name)
            leaf("description", obj.description)
            category?.let { leaf("CATEGORYMEM", null, "index" to "0", "category" to (it + 1).toString()) }
        }
        val mask = obj.bitMask
        if (obj.kind == ObjectKind.VALUE && mask != null) {
            if (mask and (mask - 1) != 0L) return "it uses several bits of its byte (a mask of 0x${mask.toString(16)})"
            xml.element("XDFFLAG", "uniqueid" to uniqueId) {
                common()
                leaf("EMBEDDEDDATA", null, "mmedaddress" to address, "mmedelementsizebits" to bits, "mmedtypeflags" to hex(flags))
                leaf("mask", "0x${mask.toString(16).uppercase()}")
            }
            return null
        }
        if (mask != null) return "it uses only some bits of its values"
        when (obj.kind) {
            ObjectKind.VALUE -> xml.element("XDFCONSTANT", "uniqueid" to uniqueId) {
                common()
                leaf("EMBEDDEDDATA", null, "mmedtypeflags" to hex(flags), "mmedaddress" to address, "mmedelementsizebits" to bits, "mmedmajorstridebits" to 0, "mmedminorstridebits" to 0)
                leaf("units", obj.unit)
                leaf("decimalpl", decimals)
                limits(obj.lowerLimit, obj.upperLimit)
                leaf("datatype", "0")
                leaf("unittype", "0")
                leaf("DALINK", null, "index" to "0")
                math(equation)
            }
            ObjectKind.CURVE, ObjectKind.MAP, ObjectKind.VAL_BLK -> {
                val columns = values.dimensions.getOrElse(0) { 1 }
                val rows = values.dimensions.getOrElse(1) { 1 }
                if (values.dimensions.size > 2) return "it has more than two dimensions"
                val columnMajor = when {
                    isGrid(slots.addresses, start, type.size, columns, rows, columnMajor = false) -> false
                    isGrid(slots.addresses, start, type.size, columns, rows, columnMajor = true) -> true
                    else -> return "its values aren't stored one after another"
                }
                val tableFlags = if (columnMajor && rows > 1) flags or COLUMN_MAJOR else flags
                xml.element("XDFTABLE", "uniqueid" to uniqueId, "flags" to "0x0") {
                    common()
                    axis(this, "x", values, 0, columns, imageStart)
                    axis(this, "y", values, 1, rows, imageStart)
                    element("XDFAXIS", "id" to "z") {
                        leaf("EMBEDDEDDATA", null, "mmedtypeflags" to hex(tableFlags), "mmedaddress" to address, "mmedelementsizebits" to bits,
                            "mmedrowcount" to rows, "mmedcolcount" to columns, "mmedmajorstridebits" to 0, "mmedminorstridebits" to 0)
                        leaf("units", obj.unit)
                        leaf("decimalpl", decimals)
                        limits(obj.lowerLimit, obj.upperLimit)
                        leaf("outputtype", "1")
                        math(equation)
                    }
                }
            }
            else -> return "it's a ${obj.kind.name.lowercase().replace('_', ' ')}"
        }
        return null
    }

    /** An x or y axis: embedded when its points are stored in a row, labels with their physical values otherwise */
    private fun axis(xml: XmlWriter, id: String, values: CalibrationValues, axis: Int, count: Int, imageStart: Long) {
        val points = values.axes.getOrNull(axis)
        xml.element("XDFAXIS", "id" to id, "uniqueid" to "0x0") {
            val slots = points?.slots
            val equation = points?.let { equation(it.conversion) }
            val flags = slots?.let { typeFlags(it.type, it.byteOrder) }
            val start = slots?.addresses?.firstOrNull()
            val inARow = slots != null && start != null && !slots.difference &&
                slots.addresses.withIndex().all { (i, a) -> a == start + i * slots.type.size.toLong() } && start >= imageStart
            if (inARow && equation != null && flags != null) {
                leaf("EMBEDDEDDATA", null, "mmedtypeflags" to hex(flags), "mmedaddress" to "0x${(start!! - imageStart).toString(16).uppercase()}",
                    "mmedelementsizebits" to slots!!.type.size * 8, "mmedmajorstridebits" to 0, "mmedminorstridebits" to 0)
                leaf("units", points.axis.unit)
                leaf("indexcount", count.toString())
                leaf("decimalpl", org.mr0btain.tunerstoolkit.core.calibration.decimalsOf(points.axis.format)?.toString() ?: "2")
                leaf("embedinfo", null, "type" to "2")
                leaf("datatype", "0")
                leaf("unittype", "0")
                leaf("DALINK", null, "index" to "0")
                math(equation)
            } else {
                leaf("units", points?.axis?.unit.orEmpty())
                leaf("indexcount", count.toString())
                leaf("embedinfo", null, "type" to "1")
                leaf("datatype", "0")
                leaf("unittype", "0")
                leaf("DALINK", null, "index" to "0")
                for (i in 0 until count) leaf("LABEL", null, "index" to i, "value" to (points?.let { it.text(i) ?: plainNumber(it.physical[i]) } ?: i.toString()))
                math("X")
            }
        }
    }

    private fun XmlWriter.math(equation: String) = element("MATH", "equation" to equation) { leaf("VAR", null, "id" to "X") }

    private fun XmlWriter.limits(lower: Double, upper: Double) {
        if (lower.isFinite()) leaf("min", plainNumber(lower))
        if (upper.isFinite()) leaf("max", plainNumber(upper))
    }

    /** Whether [addresses] (x first) are a packed grid starting at [start], by rows or by columns */
    private fun isGrid(addresses: LongArray, start: Long, size: Int, columns: Int, rows: Int, columnMajor: Boolean): Boolean =
        addresses.indices.all { i ->
            val x = i % columns
            val y = i / columns
            val position = if (columnMajor) x.toLong() * rows + y else y.toLong() * columns + x
            addresses[i] == start + position * size
        }

    private fun typeFlags(type: DataType, order: ByteOrder): Int? {
        val base = if (order == ByteOrder.LITTLE_ENDIAN) LSB_FIRST else 0
        return when (type) {
            DataType.UBYTE, DataType.UWORD, DataType.ULONG -> base
            DataType.SBYTE, DataType.SWORD, DataType.SLONG -> base or SIGNED
            DataType.FLOAT32_IEEE -> base or FLOAT
            else -> null
        }
    }

    private fun hex(value: Int) = "0x${value.toString(16).uppercase()}"

    /** A conversion as an XDF equation in X, or null when it can't be one */
    internal fun equation(conversion: Conversion): String? = when (conversion) {
        Conversion.Identical -> "X"
        is Conversion.Linear -> buildString {
            append("X")
            if (conversion.factor != 1.0) append("*").append(plainExact(conversion.factor))
            if (conversion.offset > 0) append("+").append(plainExact(conversion.offset))
            if (conversion.offset < 0) append("-").append(plainExact(-conversion.offset))
        }
        is Conversion.Formula -> conversion.formula.text.takeIf { conversion.formula.inputCount <= 1 && it.none { c -> c in "^&|<>!~%" } }
            ?.replace(Regex("\\b[xX]1\\b"), "X")
        // The texts are lost, the numbers stay right
        is Conversion.Verbal -> "X"
        is Conversion.WithStatus -> equation(conversion.base)
        is Conversion.Rational, is Conversion.Table -> null
    }

    /** A number exactly, without an exponent where it can be helped */
    private fun plainExact(value: Double): String {
        val plain = plainNumber(value)
        return if (plain.toDoubleOrNull() == value) plain else value.toString()
    }
}
