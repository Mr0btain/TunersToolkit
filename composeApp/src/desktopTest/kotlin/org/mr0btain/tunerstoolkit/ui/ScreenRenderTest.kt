package org.mr0btain.tunerstoolkit.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import org.mr0btain.tunerstoolkit.Destination
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.editor.EditorScreen
import org.mr0btain.tunerstoolkit.ui.files.FilesScreen
import org.mr0btain.tunerstoolkit.ui.gallery.GalleryScreen
import org.mr0btain.tunerstoolkit.ui.settings.SettingsHandler
import org.mr0btain.tunerstoolkit.ui.settings.SettingsScreen
import org.mr0btain.uikit.navigation.TmAppShell
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalettes
import org.mr0btain.uikit.theme.TmTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the main screens with the demo image open to PNGs in composeApp/build/screen-renders, wide and at phone
 * width, so a UI change can be looked over without running the app
 */
class ScreenRenderTest {
    private val outputDir = File("build/screen-renders").apply { mkdirs() }
    private val demo = File("../testdata/demo/tt-demo-1.hex").readBytes()

    private fun render(name: String, destination: Destination, width: Int = 1440, height: Int = 900, dark: Boolean = true) {
        val documents = DocumentHandler(CoroutineScope(Dispatchers.Unconfined))
        documents.open(demo, "tt-demo-1.hex", inBackground = false)
        val settings = SettingsHandler(CoroutineScope(Dispatchers.Unconfined), SettingsData(darkMode = dark))

        val bytes = ImageComposeScene(width, height, Density(1f)) {
            val snackbar = remember { SnackbarHostState() }
            TmTheme(palette = TmPalettes.default, font = TmFonts.default, darkTheme = dark) {
                TmAppShell(
                    items = Destination.inNav.map { it.navItem },
                    selectedKey = destination.name,
                    onSelect = {},
                    snackbarState = snackbar,
                    wide = width >= 840
                ) {
                    when (destination) {
                        Destination.FILES -> FilesScreen(documents, onOpenEditor = {})
                        Destination.EDITOR -> EditorScreen(documents, settings, snackbar, onOpenFiles = {})
                        Destination.SETTINGS -> SettingsScreen(settings, onOpenGallery = {})
                        Destination.GALLERY -> GalleryScreen(settings)
                        else -> Unit
                    }
                }
            }
        }.use { scene ->
            var frame = scene.render(0)
            repeat(8) { index ->
                Thread.sleep(60)
                frame = scene.render((index + 1) * 16_000_000L)
            }
            frame.encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
        val file = File(outputDir, "$name.png").apply { writeBytes(bytes) }
        assertTrue(file.length() > 0)
    }

    @Test
    fun editor() {
        render("editor", Destination.EDITOR)
        render("editor-light", Destination.EDITOR, dark = false)
        render("editor-narrow", Destination.EDITOR, width = 420, height = 860)
    }

    @Test
    fun files() = render("files", Destination.FILES)

    @Test
    fun settings() = render("settings", Destination.SETTINGS)

    @Test
    fun gallery() = render("gallery", Destination.GALLERY, height = 5200)
}
