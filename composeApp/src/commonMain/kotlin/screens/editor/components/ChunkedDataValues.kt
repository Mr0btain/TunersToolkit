package screens.editor.components

import components.calibrationdata.classes.MapDataValues
import components.datastorage.components.chunkByteArray
import components.parsers.hex.ByteSegmentData

data class ChunkedDataValues(
    val data: List<ByteChunk> = listOf()
)

data class ByteChunk(
    val startingAddress: Int,
    val data: ByteArray,
    val maps: List<MapDataValues>
)

fun ByteSegmentData.chunkDataValue(): ChunkedDataValues {
    val mResult = mutableListOf<ByteChunk>()
    val mByteChunked = this.data.chunkByteArray(6000, this.baseOffset.toInt())

    mByteChunked.forEach { (addr, data) ->
        mResult.add(
            ByteChunk(
                startingAddress = addr,
                data = data,
                maps = listOf()
            )
        )
    }

    return ChunkedDataValues(mResult)
}