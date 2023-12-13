package components.calibrationdata.classes

import androidx.compose.ui.graphics.Path
import components.parsers.a2l.classes.enums.DataType
import kotlinx.serialization.Serializable

@Serializable
data class AxisDataValues(
    val axisDataType: AxisDataType = AxisDataType.NOT_SET,
    val byteSegmentId: String = "",
    val startPosition: Int = Int.MIN_VALUE,
    val endPosition: Int = Int.MIN_VALUE,
    val values: MutableList<AxisDataValue> = mutableListOf(),
    val dataType: DataType = DataType.NOT_SET,
    val format: String = "",
    val upperLimit: Double = Double.NaN,
    val lowerLimit: Double = Double.NaN,
    val overlayPath: Path = Path(),
    val unit: String = ""
) {
    fun changeAxisValue(input: String, axis: AxisDataType, position: Int) {
        values.forEach { axisData ->
            if (axisData.axis == axis && axisData.position == position) {
                axisData.changeFormattedValue(input)
            }
        }
    }
}

/**
 * The result from the data from the ByteSegments for an Axis
 */

@Serializable
data class AxisDataValue(
    val originalValue: String = "",
    var formattedValue: String = "",
    val axis: AxisDataType = AxisDataType.NOT_SET,
    val position: Int = Int.MIN_VALUE
) {
    private val oldValues: MutableMap<Int, String> = mutableMapOf()
    private var numberOfChanges = 0

    // TODO: Add logic to confirm input is within limits
    fun changeFormattedValue(input: Any) {
        // Store old value
        oldValues[numberOfChanges] = formattedValue

        // Advance change counter
        numberOfChanges += 1

        // Update new value
        formattedValue = input.toString()

        // Undo formatting and update Hex String
    }

    fun getOldValue(selection: Int): Any {
        return if (oldValues.containsKey(selection)) {
            oldValues[selection] ?: Double.NaN
        } else Double.NaN
    }
}