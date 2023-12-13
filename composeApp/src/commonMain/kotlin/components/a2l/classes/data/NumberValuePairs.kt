package components.a2l.classes.data

import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * NumberValuePairs number of successive value pairs for this conversion
 * table.
 * @param total The total number of value pairs
 * @param pairs List of the pairs themselves, first being their id, second being the actual value
 */

@Serializable
data class NumberValuePairs(
    val total: Int = Int.MIN_VALUE,
    val pairs: Map<Long, Float> = mapOf()
): A2lOptional