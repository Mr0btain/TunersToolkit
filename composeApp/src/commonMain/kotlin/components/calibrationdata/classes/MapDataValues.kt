package components.calibrationdata.classes

import androidx.compose.ui.graphics.Path
import components.a2l.classes.enums.CharacteristicType
import components.a2l.classes.enums.DataType
import kotlinx.serialization.Serializable

@Serializable
data class MapDataValues(
    val name: String = "",
    val mapType: CharacteristicType = CharacteristicType.NOT_SET,
    val charValues: CharDataValues = CharDataValues(),
    val axisValues: List<AxisDataValues> = listOf(),
    val charOffsetsInSeg: Pair<Int, Int> = Pair(0,0),
    val axisOffsetsInSeg: Pair<Int, Int> = Pair(0,0)
)

@Serializable
data class DataValues(
    val data: List<String> = listOf(),
    val dataOffsetInHex: Pair<Int, Int> = Pair(0,0),
    val dataOffsetInMemory: Pair<ULong, ULong> = Pair(0u,0u),
    val dataType: DataType = DataType.NOT_SET,
    val format: String = "",
    val formula: String = "",
    val overlayPath: Path = Path()
)

@Serializable
data class MapValues(
    val identifier: String = "",
    val dataValues: DataValues = DataValues(),
    val axisValues: List<DataValues> = listOf()
)

@Serializable
data class EditorData(
    val startOffsetInHex: Int,
    val hexString: String,
    val containedMaps: Map<MapType, MapValues>,
    val startOffsetInMemory: ULong = ULong.MIN_VALUE,
    val endOffsetInMemory: ULong = ULong.MIN_VALUE,
)

/**
 * A Segment of Hex data and its Start, End, and Base offset
 */

enum class MapType {
    Ascii,
    Value,
    Curve,
    Map2D,
    Cuboid,
    Cuboid4D,
    Cuboid5D
}