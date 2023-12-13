package components.functions.filetools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPresentationController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeContent
import platform.darwin.NSObject

@Composable
actual fun FilePicker(
    fileType: FileType,
    fileAction: (ByteArray) -> Unit,
    filePickCloser: MutableState<Boolean>,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    val pickerDelegate = object : NSObject(),
        UIDocumentPickerDelegateProtocol,
        UIAdaptivePresentationControllerDelegateProtocol {

        override fun documentPicker(
            controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>
        ) {
            (didPickDocumentsAtURLs.firstOrNull() as? NSURL).let { selected ->
                fileAction(selected?.readByteArray() ?: ByteArray(0))
            }
        }

        override fun documentPickerWasCancelled(
            controller: UIDocumentPickerViewController
        ) {}

        override fun presentationControllerWillDismiss(
            presentationController: UIPresentationController
        ) {
            (presentationController.presentedViewController as? UIDocumentPickerViewController)
                ?.let { documentPickerWasCancelled(it) }
        }
    }

    val contentTypes: List<UTType> = when (fileType) {
        FileType.ALL -> {
            listOf("*").mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { listOf(UTTypeContent) }
        }
        FileType.TEXT -> {
            listOf("txt").mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { listOf(UTTypeContent) }
        }
        FileType.HEX -> {
            listOf("hex", "Hex").mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { listOf(UTTypeContent) }
        }
        FileType.A2l -> {
            listOf("a2l", "A2l", "A2L").mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { listOf(UTTypeContent) }
        }
    }

    fun createPicker() = UIDocumentPickerViewController(
        forOpeningContentTypes = contentTypes
    ).apply { delegate = pickerDelegate }

    try {
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            // Reusing a closed/dismissed picker causes problems with
            // triggering delegate functions, launch with a new one.
            createPicker(),
            animated = true,
            completion = null
        )
    } catch (e: Exception) { e.printStackTrace() }
    finally {
        filePickCloser.value = false
    }
}