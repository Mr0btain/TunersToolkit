package components.errorstuff

enum class Process {
    AlignChunksWithMaps,
    BuildAxisDescr,
    BuildAxisPts,
    BuildCharacteristic,
    BuildCompuMethod,
    BuildCompuTab,
    BuildCompuVTab,
    BuildCompuVTabRange,
    BuildFunction,
    BuildGroup,
    BuildHeader,
    BuildIfData,
    BuildMeasurement,
    BuildMemSeg,
    BuildModCommon,
    BuildModPar,
    BuildRecordLayout,
    GetCharacteristicData,
    ParseIntelHex,
    ProcessHex,
    NONE
}

fun decodeProcess(input: String): Process {
    Process.entries.forEach {  process ->
        if (process.name == input) return process
    }
    return Process.NONE
}