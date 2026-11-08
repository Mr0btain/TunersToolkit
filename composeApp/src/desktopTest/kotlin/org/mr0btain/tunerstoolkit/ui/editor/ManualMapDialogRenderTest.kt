package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import org.mr0btain.tunerstoolkit.core.calibration.ManualAxis
import org.mr0btain.tunerstoolkit.core.calibration.ManualMap
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalettes
import org.mr0btain.uikit.theme.TmTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Draws the form for a map made by hand to composeApp/build/screen-renders */
class ManualMapDialogRenderTest {
    @Test
    fun theForm() {
        val map = ManualMap(
            name = "Boost limit", address = 0x0080_4400, columns = 8, rows = 6,
            x = ManualAxis(ManualAxis.Source.MEMORY, address = 0x0080_4380), y = ManualAxis(ManualAxis.Source.EVEN, 20.0, 120.0)
        )
        val bytes = ImageComposeScene(1200, 1000, Density(1f)) {
            TmTheme(palette = TmPalettes.default, font = TmFonts.default, darkTheme = true) {
                ManualMapDialog(initial = map, editing = false, taken = emptySet(), onSave = {}, onDismiss = {})
            }
        }.use { scene ->
            var frame = scene.render(0)
            repeat(6) { frame = scene.render((it + 1) * 16_000_000L) }
            frame.encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
        val file = File("build/screen-renders/manual-map.png").apply { parentFile.mkdirs(); writeBytes(bytes) }
        assertTrue(file.length() > 0)
    }
}
