package org.mr0btain.tunerstoolkit.ui.exchange

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.Compare
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.formats.ttdef.TtDef
import org.mr0btain.tunerstoolkit.core.formats.values.ApplyReport
import org.mr0btain.tunerstoolkit.core.formats.values.CalibrationData
import org.mr0btain.tunerstoolkit.core.formats.values.Cdfx
import org.mr0btain.tunerstoolkit.core.formats.values.Csv
import org.mr0btain.tunerstoolkit.core.formats.values.DataRecord
import org.mr0btain.tunerstoolkit.core.formats.values.Dcm
import org.mr0btain.tunerstoolkit.core.formats.xdf.Xdf
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionFormat
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler

/** Formats calibration values are exchanged in */
enum class ValuesFormat(val label: String, val extension: String) {
    DCM("DCM", "dcm"),
    CDFX("CDFX", "cdfx")
}

/** Which maps an export holds */
enum class ExportScope(val label: String) {
    ALL("Every map"),
    CHANGED("Only the changed ones")
}

/** A file made for saving, and what couldn't go in it */
class Exported(val bytes: ByteArray, val name: String, val extension: String, val skipped: List<Diagnostic>, val count: Int)

/** What importing a values file did */
class ValuesImport(val name: String, val report: ApplyReport, val readProblems: List<Diagnostic>)

/**
 * Calibration values in and out by name (DCM, CDFX and PaCo in, DCM and CDFX out, a map as CSV both ways) and the
 * definition out (.ttdef and XDF). Values go into the open image as one undo step
 */
@Stable
class ExchangeHandler(private val documents: DocumentHandler, private val definitions: DefinitionHandler) {
    /** The last values import, for its report */
    var lastImport by mutableStateOf<ValuesImport?>(null)
        private set

    /**
     * Reads a DCM, CDFX or PaCo file and writes its values into the open image by name
     * @return What happened, in a line, or why nothing could
     */
    fun importValues(bytes: ByteArray, name: String): String {
        val set = definitions.definition?.set ?: return "Open a definition first: values go in by name"
        if (documents.document == null) return "Open an image first"
        val read: TkResult<Imported<List<DataRecord>>, ImportError> = when {
            Dcm.looksLike(bytes) -> Dcm.read(bytes, name)
            Cdfx.looksLike(bytes) -> Cdfx.read(bytes, name)
            else -> return "$name isn't a DCM, CDFX or PaCo file"
        }
        val records = when (read) {
            is TkResult.Success -> read.data
            is TkResult.Failure -> return "$name couldn't be read: ${read.error.message}"
        }
        val report = documents.edit { CalibrationData.apply(records.value, set, it) } ?: return "Open an image first"
        lastImport = ValuesImport(name, report, records.diagnostics)
        AppLogger.i("$TAG:Import", "Imported values from $name: ${report.summary}")
        return report.summary
    }

    fun clearImport() {
        lastImport = null
    }

    /** The open image's values in [format], or why not */
    fun exportValues(format: ValuesFormat, scope: ExportScope): TkResult<Exported, ExchangeError> {
        val definition = definitions.definition ?: return TkResult.Failure(ExchangeError("Open a definition first"))
        val document = documents.document ?: return TkResult.Failure(ExchangeError("Open an image first"))
        val names = when (scope) {
            ExportScope.ALL -> null
            ExportScope.CHANGED -> {
                val index = definitions.onImage?.build?.index ?: return TkResult.Failure(ExchangeError("The definition is still being read"))
                val original = documents.original() ?: return TkResult.Failure(ExchangeError("Open an image first"))
                Compare.images(document.image, original, index).changedObjects.map { it.name }.ifEmpty {
                    return TkResult.Failure(ExchangeError("No maps have changed"))
                }
            }
        }
        val (records, unread) = CalibrationData.records(definition.set, document.image, names)
        val (text, skipped) = when (format) {
            ValuesFormat.DCM -> Dcm.write(records, "${document.name}, values by Tuners Toolkit")
            ValuesFormat.CDFX -> Cdfx.write(records, definition.a2l?.module?.name ?: definition.title, document.name)
        }
        AppLogger.i("$TAG:Export", "Exported ${records.size} objects as ${format.label}")
        return TkResult.Success(Exported(text.encodeToByteArray(), document.name.substringBeforeLast('.'), format.extension, unread + skipped, records.size - skipped.size))
    }

    /** The open definition (with maps made by hand) as [format]: .ttdef or XDF */
    fun exportDefinition(format: DefinitionFormat): TkResult<Exported, ExchangeError> {
        val definition = definitions.definition ?: return TkResult.Failure(ExchangeError("There's no definition open"))
        val folders = definition.groupings.firstOrNull()?.folders.orEmpty()
        val baseName = definition.name.substringBeforeLast('.')
        return when (format) {
            DefinitionFormat.TTDEF -> {
                val text = TtDef.write(definition.set, definition.title, "Saved by Tuners Toolkit from ${definition.name}", folders)
                TkResult.Success(Exported(text.encodeToByteArray(), baseName, format.extension, emptyList(), definition.set.objects.size))
            }
            DefinitionFormat.XDF -> {
                val image = documents.document?.image ?: return TkResult.Failure(ExchangeError("Open the image too: an XDF's sizes and offsets come from it"))
                val written = Xdf.write(definition.set, image, definition.title, folders)
                TkResult.Success(Exported(written.text.encodeToByteArray(), baseName, format.extension, written.skipped, written.count))
            }
            DefinitionFormat.A2L -> TkResult.Failure(ExchangeError("Writing a whole A2L isn't offered; save it as a .ttdef or XDF"))
        }
    }

    /** Every object as a CSV list */
    fun exportMapList(): TkResult<Exported, ExchangeError> {
        val definition = definitions.definition ?: return TkResult.Failure(ExchangeError("There's no definition open"))
        val text = Csv.writeList(definition.set, definitions.onImage?.build?.index)
        return TkResult.Success(Exported(text.encodeToByteArray(), definition.name.substringBeforeLast('.') + "-maps", "csv", emptyList(), definition.set.objects.size))
    }

    /** One map as a CSV grid */
    fun exportMapCsv(obj: CalibrationObject): TkResult<Exported, ExchangeError> {
        val values = when (val result = definitions.decode(obj)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> return TkResult.Failure(ExchangeError(result.error.message))
            null -> return TkResult.Failure(ExchangeError("Open an image first"))
        }
        if (obj.kind == ObjectKind.ASCII || values.dimensions.size > 2) return TkResult.Failure(ExchangeError("Only values, curves and maps go to CSV"))
        return TkResult.Success(Exported(Csv.writeMap(values).encodeToByteArray(), obj.name, "csv", emptyList(), 1))
    }

    /**
     * Reads a CSV grid into [obj] (its size has to match), as one undo step
     * @return What happened, in a line
     */
    fun importMapCsv(obj: CalibrationObject, bytes: ByteArray): String {
        val set: CalibrationSet = definitions.definition?.set ?: return "There's no definition open"
        val values = (definitions.decode(obj) as? TkResult.Success)?.data ?: return "${obj.name} can't be read from the image"
        val record = when (val result = Csv.readMap(bytes.decodeToString(), values)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> return result.error.message
        }
        val report = documents.edit { CalibrationData.apply(listOf(record), set, it) } ?: return "Open an image first"
        return report.problems.firstOrNull()?.message ?: if (report.applied > 0) "Read into ${obj.name}" + if (report.clamped > 0) ", ${report.clamped} values set to their limits" else "" else "${obj.name} already has those values"
    }

    private companion object {
        const val TAG = "ExchangeHandler"
    }
}

/** Why an export couldn't be made */
class ExchangeError(override val message: String) : org.mr0btain.tunerstoolkit.core.error.Error
