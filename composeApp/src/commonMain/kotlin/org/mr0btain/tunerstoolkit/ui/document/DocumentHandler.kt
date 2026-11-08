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
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryFormat
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryImport
import org.mr0btain.tunerstoolkit.core.formats.binary.IntelHex
import org.mr0btain.tunerstoolkit.core.formats.binary.RawBinary
import org.mr0btain.tunerstoolkit.core.formats.binary.SRecord
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.BytePattern
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.find
import org.mr0btain.tunerstoolkit.core.image.toHexAddress

/**
 * An ECU image that's open
 * @param diagnostics What reading it found wrong, for the import report
 */
class OpenDocument(
    val name: String,
    val format: BinaryFormat,
    val image: MemoryImage,
    val diagnostics: List<Diagnostic>
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
     * @param onOpened Called once it's open
     * @param inBackground Reads on a background thread; false reads it now, for tests and previews
     */
    fun open(bytes: ByteArray, name: String, rawBaseAddress: Long = 0, inBackground: Boolean = true, onOpened: () -> Unit = {}) {
        loading = true
        openError = null
        val format = BinaryFormat.detect(bytes)
        if (!inBackground) {
            opened(BinaryImport.read(bytes, name, rawBaseAddress, format), name, format, onOpened)
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.Default) { BinaryImport.read(bytes, name, rawBaseAddress, format) }
            opened(result, name, format, onOpened)
        }
    }

    private fun opened(
        result: TkResult<org.mr0btain.tunerstoolkit.core.formats.Imported<MemoryImage>, org.mr0btain.tunerstoolkit.core.formats.ImportError>,
        name: String,
        format: BinaryFormat,
        onOpened: () -> Unit
    ) {
        loading = false
        when (result) {
                is TkResult.Success -> {
                    val image = result.data.value
                    document = OpenDocument(name, format, image, result.data.diagnostics)
                    version = image.version
                    lastSearch = null
                    AppLogger.i(
                        "$TAG:Open",
                        "Opened $name as ${format.label}: ${image.ranges.size} segments, ${image.size} bytes, " +
                            "${result.data.errorCount} errors, ${result.data.warningCount} warnings"
                    )
                    onOpened()
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
