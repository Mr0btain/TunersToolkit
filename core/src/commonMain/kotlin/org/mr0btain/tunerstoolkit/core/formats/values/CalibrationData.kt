package org.mr0btain.tunerstoolkit.core.formats.values

import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.math.abs

/**
 * One axis of a value record
 * @property shared The shared axis object it's stored in, when it is
 * @property fixed Its points aren't stored anywhere (FIX_AXIS)
 */
class DataAxis(
    val values: List<Double>,
    val texts: List<String>? = null,
    val unit: String = "",
    val shared: String? = null,
    val fixed: Boolean = false
)

/**
 * One object's values, by name, as the value exchange formats (DCM, CDFX, CSV) hold them: physical numbers, x first,
 * with their axes. [texts] are the verbal values when the object has them; [text] is an ASCII object's text
 */
class DataRecord(
    val name: String,
    val kind: ObjectKind,
    val dimensions: List<Int>,
    val values: List<Double>,
    val texts: List<String?>? = null,
    val text: String? = null,
    val axes: List<DataAxis> = emptyList(),
    val unit: String = "",
    val description: String = "",
    val format: String? = null,
    val location: SourceLocation? = null
)

/**
 * What applying values did
 * @property applied Objects whose values were written (or were already the same)
 * @property missing Names the definition doesn't have
 * @property problems Everything that couldn't be written, and why
 */
class ApplyReport(val applied: Int, val unchanged: Int, val missing: List<String>, val clamped: Int, val problems: List<Diagnostic>) {
    val summary: String
        get() = buildList {
            add("$applied maps written")
            if (unchanged > 0) add("$unchanged already the same")
            if (missing.isNotEmpty()) add("${missing.size} not in the definition")
            if (clamped > 0) add("$clamped values set to their limits")
            val errors = problems.count { it.severity == Severity.ERROR }
            if (errors > 0) add("$errors couldn't be written")
        }.joinToString(", ")
}

/** Turning objects into records and records back into an image, by name */
object CalibrationData {
    /**
     * Records of [names] (every object when null) read from [image]. Objects that can't be read are reported and left
     * out
     */
    fun records(set: CalibrationSet, image: MemoryImage, names: Collection<String>? = null): Pair<List<DataRecord>, List<Diagnostic>> {
        val problems = mutableListOf<Diagnostic>()
        val records = (names?.mapNotNull { set[it] } ?: set.objects).mapNotNull { obj ->
            when (val result = CalibrationCodec.decode(image, obj, set)) {
                is TkResult.Success -> record(result.data)
                is TkResult.Failure -> {
                    problems += Diagnostic(Severity.WARNING, result.error.message, obj.source, obj.name)
                    null
                }
            }
        }
        return records to problems
    }

    fun record(values: CalibrationValues): DataRecord {
        val obj = values.obj
        val texts = List(values.size) { values.textAt(it) }.takeIf { list -> list.isNotEmpty() && list.all { it != null } }
        return DataRecord(
            name = obj.name,
            kind = obj.kind,
            dimensions = values.dimensions.toList(),
            values = values.physical.toList(),
            texts = texts,
            text = values.text,
            axes = values.axes.map { axis ->
                val axisTexts = List(axis.count) { axis.text(it) }.takeIf { list -> list.isNotEmpty() && list.all { it != null } }?.map { it!! }
                DataAxis(axis.physical.toList(), axisTexts, axis.axis.unit, axis.sharedWith?.takeIf { axis.axis.source is AxisSource.Shared }, axis.axis.source is AxisSource.Fixed)
            },
            unit = obj.unit,
            description = obj.description,
            format = obj.format
        )
    }

    /**
     * Writes [records] into [image] by name, as one undo step. Values are clamped to their limits; an object whose
     * size differs is left alone; a record's axis points are written when the object stores its own, and compared
     * (with a warning when they differ) when they're shared or fixed
     */
    fun apply(records: List<DataRecord>, set: CalibrationSet, image: MemoryImage): ApplyReport {
        val problems = mutableListOf<Diagnostic>()
        val missing = mutableListOf<String>()
        var applied = 0
        var unchanged = 0
        var clamped = 0
        image.edit {
            for (record in records) {
                val obj = set[record.name]
                if (obj == null) {
                    missing += record.name
                    continue
                }
                fun problem(severity: Severity, message: String) {
                    problems += Diagnostic(severity, message, record.location ?: obj.source, obj.name)
                }
                val values = when (val result = CalibrationCodec.decode(this, obj, set)) {
                    is TkResult.Success -> result.data
                    is TkResult.Failure -> {
                        problem(Severity.ERROR, "Can't be read from the image: ${result.error.message}")
                        continue
                    }
                }
                if (obj.readOnly) {
                    problem(Severity.WARNING, "It's read only, left as it is")
                    continue
                }
                if (record.text != null || obj.kind == ObjectKind.ASCII) {
                    if (record.text == values.text) {
                        unchanged++
                        continue
                    }
                    when (val result = CalibrationCodec.writeAscii(this, values, record.text.orEmpty())) {
                        is TkResult.Success -> applied++
                        is TkResult.Failure -> problem(Severity.ERROR, result.error.message)
                    }
                    continue
                }
                val wanted = wantedValues(record, values) ?: run {
                    problem(Severity.ERROR, "It has ${record.values.size.takeIf { it > 0 } ?: record.texts?.size ?: 0} values (${shape(record.dimensions)}) here, but ${values.size} (${shape(values.dimensions.toList())}) in the image; left as it is")
                    null
                } ?: continue
                if (record.dimensions.isNotEmpty() && values.dimensions.isNotEmpty() && record.dimensions != values.dimensions.toList()) {
                    problem(Severity.ERROR, "It's ${shape(record.dimensions)} here but ${shape(values.dimensions.toList())} in the image; left as it is")
                    continue
                }
                val axesWritten = applyAxes(record, values, ::problem)
                val changes = wanted.withIndex().filter { (i, v) -> !sameStored(values, i, v) }.associate { it.index to it.value }
                if (changes.isEmpty()) {
                    if (axesWritten) applied++ else unchanged++
                    continue
                }
                val summary = CalibrationCodec.writeAll(this, values, changes)
                clamped += summary.clamped
                if (summary.failed.isNotEmpty()) problem(Severity.ERROR, "${summary.failed.size} values couldn't be written: ${summary.failed.values.first().message}")
                if (summary.written > 0) applied++
            }
        }
        return ApplyReport(applied, unchanged, missing, clamped, problems)
    }

    /** The physical values a record asks for, by number or by text; null when the count doesn't match */
    private fun wantedValues(record: DataRecord, values: CalibrationValues): List<Double>? {
        val conversion = values.obj.conversion
        val wanted = if (record.values.isNotEmpty()) {
            record.values
        } else {
            record.texts?.mapIndexed { i, text ->
                text?.let { conversion.rawForText(it)?.let(conversion::toPhysical) } ?: values.physical.getOrElse(i) { Double.NaN }
            } ?: return null
        }
        return wanted.takeIf { it.size == values.size }
    }

    /** Writes or checks a record's axes; true when one was written */
    private fun MemoryImage.applyAxes(record: DataRecord, values: CalibrationValues, problem: (Severity, String) -> Unit): Boolean {
        var written = false
        record.axes.forEachIndexed { i, axis ->
            val mine = values.axes.getOrNull(i) ?: return@forEachIndexed
            if (axis.values.isEmpty() || axis.values.size != mine.count) return@forEachIndexed
            val same = axis.values.indices.all { close(axis.values[it], mine.physical[it]) }
            if (same) return@forEachIndexed
            when {
                mine.sharedWith != null || mine.slots == null ->
                    problem(Severity.WARNING, "Its ${AXES[i]} axis differs, but it's ${if (mine.sharedWith != null) "shared (${mine.sharedWith})" else "fixed"}, so it's left as it is")
                else -> when (val result = CalibrationCodec.writeAxisAll(this, values, i, axis.values)) {
                    is TkResult.Success -> written = true
                    is TkResult.Failure -> problem(Severity.ERROR, "Its ${AXES[i]} axis couldn't be written: ${result.error.message}")
                }
            }
        }
        return written
    }

    private val AXES = listOf("X", "Y", "Z", "4th", "5th")

    /** Whether writing [physical] at [index] would store what's there already */
    private fun sameStored(values: CalibrationValues, index: Int, physical: Double): Boolean {
        if (close(physical, values.physical[index])) return true
        val slots = values.slots ?: return false
        val raw = values.obj.conversion.toRaw(physical, slots.type.range)
        return !raw.isNaN() && slots.type.stored(raw) == values.raw[index]
    }

    private fun shape(dimensions: List<Int>) = if (dimensions.isEmpty()) "one value" else dimensions.joinToString(" × ")

    /** Equal within rounding of what was written out */
    internal fun close(a: Double, b: Double): Boolean = a == b || abs(a - b) <= 1e-9 * maxOf(1.0, abs(a), abs(b))
}
