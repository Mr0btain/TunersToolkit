package org.mr0btain.tunerstoolkit

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.mr0btain.tunerstoolkit.ui.compare.CompareHandler
import org.mr0btain.tunerstoolkit.ui.compare.CompareScreen
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.versions.VersionsScreen
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.editor.EditorHandler
import org.mr0btain.tunerstoolkit.ui.editor.EditorScreen
import org.mr0btain.tunerstoolkit.ui.files.FilesScreen
import org.mr0btain.tunerstoolkit.ui.gallery.GalleryScreen
import org.mr0btain.tunerstoolkit.ui.log.LogScreen
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.tunerstoolkit.ui.settings.SettingsHandler
import org.mr0btain.tunerstoolkit.ui.settings.SettingsScreen
import org.mr0btain.uikit.navigation.TmAppShell
import org.mr0btain.uikit.navigation.TmNavItem
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_compare
import tunerstoolkit.composeapp.generated.resources.tt_cpu
import tunerstoolkit.composeapp.generated.resources.tt_history
import tunerstoolkit.composeapp.generated.resources.tt_folder
import tunerstoolkit.composeapp.generated.resources.tt_sliders
import tunerstoolkit.composeapp.generated.resources.tt_terminal

/** The places in the app. [GALLERY] isn't in the nav; Settings opens it */
enum class Destination(val label: String, val icon: DrawableResource) {
    FILES("Files", Res.drawable.tt_folder),
    EDITOR("Editor", Res.drawable.tt_cpu),
    VERSIONS("Versions", Res.drawable.tt_history),
    COMPARE("Compare", Res.drawable.tt_compare),
    LOG("Log", Res.drawable.tt_terminal),
    SETTINGS("Settings", Res.drawable.tt_sliders),
    GALLERY("Gallery", Res.drawable.tt_sliders);

    val navItem: TmNavItem get() = TmNavItem(key = name, label = label, icon = icon)

    companion object {
        val inNav = listOf(FILES, EDITOR, VERSIONS, COMPARE, LOG, SETTINGS)
    }
}

/** Width from which phones and tablets get the nav rail instead of the bottom bar */
private val WIDE_FROM = 840.dp

/**
 * The app: the UI kit's theme and shell (a nav rail on wide windows and desktop, a bottom bar on phones) around
 * whichever place is open
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsHandler(scope) }
    val documents = remember { DocumentHandler(scope) }
    val snackbar = remember { SnackbarHostState() }
    val definitions = remember { DefinitionHandler(scope, documents) }
    val editor = remember { EditorHandler() }
    val projects = remember { ProjectHandler(scope, documents, definitions = definitions, notify = { scope.launch { snackbar.showSnackbar(it) } }) }
    val compare = remember { CompareHandler(scope, documents, definitions, projects) }
    var destination by rememberSaveable { mutableStateOf(Destination.FILES) }
    val data = settings.data

    TmTheme(
        palette = settings.palette,
        font = settings.font,
        darkTheme = data.darkMode,
        reduceTransparency = data.reduceTransparency
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // A desktop window is used with a mouse however narrow it is, so it keeps the rail
            val wide = getPlatform().name == "Java" || maxWidth >= WIDE_FROM

            TmAppShell(
                items = Destination.inNav.map { it.navItem },
                selectedKey = (if (destination == Destination.GALLERY) Destination.SETTINGS else destination).name,
                onSelect = { destination = Destination.valueOf(it.key) },
                snackbarState = snackbar,
                wide = wide
            ) {
                when (destination) {
                    Destination.FILES -> FilesScreen(documents, projects, definitions, onOpenEditor = { destination = Destination.EDITOR })
                    Destination.EDITOR -> EditorScreen(documents, projects, definitions, settings, snackbar, onOpenFiles = { destination = Destination.FILES }, editor = editor)
                    Destination.VERSIONS -> VersionsScreen(
                        projects,
                        definitions,
                        onOpenFiles = { destination = Destination.FILES },
                        onOpenEditor = { destination = Destination.EDITOR },
                        onCompare = { manifest, versionId ->
                            compare.compareWithVersion(manifest, versionId)
                            destination = Destination.COMPARE
                        }
                    )
                    Destination.COMPARE -> CompareScreen(
                        documents,
                        projects,
                        definitions,
                        compare,
                        onOpenFiles = { destination = Destination.FILES },
                        onOpenMap = { name ->
                            editor.open(name)
                            destination = Destination.EDITOR
                        }
                    )
                    Destination.LOG -> LogScreen()
                    Destination.SETTINGS -> SettingsScreen(settings, onOpenGallery = { destination = Destination.GALLERY })
                    Destination.GALLERY -> GalleryScreen(settings)
                }
            }
        }
    }
}
