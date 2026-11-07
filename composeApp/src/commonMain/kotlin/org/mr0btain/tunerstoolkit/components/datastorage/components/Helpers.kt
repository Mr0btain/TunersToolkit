package org.mr0btain.tunerstoolkit.components.datastorage.components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class Keyboard {
    Opened, Closed
}

// State to see if keyboard is up or down
@Composable
fun keyboardAsState(): State<Keyboard> {
    // val view = LocalViewConfiguration.current
    /*
    DisposableEffect(view) {
        val onGlobalListener =

            ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
                Keyboard.Opened
            } else {
                Keyboard.Closed
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }
     */

    return remember { mutableStateOf(Keyboard.Closed) }
}

@Composable
fun BasicDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true,
        )
    ) {
        content()
    }
}

/**
 * Converts a String of Hexadecimal digits into a ByteArray
 */
fun String.hexToByteArray(): ByteArray {
    val byteArray = ByteArray(this.length / 2)

    for (i in byteArray.indices) {
        val index = i * 2
        val byteValue = this.substring(index, index + 2).toInt(16).toByte()
        byteArray[i] = byteValue
    }

    return byteArray
}

/**
 * Chunks a ByteArray into a list of ByteArrays with the size specified by chunkSize
 * @param chunkSize Size each array should be split into
 */
fun ByteArray.chunkByteArray(chunkSize: Int, startOffset: Int): Map<Int, ByteArray> {
    val chunks = mutableMapOf<Int, ByteArray>()

    for ((index, i) in (indices step chunkSize).withIndex()) {
        val chunk = this.copyOfRange(i, minOf(i + chunkSize, this.size))
        val start = startOffset + ((chunkSize * index) * 2)
        chunks[start] = chunk
    }

    return chunks
}

/**
 * Converts a ByteArray into a Hexadecimal String
 */

fun ByteArray.toHexString(): String {
    val hexChars = "0123456789ABCDEF"
    val hexString = StringBuilder(this.size * 2)

    for (byte in this) {
        val intValue = byte.toInt() and 0xFF
        hexString.append(hexChars[intValue shr 4])
        hexString.append(hexChars[intValue and 0x0F])
    }

    // byteValue.toInt().toString(16).padStart(2, '0')

    return hexString.toString()
}