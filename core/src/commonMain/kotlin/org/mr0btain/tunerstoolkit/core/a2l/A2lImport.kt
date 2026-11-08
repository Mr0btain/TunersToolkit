package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported

/**
 * Reads an A2L file: decodes the text, parses it and builds the model. Like the binary readers it never throws;
 * everything it found wrong is in the result's diagnostics, and it only fails when there's no file or no PROJECT
 */
object A2lImport {
    /**
     * @param name The file's name, for diagnostics and for resolving `/include`s against
     * @param resolver Finds included files; without one, `/include`s are skipped with a warning
     */
    fun read(bytes: ByteArray, name: String = "", resolver: A2lFileResolver? = null): TkResult<Imported<A2lFile>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        return readText(A2lText.decode(bytes), name, resolver)
    }

    fun readText(text: String, name: String = "", resolver: A2lFileResolver? = null): TkResult<Imported<A2lFile>, ImportError> {
        if (text.isBlank()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val parser = A2lParser(text, name, resolver)
        val tree = parser.parse()
        val builder = A2lModelBuilder(tree)
        val file = builder.build()
        if (file.project == null) return TkResult.Failure(ImportError.NO_DATA)
        val diagnostics = (parser.diagnostics + builder.diagnostics)
            .sortedWith(compareBy({ it.location?.file != name }, { it.location?.file }, { it.location?.line ?: 0 }, { it.location?.column ?: 0 }))
        return TkResult.Success(Imported(file, diagnostics))
    }

    /** A one line summary of what was read, for the log: never the file's contents */
    fun summary(imported: Imported<A2lFile>): String {
        val modules = imported.value.modules
        return "${modules.size} modules, ${modules.sumOf { it.characteristics.size }} characteristics, " +
            "${modules.sumOf { it.axisPts.size }} axis points, ${modules.sumOf { it.measurements.size }} measurements, " +
            "${imported.diagnostics.count { it.severity == Severity.ERROR }} errors, ${imported.diagnostics.count { it.severity == Severity.WARNING }} warnings"
    }
}
