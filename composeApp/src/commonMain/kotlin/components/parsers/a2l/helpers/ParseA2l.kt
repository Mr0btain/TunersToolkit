package components.parsers.a2l.helpers

import components.parsers.a2l.axis.AxisPts
import components.parsers.a2l.axis.buildAxisPts
import components.parsers.a2l.axis.validate
import components.parsers.a2l.characteristic.Characteristic
import components.parsers.a2l.characteristic.buildCharacteristic
import components.parsers.a2l.characteristic.validate
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.compumethod.CompuMethod
import components.parsers.a2l.compumethod.buildCompuMethod
import components.parsers.a2l.compumethod.validate
import components.parsers.a2l.computab.CompuTab
import components.parsers.a2l.computab.buildCompuTab
import components.parsers.a2l.computab.validate
import components.parsers.a2l.compuvtab.CompuVTab
import components.parsers.a2l.compuvtab.buildCompuVTab
import components.parsers.a2l.compuvtab.validate
import components.parsers.a2l.compuvtabrange.CompuVTabRange
import components.parsers.a2l.compuvtabrange.buildCompuVTabRange
import components.parsers.a2l.compuvtabrange.validate
import components.parsers.a2l.functions.A2LFunction
import components.parsers.a2l.functions.buildA2lFunction
import components.parsers.a2l.functions.validate
import components.parsers.a2l.group.Group
import components.parsers.a2l.group.buildGroup
import components.parsers.a2l.group.validate
import components.parsers.a2l.header.Header
import components.parsers.a2l.header.buildHeader
import components.parsers.a2l.measurement.Measurement
import components.parsers.a2l.measurement.buildMeasurement
import components.parsers.a2l.measurement.validate
import components.parsers.a2l.modcommon.ModCommon
import components.parsers.a2l.modcommon.buildModCommon
import components.parsers.a2l.modpar.ModPar
import components.parsers.a2l.modpar.buildModPar
import components.parsers.a2l.recordlayout.RecordLayout
import components.parsers.a2l.recordlayout.buildRecordLayout
import components.parsers.a2l.recordlayout.validate

fun List<String>.parseA2l(): A2lData {
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
    var mVersion = Float.MIN_VALUE

    var line = this[0].trim()
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

        while (lineIndex < this.size) {
            when {
                // Skip the empty lines
                line.isBlank() -> {
                    line = this[lineIndex].trim()
                    lineIndex++
                }

                // Parse and build Characteristics
                line.startsWith("/begin CHARACTERISTIC") -> {
                    val characList = mutableListOf<String>()
                    characList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end CHARACTERISTIC")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) {
                            characList.add(line.trim())
                        }
                        lineIndex++
                    }

                    val result = buildCharacteristic(characList)

                    if (result.validate()) {
                        mCharacteristics[result.name] = result
                    }
                }

                // Parse and build Axis points
                line.startsWith("/begin AXIS_PTS") -> {
                    val axisPtsList = mutableListOf<String>()
                    axisPtsList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end AXIS_PTS")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { axisPtsList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildAxisPts(axisPtsList)
                    if (result.validate()) {
                        mAxisPoints[result.name] = result
                    }
                }

                // Parse and build Record Layouts
                line.startsWith("/begin RECORD_LAYOUT") -> {
                    val recordLayoutList = mutableListOf<String>()
                    recordLayoutList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end RECORD_LAYOUT")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { recordLayoutList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildRecordLayout(recordLayoutList)
                    if (result.validate()) {
                        mRecordLayouts[result.name] = result
                    }
                }

                // Parse and build Measurements
                line.startsWith("/begin MEASUREMENT") -> {
                    val measurementList = mutableListOf<String>()
                    measurementList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end MEASUREMENT")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { measurementList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildMeasurement(measurementList)
                    if (result.validate()) {
                        mMeasurements[result.name] = result
                    }
                }

                // Parse and build Compu Methods
                line.startsWith("/begin COMPU_METHOD") -> {
                    val compuMethodList = mutableListOf<String>()
                    compuMethodList.add(line)
                    while (lineIndex < this.size && !line.startsWith("/end COMPU_METHOD")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { compuMethodList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildCompuMethod(compuMethodList)
                    if (result.validate()) {
                        mCompuMethods[result.name] = result
                    }
                }

                // Parse and build Compu V Tabs
                line.startsWith("/begin COMPU_VTAB") -> {
                    val compuVTabList = mutableListOf<String>()
                    compuVTabList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end COMPU_VTAB")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { compuVTabList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildCompuVTab(compuVTabList)
                    if (result.validate()) {
                        mCompuVTabs[result.name] = result
                    }
                }

                // Parse and build Compu V Tab Ranges
                line.startsWith("/begin COMPU_VTAB_RANGE") -> {
                    val compuVTabRangeList = mutableListOf<String>()
                    compuVTabRangeList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end COMPU_VTAB_RANGE")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { compuVTabRangeList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildCompuVTabRange(compuVTabRangeList)
                    if (result.validate()) {
                        mCompuVTabRanges[result.name] = result
                    }
                }

                // Parse and build CompuTabs
                line.startsWith("/begin COMPU_TAB") -> {
                    val compuTabList = mutableListOf<String>()
                    compuTabList.add(line)
                    while (lineIndex < this.size && !line.startsWith("/end COMPU_TAB")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { compuTabList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildCompuTab(compuTabList)
                    if (result.validate()) {
                        mCompuTabs[result.name] = result
                    }
                }

                // Parse and build Group
                line.startsWith("/begin GROUP") -> {
                    val groupList = mutableListOf<String>()
                    groupList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end GROUP")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { groupList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildGroup(groupList)
                    if (result.validate()) {
                        mGroups[result.name] = result
                    }
                }

                // Parse and build Functions
                line.startsWith("/begin FUNCTION") -> {
                    val functionList = mutableListOf<String>()
                    functionList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end FUNCTION")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { functionList.add(line.trim()) }
                        lineIndex++
                    }

                    val result = buildA2lFunction(functionList)
                    if (result.validate()) {
                        mFunctions[result.name] = result
                    }
                }

                // Get asap2 Version
                line.startsWith("ASAP2_VERSION") -> {
                    val result = line.replace("ASAP2_VERSION", "")
                        .trim().replace(" ", ".").toFloatOrNull()
                    if (result != null) { mVersion = result }
                    line = this[lineIndex].trim()
                    lineIndex++
                }

                // Parse and build Header
                line.startsWith("/begin HEADER") -> {
                    val h = mutableListOf<String>()
                    h.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end HEADER")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { h.add(line.trim()) }
                        lineIndex++
                    }

                    mHeader = buildHeader(h)
                }

                // Parse and build Mod Par
                line.startsWith("/begin MOD_PAR") -> {
                    val modParList = mutableListOf<String>()
                    modParList.add(line)
                    while (lineIndex < this.size && !line.startsWith("/end MOD_PAR")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { modParList.add(line.trim()) }
                        lineIndex++
                    }

                    mModPar = buildModPar(modParList)
                }

                // Parse and build Mod Common
                line.startsWith("/begin MOD_COMMON") -> {
                    val modCommonList = mutableListOf<String>()
                    modCommonList.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end MOD_COMMON")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { modCommonList.add(line.trim()) }
                        lineIndex++
                    }

                    mModCommon = buildModCommon(modCommonList)
                }

                // Parse and build IfData
                line.startsWith("/begin IF_DATA") -> {
                    val ifDataFinds = mutableListOf<String>()
                    ifDataFinds.add(line)

                    while (lineIndex < this.size && !line.startsWith("/end IF_DATA")) {
                        line = this[lineIndex].trim()
                        if (line.isNotBlank()) { ifDataFinds.add(line.trim()) }
                        lineIndex++
                    }

                    // mIfDataList.add(ifDataFinds) TODO Figure this out or make specific parser
                }

                else -> {
                    line = this[lineIndex].trim()
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