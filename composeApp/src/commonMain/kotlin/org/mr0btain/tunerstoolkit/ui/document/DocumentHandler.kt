package org.mr0btain.tunerstoolkit.ui.document

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ExportError
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.error.map
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryFormat
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryImport
import org.mr0btain.tunerstoolkit.core.formats.binary.IntelHex
import org.mr0btain.tunerstoolkit.core.formats.binary.RawBinary
import org.mr0btain.tunerstoolkit.core.formats.binary.SRecord
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.BytePattern
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.Patch
import org.mr0btain.tunerstoolkit.core.image.find
import org.mr0btain.tunerstoolkit.core.image.toHexAddress

/**
 * An ECU image that's open
 * @param diagnostics What reading it found wrong, for the import report
 * @param source The file it was read from, unchanged, so it can be saved as a project
 * @param rawBaseAddress Where it starts in memory when it's a raw binary
 */
class OpenDocument(
    val name: String,
    val format: BinaryFormat,
    val image: MemoryImage,
    val diagnostics: List<Diagnostic>,
    val source: ByteArray = ByteArray(0),
    val rawBaseAddress: Long = 0
)

/**
 * The open image and every change to it. All writes go through here, so [version] moves and the hex view redraws
 */
@Stable
class DocumentHandler(private val scope: CoroutineScope) {
    var document by mutableStateOf<OpenDocument?>(null)
        private set

    /** The open image's MemoryImage.version, as state */
    var version by mutableLongStateOf(0L)
        private set

    var loading by mutableStateOf(false)
        private set

    /** Why the last open failed, for the files screen */
    var openError by mutableStateOf<String?>(null)
        private set

    /** The last thing searched for, for find next */
    var lastSearch by mutableStateOf<BytePattern?>(null)

    /**
     * Reads [bytes] as an ECU image, in whichever format they're in
     * @param rawBaseAddress Where a raw binary starts in memory
     * @param patches Changes to make once it's read, as a saved version's; they can't be undone
     * @param onOpened Called with the document once it's open
     * @param inBackground Reads on a background thread; false reads it now, for tests and previews
     */
    fun open(
        bytes: ByteArray,
        name: String,
        rawBaseAddress: Long = 0,
        patches: List<Patch> = emptyList(),
        inBackground: Boolean = true,
        onOpened: (OpenDocument) -> Unit = {}
    ) {
        loading = true
        openError = null
        val format = BinaryFormat.detect(bytes)
        val read = { BinaryImport.read(bytes, name, rawBaseAddress, format).map { it.also { imported -> imported.value.restore(patches) } } }
        if (!inBackground) {
            opened(read(), name, format, bytes, rawBaseAddress, onOpened)
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.Default) { read() }
            opened(result, name, format, bytes, rawBaseAddress, onOpened)
        }
    }

    /** Puts a saved version's changes back, as where undo starts */
    private fun MemoryImage.restore(patches: List<Patch>) {
        if (patches.isEmpty()) return
        apply(patches.filter { contains(it.address, it.after.size) })
        clearHistory()
    }

    private fun opened(
        result: TkResult<Imported<MemoryImage>, ImportError>,
        name: String,
        format: BinaryFormat,
        source: ByteArray,
        rawBaseAddress: Long,
        onOpened: (OpenDocument) -> Unit
    ) {
        loading = false
        when (result) {
                is TkResult.Success -> {
                    val image = result.data.value
                    val opened = OpenDocument(name, format, image, result.data.diagnostics, source, rawBaseAddress)
                    document = opened
                    version = image.version
                    lastSearch = null
                    AppLogger.i(
                        "$TAG:Open",
                        "Opened $name as ${format.label}: ${image.ranges.size} segments, ${image.size} bytes, " +
                            "${result.data.errorCount} errors, ${result.data.warningCount} warnings"
                    )
                    onOpened(opened)
                }
                is TkResult.Failure -> {
                    openError = result.error.message
                    AppLogger.w("$TAG:Open", "Couldn't open $name: ${result.error.message}")
                }
            }
    }

    fun close() {
        document = null
        version = 0
    }

    fun write(address: Long, bytes: ByteArray) {
        val image = document?.image ?: return
        try {
            if (image.write(address, bytes)) version = image.version
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.w("$TAG:Write", "Write outside the image at ${address.toHexAddress()}", e)
        }
    }

    fun undo() {
        val image = document?.image ?: return
        if (image.undo()) version = image.version
    }

    fun redo() {
        val image = document?.image ?: return
        if (image.redo()) version = image.version
    }

    /** The next match of [pattern] after (or before) [from], wrapping round. Remembered for find next */
    fun find(pattern: BytePattern, from: Long, backwards: Boolean = false): AddressRange? {
        lastSearch = pattern
        val image = document?.image ?: return null
        val start = if (backwards) from - 1 else from + 1
        return image.find(pattern, start.coerceAtLeast(0), backwards, wrap = true)
            ?.let { AddressRange.ofSize(it, pattern.size.toLong()) }
    }

    /** The image in [format], as file bytes, or why it can't be */
    fun export(format: BinaryFormat): TkResult<ByteArray, ExportError> {
        val image = document?.image ?: return TkResult.Failure(ExportError.EMPTY_IMAGE)
        val result = when (format) {
            BinaryFormat.INTEL_HEX -> IntelHex.write(image, bytesPerRecord = 32).toBytes()
            BinaryFormat.S_RECORD -> SRecord.write(image).toBytes()
            BinaryFormat.RAW -> RawBinary.write(image)
        }
        when (result) {
            is TkResult.Success -> AppLogger.i("$TAG:Export", "Exported ${document?.name} as ${format.label}, ${result.data.size} bytes")
            is TkResult.Failure -> AppLogger.w("$TAG:Export", "Couldn't export as ${format.label}: ${result.error.message}")
        }
        return result
    }

    private fun TkResult<String, ExportError>.toBytes(): TkResult<ByteArray, ExportError> = when (this) {
        is TkResult.Success -> TkResult.Success(data.encodeToByteArray())
        is TkResult.Failure -> this
    }

    private companion object {
        const val TAG = "DocumentHandler"
    }
}
