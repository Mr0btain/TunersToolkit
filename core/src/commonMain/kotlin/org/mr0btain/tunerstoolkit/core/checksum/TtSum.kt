package org.mr0btain.tunerstoolkit.core.checksum

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import org.mr0btain.tunerstoolkit.core.formats.Imported
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/**
 * Checksum modules as JSON (.ttsum), so a family's checksums can be added without code: copy a file into the app,
 * or send one in to be built in. docs/checksums.md describes every field.
 *
 * ```json
 * {
 *   "format": "ttsum", "formatVersion": 1,
 *   "id": "tt-demo-1", "name": "TT-DEMO-1", "family": "Tuners Toolkit demo",
 *   "status": "VERIFIED", "source": "Made for the demo",
 *   "detect": [ { "text": "TTDEMO1", "address": "0x800100" } ],
 *   "checks": [ {
 *     "name": "Calibration CRC",
 *     "algorithm": { "crc": "CRC-32/ISO-HDLC" },
 *     "ranges": [ "0x804000-0x807FFB" ],
 *     "store": { "address": "0x807FFC", "size": 4, "byteOrder": "LITTLE_ENDIAN", "form": "VALUE" }
 *   } ]
 * }
 * ```
 * Ranges are `first-last`, both included. An algorithm is `{"crc": "<catalogue name>"}`, a CRC by its parameters
 * (`width`, `poly`, `init`, `refIn`, `refOut`, `xorOut`), `{"sum": {...}}` with `word` (bytes), `width` (bits),
 * `byteOrder` and `init`, `{"xor": {...}}`, or `{"adler32": true}`
 */
object TtSum {
    const val FORMAT = "ttsum"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun looksLike(bytes: ByteArray): Boolean = bytes.copyOf(minOf(bytes.size, 512)).decodeToString().let { it.trimStart().startsWith("{") && "\"ttsum\"" in it }

    fun read(bytes: ByteArray, name: String = ""): TkResult<Imported<DefinedModule>, ImportError> {
        if (bytes.isEmpty()) return TkResult.Failure(ImportError.EMPTY_FILE)
        val file = try {
            json.decodeFromString(SumFile.serializer(), bytes.decodeToString())
        } catch (e: Exception) {
            return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        }
        if (file.format != FORMAT) return TkResult.Failure(ImportError.UNKNOWN_FORMAT)
        val problems = mutableListOf<Diagnostic>()
        if (file.formatVersion > VERSION) problems += Diagnostic(Severity.WARNING, "$name was made by a newer Tuners Toolkit; what this one doesn't know is left out")
        val checks = file.checks.mapNotNull { dto ->
            try {
                check(dto)
            } catch (e: Exception) {
                problems += Diagnostic(Severity.ERROR, "Check \"${dto.name}\" can't be read: ${e.message}, left out", objectName = dto.name)
                null
            }
        }
        val detectors = file.detect.mapNotNull { dto ->
            try {
                detector(dto)
            } catch (e: Exception) {
                problems += Diagnostic(Severity.ERROR, "A detector can't be read: ${e.message}, left out")
                null
            }
        }
        val status = ModuleStatus.entries.firstOrNull { it.name == file.status } ?: ModuleStatus.UNVERIFIED
        val module = DefinedModule(file.id, file.name, file.family, file.description, status, file.source, detectors, checks)
        return TkResult.Success(Imported(module, problems))
    }

    fun write(module: DefinedModule): String = json.encodeToString(
        SumFile.serializer(),
        SumFile(
            format = FORMAT,
            formatVersion = VERSION,
            id = module.id,
            name = module.name,
            family = module.family,
            description = module.description,
            status = module.status.name,
            source = module.source,
            detect = module.detectors.map(::detectorOf),
            checks = module.checkList.map(::checkOf)
        )
    )

    // ------------------------------------------------ The file

    @Serializable
    private class SumFile(
        val format: String,
        val formatVersion: Int,
        val id: String,
        val name: String,
        val family: String = "",
        val description: String = "",
        val status: String = "UNVERIFIED",
        val source: String = "",
        val detect: List<DetectDto> = emptyList(),
        val checks: List<CheckDto> = emptyList()
    )

    @Serializable
    private class DetectDto(val address: String? = null, val text: String? = null, val bytes: String? = null, val size: String? = null)

    @Serializable
    private class CheckDto(
        val name: String,
        val description: String = "",
        val algorithm: AlgorithmDto,
        val ranges: List<String>,
        val store: StoreDto
    )

    @Serializable
    private class AlgorithmDto(
        val crc: String? = null,
        val width: Int? = null,
        val poly: String? = null,
        val init: String? = null,
        val refIn: Boolean? = null,
        val refOut: Boolean? = null,
        val xorOut: String? = null,
        val sum: SumDto? = null,
        val xor: SumDto? = null,
        val adler32: Boolean? = null
    )

    @Serializable
    private class SumDto(val word: Int = 1, val width: Int = 16, val byteOrder: String = "BIG_ENDIAN", val init: String = "0x0")

    @Serializable
    private class StoreDto(val address: String, val size: Int, val byteOrder: String = "BIG_ENDIAN", val form: String = "VALUE", val target: String? = null)

    // ------------------------------------------------ Reading (throws on nonsense; caught per item)

    private fun detector(dto: DetectDto): Detector = when {
        dto.text != null -> Detector.Text(address(dto.address ?: error("a text needs an address")), dto.text.also { require(it.length <= 256) { "a text to look for is at most 256 characters" } })
        dto.bytes != null -> Detector.Bytes(address(dto.address ?: error("bytes need an address")), hex(dto.bytes).also { require(it.size <= 256) { "bytes to look for are at most 256" } })
        dto.size != null -> Detector.Size(number(dto.size).toLong())
        else -> error("it says nothing to look for")
    }

    private fun check(dto: CheckDto): ChecksumCheck {
        val ranges = dto.ranges.map(::range)
        require(ranges.isNotEmpty()) { "it has no ranges" }
        val store = ChecksumStore(
            address = address(dto.store.address),
            size = dto.store.size.also { require(it in 1..8) { "a stored checksum is 1 to 8 bytes" } },
            byteOrder = ByteOrder.valueOf(dto.store.byteOrder),
            form = StoreForm.valueOf(dto.store.form),
            target = dto.store.target?.let(::number) ?: 0u
        )
        return ChecksumCheck(dto.name, algorithm(dto.algorithm), ranges, store, dto.description)
    }

    private fun algorithm(dto: AlgorithmDto): ChecksumAlgorithm = when {
        dto.adler32 == true -> ChecksumAlgorithm.Adler32
        dto.sum != null -> ChecksumAlgorithm.Sum(dto.sum.word, dto.sum.width, ByteOrder.valueOf(dto.sum.byteOrder), number(dto.sum.init))
        dto.xor != null -> ChecksumAlgorithm.Xor(dto.xor.word, ByteOrder.valueOf(dto.xor.byteOrder), number(dto.xor.init))
        dto.crc != null && dto.poly == null -> ChecksumAlgorithm.crc(dto.crc) ?: error("there's no CRC called ${dto.crc}")
        dto.poly != null -> ChecksumAlgorithm.Crc(
            width = dto.width ?: error("a CRC needs its width"),
            poly = number(dto.poly),
            init = dto.init?.let(::number) ?: 0u,
            refIn = dto.refIn ?: false,
            refOut = dto.refOut ?: false,
            xorOut = dto.xorOut?.let(::number) ?: 0u,
            name = dto.crc
        )
        else -> error("it has no algorithm")
    }

    /** `first-last`, both included */
    internal fun range(text: String): AddressRange {
        val parts = text.split('-').map { it.trim() }
        require(parts.size == 2) { "\"$text\" isn't a range like 0x8000-0xFFFF" }
        val first = address(parts[0])
        val last = address(parts[1])
        require(last >= first) { "\"$text\" ends before it starts" }
        return AddressRange(first, last + 1)
    }

    /** An address: 0 to 0xFFFFFFFF, all a MemoryImage holds */
    private fun address(text: String): Long {
        val value = number(text)
        require(value <= MAX_ADDRESS) { "$text is past 0xFFFFFFFF" }
        return value.toLong()
    }

    private const val MAX_ADDRESS = 0xFFFFFFFFuL

    private fun number(text: String): ULong {
        val trimmed = text.trim()
        return if (trimmed.startsWith("0x", ignoreCase = true)) trimmed.substring(2).toULong(16) else trimmed.toULong()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun hex(text: String): ByteArray = text.replace(" ", "").hexToByteArray()

    // ------------------------------------------------ Writing

    private fun hexOf(value: Long) = "0x" + value.toString(16).uppercase()
    private fun hexOf(value: ULong) = "0x" + value.toString(16).uppercase()

    @OptIn(ExperimentalStdlibApi::class)
    private fun detectorOf(detector: Detector): DetectDto = when (detector) {
        is Detector.Text -> DetectDto(address = hexOf(detector.address), text = detector.text)
        is Detector.Bytes -> DetectDto(address = hexOf(detector.address), bytes = detector.bytes.toHexString())
        is Detector.Size -> DetectDto(size = hexOf(detector.size))
    }

    private fun checkOf(check: ChecksumCheck) = CheckDto(
        name = check.name,
        description = check.description,
        algorithm = algorithmOf(check.algorithm),
        ranges = check.ranges.map { "${hexOf(it.start)}-${hexOf(it.endExclusive - 1)}" },
        store = StoreDto(hexOf(check.store.address), check.store.size, check.store.byteOrder.name, check.store.form.name, if (check.store.form == StoreForm.FORCE) hexOf(check.store.target) else null)
    )

    private fun algorithmOf(algorithm: ChecksumAlgorithm): AlgorithmDto = when (algorithm) {
        is ChecksumAlgorithm.Sum -> AlgorithmDto(sum = SumDto(algorithm.word, algorithm.width, algorithm.byteOrder.name, hexOf(algorithm.init)))
        is ChecksumAlgorithm.Xor -> AlgorithmDto(xor = SumDto(algorithm.word, algorithm.width, algorithm.byteOrder.name, hexOf(algorithm.init)))
        ChecksumAlgorithm.Adler32 -> AlgorithmDto(adler32 = true)
        is ChecksumAlgorithm.Crc -> if (algorithm.name != null && ChecksumAlgorithm.crc(algorithm.name) == algorithm) AlgorithmDto(crc = algorithm.name)
        else AlgorithmDto(crc = algorithm.name, width = algorithm.width, poly = hexOf(algorithm.poly), init = hexOf(algorithm.init), refIn = algorithm.refIn, refOut = algorithm.refOut, xorOut = hexOf(algorithm.xorOut))
    }
}
