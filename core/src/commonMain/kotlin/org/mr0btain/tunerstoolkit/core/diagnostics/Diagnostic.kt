package org.mr0btain.tunerstoolkit.core.diagnostics

/** How bad a [Diagnostic] is */
enum class Severity {
    /** Worth knowing, nothing was lost */
    INFO,

    /** Something was off but we carried on, like an unknown A2L keyword we kept as is */
    WARNING,

    /** Something couldn't be read or decoded and was left out */
    ERROR
}

/**
 * Where in a source file a [Diagnostic] points. [line] and [column] start at 1
 */
data class SourceLocation(
    val file: String,
    val line: Int,
    val column: Int = 1
) {
    override fun toString(): String = "$file:$line:$column"
}

/**
 * One problem found while importing, decoding or exporting. Parsers report these instead of throwing or dropping
 * things silently, so the import report can say exactly what was skipped and why
 * @param objectName The calibration object it's about (a CHARACTERISTIC's name, say), when there is one
 */
data class Diagnostic(
    val severity: Severity,
    val message: String,
    val location: SourceLocation? = null,
    val objectName: String? = null
) {
    override fun toString(): String = buildString {
        append(severity.name)
        location?.let { append(" ").append(it) }
        objectName?.let { append(" [").append(it).append("]") }
        append(": ").append(message)
    }
}

/**
 * Collects [Diagnostic]s during one import or export run
 */
class Diagnostics {
    private val entries = mutableListOf<Diagnostic>()

    val all: List<Diagnostic> get() = entries.toList()
    val errorCount: Int get() = entries.count { it.severity == Severity.ERROR }
    val warningCount: Int get() = entries.count { it.severity == Severity.WARNING }
    val hasErrors: Boolean get() = errorCount > 0

    fun add(diagnostic: Diagnostic) {
        entries += diagnostic
    }

    fun info(message: String, location: SourceLocation? = null, objectName: String? = null) =
        add(Diagnostic(Severity.INFO, message, location, objectName))

    fun warning(message: String, location: SourceLocation? = null, objectName: String? = null) =
        add(Diagnostic(Severity.WARNING, message, location, objectName))

    fun error(message: String, location: SourceLocation? = null, objectName: String? = null) =
        add(Diagnostic(Severity.ERROR, message, location, objectName))

    /** "3 errors, 1 warning", for the import summary */
    fun summary(): String {
        fun count(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"
        return "${count(errorCount, "error")}, ${count(warningCount, "warning")}"
    }
}
