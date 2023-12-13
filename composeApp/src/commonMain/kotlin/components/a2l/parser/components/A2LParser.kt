package components.a2l.parser.components

import components.a2l.axis.AxisPts
import components.a2l.axis.AxisPtsBuilder
import components.a2l.characteristic.Characteristic
import components.a2l.characteristic.CharacteristicBuilder
import components.a2l.classes.data.A2lData
import components.a2l.compumethod.CompuMethod
import components.a2l.compumethod.CompuMethodBuilder
import components.a2l.computab.CompuTab
import components.a2l.computab.CompuTabBuilder
import components.a2l.compuvtab.CompuVTab
import components.a2l.compuvtab.CompuVTabBuilder
import components.a2l.compuvtabrange.CompuVTabRange
import components.a2l.compuvtabrange.CompuVTabRangeBuilder
import components.a2l.functions.A2LFunction
import components.a2l.functions.FunctionBuilder
import components.a2l.group.Group
import components.a2l.group.GroupBuilder
import components.a2l.header.Header
import components.a2l.header.HeaderBuilder
import components.a2l.measurement.Measurement
import components.a2l.measurement.MeasurementBuilder
import components.a2l.modcommon.ModCommon
import components.a2l.modcommon.ModCommonBuilder
import components.a2l.modpar.ModPar
import components.a2l.modpar.ModParBuilder
import components.a2l.recordlayout.RecordLayout
import components.a2l.recordlayout.RecordLayoutBuilder
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.getCharacteristicData
import components.hex.HexSegmentData
import components.hex.toAsciiString
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.maplisttab.classes.MapDataContainer
import screens.editor.components.MainViewModel
import components.helpers.alignChunksWithMaps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

// TODO : Finish module if data parsing
// TODO: Finish entire if data parsing
// TODO(Add A2l Version logic and methods for specific conditions)

// *****************
// TODO: This may need to be custom for iphone and android specifically

class A2LParser {
    private fun parse(
        input: List<String>
    ): A2lData {
        var mHeader = Header()
        var mModPar = ModPar()
        var mModCommon = ModCommon()
        val mCharacteristics = mutableMapOf<String, Characteristic>()
        val mCompuMethods = mutableMapOf<String, CompuMethod>()
        val mCompuTabs = mutableMapOf<String, CompuTab>()
        val mCompuVTabs = mutableMapOf<String, CompuVTab>()
        val mCompuVTabRanges = mutableMapOf<String, CompuVTabRange>()
        val mFunctions = mutableMapOf<String, A2LFunction>()
        val mGroups = mutableMapOf<String, Group>()
        val mMeasurements = mutableMapOf<String, Measurement>()
        val mAxisPoints = mutableMapOf<String, AxisPts>()
        val mRecordLayouts = mutableMapOf<String, RecordLayout>()
        val mEmptyCharacteristic = Characteristic()
        val mEmptyCompuMethod = CompuMethod()
        val mEmptyCompuTab = CompuTab()
        val mEmptyCompuVTab = CompuVTab()
        val mEmptyCompuVTabRange = CompuVTabRange()
        val mEmptyFunction = A2LFunction()
        val mEmptyGroup = Group()
        val mEmptyMeasurement = Measurement()
        val mEmptyAxisPts = AxisPts()
        val mEmptyRecordLayout = RecordLayout()
        var mVersion = Float.MIN_VALUE

        var line = input[0]
        var lineIndex = 1

        try {
            val testInput = """.*ASAP2_VERSION.*""".toRegex()
            val inputTestMatch = line.let { testInput.find(it) }

            // This is a check to mke sure were working with an actual Asap2 file
            if (inputTestMatch.toString().isBlank()) {
                println("ERROR: This is not a Recognized file!")
                println(line)
                return A2lData()
            }

            while (lineIndex < input.size) {
                when {
                    // Skip the empty lines
                    line.isBlank() -> {
                        line = input[lineIndex]
                        lineIndex++
                    }

                    // Parse and build Characteristics
                    line.startsWith("/begin CHARACTERISTIC") -> {
                        val characList = mutableListOf<String>()
                        characList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end CHARACTERISTIC")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                characList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = CharacteristicBuilder().build(characList)

                        if (result != mEmptyCharacteristic) {
                            mCharacteristics[result.name] = result
                        }
                    }

                    // Parse and build Axis points
                    line.startsWith("/begin AXIS_PTS") -> {
                        val axisPtsList = mutableListOf<String>()
                        axisPtsList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end AXIS_PTS")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                axisPtsList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = AxisPtsBuilder().build(axisPtsList)

                        if (result != mEmptyAxisPts) {
                            mAxisPoints[result.name] = result
                        }
                    }

                    // Parse and build Record Layouts
                    line.startsWith("/begin RECORD_LAYOUT") -> {
                        val recordLayoutList = mutableListOf<String>()
                        recordLayoutList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end RECORD_LAYOUT")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                recordLayoutList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = RecordLayoutBuilder().build(recordLayoutList)

                        if (result != mEmptyRecordLayout) {
                            mRecordLayouts[result.name] = result
                        }
                    }

                    // Parse and build Measurements
                    line.startsWith("/begin MEASUREMENT") -> {
                        val measurementList = mutableListOf<String>()
                        measurementList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end MEASUREMENT")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                measurementList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = MeasurementBuilder().build(measurementList)

                        if (result != mEmptyMeasurement) {
                            mMeasurements[result.name] = result
                        }
                    }

                    // Parse and build Compu Methods
                    line.startsWith("/begin COMPU_METHOD") -> {
                        val compuMethodList = mutableListOf<String>()
                        compuMethodList.add(line)
                        while (lineIndex < input.size && !line.startsWith("/end COMPU_METHOD")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                compuMethodList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = CompuMethodBuilder().build(compuMethodList)

                        if (result != mEmptyCompuMethod) {
                            mCompuMethods[result.name] = result
                        }
                    }

                    // Parse and build Compu V Tabs
                    line.startsWith("/begin COMPU_VTAB") -> {
                        val compuVTabList = mutableListOf<String>()
                        compuVTabList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end COMPU_VTAB")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                compuVTabList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = CompuVTabBuilder().build(compuVTabList)

                        if (result != mEmptyCompuVTab) {
                            mCompuVTabs[result.name] = result
                        }
                    }

                    // Parse and build Compu V Tab Ranges
                    line.startsWith("/begin COMPU_VTAB_RANGE") -> {
                        val compuVTabRangeList = mutableListOf<String>()
                        compuVTabRangeList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end COMPU_VTAB_RANGE")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                compuVTabRangeList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = CompuVTabRangeBuilder().build(compuVTabRangeList)

                        if (result != mEmptyCompuVTabRange) {
                            mCompuVTabRanges[result.name] = result
                        }
                    }

                    // Parse and build CompuTabs
                    line.startsWith("/begin COMPU_TAB") -> {
                        val compuTabList = mutableListOf<String>()
                        compuTabList.add(line)
                        while (lineIndex < input.size && !line.startsWith("/end COMPU_TAB")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                compuTabList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = CompuTabBuilder().build(compuTabList)

                        if (result != mEmptyCompuTab) {
                            mCompuTabs[result.name] = result
                        }
                    }

                    // Parse and build Group
                    line.startsWith("/begin GROUP") -> {
                        val groupList = mutableListOf<String>()
                        groupList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end GROUP")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                groupList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = GroupBuilder().build(groupList)

                        if (result != mEmptyGroup) {
                            mGroups[result.name] = result
                        }
                    }

                    // Parse and build Functions
                    line.startsWith("/begin FUNCTION") -> {
                        val functionList = mutableListOf<String>()
                        functionList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end FUNCTION")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                functionList.add(line.trim())
                            }
                            lineIndex++
                        }

                        val result = FunctionBuilder().build(functionList)

                        if (result != mEmptyFunction) {
                            mFunctions[result.name] = result
                        }
                    }

                    // Get asap2 Version
                    line.startsWith("ASAP2_VERSION") -> {
                        val result = line.replace("ASAP2_VERSION", "")
                            .trim().replace(" ", ".").toFloatOrNull()
                        if (result != null) {
                            mVersion = result
                        }

                        line = input[lineIndex]
                        lineIndex++
                    }

                    // Parse and build Header
                    line.startsWith("/begin HEADER") -> {
                        val h = mutableListOf<String>()
                        h.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end HEADER")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                h.add(line.trim())
                            }
                            lineIndex++
                        }

                        mHeader = HeaderBuilder().build(h)
                    }

                    // Parse and build Mod Par
                    line.startsWith("/begin MOD_PAR") -> {
                        val modParList = mutableListOf<String>()
                        modParList.add(line)
                        while (lineIndex < input.size && !line.startsWith("/end MOD_PAR")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                modParList.add(line.trim())
                            }
                            lineIndex++
                        }

                        mModPar = ModParBuilder().build(modParList)
                    }

                    // Parse and build Mod Common
                    line.startsWith("/begin MOD_COMMON") -> {
                        val modCommonList = mutableListOf<String>()
                        modCommonList.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end MOD_COMMON")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                modCommonList.add(line.trim())
                            }
                            lineIndex++
                        }

                        mModCommon = ModCommonBuilder().build(modCommonList)
                    }

                    // Parse and build IfData
                    line.startsWith("/begin IF_DATA") -> {
                        val ifDataFinds = mutableListOf<String>()
                        ifDataFinds.add(line)

                        while (lineIndex < input.size && !line.startsWith("/end IF_DATA")) {
                            line = input[lineIndex]
                            if (line.isNotBlank()) {
                                ifDataFinds.add(line.trim())
                            }
                            lineIndex++
                        }

                        // mIfDataList.add(ifDataFinds) TODO Figure this out or make specific parser
                    }

                    else -> {
                        line = input[lineIndex]
                        lineIndex++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return collection of Objects
        return A2lData(
            axisPoints = mAxisPoints,
            header = mHeader,
            modPar = mModPar,
            modCommon = mModCommon,
            characteristics = mCharacteristics,
            measurements = mMeasurements,
            recordLayouts = mRecordLayouts,
            compuMethods = mCompuMethods,
            compuTabs = mCompuTabs,
            compuVTabRanges = mCompuVTabRanges,
            compuVTabs = mCompuVTabs,
            functions = mFunctions,
            groups = mGroups
        )
    }

    fun parseInBackground(
        input: ByteArray,
        viewModel: MainViewModel,
        scope: CoroutineScope
    ) {
        val parser = A2LParser()
        val mHexData = viewModel.hexDataSegments
        var mSegmentId = ""

        // Parse the file in the background
        scope.launch(Dispatchers.IO) {
            // Set the loading to true
            viewModel.isLoading.value = true

            // Convert to a List of Strings to parse
            val parsed = parser.parse(input.toAsciiString().splitToSequence("\n").toList())

            if (parsed != A2lData()) {
                // Update the view model data
                viewModel.a2lData.value = parsed

                val mCharResult = mutableListOf<MapDataValues>()
                var mBaseOffset = ULong.MIN_VALUE

                // Update our map list view with our groups
                val mMapList = mutableListOf<MapDataContainer>()

                parsed.functions.forEach { (_, function) ->
                    val mResultChildren = mutableListOf<String>()
                    mResultChildren.addAll(function.getDefCharacteristics())

                    if (mResultChildren.isNotEmpty()) {
                        viewModel.mapListItems.add(
                            MapDataContainer(
                                title = function.name,
                                children = mResultChildren
                            )
                        )
                    }
                }

                // Format the results
                parsed.characteristics.forEach { (_, characteristic) ->

                    if (mBaseOffset == ULong.MIN_VALUE) {
                        val testData = getCharacteristicData(
                            parsed,
                            viewModel.hexDataSegments,
                            characteristic
                        )

                        mBaseOffset = testData.charValues.baseOffset

                        mCharResult.add(
                            testData
                        )
                    } else {
                        mCharResult.add(
                            getCharacteristicData(
                                parsed,
                                viewModel.hexDataSegments,
                                characteristic
                            )
                        )
                    }

                    /*

                    val mAxisDescs = characteristic.getAxisDescrMap()

                    val testData = getCharacteristicData(
                        parsed,
                        viewModel.hexDataSegments.value,
                        characteristic
                    )

                    // Test char data
                    if (testData.charValues.values.isEmpty()) {
                        println("Characteristic Data is empty!")
                        println(testData.name)
                        println(characteristic)
                        println(testData.mapType)
                        println(testData.charValues)
                        println(testData.axisValues)
                        println("---------------------------------")
                    }

                    // Test Axis data
                    if (testData.axisValues.size != mAxisDescs.size) {
                        println("Axis Values Blank!")
                        println(testData.name)
                        println(characteristic)
                        println(testData.mapType)
                        println(testData.charValues)
                        println(testData.axisValues)
                        println("---------------------------------")
                    } else if (mAxisDescs.isNotEmpty()) {
                        testData.axisValues.forEach {
                            if (it.values.isEmpty()) {
                                println("Axis Values Blank!")
                                println(testData.name)
                                println(characteristic)
                                println(testData.mapType)
                                println(testData.charValues)
                                println(testData.axisValues)
                                println("---------------------------------")
                            }
                        }
                    }
                     */
                }

                // Group maps into their segments
                mHexData.forEach { (key, _) ->
                    if (key.toULong() == 800000uL) { mSegmentId = key }
                }

                val mSegment = if (mHexData.containsKey(mSegmentId)) {
                    mHexData[mSegmentId] ?: HexSegmentData()
                } else HexSegmentData()

                val mFinalData = alignChunksWithMaps(
                    hexSegmentData = mSegment,
                    mapDataValues = mCharResult,
                    lineCount = viewModel.digitsPerLine.value,
                    minChunkSize = 4000
                )

                val mHexSegmentResult = mHexData.toMutableMap()

                mHexSegmentResult[mSegmentId] = HexSegmentData(
                    data = mFinalData.first,
                    baseOffset = mSegment.baseOffset,
                    startOffset = mSegment.startOffset,
                    endOffset = mSegment.endOffset
                )

                // Update Hex Segment with newly aligned value
                viewModel.hexDataSegments.clear()
                mHexSegmentResult.forEach { (id, data) ->
                    viewModel.hexDataSegments[id] = data
                }

                // Update the Sorted Map List in View Model
                viewModel.mapData.value = MapValuesSorted(
                    segmentId = mSegmentId,
                    mapsSortedByChunk = mFinalData.second
                )
            }

            // Set the loading to false
            viewModel.isLoading.value = false
        }
    }
}