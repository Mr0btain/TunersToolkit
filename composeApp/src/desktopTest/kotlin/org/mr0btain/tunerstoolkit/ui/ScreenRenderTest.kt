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
import org.mr0btain.tunerstoolkit.ui.settings.SettingsData
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.ui.checksum.ChecksumHandler
import org.mr0btain.tunerstoolkit.ui.checksum.ChecksumsScreen
import org.mr0btain.tunerstoolkit.ui.compare.CompareHandler
import org.mr0btain.tunerstoolkit.ui.compare.CompareScreen
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.exchange.ExchangeHandler
import org.mr0btain.tunerstoolkit.ui.versions.VersionsScreen
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.tunerstoolkit.ui.editor.EditorHandler
import org.mr0btain.tunerstoolkit.ui.editor.EditorScreen
import org.mr0btain.tunerstoolkit.ui.editor.MapView
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
    private val demoA2l = File("../testdata/demo/tt-demo-1.a2l").readBytes()

    private fun render(name: String, destination: Destination, width: Int = 1440, height: Int = 900, dark: Boolean = true, startAt: Long? = null, editor: EditorHandler = EditorHandler()) {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val documents = DocumentHandler(scope)
        // A project with a version, saved in memory, so the files screen lists it and the editor shows it's open
        val repository = ProjectRepository(MemoryProjectStore())
        val definitions = DefinitionHandler(scope, documents, inBackground = false)
        val projects = ProjectHandler(scope, documents, repository, definitions, readInBackground = false)
        definitions.open(demoA2l, "tt-demo-1.a2l")
        projects.openFile(demo, "tt-demo-1.hex")
        documents.write(0x0010L, byteArrayOf(0x12, 0x34))
        projects.saveAsProject("TT-DEMO-1 bench ECU", "Stage 1", "Richer part load")
        documents.write(0x0020L, byteArrayOf(0x56))
        // A tuned map, so the compare screen has something to show
        val ignition = definitions.definition!!.set["IgnitionBase"]!!
        val values = (definitions.decode(ignition) as TkResult.Success).data
        documents.edit { CalibrationCodec.writeAll(it, values, mapOf(0 to 15.0, 1 to 16.0, 17 to 20.0)) }
        val exchange = ExchangeHandler(documents, definitions)
        val checksums = ChecksumHandler(scope, documents, inBackground = false).apply { refresh() }
        val compare = CompareHandler(scope, documents, definitions, projects, inBackground = false).apply { compareWithOriginal() }
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
                        Destination.FILES -> FilesScreen(documents, projects, definitions, exchange, onOpenEditor = {})
                        Destination.EDITOR -> EditorScreen(documents, projects, definitions, exchange, settings, snackbar, onOpenFiles = {}, editor = editor, startAt = startAt)
                        Destination.SETTINGS -> SettingsScreen(settings, onOpenGallery = {})
                        Destination.CHECKSUMS -> ChecksumsScreen(documents, checksums, onOpenFiles = {}, onMessage = {})
                        Destination.VERSIONS -> VersionsScreen(projects, definitions, onOpenFiles = {}, onOpenEditor = {}, onCompare = { _, _ -> })
                        Destination.COMPARE -> CompareScreen(documents, projects, definitions, compare, onOpenFiles = {}, onOpenMap = {})
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
        // The calibration area, with the A2L's maps marked
        render("editor-maps", Destination.EDITOR, startAt = 0x0080_4100L)
    }

    @Test
    fun mapEditor() {
        fun withMap(name: String, view: MapView = MapView.TABLE): EditorHandler = EditorHandler().apply {
            expanded = setOf("Ignition", "Fueling")
            open(name)
            active!!.view = view
            active!!.table.select(org.mr0btain.tunerstoolkit.core.calibration.CellSelection(2..5, 3..6))
        }
        render("map-table", Destination.EDITOR, editor = withMap("IgnitionBase"))
        render("map-table-light", Destination.EDITOR, dark = false, editor = withMap("VolumetricEfficiency"))
        render("map-chart", Destination.EDITOR, editor = withMap("IgnitionBase", MapView.CHART))
        render("map-surface", Destination.EDITOR, editor = withMap("BoostTarget", MapView.SURFACE))
        render("map-curve", Destination.EDITOR, editor = withMap("WarmupEnrichment", MapView.CHART))
        render("map-narrow", Destination.EDITOR, width = 420, height = 860, editor = withMap("IgnitionBase"))
    }

    @Test
    fun files() = render("files", Destination.FILES, height = 2200)

    @Test
    fun versionsAndCompare() {
        render("versions", Destination.VERSIONS, height = 1100)
        render("compare", Destination.COMPARE, height = 1100)
        render("compare-narrow", Destination.COMPARE, width = 420, height = 1400)
        // The setup's edits broke the calibration and boot checksums
        render("checksums", Destination.CHECKSUMS, height = 1000)
    }

    @Test
    fun settings() = render("settings", Destination.SETTINGS)

    @Test
    fun gallery() = render("gallery", Destination.GALLERY, height = 7000)
}
