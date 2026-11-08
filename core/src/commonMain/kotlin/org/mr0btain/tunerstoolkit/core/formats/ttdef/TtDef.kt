package org.mr0btain.tunerstoolkit.core.formats.ttdef

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationAxis
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.Expression
import org.mr0btain.tunerstoolkit.core.calibration.ObjectFolder
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.Storage
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/** A definition read from a .ttdef */
class TtDefinition(val title: String, val description: String, val set: CalibrationSet, val folders: List<ObjectFolder>)

/**
 * Tuners Toolkit's own definition format: the calibration model as JSON, so a definition made in the app (by hand or
 * by the map finder), or one converted from another format, can be saved and read back exactly. Addresses are hex
 * strings; limits that aren't there are left out
 */
object TtDef {
    const val FORMAT = "ttdef"
    const val VERSION = 1

    /** Deeper than folders within folders ever go */
    const val MAX_NESTING = 64

    /** How deeply [text]'s brackets and braces nest, outside strings */
    internal fun nesting(text: String): Int {
        var depth = 0
        var deepest = 0
        var inString = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inString && c == '\\' -> i++
                c == '"' -> inString = !inString
                !inString && (c == '{' || c == '[') -> deepest = maxOf(deepest, ++depth)
                !inString && (c == '}' || c == ']') -> depth--
            }
            i++
        }
        return deepest
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        allowSpecialFloatingPointValues = true
    }

    fun looksLike(bytes: ByteArray): Boolean {
        val start = bytes.copyOf(minOf(bytes.size, 512)).decodeToString().trimStart()
        return start.startsWith("{") && "\"ttdef\"" in start
    }

    fun write(set: CalibrationSet, title: String, description: String = "", folders: List<ObjectFolder> = emptyList()): String =
        json.encodeToString(TtFile.serializer(), TtFile(FORMAT, VERSION, title, description, set.objects.map(::objectOf), folders.map(::folderOf)))

    fun read(bytes: ByteArray, name: String = ""): TkResult<Imported<TtDefinition>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val text = bytes.decodeToString()
        // Real files nest a dozen deep; refusing absurd nesting first keeps decoding (which recurses) off the stack's end
        if (nesting(text) > MAX_NESTING) return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        val file = try {
            json.decodeFromString(TtFile.serializer(), text)
        } catch (e: Exception) {
            return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        }
        if (file.format != FORMAT) return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        val problems = mutableListOf<Diagnostic>()
        if (file.formatVersion > VERSION) problems += Diagnostic(Severity.WARNING, "$name was made by a newer Tuners Toolkit; what this one doesn't know is left out")
        val objects = file.objects.mapNotNull { dto ->
            try {
                modelOf(dto)
            } catch (e: Exception) {
                problems += Diagnostic(Severity.ERROR, "${dto.name} can't be read: ${e.message}, left out", objectName = dto.name)
                null
            }
        }
        val set = CalibrationSet(objects, problems.toList())
        return TkResult.Success(Imported(TtDefinition(file.title, file.description, set, file.folders.map(::folderModel)), problems))
    }

    // ------------------------------------------------ The file

    @Serializable
    private class TtFile(
        val format: String,
        val formatVersion: Int,
        val title: String,
        val description: String = "",
        val objects: List<TtObject> = emptyList(),
        val folders: List<TtFolder> = emptyList()
    )

    @Serializable
    private class TtFolder(val name: String, val description: String = "", val folders: List<TtFolder> = emptyList(), val objects: List<String> = emptyList())

    @Serializable
    private class TtObject(
        val name: String,
        val description: String = "",
        val kind: String,
        val address: String,
        val storage: TtStorage,
        val conversion: TtConversion = TtConversion("identical"),
        val axes: List<TtAxis> = emptyList(),
        val dimensions: List<Int> = emptyList(),
        val unit: String = "",
        val format: String? = null,
        val displayName: String? = null,
        val lowerLimit: Double? = null,
        val upperLimit: Double? = null,
        val extendedLimits: List<Double>? = null,
        val readOnly: Boolean = false,
        val bitMask: String? = null,
        val encoding: String? = null,
        val variantAddresses: List<String> = emptyList()
    )

    @Serializable
    private class TtStorage(val byteOrder: String, val fields: List<TtField>, val alignments: Map<String, Int> = emptyMap(), val static: Boolean = false)

    /** [field] is values, axisPoints, axisCount, rescale, rescaleCount, axisOffset, axisDistance, axisShift or skip */
    @Serializable
    private class TtField(
        val field: String,
        val type: String,
        val axis: Int = -1,
        val order: String? = null,
        val decreasing: Boolean = false,
        val pointer: String? = null,
        val maxPairs: Int = 0,
        val what: String? = null
    )

    /** [type] is identical, linear, rational, table, verbal, formula or status */
    @Serializable
    private class TtConversion(
        val type: String,
        val factor: Double? = null,
        val offset: Double? = null,
        val coeffs: List<Double>? = null,
        val pairs: List<List<Double>>? = null,
        val interpolate: Boolean? = null,
        val default: Double? = null,
        val texts: List<TtText>? = null,
        val ranges: List<TtRange>? = null,
        val defaultText: String? = null,
        val formula: String? = null,
        val inverse: String? = null,
        val caretIsPower: Boolean = false,
        val base: TtConversion? = null,
        val status: TtConversion? = null
    )

    @Serializable
    private class TtText(val value: Double, val text: String)

    @Serializable
    private class TtRange(val min: Double, val max: Double, val text: String)

    /** [source] is record, shared, rescaled, fixed or curve */
    @Serializable
    private class TtAxis(
        val source: String,
        val ref: String? = null,
        val points: List<Double>? = null,
        val conversion: TtConversion = TtConversion("identical"),
        val maxPoints: Int = 0,
        val input: String? = null,
        val unit: String = "",
        val format: String? = null,
        val lowerLimit: Double? = null,
        val upperLimit: Double? = null,
        val fixedCount: Int? = null,
        val difference: Boolean = false
    )

    // ------------------------------------------------ Model to file

    private fun folderOf(folder: ObjectFolder): TtFolder = TtFolder(folder.name, folder.description, folder.folders.map(::folderOf), folder.objects)

    private fun objectOf(obj: CalibrationObject) = TtObject(
        name = obj.name,
        description = obj.description,
        kind = obj.kind.name,
        address = hex(obj.address),
        storage = TtStorage(
            byteOrder = obj.storage.byteOrder.name,
            fields = obj.storage.fields.map(::fieldOf),
            alignments = obj.storage.alignments.mapKeys { it.key.name },
            static = obj.storage.static
        ),
        conversion = conversionOf(obj.conversion),
        axes = obj.axes.map(::axisOf),
        dimensions = obj.dimensions,
        unit = obj.unit,
        format = obj.format,
        displayName = obj.displayName,
        lowerLimit = obj.lowerLimit.takeIf { it.isFinite() },
        upperLimit = obj.upperLimit.takeIf { it.isFinite() },
        extendedLimits = obj.extendedLimits?.let { listOf(it.start, it.endInclusive) },
        readOnly = obj.readOnly,
        bitMask = obj.bitMask?.let(::hex),
        encoding = obj.encoding,
        variantAddresses = obj.variantAddresses.map(::hex)
    )

    private fun fieldOf(field: RecordField): TtField = when (field) {
        is RecordField.Values -> TtField("values", field.type.name, order = field.order.name, pointer = field.pointer?.name)
        is RecordField.AxisPoints -> TtField("axisPoints", field.type.name, field.axis, decreasing = field.decreasing, pointer = field.pointer?.name)
        is RecordField.AxisCount -> TtField("axisCount", field.type.name, field.axis)
        is RecordField.Rescale -> TtField("rescale", field.type.name, field.axis, decreasing = field.decreasing, pointer = field.pointer?.name, maxPairs = field.maxPairs)
        is RecordField.RescaleCount -> TtField("rescaleCount", field.type.name, field.axis)
        is RecordField.AxisOffset -> TtField("axisOffset", field.type.name, field.axis)
        is RecordField.AxisDistance -> TtField("axisDistance", field.type.name, field.axis)
        is RecordField.AxisShift -> TtField("axisShift", field.type.name, field.axis)
        is RecordField.Skip -> TtField("skip", field.type.name, what = field.what)
    }

    private fun conversionOf(conversion: Conversion): TtConversion = when (conversion) {
        Conversion.Identical -> TtConversion("identical")
        is Conversion.Linear -> TtConversion("linear", factor = conversion.factor, offset = conversion.offset)
        is Conversion.Rational -> TtConversion("rational", coeffs = listOf(conversion.a, conversion.b, conversion.c, conversion.d, conversion.e, conversion.f))
        is Conversion.Table -> TtConversion("table", pairs = conversion.pairs.map { listOf(it.first, it.second) }, interpolate = conversion.interpolate, default = conversion.default)
        is Conversion.Verbal -> TtConversion(
            "verbal",
            texts = conversion.entries.map { TtText(it.first, it.second) },
            ranges = conversion.ranges.map { TtRange(it.min, it.max, it.text) },
            defaultText = conversion.default
        )
        is Conversion.Formula -> TtConversion("formula", formula = conversion.formula.text, inverse = conversion.inverse?.text, caretIsPower = conversion.formula.caretIsPower)
        is Conversion.WithStatus -> TtConversion("status", base = conversionOf(conversion.base), status = conversionOf(conversion.status))
    }

    private fun axisOf(axis: CalibrationAxis): TtAxis {
        val (source, ref, points) = when (val s = axis.source) {
            AxisSource.Record -> Triple("record", null, null)
            is AxisSource.Shared -> Triple("shared", s.name, null)
            is AxisSource.Rescaled -> Triple("rescaled", s.name, null)
            is AxisSource.Fixed -> Triple("fixed", null, s.points)
            is AxisSource.Curve -> Triple("curve", s.name, null)
        }
        return TtAxis(
            source, ref, points, conversionOf(axis.conversion), axis.maxPoints, axis.input, axis.unit, axis.format,
            axis.lowerLimit.takeIf { it.isFinite() }, axis.upperLimit.takeIf { it.isFinite() }, axis.fixedCount, axis.difference
        )
    }

    private fun hex(value: Long) = "0x" + value.toString(16).uppercase()

    // ------------------------------------------------ File to model (throws on nonsense, caught per object)

    private fun folderModel(folder: TtFolder): ObjectFolder = ObjectFolder(folder.name, folder.description, folder.folders.map(::folderModel), folder.objects)

    private fun modelOf(dto: TtObject) = CalibrationObject(
        name = dto.name,
        description = dto.description,
        kind = ObjectKind.valueOf(dto.kind),
        address = address(dto.address),
        storage = Storage(
            fields = dto.storage.fields.map(::fieldModel),
            byteOrder = ByteOrder.valueOf(dto.storage.byteOrder),
            alignments = dto.storage.alignments.mapKeys { DataType.valueOf(it.key) },
            static = dto.storage.static
        ),
        conversion = conversionModel(dto.conversion),
        axes = dto.axes.map(::axisModel),
        dimensions = dto.dimensions,
        unit = dto.unit,
        format = dto.format,
        displayName = dto.displayName,
        lowerLimit = dto.lowerLimit ?: Double.NEGATIVE_INFINITY,
        upperLimit = dto.upperLimit ?: Double.POSITIVE_INFINITY,
        extendedLimits = dto.extendedLimits?.takeIf { it.size == 2 }?.let { it[0]..it[1] },
        readOnly = dto.readOnly,
        bitMask = dto.bitMask?.let(::address),
        encoding = dto.encoding,
        variantAddresses = dto.variantAddresses.map(::address)
    )

    private fun fieldModel(dto: TtField): RecordField {
        val type = DataType.valueOf(dto.type)
        val pointer = dto.pointer?.let(DataType::valueOf)
        return when (dto.field) {
            "values" -> RecordField.Values(type, ValueOrder.valueOf(dto.order ?: "X_FIRST"), pointer)
            "axisPoints" -> RecordField.AxisPoints(dto.axis, type, dto.decreasing, pointer)
            "axisCount" -> RecordField.AxisCount(dto.axis, type)
            "rescale" -> RecordField.Rescale(dto.axis, type, dto.maxPairs, dto.decreasing, pointer)
            "rescaleCount" -> RecordField.RescaleCount(dto.axis, type)
            "axisOffset" -> RecordField.AxisOffset(dto.axis, type)
            "axisDistance" -> RecordField.AxisDistance(dto.axis, type)
            "axisShift" -> RecordField.AxisShift(dto.axis, type)
            "skip" -> RecordField.Skip(type, dto.what.orEmpty())
            else -> error("unknown record field ${dto.field}")
        }
    }

    private fun conversionModel(dto: TtConversion): Conversion = when (dto.type) {
        "identical" -> Conversion.Identical
        "linear" -> Conversion.Linear(dto.factor ?: 1.0, dto.offset ?: 0.0)
        "rational" -> dto.coeffs?.takeIf { it.size == 6 }?.let { Conversion.Rational(it[0], it[1], it[2], it[3], it[4], it[5]) } ?: error("a rational conversion needs six coefficients")
        "table" -> Conversion.Table(dto.pairs.orEmpty().filter { it.size == 2 }.map { it[0] to it[1] }, dto.interpolate ?: true, dto.default)
        "verbal" -> Conversion.Verbal(dto.texts.orEmpty().map { it.value to it.text }, dto.ranges.orEmpty().map { Conversion.VerbalRange(it.min, it.max, it.text) }, dto.defaultText)
        "formula" -> Conversion.Formula(expression(dto.formula ?: error("a formula conversion needs a formula"), dto.caretIsPower), dto.inverse?.let { expression(it, dto.caretIsPower) })
        "status" -> Conversion.WithStatus(
            conversionModel(dto.base ?: error("a status conversion needs a base")),
            conversionModel(dto.status ?: error("a status conversion needs its texts")) as? Conversion.Verbal ?: error("status texts must be verbal")
        )
        else -> error("unknown conversion ${dto.type}")
    }

    private fun expression(text: String, caretIsPower: Boolean): Expression = when (val result = Expression.parse(text, caretIsPower = caretIsPower)) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> error(result.error.message)
    }

    private fun axisModel(dto: TtAxis) = CalibrationAxis(
        source = when (dto.source) {
            "record" -> AxisSource.Record
            "shared" -> AxisSource.Shared(dto.ref ?: error("a shared axis needs its object"))
            "rescaled" -> AxisSource.Rescaled(dto.ref ?: error("a rescaled axis needs its object"))
            "fixed" -> AxisSource.Fixed(dto.points ?: error("a fixed axis needs its points"))
            "curve" -> AxisSource.Curve(dto.ref ?: error("a curve axis needs its curve"))
            else -> error("unknown axis source ${dto.source}")
        },
        conversion = conversionModel(dto.conversion),
        maxPoints = dto.maxPoints,
        input = dto.input,
        unit = dto.unit,
        format = dto.format,
        lowerLimit = dto.lowerLimit ?: Double.NEGATIVE_INFINITY,
        upperLimit = dto.upperLimit ?: Double.POSITIVE_INFINITY,
        fixedCount = dto.fixedCount,
        difference = dto.difference
    )

    private fun address(text: String): Long {
        val trimmed = text.trim()
        return if (trimmed.startsWith("0x", ignoreCase = true)) trimmed.substring(2).toULong(16).toLong() else trimmed.toLong()
    }
}
