package components.parsers.a2l.ifdata.components.tpblob

data class TPBCCPVersion(val version: String): TpBlobItems
data class TPBBlobVersion(val version: String): TpBlobItems
data class TPBCANMsgIdSend(val id: String): TpBlobItems
data class TPBCANMsgIdReceive(val id: String): TpBlobItems
data class TPBStationAddress(val address: String): TpBlobItems
data class TPBByteOrder(val byteOrder: String): TpBlobItems
data class CANParam(
    val crystalFrequency: String,
    val timingRegister0: String,
    val timingRegister1: String
): TpBlobItems
data class Consistency(val consist: String): TpBlobItems
data class AddressExtension(val extension: String): TpBlobItems
data class ChecksumParam(
    val algorithm: String,
    val limit: String,
    val checksumCalculation: String,
    val rncResults: String
): TpBlobItems
data class DefinedPages(
    val pageNumber: String,
    val identifier: String,
    val addressExt: String,
    val baseAddress: String,
    val pageLength: String,
    val location: String
): TpBlobItems
data class OptionalCMD(
    val commands: Map<String, String>
): TpBlobItems
data class KLine(
    val simMode: String,
    val ecuAddress: String,
    val testerAddress: String
): TpBlobItems
data class TPCanBlock(
    val baudRate: String,
    val samplePointPercent: String,
    val samplesPerBit: String,
    val btlCycles: String,
    val sjwLength: String,
    val syncEdge: String,
    val addressBlock: TpCANAddressBlock?,
    val networkLimits: TpCANNetworkLimits?,
    val startStop: Int
): TpBlobItems

data class TpCANAddressBlock(
    val addrCanIdEcuToInca: String,
    val addrCanIdIncaToEcu: String
)
data class TpCANNetworkLimits(
    val maxWaitFrames: String,
    val maxDataLength: String
)

data class TpDataAccess(
    val ecuFlashAddressType: String,
    val ecuRamAddressType: String,
    val ecuFlashReadAccess: Int,
    val ecuFlashAccessCodeVerify: Int,
    val ecuFlashReadAccessCode: Int,
    val uploadDownloadOnlyOnActivePage: Int
): TpBlobItems

data class TpChecksumBlock(
    val typeQualifier: String,
    val calcOnlyOnActivePage: Int,
    val localRoutineNumber: Int,
    val resultOfRoutine: String?,
    val rncResult: String
): TpBlobItems

data class TpCopyBlock(
    val ecuModeForFlashCopy: String,
    val diagModeForCopyFlashToRam: Int,
    val copyParam: List<String>
): TpBlobItems

data class TpFlashBlock(
    val ecuModeForCopyRamToFlash: String,
    val ramToFlashRoutineNum: Int,
    val copyResultDelivery: String,
    val copyFrame: List<String>,
    val rncResult: List<String>
): TpBlobItems

data class TpDiagBaudBlock(
    val baudRateSwitch: String,
    val baudRate: String,
    val diagMode: String,
    val ecuBaudrateSwitchBytes: List<String>
): TpBlobItems

data class TpTimeDefBlock(
    val kwpTiming: Map<String, Int>,
    val usdtpTiming: Map<String, Int>
): TpBlobItems

data class TpSecurityAccess(
    val kwp2000AccessMode: Int,
    val seedkeyCalcMethod: Int,
    val seedDelayTime: Int
): TpBlobItems

data class TpPageSwitch(
    val eduPageSwitchMode: String,
    val escapeCodeSetParam: String,
    val escapeCodeGetParam: String,
    val routineParam: TpPSRoutineParam?,
    val pageCode: List<String>
): TpBlobItems

data class TpPSRoutineParam(
    val pageSwitchNum: Int,
    val routineSwitchResult: String,
    val rncResult: List<String>,
)