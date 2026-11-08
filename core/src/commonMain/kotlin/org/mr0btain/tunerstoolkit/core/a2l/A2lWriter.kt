package org.mr0btain.tunerstoolkit.core.a2l

/**
 * Writes A2L text. The parser keeps every element of a file in its [A2lTree] (unknown keywords and IF_DATA and A2ML
 * text included), so writing the tree gives a file that reads back the same: the same keywords, values and blocks,
 * in the same order. Layout and comments aren't kept, and `/include`d files come back inline.
 *
 * [subset] writes a smaller A2L holding only some objects and what they need, for sharing a few maps.
 */
object A2lWriter {
    fun write(tree: A2lTree): String = buildString {
        tree.elements.forEach { appendElement(it, 0) }
    }

    fun write(element: A2lElement): String = buildString { appendElement(element, 0) }

    /**
     * A2L text with only the CHARACTERISTICs and AXIS_PTS in [names] from [module], plus what they refer to: shared
     * axes, curves used as axes, RECORD_LAYOUTs, COMPU_METHODs and their tables, and UNITs. MOD_COMMON, MOD_PAR, A2ML
     * and the module's IF_DATA are kept; measurements, functions, groups and everything else are left out
     */
    fun subset(file: A2lFile, module: A2lModule, names: Set<String>): String {
        val needed = Needed(module).apply { names.forEach(::obj) }
        val keptModule = module.element.copy(children = module.element.children.filter { child ->
            when (child.keyword) {
                "MOD_COMMON", "MOD_PAR", "A2ML", "IF_DATA" -> true
                "CHARACTERISTIC" -> child.text(0) in needed.characteristics
                "AXIS_PTS" -> child.text(0) in needed.axisPts
                "RECORD_LAYOUT" -> child.text(0) in needed.layouts
                "COMPU_METHOD" -> child.text(0) in needed.methods
                "COMPU_TAB", "COMPU_VTAB", "COMPU_VTAB_RANGE" -> child.text(0) in needed.tables
                "UNIT" -> child.text(0) in needed.units
                else -> false
            }
        })
        val project = file.project?.element ?: return ""
        val keptProject = project.copy(children = project.children.mapNotNull { child ->
            when {
                child === module.element -> keptModule
                child.keyword == "MODULE" -> null
                else -> child
            }
        })
        return buildString {
            file.tree.elements.forEach { element ->
                if (element === project) appendElement(keptProject, 0)
                else if (element.keyword != "PROJECT") appendElement(element, 0)
            }
        }
    }

    /** Collects the names a set of objects needs, following references */
    private class Needed(private val module: A2lModule) {
        val characteristics = mutableSetOf<String>()
        val axisPts = mutableSetOf<String>()
        val layouts = mutableSetOf<String>()
        val methods = mutableSetOf<String>()
        val tables = mutableSetOf<String>()
        val units = mutableSetOf<String>()

        fun obj(name: String) {
            module.characteristic(name)?.let { c ->
                if (!characteristics.add(name)) return
                layouts += c.recordLayout
                method(c.conversion)
                c.axes.forEach { axis ->
                    method(axis.conversion)
                    axis.axisPtsRef?.let(::obj)
                    axis.curveAxisRef?.let(::obj)
                }
                return
            }
            module.axisPts(name)?.let { a ->
                if (!axisPts.add(name)) return
                layouts += a.recordLayout
                method(a.conversion)
            }
        }

        private fun method(name: String) {
            val method = module.compuMethod(name) ?: return
            if (!methods.add(name)) return
            method.compuTabRef?.let { tables += it }
            method.statusStringRef?.let { tables += it }
            method.refUnit?.let { units += it }
        }
    }

    private fun A2lElement.copy(children: List<A2lElement>) = A2lElement(keyword, block, values, children, location, known, raw)

    private fun StringBuilder.appendElement(element: A2lElement, depth: Int) {
        val indent = INDENT.repeat(depth)
        append(indent)
        if (element.block) append("/begin ")
        append(element.keyword)
        element.values.forEach { append(' ').append(format(it)) }
        when {
            element.raw != null -> {
                val raw = element.raw
                if (raw.isNotEmpty() && !raw.first().isWhitespace()) append(' ')
                append(raw)
                if (raw.isNotEmpty() && !raw.last().isWhitespace()) append(' ')
                if (raw.isEmpty()) append(' ')
                append("/end ").append(element.keyword).append('\n')
            }
            element.block -> {
                append('\n')
                element.children.forEach { appendElement(it, depth + 1) }
                append(indent).append("/end ").append(element.keyword).append('\n')
            }
            else -> append('\n')
        }
    }

    /** A value as A2L writes it: strings quoted with their escapes put back, everything else as it was written */
    fun format(value: A2lValue): String = if (value.kind == ValueKind.STRING) quote(value.text) else value.text

    fun quote(text: String): String = buildString(text.length + 2) {
        append('"')
        for (char in text) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
        append('"')
    }

    private const val INDENT = "  "
}
