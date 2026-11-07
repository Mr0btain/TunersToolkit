package org.mr0btain.tunerstoolkit.components.helpers
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData

fun alignChunksWithMaps(
    hexSegmentData: HexSegmentData,
    mapDataValues: List<MapDataValues>,
    lineCount: Int,
    minChunkSize: Int
): Pair<Map<Int, String>, Map<Int, List<MapDataValues>>> {
    val wholeHexData = hexSegmentData.data.values.joinToString("")
    val hexChunkResults = mutableMapOf<Int, String>()
    var currentPosition = 0
    val mapsInChunk = mutableMapOf<Int, List<MapDataValues>>()

    // Until we hit the end of the Hex data, loop through and make chunks
    while (currentPosition < wholeHexData.length) {
        // Starting end position
        var end = currentPosition + minChunkSize

        // Check each map, if one starts in a chunk, make sure its end also falls inside chunk
        // else extend the end by one line until it fits to make sure there is no overlap
        mapDataValues.forEach { map ->
            // Check if start position lies inside start and end
            if (map.charValues.actualStartPosition in currentPosition..end) {
                // Check if end position is inside if it is, add the map
                if (map.charValues.actualEndPosition in currentPosition..end) {
                    if (mapsInChunk.containsKey(currentPosition)) {
                        mapsInChunk[currentPosition] = mapsInChunk[currentPosition]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - currentPosition,
                                    map.charValues.actualEndPosition - currentPosition
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mapsInChunk[currentPosition] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - currentPosition,
                                    map.charValues.actualEndPosition - currentPosition
                                )
                            )
                        )
                    }
                } else {
                    // else extend the End one line at a time until it fits
                    while (map.charValues.actualEndPosition > end) {
                        end += (lineCount * 2)
                    }

                    if (mapsInChunk.containsKey(currentPosition)) {
                        mapsInChunk[currentPosition] = mapsInChunk[currentPosition]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - currentPosition,
                                    map.charValues.actualEndPosition - currentPosition
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mapsInChunk[currentPosition] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - currentPosition,
                                    map.charValues.actualEndPosition - currentPosition
                                )
                            )
                        )
                    }
                }
            }
        }

        // Check we haven't exceeded the length of the data
        if (end < wholeHexData.length) {
            hexChunkResults[currentPosition] = wholeHexData.substring(currentPosition, end)
            currentPosition = end
        } else {
            // else make last chunk with length
            hexChunkResults[currentPosition] = wholeHexData.substring(currentPosition, wholeHexData.length)
            currentPosition = wholeHexData.length
        }
    }

    return Pair(hexChunkResults, mapsInChunk.mapValues { (_, list) ->
        list.sortedBy { it.charValues.actualStartPosition }
    })
}