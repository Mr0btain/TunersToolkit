package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableState

/** How an open map is shown */
enum class MapView(val label: String) { TABLE("Table"), CHART("Chart"), SURFACE("3D") }


/** A map open in its own tab, with where its cursor is and how it's shown */
@Stable
class OpenMap(val name: String) {
    val table = MapTableState()
    var view by mutableStateOf(MapView.TABLE)
}

/**
 * What the editor shows, kept while other screens are open: the map tabs, which tab is in front, the selected object,
 * and the map list's mode, search and open folders
 */
@Stable
class EditorHandler {
    val openMaps = mutableStateListOf<OpenMap>()

    /** The map tab in front; null for the hex view */
    var activeMap by mutableStateOf<String?>(null)
        private set

    /** The object picked last, outlined in the hex view and the list */
    var selectedObject by mutableStateOf<String?>(null)

    /** An address the hex view should scroll to next, taken by it once done */
    var revealRequest by mutableStateOf<Long?>(null)

    /** Which of the definition's groupings the map list uses, by label; null for the first, [ALL] for none */
    var listGrouping by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var expanded by mutableStateOf<Set<String>>(emptySet())

    /** Opens [name] in a tab (or brings its tab to the front) */
    fun open(name: String) {
        if (openMaps.none { it.name == name }) {
            if (openMaps.size >= MAX_TABS) openMaps.removeAt(0)
            openMaps += OpenMap(name)
        }
        activeMap = name
        selectedObject = name
    }

    fun close(name: String) {
        val index = openMaps.indexOfFirst { it.name == name }
        if (index < 0) return
        openMaps.removeAt(index)
        if (activeMap == name) activeMap = openMaps.getOrNull(index)?.name ?: openMaps.lastOrNull()?.name
    }

    /** Brings the hex view to the front, scrolled to [address] when there is one */
    fun showHex(address: Long? = null) {
        activeMap = null
        address?.let { revealRequest = it }
    }

    fun toggle(path: String) {
        expanded = if (path in expanded) expanded - path else expanded + path
    }

    /** Forgets maps that belonged to another definition */
    fun reset() {
        openMaps.clear()
        activeMap = null
        selectedObject = null
        expanded = emptySet()
    }

    val active: OpenMap? get() = openMaps.firstOrNull { it.name == activeMap }

    companion object {
        /** More tabs than this and the oldest closes */
        private const val MAX_TABS = 12

        /** The map list's choice of every object in one list */
        const val ALL = "All"
    }
}
