package org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.AxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.buildAxisPts
import org.mr0btain.tunerstoolkit.components.parsers.a2l.axis.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.Characteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.buildCharacteristic
import org.mr0btain.tunerstoolkit.components.parsers.a2l.characteristic.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.CompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.buildCompuMethod
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compumethod.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.computab.CompuTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.computab.buildCompuTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.computab.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtab.CompuVTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtab.buildCompuVTab
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtab.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtabrange.CompuVTabRange
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtabrange.buildCompuVTabRange
import org.mr0btain.tunerstoolkit.components.parsers.a2l.compuvtabrange.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.functions.A2LFunction
import org.mr0btain.tunerstoolkit.components.parsers.a2l.functions.buildA2lFunction
import org.mr0btain.tunerstoolkit.components.parsers.a2l.functions.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.group.Group
import org.mr0btain.tunerstoolkit.components.parsers.a2l.group.buildGroup
import org.mr0btain.tunerstoolkit.components.parsers.a2l.group.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.header.Header
import org.mr0btain.tunerstoolkit.components.parsers.a2l.header.buildHeader
import org.mr0btain.tunerstoolkit.components.parsers.a2l.measurement.Measurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.measurement.buildMeasurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.measurement.validate
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modcommon.ModCommon
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modcommon.buildModCommon
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modpar.ModPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.modpar.buildModPar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.RecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.buildRecordLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.recordlayout.validate

fun parseA2l(input: List<String>): Pair<A2lData, List<LogDataEntry>> {
    var header = Header()
    var modPar = ModPar()
    var modCommon = ModCommon()
    val characteristics = mutableMapOf<String, Characteristic>()
    val compuMethods = mutableMapOf<String, CompuMethod>()
    val compuTabs = mutableMapOf<String, CompuTab>()
    val compuVTabs = mutableMapOf<String, CompuVTab>()
    val compuVTabRanges = mutableMapOf<String, CompuVTabRange>()
    val functions = mutableMapOf<String, A2LFunction>()
    val groups = mutableMapOf<String, Group>()
    val measurements = mutableMapOf<String, Measurement>()
    val axisPoints = mutableMapOf<String, AxisPts>()
    val recordLayouts = mutableMapOf<String, RecordLayout>()
    var version = Float.MIN_VALUE
    val errors = mutableListOf<LogDataEntry>()

    val filteredInput = input.filter { it.isNotBlank() }.map { it.trim() }

    var line = filteredInput[0]
    var lineIndex = 1

    val testInput = """.*ASAP2_VERSION.*""".toRegex()
    val inputTestMatch = line.let { testInput.find(it) }

    // This is a check to make sure were working with an actual Asap2 file
    if (inputTestMatch.toString().isBlank()) {
        errors.add(
            LogDataEntry(
                process = "ParseA2lFile",
                message = "Not a valid A2l File!",
                cause = line,
                error = true
            )
        )

        return Pair(A2lData(), errors)
    }

    while (lineIndex < filteredInput.size) {
        if (line.startsWith("/begin")) {
            when {
                // Parse and build Characteristics
                line.startsWith("/begin CHARACTERISTIC") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end CHARACTERISTIC")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildCharacteristic(segment)
                    if (result.validate()) characteristics[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Axis points
                line.startsWith("/begin AXIS_PTS") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end AXIS_PTS")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildAxisPts(segment)
                    if (result.validate()) axisPoints[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Record Layouts
                line.startsWith("/begin RECORD_LAYOUT") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end RECORD_LAYOUT")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildRecordLayout(segment)
                    if (result.validate()) recordLayouts[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Measurements
                line.startsWith("/begin MEASUREMENT") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end MEASUREMENT")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildMeasurement(segment)
                    if (result.validate()) measurements[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Compu Methods
                line.startsWith("/begin COMPU_METHOD") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)
                    while (lineIndex < filteredInput.size && !line.startsWith("/end COMPU_METHOD")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildCompuMethod(segment)
                    if (result.validate()) compuMethods[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Compu V Tabs
                line.startsWith("/begin COMPU_VTAB") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end COMPU_VTAB")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildCompuVTab(segment)
                    if (result.validate()) compuVTabs[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Compu V Tab Ranges
                line.startsWith("/begin COMPU_VTAB_RANGE") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end COMPU_VTAB_RANGE")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildCompuVTabRange(segment)
                    if (result.validate()) compuVTabRanges[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build CompuTabs
                line.startsWith("/begin COMPU_TAB") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)
                    while (lineIndex < filteredInput.size && !line.startsWith("/end COMPU_TAB")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildCompuTab(segment)
                    if (result.validate()) compuTabs[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Group
                line.startsWith("/begin GROUP") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end GROUP")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildGroup(segment)
                    if (result.validate()) groups[result.name] = result
                    errors.addAll(error)
                }

                // Parse and build Functions
                line.startsWith("/begin FUNCTION") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end FUNCTION")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildA2lFunction(segment)
                    if (result.validate()) functions[result.name] = result
                    errors.addAll(error)
                }

                // Get asap2 Version
                line.startsWith("ASAP2_VERSION") -> {
                    val result = line.replace("ASAP2_VERSION", "")
                        .trim().replace(" ", ".").toFloatOrNull()
                    if (result != null) { version = result }
                    line = filteredInput[lineIndex].trim()
                    lineIndex++
                }

                // Parse and build Header
                line.startsWith("/begin HEADER") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end HEADER")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    header = buildHeader(segment)
                }

                // Parse and build Mod Par
                line.startsWith("/begin MOD_PAR") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)
                    while (lineIndex < filteredInput.size && !line.startsWith("/end MOD_PAR")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    val (result, error) = buildModPar(segment)
                    modPar = result
                    errors.addAll(error)
                }

                // Parse and build Mod Common
                line.startsWith("/begin MOD_COMMON") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end MOD_COMMON")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    modCommon = buildModCommon(segment)
                }

                // Parse and build IfData
                line.startsWith("/begin IF_DATA") -> {
                    val segment = mutableListOf<String>()
                    segment.add(line)

                    while (lineIndex < filteredInput.size && !line.startsWith("/end IF_DATA")) {
                        line = filteredInput[lineIndex]
                        segment.add(line)
                        lineIndex++
                    }

                    // mIfDataList.add(ifDataFinds) TODO Figure this out or make specific parser
                }

                else -> {
                    line = filteredInput[lineIndex]
                    lineIndex++
                }
            }
        } else {
            line = filteredInput[lineIndex]
            lineIndex++
        }
    }

    // Return collection of Objects
    return Pair(
        A2lData(
            axisPoints = axisPoints,
            header = header,
            modPar = modPar,
            modCommon = modCommon,
            characteristics = characteristics,
            measurements = measurements,
            recordLayouts = recordLayouts,
            compuMethods = compuMethods,
            compuTabs = compuTabs,
            compuVTabRanges = compuVTabRanges,
            compuVTabs = compuVTabs,
            functions = functions,
            groups = groups
        ),
        errors
    )
}