package org.mr0btain.tunerstoolkit.components.calibrationdata.classes
import kotlinx.serialization.Serializable

/**
 * The result from the data from the ByteSegments
 */
@Serializable

data class FormattedValue(
    val hexData: String = "",
    var originalValue: String = "",
    var formattedValue: String = "",
    val column: Int = Int.MIN_VALUE,
    val row: Int = Int.MIN_VALUE
) {
    private val oldValues: MutableMap<Int, String> = mutableMapOf()
    private var numberOfChanges = 0

    // TODO: Add logic to confirm input is within limits
    fun changeValue(input: Any) {
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

