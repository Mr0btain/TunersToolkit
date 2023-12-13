package components.a2l.ifdata.components

import components.a2l.objects.A2lBlob

data class QpA2lBlob(
    val daqListNumber: Int,  /* Number of the DAQ-List 0..n*/
    val length: Int,  // Length of the DAQ-Liste, maximum number of the useable ODTs
    val canIdVariable: Boolean,  /* CAN-Message-ID is variable                */
    val canIdFixed: String,  // CAN-Message-ID of the DTOs is fixed Default DTO Bit31 = 1: extended Identifier Bit31 = 0: standard Identifier
    val raster: Int,   // points to which raster in the If_Data that it uses
    val exclusive: Int,  // Exclusion of other DAQ-Lists
    val reductionAllowed: Boolean,  // Data reduction possible
    val firstPid: Int  // First Packet ID (PID) of the DAQ List
): A2lBlob() {

    /*
    /begin QP_BLOB
    1
    LENGTH 5
    CAN_ID_FIXED 0x7C6
    RASTER 1
    /end QP_BLOB

     */
}

fun parseQpBlob(input: List<String>): QpA2lBlob {
    val daqListNumber = input[1].trim().toInt() /* Number of the DAQ-List 0..n*/
    var length = 0  // Length of the DAQ-Liste, maximum number of the useable ODTs
    var canIdVariable = false  /* CAN-Message-ID is variable                */
    var canIdFixed = ""  // CAN-Message-ID of the DTOs is fixed Default DTO Bit31 = 1: extended Identifier Bit31 = 0: standard Identifier
    var raster = 0  // points to which raster in the If_Data that it uses
    var exclusive = 0  // Exclusion of other DAQ-Lists
    var reductionAllowed = false  // Data reduction possible
    var firstPid = 0 // First Packet ID (PID) of the DAQ List

    input.forEach { line ->
        when {
            line.startsWith("LENGTH") -> {
                length = line.replace("LENGTH", "").trim().toInt()
            }
            line.startsWith("CAN_ID_FIXED") -> {
                canIdFixed = line.replace("CAN_ID_FIXED", "").trim()
            }
            line.startsWith("RASTER") -> {
                raster = line.replace("RASTER", "").trim().toInt()
            }
            line.startsWith("REDUCTION_ALLOWED") -> reductionAllowed = true
            line.startsWith("CAN_ID_VARIABLE") -> canIdVariable = true
        }
    }

    return QpA2lBlob(
        daqListNumber,
        length,
        canIdVariable,
        canIdFixed,
        raster,
        exclusive,
        reductionAllowed,
        firstPid
    )
}