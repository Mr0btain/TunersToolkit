package org.mr0btain.tunerstoolkit.core.formats

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.Error

/**
 * What an import read, with every problem it found along the way. Problems that left something out are
 * [Severity.ERROR] diagnostics; the import as a whole only fails (an [ImportError]) when nothing usable was read
 */
data class Imported<out T>(
    val value: T,
    val diagnostics: List<Diagnostic> = emptyList()
) {
    val errorCount: Int get() = diagnostics.count { it.severity == Severity.ERROR }
    val warningCount: Int get() = diagnostics.count { it.severity == Severity.WARNING }
}

/** Why an import failed outright */
enum class ImportError(override val message: String) : Error {
    EMPTY_FILE("The file is empty"),
    NO_DATA("No data was found in the file"),
    UNKNOWN_FORMAT("The file's format isn't one that can be read")
}

/** Why an export failed */
enum class ExportError(override val message: String) : Error {
    EMPTY_IMAGE("There's nothing to export"),
    ADDRESS_TOO_HIGH("An address is past what the format can hold"),
    TOO_BIG("The range is too big to export as one file")
}
