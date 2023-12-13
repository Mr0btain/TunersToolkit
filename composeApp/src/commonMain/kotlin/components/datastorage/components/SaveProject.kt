package components.datastorage.components

import components.datastorage.ProjectFilesManager
import components.parsers.hex.HexSegmentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import screens.editor.components.MainViewModel

fun saveNewProject(
    viewModel: MainViewModel,
    filesManager: ProjectFilesManager,
    scope: CoroutineScope,
    projectName: String,
    projectVersion: String = "Original"
) {
    scope.launch(Dispatchers.IO) {
        val mMapDataSer = Json.encodeToString(viewModel.mapData.value)
        val mHexData = Json.encodeToString(HexSegmentMap(viewModel.hexDataSegments))
        val mMapList = Json.encodeToString(viewModel.mapListItems.toList())
        //val mA2lData = Json.encodeToString(viewModel.a2lData.value)

        if (projectName.isBlank()) {
            var nameNum = 0
            val takenNames = filesManager.getProjectList()

            takenNames.forEach {
                if (it.contains("Project")) {
                    val nameTaken = it.replace("Project", "").toIntOrNull()
                    nameTaken?.let { num -> if (num >= nameNum) { nameNum = num + 1 } }
                }
            }

            viewModel.projectInfo.value = viewModel.projectInfo.value.copy(name = "Project$nameNum")

            val mInfoData = Json.encodeToString(viewModel.projectInfo.value)

            filesManager.addProjectVersion(
                name = "Project$nameNum",
                version = projectVersion,
                data = listOf(
                    Pair(ProjectFileType.HexData, mHexData.encodeToByteArray()),
                    Pair(ProjectFileType.MapData, mMapDataSer.encodeToByteArray()),
                    Pair(ProjectFileType.ProjectInfo, mInfoData.encodeToByteArray()),
                    Pair(ProjectFileType.MapListData, mMapList.encodeToByteArray())
                )
            )
        } else {
            val mInfoData = Json.encodeToString(viewModel.projectInfo.value)

            filesManager.addProjectVersion(
                name = projectName,
                version = projectVersion,
                data = listOf(
                    Pair(ProjectFileType.HexData, mHexData.encodeToByteArray()),
                    Pair(ProjectFileType.MapData, mMapDataSer.encodeToByteArray()),
                    Pair(ProjectFileType.ProjectInfo, mInfoData.encodeToByteArray()),
                    Pair(ProjectFileType.MapListData, mMapList.encodeToByteArray())
                )
            )
        }
        //  Pair(ProjectFileType.A2lData, mA2lData.encodeToByteArray()
    }
}

fun saveNewVersion(
    viewModel: MainViewModel,
    filesManager: ProjectFilesManager,
    scope: CoroutineScope,
    projectName: String
) {
    scope.launch(Dispatchers.IO) {
        val mMapDataSer = Json.encodeToString(viewModel.mapData.value)
        val mHexData = Json.encodeToString(HexSegmentMap(viewModel.hexDataSegments))
        val mInfoData = Json.encodeToString(viewModel.projectInfo)
        val mMapList = Json.encodeToString(viewModel.mapListItems)
        //val mA2lData = Json.encodeToString(viewModel.a2lData.value)

        val mVersion = viewModel.projectInfo.value.projectVersion

        filesManager.addProjectVersion(
            name = projectName,
            version = mVersion,
            data = listOf(
                Pair(ProjectFileType.HexData, mHexData.encodeToByteArray()),
                Pair(ProjectFileType.MapData, mMapDataSer.encodeToByteArray()),
                Pair(ProjectFileType.ProjectInfo, mInfoData.encodeToByteArray()),
                Pair(ProjectFileType.MapListData, mMapList.encodeToByteArray())
            )
        )
        //  Pair(ProjectFileType.A2lData, mA2lData.encodeToByteArray()
    }
}