package components.helpers

import components.calibrationdata.classes.MapDataValues
import components.parsers.hex.HexSegmentData

fun alignChunksWithMaps(
    hexSegmentData: HexSegmentData,
    mapDataValues: List<MapDataValues>,
    lineCount: Int,
    minChunkSize: Int
): Pair<Map<Int, String>, Map<Int, List<MapDataValues>>> {
    val wholeHexData = hexSegmentData.data.values.joinToString("")
    val hexChunkResults = mutableMapOf<Int, String>()
    var position = 0
    val mapsInChunk = mutableMapOf<Int, List<MapDataValues>>()

    // Until we hit the end of the Hex data, loop through and make chunks
    while (position < wholeHexData.length) {
        // Starting end position
        var end = position + minChunkSize

        // Check each map, if one starts in a chunk, make sure its end also falls inside chunk
        // else extend the end by one line until it fits to make sure there is no overlap
        mapDataValues.forEach { map ->
            // Check if start position lies inside start and end
            if (map.charValues.actualStartPosition in position..end) {
                // Check if end position is inside if it is, add the map
                if (map.charValues.actualEndPosition in position..end) {
                    if (mapsInChunk.containsKey(position)) {
                        mapsInChunk[position] = mapsInChunk[position]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - position,
                                    map.charValues.actualEndPosition - position
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mapsInChunk[position] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - position,
                                    map.charValues.actualEndPosition - position
                                )
                            )
                        )
                    }
                } else {
                    // else extend the End one line at a time until it fits
                    while (map.charValues.actualEndPosition > end) {
                        end += (lineCount * 2)
                    }

                    if (mapsInChunk.containsKey(position)) {
                        mapsInChunk[position] = mapsInChunk[position]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - position,
                                    map.charValues.actualEndPosition - position
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mapsInChunk[position] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - position,
                                    map.charValues.actualEndPosition - position
                                )
                            )
                        )
                    }
                }
            }
        }

        // Check we haven't exceeded the length of the data
        if (end < wholeHexData.length) {
            hexChunkResults[position] = wholeHexData.substring(position, end)
            position = end
        } else {
            // else make last chunk with length
            hexChunkResults[position] = wholeHexData.substring(position, wholeHexData.length)
            position = wholeHexData.length
        }
    }

    return Pair(hexChunkResults, mapsInChunk.mapValues { (_, list) ->
        list.sortedBy { it.charValues.actualStartPosition }
    })
}