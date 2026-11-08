package org.mr0btain.tunerstoolkit.core.formats.values

import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.exactNumber
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported

/**
 * DAMOS DCM 2.0 (KONSERVIERUNG_FORMAT 2.0): calibration values by name as text. FESTWERT (value), FESTWERTEBLOCK
 * (block, `n @ m` for two dimensions), KENNLINIE/FESTKENNLINIE/GRUPPENKENNLINIE (curve with its own, a fixed or a
 * shared axis), KENNFELD/FESTKENNFELD/GRUPPENKENNFELD (map), STUETZSTELLENVERTEILUNG (shared axis) and TEXTSTRING.
 * Maps are written a row at a time: ST/Y then that row's WERT across x. Lines starting with `*` are comments
 */
object Dcm {
    private const val PER_LINE = 6

    fun looksLike(bytes: ByteArray): Boolean {
        val start = bytes.copyOf(minOf(bytes.size, 4096)).decodeToString()
        return "KONSERVIERUNG_FORMAT" in start || Regex("^\\s*(FESTWERT|KENNLINIE|KENNFELD|FESTWERTEBLOCK)\\s", RegexOption.MULTILINE).containsMatchIn(start)
    }

    // ------------------------------------------------ Writing

    /** DCM text for [records], and the ones DCM can't hold */
    fun write(records: List<DataRecord>, comment: String = "Written by Tuners Toolkit"): Pair<String, List<Diagnostic>> {
        val skipped = mutableListOf<Diagnostic>()
        val text = buildString {
            comment.lines().forEach { appendLine("* $it") }
            appendLine()
            appendLine("KONSERVIERUNG_FORMAT 2.0")
            appendLine()
            for (record in records) {
                val why = writeRecord(this, record)
                if (why != null) skipped += Diagnostic(Severity.WARNING, "Not in the DCM: $why", objectName = record.name)
            }
        }
        return text to skipped
    }

    private fun writeRecord(out: StringBuilder, record: DataRecord): String? {
        val dims = record.dimensions
        val x = record.axes.getOrNull(0)
        val y = record.axes.getOrNull(1)
        fun header(keyword: String, sizes: String = "") {
            out.append(keyword).append(' ').append(record.name)
            if (sizes.isNotEmpty()) out.append(' ').append(sizes)
            out.appendLine()
            if (record.description.isNotBlank()) out.appendLine("   LANGNAME ${quote(record.description)}")
        }
        fun units(vararg axes: DataAxis?) {
            axes.getOrNull(0)?.let { out.appendLine("   EINHEIT_X ${quote(it.unit)}") }
            axes.getOrNull(1)?.let { out.appendLine("   EINHEIT_Y ${quote(it.unit)}") }
            out.appendLine("   EINHEIT_W ${quote(record.unit)}")
        }
        fun numbers(keyword: String, values: List<Double>) = values.chunked(PER_LINE).forEach { line ->
            out.appendLine("   $keyword   ${line.joinToString("   ") { exactNumber(it) }}")
        }
        fun texts(keyword: String, values: List<String?>) = values.chunked(PER_LINE).forEach { line ->
            out.appendLine("   $keyword   ${line.joinToString("   ") { quote(it.orEmpty()) }}")
        }
        fun points(keyword: String, textKeyword: String, axis: DataAxis) = if (axis.texts != null) texts(textKeyword, axis.texts) else numbers(keyword, axis.values)
        fun shared(letter: String, axis: DataAxis?) = axis?.shared?.let { out.appendLine("*SST$letter   $it") }
        fun prefix(axes: List<DataAxis?>) = when {
            axes.any { it?.shared != null } -> "GRUPPEN"
            axes.all { it?.fixed == true } -> "FEST"
            else -> ""
        }

        when (record.kind) {
            ObjectKind.ASCII -> {
                header("TEXTSTRING")
                out.appendLine("   TEXT ${quote(record.text.orEmpty())}")
            }
            ObjectKind.VALUE -> {
                header("FESTWERT")
                units()
                if (record.texts != null) out.appendLine("   TEXT ${quote(record.texts.single().orEmpty())}") else numbers("WERT", record.values)
            }
            ObjectKind.VAL_BLK -> {
                if (dims.size > 2) return "it has more than two dimensions"
                header("FESTWERTEBLOCK", if (dims.size == 2) "${dims[0]} @ ${dims[1]}" else "${dims[0]}")
                units()
                record.values.chunked(dims[0]).forEach { row -> numbers("WERT", row) }
            }
            ObjectKind.AXIS -> {
                header("STUETZSTELLENVERTEILUNG", "${record.values.size}")
                out.appendLine("   EINHEIT_X ${quote(record.unit)}")
                numbers("ST/X", record.values)
            }
            ObjectKind.CURVE -> {
                header("${prefix(listOf(x))}KENNLINIE", "${dims[0]}")
                units(x)
                shared("X", x)
                x?.let { points("ST/X", "ST_TX/X", it) }
                if (record.texts != null) texts("TEXT", record.texts) else numbers("WERT", record.values)
            }
            ObjectKind.MAP -> {
                header("${prefix(listOf(x, y))}KENNFELD", "${dims[0]} ${dims[1]}")
                units(x, y)
                shared("X", x)
                shared("Y", y)
                x?.let { points("ST/X", "ST_TX/X", it) }
                for (row in 0 until dims[1]) {
                    when {
                        y?.texts != null -> out.appendLine("   ST_TX/Y   ${quote(y.texts[row])}")
                        y != null -> out.appendLine("   ST/Y   ${exactNumber(y.values[row])}")
                    }
                    numbers("WERT", record.values.subList(row * dims[0], (row + 1) * dims[0]))
                }
            }
            else -> return "DCM has no ${record.kind.name.lowercase().replace('_', ' ')}"
        }
        out.appendLine("END")
        out.appendLine()
        return null
    }

    private fun quote(text: String) = "\"" + text.replace("\"", "'").replace("\n", " ") + "\""

    // ------------------------------------------------ Reading

    fun read(bytes: ByteArray, name: String = ""): TkResult<Imported<List<DataRecord>>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val reader = Reader(bytes.decodeToString(), name)
        val records = reader.read()
        if (records.isEmpty() && reader.problems.none { it.severity == Severity.ERROR }) return TkResult.Failure(ImportError.NO_DATA)
        return TkResult.Success(Imported(records, reader.problems))
    }

    private val KINDS = mapOf(
        "FESTWERT" to ObjectKind.VALUE,
        "FESTWERTEBLOCK" to ObjectKind.VAL_BLK,
        "KENNLINIE" to ObjectKind.CURVE, "FESTKENNLINIE" to ObjectKind.CURVE, "GRUPPENKENNLINIE" to ObjectKind.CURVE,
        "KENNFELD" to ObjectKind.MAP, "FESTKENNFELD" to ObjectKind.MAP, "GRUPPENKENNFELD" to ObjectKind.MAP,
        "STUETZSTELLENVERTEILUNG" to ObjectKind.AXIS,
        "TEXTSTRING" to ObjectKind.ASCII
    )

    /** Blocks that hold no values, skipped to their END */
    private val SKIPPED = setOf("FUNKTIONEN", "VARIANTENKODIERUNG", "MODULKOPF")

    private class Reader(private val text: String, private val file: String) {
        val problems = mutableListOf<Diagnostic>()
        private val lines = text.lines()
        private var index = 0

        fun read(): List<DataRecord> {
            val records = mutableListOf<DataRecord>()
            while (index < lines.size) {
                val lineNumber = index + 1
                val tokens = tokens(lines[index++]) ?: continue
                val keyword = tokens.first().uppercase()
                when {
                    keyword == "KONSERVIERUNG_FORMAT" -> Unit
                    keyword in SKIPPED -> skipToEnd()
                    keyword in KINDS -> record(keyword, tokens, lineNumber)?.let { records += it }
                    else -> warn(lineNumber, "Unknown keyword $keyword, skipped")
                }
            }
            return records
        }

        private fun skipToEnd() {
            while (index < lines.size) if (tokens(lines[index++])?.firstOrNull()?.uppercase() == "END") return
        }

        private fun record(keyword: String, header: List<String>, line: Int): DataRecord? {
            val name = header.getOrNull(1) ?: return null.also { error(line, "$keyword without a name, skipped") }
            val sizes = header.drop(2).filter { it != "@" }.mapNotNull { it.toIntOrNull() }
            val kind = KINDS.getValue(keyword)
            var description = ""
            var unit = ""
            val units = arrayOf("", "")
            val values = mutableListOf<Double>()
            val texts = mutableListOf<String>()
            val xs = mutableListOf<Double>()
            val xTexts = mutableListOf<String>()
            val ys = mutableListOf<Double>()
            val yTexts = mutableListOf<String>()
            val shared = arrayOfNulls<String>(2)
            var ended = false
            while (index < lines.size) {
                val raw = lines[index]
                val number = ++index
                val trimmed = raw.trim()
                if (trimmed.startsWith("*SSTX")) shared[0] = trimmed.removePrefix("*SSTX").trim()
                if (trimmed.startsWith("*SSTY")) shared[1] = trimmed.removePrefix("*SSTY").trim()
                val tokens = tokens(raw) ?: continue
                val rest = tokens.drop(1)
                when (tokens.first().uppercase()) {
                    "END" -> {
                        ended = true
                        break
                    }
                    "LANGNAME" -> description = rest.joinToString(" ")
                    "DISPLAYNAME", "FUNKTION", "VAR" -> Unit
                    "EINHEIT_W" -> unit = rest.joinToString(" ")
                    "EINHEIT_X" -> units[0] = rest.joinToString(" ")
                    "EINHEIT_Y" -> units[1] = rest.joinToString(" ")
                    "WERT" -> values += numbers(rest, number)
                    "TEXT" -> texts += rest
                    "ST/X" -> xs += numbers(rest, number)
                    "ST_TX/X" -> xTexts += rest
                    "ST/Y" -> ys += numbers(rest, number)
                    "ST_TX/Y" -> yTexts += rest
                    else -> {
                        if (tokens.first().uppercase() in KINDS) {
                            index--
                            break
                        }
                        warn(number, "$name: unknown line ${tokens.first()}, skipped")
                    }
                }
            }
            if (!ended) warn(line, "$keyword $name has no END")
            val location = SourceLocation(file, line)
            fun axis(points: List<Double>, labels: List<String>, unit: String, which: Int, fixed: Boolean) =
                DataAxis(points.ifEmpty { labels.indices.map { it.toDouble() } }, labels.takeIf { it.isNotEmpty() }, unit, shared[which], fixed)
            val fixed = keyword.startsWith("FEST")
            return when (kind) {
                ObjectKind.ASCII -> DataRecord(name, kind, emptyList(), emptyList(), text = texts.joinToString(""), description = description, location = location)
                ObjectKind.AXIS -> DataRecord(name, kind, listOf(xs.size), xs, unit = units[0].ifEmpty { unit }, description = description, location = location)
                else -> {
                    val dims = when (kind) {
                        ObjectKind.VALUE -> emptyList()
                        ObjectKind.CURVE -> listOf(sizes.firstOrNull() ?: maxOf(values.size, texts.size))
                        ObjectKind.MAP -> sizes.take(2).takeIf { it.size == 2 } ?: return null.also { error(line, "$name: KENNFELD needs two sizes") }
                        else -> sizes.take(2).ifEmpty { listOf(values.size) }
                    }
                    val expected = dims.fold(1) { a, b -> a * b }
                    val count = if (texts.isNotEmpty() && values.isEmpty()) texts.size else values.size
                    if (count != expected) warn(line, "$name says it has $expected values but has $count")
                    val axes = when (kind) {
                        ObjectKind.CURVE -> listOf(axis(xs, xTexts, units[0], 0, fixed))
                        ObjectKind.MAP -> listOf(axis(xs, xTexts, units[0], 0, fixed), axis(ys, yTexts, units[1], 1, fixed))
                        else -> emptyList()
                    }
                    DataRecord(
                        name, kind, dims, values,
                        texts = texts.takeIf { it.isNotEmpty() && values.isEmpty() },
                        axes = axes, unit = unit, description = description, location = location
                    )
                }
            }
        }

        private fun numbers(tokens: List<String>, line: Int): List<Double> = tokens.mapNotNull { token ->
            token.replace(',', '.').toDoubleOrNull() ?: null.also { warn(line, "\"$token\" isn't a number, skipped") }
        }

        /** A line's words, quoted strings kept whole without their quotes; null for blank lines and comments */
        private fun tokens(line: String): List<String>? {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("!")) return null
            val tokens = mutableListOf<String>()
            var i = 0
            while (i < trimmed.length) {
                val c = trimmed[i]
                when {
                    c.isWhitespace() -> i++
                    c == '"' -> {
                        val end = trimmed.indexOf('"', i + 1).let { if (it < 0) trimmed.length else it }
                        tokens += trimmed.substring(i + 1, end)
                        i = end + 1
                    }
                    else -> {
                        val start = i
                        while (i < trimmed.length && !trimmed[i].isWhitespace()) i++
                        tokens += trimmed.substring(start, i)
                    }
                }
            }
            return tokens.ifEmpty { null }
        }

        private fun warn(line: Int, message: String) {
            problems += Diagnostic(Severity.WARNING, message, SourceLocation(file, line))
        }

        private fun error(line: Int, message: String) {
            problems += Diagnostic(Severity.ERROR, message, SourceLocation(file, line))
        }
    }
}
