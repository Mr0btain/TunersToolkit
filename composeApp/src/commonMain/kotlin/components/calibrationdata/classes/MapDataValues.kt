package components.calibrationdata.classes

import components.parsers.a2l.classes.enums.CharacteristicType
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