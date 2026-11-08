package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalettes
import org.mr0btain.uikit.theme.TmTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the hex view with a selection over whole rows, marks and the cursor at the end of a row, at several screen
 * densities and row widths, to composeApp/build/screen-renders/hex-*.png: the tints should sit exactly over their
 * digits all the way to the end of each row
 */
class HexViewRenderTest {
    private val image = MemoryImage.Builder().apply { put(0x1000, ByteArray(512) { (it * 7).toByte() }) }.build().image

    private fun render(name: String, density: Float, bytesPerRow: Int, wordSize: Int = 1) {
        val state = HexViewState(bytesPerRow).apply {
            display = HexDisplay(wordSize = wordSize)
            select(AddressRange(0x1000L + bytesPerRow, 0x1000L + bytesPerRow * 3))
            moveTo(0x1000L + bytesPerRow * 5 - 1)
        }
        val marks = HexMarks { row ->
            listOf(
                HexMark(AddressRange(0x1000L + bytesPerRow * 6 + 3, 0x1000L + bytesPerRow * 9 + 4), "Map", HexMarkRole.VALUES),
                HexMark(AddressRange(0x1000L + bytesPerRow * 10, 0x1000L + bytesPerRow * 10 + 3), "Map", HexMarkRole.RECORD),
                HexMark(AddressRange(0x1000L + bytesPerRow * 10 + 4, 0x1000L + bytesPerRow * 11 + 7), "Axis", HexMarkRole.AXIS, selected = true)
            ).filter { it.range.overlaps(row) }
        }
        val width = (1100 * density).toInt()
        val bytes = ImageComposeScene(width, (420 * density).toInt(), Density(density)) {
            TmTheme(palette = TmPalettes.default, font = TmFonts.default, darkTheme = true) {
                TmHexView(image = image, version = 0, state = state, onWrite = { _, _ -> }, marks = marks)
            }
        }.use { scene ->
            var frame = scene.render(0)
            repeat(4) { frame = scene.render((it + 1) * 16_000_000L) }
            frame.encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
        val file = File("build/screen-renders/hex-$name.png").apply { parentFile.mkdirs(); writeBytes(bytes) }
        assertTrue(file.length() > 0)
    }

    @Test
    fun selectionLinesUp() {
        render("16-1x", 1f, 16)
        render("32-1x", 1f, 32)
        render("32-1.25x", 1.25f, 32)
        render("32-2x", 2f, 32)
        render("16-words-1.5x", 1.5f, 16, wordSize = 4)
    }
}
