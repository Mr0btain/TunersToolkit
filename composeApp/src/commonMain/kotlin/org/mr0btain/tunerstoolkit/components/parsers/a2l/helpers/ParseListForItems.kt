package org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers
/**
 * Parses a list of strings and returns the items inside startingId and endId
 * @param input The input list to parse
 * @param startingId The start string that should be removed in case the first item is
 * on the same line
 * @param endId Same as startingId except this marks where to stop in the list
 */
fun parseItemsFromList(
    input: List<String>,
    startingId: String,
    endId: String
): List<String> {
    return input.takeWhile { !it.contains(endId) }.joinToString(" ").trim()
        .replace(startingId, "")
        .replace(endId, "")
        .split(" ")
        .filter { it.isNotBlank() }
}