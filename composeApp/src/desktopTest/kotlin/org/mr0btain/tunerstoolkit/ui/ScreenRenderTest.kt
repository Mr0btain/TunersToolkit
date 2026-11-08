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
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
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
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val documents = DocumentHandler(scope)
        // A project with a version, saved in memory, so the files screen lists it and the editor shows it's open
        val repository = ProjectRepository(MemoryProjectStore())
        val projects = ProjectHandler(scope, documents, repository, readInBackground = false)
        projects.openFile(demo, "tt-demo-1.hex")
        documents.write(0x0010L, byteArrayOf(0x12, 0x34))
        projects.saveAsProject("TT-DEMO-1 bench ECU", "Stage 1", "Richer part load")
        documents.write(0x0020L, byteArrayOf(0x56))
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
                        Destination.FILES -> FilesScreen(documents, projects, onOpenEditor = {})
                        Destination.EDITOR -> EditorScreen(documents, projects, settings, snackbar, onOpenFiles = {})
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
