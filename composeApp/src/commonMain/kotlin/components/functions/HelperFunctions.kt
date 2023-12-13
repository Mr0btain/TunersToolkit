package components.functions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ui.theme.TunerToolkitTheme

enum class Keyboard {
    Opened, Closed
}

// State to see if keyboard is up or down
@Composable
fun keyboardAsState(): State<Keyboard> {
    val keyboardState = remember { mutableStateOf(Keyboard.Closed) }
    val view = LocalViewConfiguration.current
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

    return keyboardState
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

@Composable
fun LoadingProgress(
    isLoading: Boolean,
    onDismissRequest: () -> Unit
) {
    if (isLoading) {
        TunerToolkitTheme {
            Dialog(
                onDismissRequest = onDismissRequest,
                DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,

                    )
                }
            }
        }
    }
}

@Composable
fun BoxWithConstraintsScope.calcTextSize(
    maxFontSize: TextUnit = 80.sp,
    scaleFactor: Float = 0.9F,
    density: Density,
    text: String,
    lineCount: Int,
): TextUnit {
    var mFontSize = maxFontSize

    if (lineCount > 0) {
        val calculateParagraph = @Composable {
            Paragraph(
                text = text,
                style = TextStyle(fontSize = mFontSize),
                density = density,
                resourceLoader = LocalFontLoader.current,
                width = with(density) { maxWidth.toPx() }
            )
        }

        var intrinsics = calculateParagraph()

        with(density) {
            while (
                (intrinsics.height.toDp() > (maxHeight - 4.dp) || intrinsics.didExceedMaxLines)
            ) {
                mFontSize *= scaleFactor
                intrinsics = calculateParagraph()
            }
        }
    }
    return mFontSize
}

fun getDigitLocationFromMemoryAddress(memAddress: Long, memOffset: Long) {
   val itemSize = 16 // Number of characters per item
    val itemIndex = (memAddress - memOffset) / itemSize
    println(itemIndex * itemSize)
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