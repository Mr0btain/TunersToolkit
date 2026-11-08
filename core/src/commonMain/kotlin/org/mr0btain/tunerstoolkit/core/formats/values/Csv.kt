package org.mr0btain.tunerstoolkit.core.formats.values

import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.ObjectIndex
import org.mr0btain.tunerstoolkit.core.calibration.exactNumber
import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.toHexAddress

/** Why a CSV couldn't be read into a map */
class CsvError(override val message: String) : Error

/**
 * Maps as spreadsheets read and write them. A map is a grid: x's points across the first row, y's down the first
 * column, values in between (a curve is the x row and one row of values; a value is its name and number). Written
 * with semicolons; read with tabs, semicolons or commas, and with commas as decimal points when they can't be the
 * separator
 */
object Csv {
    const val SEPARATOR = ";"

    fun writeMap(values: CalibrationValues): String {
        val record = CalibrationData.record(values)
        val columns = values.dimensions.getOrElse(0) { 1 }
        val rows = values.dimensions.getOrElse(1) { 1 }
        val x = record.axes.getOrNull(0)
        val y = record.axes.getOrNull(1)
        fun cell(index: Int) = record.texts?.getOrNull(index) ?: exactNumber(record.values[index])
        fun point(axis: DataAxis?, i: Int) = axis?.let { it.texts?.getOrNull(i) ?: exactNumber(it.values[i]) } ?: i.toString()
        val lines = mutableListOf<List<String>>()
        when {
            values.dimensions.isEmpty() -> lines += listOf(values.obj.name, cell(0))
            values.dimensions.size == 1 -> {
                lines += listOf(x?.unit?.ifBlank { null } ?: "x") + List(columns) { point(x, it) }
                lines += listOf(values.obj.name) + List(columns) { cell(it) }
            }
            else -> {
                lines += listOf("${y?.unit.orEmpty()} \\ ${x?.unit.orEmpty()}".trim()) + List(columns) { point(x, it) }
                for (row in 0 until rows) lines += listOf(point(y, row)) + List(columns) { cell(row * columns + it) }
            }
        }
        return lines.joinToString("\n") { line -> line.joinToString(SEPARATOR) { quote(it) } } + "\n"
    }

    /**
     * Reads a grid in [writeMap]'s layout as a record for [values]' object, its size checked against it. Axis points
     * come with it when the first row and column are numbers
     */
    fun readMap(text: String, values: CalibrationValues): TkResult<DataRecord, CsvError> {
        val grid = grid(text)
        if (grid.isEmpty()) return TkResult.Failure(CsvError("The file has nothing in it"))
        val columns = values.dimensions.getOrElse(0) { 1 }
        val rows = values.dimensions.getOrElse(1) { 1 }
        val obj = values.obj
        fun numbers(cells: List<String>): List<Double>? = cells.map { number(it) ?: return null }
        return when {
            values.dimensions.isEmpty() -> {
                val cell = grid.first().lastOrNull() ?: return TkResult.Failure(CsvError("There's no value"))
                val number = number(cell)
                TkResult.Success(DataRecord(obj.name, obj.kind, emptyList(), listOfNotNull(number), texts = if (number == null) listOf(cell) else null))
            }
            values.dimensions.size == 1 -> {
                val valueRow = grid.getOrNull(if (grid.size > 1) 1 else 0) ?: return TkResult.Failure(CsvError("There are no values"))
                val cells = valueRow.drop(if (number(valueRow.first()) == null || valueRow.size == columns + 1) 1 else 0)
                if (cells.size != columns) return TkResult.Failure(CsvError("It has ${cells.size} values; ${obj.name} has $columns"))
                val axis = grid.takeIf { it.size > 1 }?.first()?.drop(1)?.let(::numbers)?.takeIf { it.size == columns }
                TkResult.Success(record(obj.name, values, cells, axis?.let { listOf(DataAxis(it)) }))
            }
            values.dimensions.size == 2 -> {
                val body = grid.drop(1)
                if (body.size != rows) return TkResult.Failure(CsvError("It has ${body.size} rows of values; ${obj.name} has $rows"))
                val wrong = body.firstOrNull { it.size - 1 != columns }
                if (wrong != null) return TkResult.Failure(CsvError("A row has ${wrong.size - 1} values; ${obj.name} has $columns"))
                val x = numbers(grid.first().drop(1))
                val y = numbers(body.map { it.first() })
                val axes = if (x != null && y != null) listOf(DataAxis(x), DataAxis(y)) else null
                TkResult.Success(record(obj.name, values, body.flatMap { it.drop(1) }, axes))
            }
            else -> TkResult.Failure(CsvError("${obj.name} has more than two dimensions; open one slice as a table and paste into it"))
        }
    }

    private fun record(name: String, values: CalibrationValues, cells: List<String>, axes: List<DataAxis>?): DataRecord {
        val numbers = cells.map { number(it) }
        val texts = if (numbers.any { it == null }) cells else null
        return DataRecord(name, values.obj.kind, values.dimensions.toList(), if (texts == null) numbers.map { it!! } else emptyList(), texts = texts, axes = axes.orEmpty())
    }

    /** Every object of [set]: name, kind, where it is, its size, unit, limits and description */
    fun writeList(set: CalibrationSet, index: ObjectIndex? = null): String {
        val header = listOf("Name", "Kind", "Address", "Size", "Unit", "Lower limit", "Upper limit", "Description")
        val lines = listOf(header) + set.objects.map { obj ->
            val size = index?.parts(obj.name)?.sumOf { it.range.size }?.takeIf { it > 0 }?.toString().orEmpty()
            listOf(obj.name, obj.kind.name, obj.address.toHexAddress(), size, obj.unit, limit(obj.lowerLimit), limit(obj.upperLimit), obj.description)
        }
        return lines.joinToString("\n") { line -> line.joinToString(SEPARATOR) { quote(it) } } + "\n"
    }

    private fun limit(value: Double) = if (value.isFinite()) exactNumber(value) else ""

    /** The cells of [text], by row, without blank rows */
    fun grid(text: String): List<List<String>> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val separator = listOf('\t', ';', ',').maxByOrNull { c -> lines.first().count { it == c } } ?: ';'
        return lines.map { split(it, separator) }
    }

    private fun split(line: String, separator: Char): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                !quoted && c == separator -> {
                    cells += cell.toString().trim()
                    cell.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        cells += cell.toString().trim()
        return cells
    }

    /** A number, with a comma as the decimal point too */
    internal fun number(cell: String): Double? {
        val text = cell.trim().replace(" ", "")
        if (text.isEmpty()) return null
        return text.toDoubleOrNull() ?: text.takeIf { it.count { c -> c == ',' } == 1 }?.replace(',', '.')?.toDoubleOrNull()
    }

    private fun quote(cell: String): String =
        if (cell.any { it == ';' || it == '"' || it == '\n' || it == ',' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
}
