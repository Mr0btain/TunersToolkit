package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
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

fun parseNumberValuePairs(input: List<String>, name: String): NumberValuePairs {
    if (name.isNotBlank()) {
        val total = input[3].toInt()
        val valuePairs = mutableMapOf<Long, Float>()

        // TODO: Needs changed up!

        /*
        7 /* number_value_pairs */
        1 4.3 THEN 2 4.7 THEN 3 5.8 4 14.2 5 16.8  <-- numbers state position!
        6 17.2 7 19.4 /* value pairs */
         */

        input.subList(3, input.size).forEach {
            val item = it.trim().replace("\"", "").split(" ")
            valuePairs[item[0].toLong()] = item[1].toFloat()
        }

        return NumberValuePairs(
            total = total,
            pairs = valuePairs
        )
    } else {
        val total = input[4].toInt()
        val valuePairs = mutableMapOf<Long, Float>()

        input.subList(4, input.size).forEach {
            val item = it.trim().replace("\"", "").split(" ")
            valuePairs[item[0].toLong()] = item[1].toFloat()
        }

        return NumberValuePairs(
            total = total,
            pairs = valuePairs
        )
    }
}