package components.datastorage

import kotlinx.serialization.Serializable

/**
 * Data holder to hold all the changes and current differences made to the original file
 * @param diff A Map of current differences from the original file where Map<Position, Change>
 * @param changes A list of changes made saved, basically like an undo back stack
 */
@Serializable
data class ProjectChanges(
    val diff: Map<Int, String>,
    val changes: Map<Int, DataChange>
)

@Serializable
data class DataChange(
    val segmentId: Int,
    val startPosition: Int,
    val newHexData: String
)