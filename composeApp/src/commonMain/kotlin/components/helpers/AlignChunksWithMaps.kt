package components.helpers

import components.calibrationdata.classes.MapDataValues
import components.hex.HexSegmentData

fun alignChunksWithMaps(
    hexSegmentData: HexSegmentData,
    mapDataValues: List<MapDataValues>,
    lineCount: Int,
    minChunkSize: Int
): Pair<Map<Int, String>, Map<Int, List<MapDataValues>>> {
    val mWholeHexData = hexSegmentData.data.values.joinToString("")
    val mHexChunkResults = mutableMapOf<Int, String>()
    var mPosition = 0
    val mMapsInChunk = mutableMapOf<Int, List<MapDataValues>>()

    // Until we hit the end of the Hex data, loop through and make chunks
    while (mPosition < mWholeHexData.length) {
        // Starting end position
        var mEnd = mPosition + minChunkSize

        // Check each map, if one starts in a chunk, make sure its end also falls inside chunk
        // else extend the end by one line until it fits to make sure there is no overlap
        mapDataValues.forEach { map ->
            // Check if start position lies inside start and end
            if (map.charValues.actualStartPosition in mPosition..mEnd) {
                // Check if end position is inside if it is, add the map
                if (map.charValues.actualEndPosition in mPosition..mEnd) {
                    if (mMapsInChunk.containsKey(mPosition)) {
                        mMapsInChunk[mPosition] = mMapsInChunk[mPosition]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - mPosition,
                                    map.charValues.actualEndPosition - mPosition
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mMapsInChunk[mPosition] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - mPosition,
                                    map.charValues.actualEndPosition - mPosition
                                )
                            )
                        )
                    }
                } else {
                    // else extend the End one line at a time until it fits
                    while (map.charValues.actualEndPosition > mEnd) {
                        mEnd += lineCount
                    }

                    if (mMapsInChunk.containsKey(mPosition)) {
                        mMapsInChunk[mPosition] = mMapsInChunk[mPosition]?.plus(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - mPosition,
                                    map.charValues.actualEndPosition - mPosition
                                )
                            )
                        ) ?: listOf()
                    } else {
                        mMapsInChunk[mPosition] = listOf(
                            map.copy(
                                charOffsetsInSeg = Pair(
                                    map.charValues.actualStartPosition - mPosition,
                                    map.charValues.actualEndPosition - mPosition
                                )
                            )
                        )
                    }
                }
            }
        }

        // Check we haven't exceeded the length of the data
        if (mEnd < mWholeHexData.length) {
            mHexChunkResults[mPosition] = mWholeHexData.substring(mPosition, mEnd)
            mPosition = mEnd
        } else {
            // else make last chunk with length
            mHexChunkResults[mPosition] = mWholeHexData.substring(mPosition, mWholeHexData.length)
            mPosition = mWholeHexData.length
        }
    }

    return Pair(mHexChunkResults, mMapsInChunk.mapValues { (_, list) ->
        list.sortedBy { it.charValues.actualStartPosition }
    })
}