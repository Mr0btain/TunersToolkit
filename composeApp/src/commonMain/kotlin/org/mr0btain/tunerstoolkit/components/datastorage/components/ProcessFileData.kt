package org.mr0btain.tunerstoolkit.components.datastorage.components
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import org.mr0btain.tunerstoolkit.components.helpers.checkItemsAre
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentMap
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab.classes.MapDataContainer
import org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.classes.ProjectInfo
import org.mr0btain.tunerstoolkit.MainViewModel
fun ProjectFilesManager.processFileData(name: String, version: String, viewModel: MainViewModel) {
    this.getProjectData(name, version).forEach { file ->
        when (file.first) {
            ProjectFileType.MapData -> {
                val res = file.first.deserialize(file.second) as? MapValuesSorted
                res?.let { viewModel.mapData.value = it }
            }

            ProjectFileType.HexData -> {
                val res = file.first.deserialize(file.second) as? HexSegmentMap
                if (res != null) {
                    viewModel.hexDataSegments.clear()
                    viewModel.hexDataSegments.putAll(res.data)
                    viewModel.selectedHexSegment.value = "800000"
                }
            }

            ProjectFileType.BinData -> {
                // TODO
            }

            ProjectFileType.A2lData -> {
                val res = file.first.deserialize(file.second) as? A2lData
                res?.let { viewModel.a2lData.value = it }
            }

            ProjectFileType.ProjectInfo -> {
                val res = file.first.deserialize(file.second) as? ProjectInfo
                res?.let { viewModel.projectInfo.value = it }
            }

            ProjectFileType.MapListData -> {
                val res = (file.first.deserialize(file.second) as? List<*>)?.checkItemsAre<MapDataContainer>()
                res?.let {
                    // Clear current list
                    viewModel.mapListItems.clear()
                    // Add current list
                    viewModel.mapListItems.addAll(it)
                }
            }

            ProjectFileType.NotFound -> {
                // not done
            }
        }
    }
}