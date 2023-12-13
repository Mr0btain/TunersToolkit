package components.a2l.parser

import components.a2l.classes.data.A2lData
import components.a2l.classes.enums.MemorySegmentProgramType
import components.a2l.parser.components.parseA2l
import components.a2l.parser.components.parseFunctionsToMapList
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.getCharacteristicData
import components.helpers.alignChunksWithMaps
import components.hex.HexSegmentData
import components.hex.toAsciiString
import components.maptextoverlay.classes.MapValuesSorted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import screens.editor.components.MainViewModel

fun parseA2lFile(
    input: ByteArray,
    viewModel: MainViewModel
) {
    val mHexData = viewModel.hexDataSegments
    var mSegmentId = ""
    var mBaseOffset = 0UL
    var mStartOffset = 0UL
    var mEndOffset = 0UL
    val parsed = input.toAsciiString().split("\n").parseA2l()

    if (parsed != A2lData()) {
        val mCharResult = mutableListOf<MapDataValues>()

        // Update the list of maps for our MapListTab
        viewModel.mapListItems.addAll(parseFunctionsToMapList(parsed.functions))

        // Format the results
        parsed.characteristics.forEach { (_, characteristic) ->
            mCharResult.add(
                getCharacteristicData(
                    parsed,
                    viewModel.hexDataSegments,
                    characteristic
                )
            )
        }

        parsed.modPar.memorySegments.forEach { memSeg ->
            if (memSeg.value.prgType == MemorySegmentProgramType.CODE) {
                mHexData.forEach { (key, data) ->
                    if (data.startOffset == memSeg.value.address) {
                        mSegmentId = key
                        mBaseOffset = data.baseOffset
                        mStartOffset = data.startOffset
                        mEndOffset = data.endOffset
                    }
                }
            }
        }

        val mFinalData = mHexData[mSegmentId]?.let {
            alignChunksWithMaps(
                hexSegmentData = it,
                mapDataValues = mCharResult,
                lineCount = viewModel.digitsPerLine.value,
                minChunkSize = 4000
            )
        }

        if (mFinalData != null) {
            viewModel.hexDataSegments[mSegmentId] = HexSegmentData(
                data = mFinalData.first,
                baseOffset = mBaseOffset,
                startOffset = mStartOffset,
                endOffset = mEndOffset
            )

            // Update the Sorted Map List in View Model
            viewModel.mapData.value = MapValuesSorted(
                segmentId = mSegmentId,
                mapsSortedByChunk = mFinalData.second
            )
        }
    }
}