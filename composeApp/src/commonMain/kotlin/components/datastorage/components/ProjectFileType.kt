package components.datastorage.components

import components.parsers.a2l.classes.data.A2lData
import components.parsers.hex.HexSegmentMap
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.maplisttab.classes.MapDataContainer
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.serialization.json.Json

enum class ProjectFileType {
    MapData,
    MapListData,
    HexData,
    BinData,
    A2lData,
    ProjectInfo,
    NotFound
}

fun String.getFileType(): ProjectFileType {
    return when (this) {
        "MapData" -> ProjectFileType.MapData
        "HexData" -> ProjectFileType.HexData
        "BinData" -> ProjectFileType.BinData
        "A2lData" -> ProjectFileType.A2lData
        "ProjectInfo" -> ProjectFileType.ProjectInfo
        "MapListData" -> ProjectFileType.MapListData
        else -> ProjectFileType.NotFound
    }
}

fun ProjectFileType.deserialize(data: ByteArray): Any {
    return when (this) {
        ProjectFileType.MapData -> Json.decodeFromString<MapValuesSorted>(data.decodeToString())
        ProjectFileType.HexData -> Json.decodeFromString<HexSegmentMap>(data.decodeToString())
        ProjectFileType.A2lData -> Json.decodeFromString<A2lData>(data.decodeToString())
        ProjectFileType.ProjectInfo -> Json.decodeFromString<ProjectInfo>(data.decodeToString())
        ProjectFileType.MapListData -> Json.decodeFromString<List<MapDataContainer>>(data.decodeToString())
        else -> data
    }
}
