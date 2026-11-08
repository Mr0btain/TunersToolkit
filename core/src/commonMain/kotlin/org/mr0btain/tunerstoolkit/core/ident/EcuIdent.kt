package org.mr0btain.tunerstoolkit.core.ident

import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.project.ProjectInfo

/** What an identification string is */
enum class IdentKind(val label: String) {
    ECU("ECU"),
    HARDWARE_NUMBER("Hardware number"),
    SOFTWARE_NUMBER("Software number"),
    PART_NUMBER("Part number"),
    CALIBRATION("Calibration ID"),
    VIN("VIN")
}

/**
 * One thing an image says about itself
 * @property address Where it was first found
 * @property count How many times it's in the image
 * @property maker Whose numbering it follows, when that's known
 * @property note What the pattern means, for showing
 * @property confidence 0 to 100: how sure the pattern is about what it found
 */
data class IdentFinding(
    val kind: IdentKind,
    val value: String,
    val address: Long,
    val count: Int,
    val maker: String?,
    val note: String,
    val confidence: Int
)

/** A pattern for one kind of ident string */
class IdentPattern(
    val kind: IdentKind,
    val regex: Regex,
    val maker: String?,
    val note: String,
    val confidence: Int,
    /** Further checks a match must pass */
    val accept: (String) -> Boolean = { true }
)

/**
 * Finds identification in an image: hardware and software numbers, part numbers, ECU family names, calibration IDs
 * and VINs, from the text it holds. The patterns are the makers' well known numbering schemes; [PATTERNS] lists them
 * and more can be added there. Like any text search it can be fooled, so findings come with a confidence and where
 * they are
 */
object EcuIdent {
    /** Strings shorter than this aren't worth reading */
    const val MIN_LENGTH = 6

    /** Longer runs are cut, so one huge block of text can't slow the scan */
    const val MAX_LENGTH = 256

    val PATTERNS: List<IdentPattern> = listOf(
        IdentPattern(
            IdentKind.HARDWARE_NUMBER, Regex("""(?<![0-9A-Z])02[68]1[0-9S][0-9]{5}(?![0-9A-Z])"""), "Bosch",
            "Bosch ECU hardware numbers start 0261 (petrol) or 0281 (diesel)", 85
        ),
        IdentPattern(
            IdentKind.SOFTWARE_NUMBER, Regex("""(?<![0-9A-Z])103[79][0-9S][0-9]{5}(?![0-9A-Z])"""), "Bosch",
            "Bosch software numbers start 1037 or 1039", 80
        ),
        IdentPattern(
            IdentKind.PART_NUMBER, Regex("""(?<![0-9A-Z])[0-9][0-9A-Z]{2}9(?:06|07|09|97)[0-9]{3}[A-Z]{0,3}(?![0-9A-Z])"""), "VAG",
            "VW group part numbers: engine and gearbox controllers are 906, 907, 909 or 997 in the middle", 70
        ) { value -> value.any { it.isLetter() } || value.length >= 9 },
        IdentPattern(
            IdentKind.HARDWARE_NUMBER, Regex("""(?<![0-9A-Z])5W[KPSY][0-9]{4,5}(?![0-9])"""), "Siemens/Continental",
            "Siemens VDO and Continental controller numbers start 5WK, 5WP, 5WS or 5WY", 70
        ),
        IdentPattern(
            IdentKind.PART_NUMBER, Regex("""(?<![0-9A-Z])A2C[0-9]{8,11}(?![0-9])"""), "Continental",
            "Continental part numbers start A2C", 70
        ),
        IdentPattern(
            IdentKind.PART_NUMBER, Regex("""(?<![0-9A-Z])89661-[0-9A-Z]{5}(?![0-9A-Z])"""), "Toyota",
            "Toyota engine controller part numbers are 89661-", 70
        ),
        IdentPattern(
            IdentKind.HARDWARE_NUMBER, Regex("""(?<![0-9A-Z])(?:MB)?27[57][0-9]{3}-[0-9]{4}(?![0-9])"""), "Denso",
            "Denso controller numbers are 275xxx-xxxx", 65
        ),
        IdentPattern(
            IdentKind.ECU, Regex("""(?<![0-9A-Z])(?:MEDC17|MED1[57]|MEVD1[67]|MED9|EDC1[5-7]|ME7|ME9|MG1|MD1|SIMOS ?[0-9]{1,2}|SID ?[0-9]{3}|PCR2\.1|IAW ?[0-9][A-Z0-9]{1,3})[A-Z0-9]{0,4}(?:[._][0-9A-Z]{1,3}){0,4}(?![0-9A-Z])"""), null,
            "An ECU family name", 60
        ),
        IdentPattern(
            IdentKind.VIN, Regex("""(?<![0-9A-Z])[A-HJ-NPR-Z0-9]{17}(?![0-9A-Z])"""), null,
            "Vehicle identification numbers are 17 letters and digits without I, O or Q", 40
        ) { value -> value.count { it.isLetter() } >= 3 && value.count { it.isDigit() } >= 3 }
    )

    /**
     * Calibration IDs and EPKs are text of several parts separated by slashes (40/1/MED17.5.2/…), which nothing else
     * looks much like
     */
    private val CALIBRATION = Regex("""[0-9A-Za-z][0-9A-Za-z._ -]{0,40}(?:/[0-9A-Za-z._ -]{1,40}){2,8}/?""")

    /** Everything identifying in [image], surest and most frequent first */
    fun scan(image: MemoryImage): List<IdentFinding> {
        val found = linkedMapOf<Pair<IdentKind, String>, IdentFinding>()
        fun add(finding: IdentFinding) {
            val key = finding.kind to finding.value
            val before = found[key]
            found[key] = before?.copy(count = before.count + 1) ?: finding
        }
        for ((address, text) in strings(image)) {
            for (pattern in PATTERNS) {
                for (match in pattern.regex.findAll(text)) {
                    val value = match.value
                    if (!pattern.accept(value)) continue
                    add(IdentFinding(pattern.kind, value, address + match.range.first, 1, pattern.maker, pattern.note, pattern.confidence))
                }
            }
            CALIBRATION.find(text)?.let { match ->
                val value = match.value.trim()
                val names = PATTERNS.first { it.kind == IdentKind.ECU }.regex
                val named = names.containsMatchIn(value)
                if (value.count { it.isLetterOrDigit() } >= 6) {
                    add(IdentFinding(IdentKind.CALIBRATION, value, address + match.range.first, 1, null,
                        "Text in parts separated by slashes, the way EPKs and calibration IDs are written", if (named) 85 else 55))
                }
            }
        }
        return found.values.sortedWith(compareByDescending<IdentFinding> { it.confidence }.thenByDescending { it.count }.thenBy { it.address })
    }

    /**
     * The printable runs of text in [image] (ASCII 0x20 to 0x7E, at least [MIN_LENGTH] long), with where each starts
     */
    fun strings(image: MemoryImage, minLength: Int = MIN_LENGTH): Sequence<Pair<Long, String>> = sequence {
        image.ranges.forEachIndexed { index, range ->
            val bytes = image.segmentBytes(index)
            var start = -1
            for (i in 0..bytes.size) {
                val printable = i < bytes.size && (bytes[i].toInt() and 0xFF) in 0x20..0x7E
                if (printable && start < 0) start = i
                if ((!printable || i - start >= MAX_LENGTH) && start >= 0) {
                    if (i - start >= minLength) yield(range.start + start to bytes.decodeLatin1(start, i))
                    start = if (printable) i else -1
                }
            }
        }
    }

    private fun ByteArray.decodeLatin1(from: Int, to: Int): String = buildString(to - from) { for (i in from until to) append((this@decodeLatin1[i].toInt() and 0xFF).toChar()) }

    /**
     * Project details from [findings]: the ECU (its family and hardware number) and the software number (or the part
     * number or calibration ID when there's no software number). Fields with nothing found stay as they were in [current]
     */
    fun suggest(findings: List<IdentFinding>, current: ProjectInfo = ProjectInfo()): ProjectInfo {
        fun best(vararg kinds: IdentKind) = kinds.firstNotNullOfOrNull { kind -> findings.firstOrNull { it.kind == kind && it.confidence >= 55 }?.value }
        val ecu = listOfNotNull(best(IdentKind.ECU), best(IdentKind.HARDWARE_NUMBER)).joinToString(" ")
        val software = best(IdentKind.SOFTWARE_NUMBER, IdentKind.PART_NUMBER, IdentKind.CALIBRATION).orEmpty()
        return current.copy(
            ecu = current.ecu.ifBlank { ecu },
            softwareNumber = current.softwareNumber.ifBlank { software }
        )
    }
}
