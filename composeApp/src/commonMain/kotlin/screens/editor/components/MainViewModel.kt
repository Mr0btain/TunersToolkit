package screens.editor.components

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import components.a2l.classes.data.A2lData
import components.datastorage.components.getSettings
import components.datastorage.components.saveSettings
import components.hex.HexSegmentData
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.maplisttab.classes.MapDataContainer
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import screens.settings.classes.SettingsData
import ui.theme.md_theme_light_error
import ui.theme.md_theme_light_inverseSurface
import ui.theme.md_theme_light_primary

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class MainViewModel() : ViewModel() {
    val appBarTitle = mutableStateOf("Tuners Toolkit")
    val a2lData = mutableStateOf(A2lData())
    val hexDataSegments = mutableStateMapOf<String, HexSegmentData>()
    val mapData = mutableStateOf(MapValuesSorted())
    val selectedHexSegment = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val projectInfo = mutableStateOf(ProjectInfo())
    val bitsPer = mutableIntStateOf(2)
    val digitsPerLine = mutableIntStateOf(8)
    val textSize = mutableStateOf(18.sp)
    val screenSize = mutableStateOf(IntSize(0,0))
    val mapListItems = mutableStateListOf<MapDataContainer>()
    val JetBrainsMono = FontFamily.Monospace
    val firstVisibleItemIdx = mutableIntStateOf(0)
    val firstVisibleItemOffset = mutableIntStateOf(0)
    val utilitySelection = mutableIntStateOf(0)
    val settings = mutableStateOf(getSettings())
    val showNewProjectDialog = mutableStateOf(false)
    val showNewVersionDialog = mutableStateOf(false)
    val showOpenVersionDialog = mutableStateOf(false)
    val showOpenProjectDialog = mutableStateOf(false)
    val showDeleteProjectDialog = mutableStateOf(false)
    val showDeleteVersionDialog = mutableStateOf(false)
    val showImportDialog = mutableStateOf(false)
    val mapFillColor = mutableStateOf(md_theme_light_inverseSurface)
    val mapOutlineColor = mutableStateOf(md_theme_light_primary)
    val mapSelectedOutlineColor = mutableStateOf(md_theme_light_error)

    /*
    font("JetBrains", "jbmono_reg", FontWeight.Normal, FontStyle.Normal),
        font("JetBrains", "jbmono_med", FontWeight.Medium, FontStyle.Normal),
        font("JetBrains", "jbmono_bold", FontWeight.Bold, FontStyle.Normal),
        font("JetBrains", "jbmono_semibold", FontWeight.SemiBold, FontStyle.Normal),
        font("JetBrains", "jbmono_extrabold", FontWeight.ExtraBold, FontStyle.Normal),
        font("JetBrains", "jetbrainsmono_italic", FontWeight.Normal, FontStyle.Italic)
     */

    fun updateSettings(input: SettingsData) {
        settings.value = input
        viewModelScope.launch(Dispatchers.IO) { saveSettings(input) }
    }

    fun toolbarNameReset() { appBarTitle.value = "Tuners Toolkit" }
}