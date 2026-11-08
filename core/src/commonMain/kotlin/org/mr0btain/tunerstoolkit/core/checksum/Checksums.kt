package org.mr0btain.tunerstoolkit.core.checksum

import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.toHexAddress

/** How a check's result is kept in the image */
enum class StoreForm(val label: String) {
    /** The result itself */
    VALUE("The checksum"),

    /** Its bits flipped */
    COMPLEMENT("Its complement"),

    /** The result, followed straight after by its complement */
    VALUE_AND_COMPLEMENT("Checksum then complement"),

    /**
     * No stored result: a correction word inside the ranges is chosen so the result comes out as the target (as
     * Bosch MED17 blocks do). Sums and XORs need the word to be one of theirs; CRCs are solved bit by bit
     */
    FORCE("Forced to a target")
}

/**
 * Where a check's result lives, or for [StoreForm.FORCE] the correction word and the [target]
 */
data class ChecksumStore(
    val address: Long,
    val size: Int,
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    val form: StoreForm = StoreForm.VALUE,
    val target: ULong = 0u
) {
    /** The bytes it occupies */
    val ranges: List<AddressRange>
        get() = if (form == StoreForm.VALUE_AND_COMPLEMENT) listOf(AddressRange.ofSize(address, size * 2L)) else listOf(AddressRange.ofSize(address, size.toLong()))
}

/** One checksum: an algorithm over some ranges of the image, and where its result goes */
data class ChecksumCheck(
    val name: String,
    val algorithm: ChecksumAlgorithm,
    val ranges: List<AddressRange>,
    val store: ChecksumStore,
    val description: String = ""
)

/** How sure a module is that an image is one of its family's */
data class Detection(val confidence: Int, val reason: String)

/** Where a module's checks come from, which says how far to trust them */
enum class ModuleStatus(val label: String) {
    /** Checked against real binaries of the family */
    VERIFIED("Verified"),

    /** Written from a description and not yet checked against real binaries: fixing with it could stop an ECU starting */
    UNVERIFIED("Not verified"),

    /** Made by the person using the app */
    USER("Your own")
}

/** Why a module couldn't give its checks for an image */
class ChecksumError(override val message: String) : Error

/**
 * The checksums of one ECU family (or one person's own set). A module says whether an image looks like its family's
 * and gives the checks for it; most are data ([DefinedModule], written as .ttsum files), but one that has to read the
 * image to find its blocks (descriptor tables, say) can be written in code against this interface
 */
interface ChecksumModule {
    val id: String
    val name: String
    val family: String
    val description: String
    val status: ModuleStatus

    /** Where its algorithm comes from: a URL or a note */
    val source: String

    /** Whether [image] looks like its family's; null when it doesn't */
    fun detect(image: MemoryImage): Detection?

    /** The checks for [image] */
    fun checks(image: MemoryImage): TkResult<List<ChecksumCheck>, ChecksumError>
}

/** Recognises a family's binaries */
sealed interface Detector {
    fun matches(image: MemoryImage): Boolean
    val label: String

    /** [text] at [address] */
    data class Text(val address: Long, val text: String) : Detector {
        override fun matches(image: MemoryImage): Boolean = image.readOrNull(address, text.length)?.decodeToString() == text
        override val label: String get() = "\"$text\" at ${address.toHexAddress()}"
    }

    /** These bytes at [address] */
    data class Bytes(val address: Long, val bytes: ByteArray) : Detector {
        override fun matches(image: MemoryImage): Boolean = image.readOrNull(address, bytes.size)?.contentEquals(bytes) == true
        override val label: String get() = "${bytes.size} bytes at ${address.toHexAddress()}"
        override fun equals(other: Any?) = other is Bytes && other.address == address && other.bytes.contentEquals(bytes)
        override fun hashCode() = address.hashCode() * 31 + bytes.contentHashCode()
    }

    /** The image holds exactly [size] bytes (a flash size) */
    data class Size(val size: Long) : Detector {
        override fun matches(image: MemoryImage): Boolean = image.size == size
        override val label: String get() = "$size bytes"
    }
}

/** A module as data: detectors (all must match) and fixed checks */
data class DefinedModule(
    override val id: String,
    override val name: String,
    override val family: String,
    override val description: String,
    override val status: ModuleStatus,
    override val source: String,
    val detectors: List<Detector>,
    val checkList: List<ChecksumCheck>
) : ChecksumModule {
    override fun detect(image: MemoryImage): Detection? {
        if (detectors.isEmpty()) return null
        if (!detectors.all { it.matches(image) }) return null
        return Detection(100, detectors.joinToString(", ") { it.label })
    }

    override fun checks(image: MemoryImage): TkResult<List<ChecksumCheck>, ChecksumError> = TkResult.Success(checkList)
}

/**
 * What a check found
 * @property computed The checksum the bytes give now (for FORCE, the result over the ranges)
 * @property stored What the image holds for it (for FORCE, the target)
 * @property problem Why it couldn't be worked out, when it couldn't
 */
class CheckResult(val check: ChecksumCheck, val computed: ULong?, val stored: ULong?, val problem: String? = null, private val matches: Boolean = computed == stored) {
    val ok: Boolean get() = problem == null && matches
}

/** What fixing did */
class FixReport(val fixed: Int, val results: List<CheckResult>, val passes: Int) {
    val allOk: Boolean get() = results.all { it.ok }
}

/** Working checksums out, checking them and fixing them */
object Checksums {
    /** Most bytes one check reads, so a broken module can't ask for gigabytes */
    const val MAX_BYTES = 64L * 1024 * 1024

    /** Most ranges one check has */
    const val MAX_RANGES = 10_000

    fun verify(image: MemoryImage, checks: List<ChecksumCheck>): List<CheckResult> = checks.map { verify(image, it) }

    fun verify(image: MemoryImage, check: ChecksumCheck): CheckResult {
        val computed = when (val result = compute(image, check)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> return CheckResult(check, null, null, result.error.message)
        }
        val store = check.store
        val bits = storedBits(check)
        val result = computed and bits
        if (store.form == StoreForm.FORCE) return CheckResult(check, result, store.target and bits)
        val value = read(image, store.address, store) ?: return CheckResult(check, result, null, "The checksum's place ${store.address.toHexAddress()} isn't in the image")
        return when (store.form) {
            StoreForm.VALUE -> CheckResult(check, result, value)
            StoreForm.COMPLEMENT -> CheckResult(check, result, value.inv() and bits)
            else -> {
                val complement = read(image, store.address + store.size, store) ?: return CheckResult(check, result, value, "Its complement isn't in the image")
                CheckResult(check, result, value, matches = value == result && complement == (result.inv() and bits))
            }
        }
    }

    /**
     * Fixes [checks] in order, as one undo step, and goes round again (up to [passes] times) while any are wrong,
     * since one check's stored value can sit inside another's ranges
     */
    fun fix(image: MemoryImage, checks: List<ChecksumCheck>, passes: Int = 4): FixReport {
        var fixed = 0
        var pass = 0
        image.edit {
            while (pass < passes) {
                pass++
                val wrong = verify(this, checks).filter { !it.ok && it.problem == null }
                if (wrong.isEmpty()) break
                for (result in wrong) if (fixOne(this, result.check)) fixed++
            }
        }
        return FixReport(fixed, verify(image, checks), pass)
    }

    /** Fixes one check; false when it can't be */
    fun fixOne(image: MemoryImage, check: ChecksumCheck): Boolean {
        val store = check.store
        if (store.form == StoreForm.FORCE) return force(image, check)
        val computed = (compute(image, check) as? TkResult.Success)?.data ?: return false
        val bits = storedBits(check)
        val value = computed and bits
        return when (store.form) {
            StoreForm.VALUE -> write(image, store.address, store, value)
            StoreForm.COMPLEMENT -> write(image, store.address, store, value.inv() and bits)
            StoreForm.VALUE_AND_COMPLEMENT -> write(image, store.address, store, value) && write(image, store.address + store.size, store, value.inv() and bits)
            StoreForm.FORCE -> false
        }
    }

    /** The checksum over the check's ranges as they are now */
    fun compute(image: MemoryImage, check: ChecksumCheck): TkResult<ULong, ChecksumError> {
        if (check.ranges.size > MAX_RANGES) return TkResult.Failure(ChecksumError("${check.name} has more than $MAX_RANGES ranges"))
        // Added up so it stops at the cap and can't wrap round
        var total = 0L
        for (range in check.ranges) {
            if (range.size < 0 || range.size > MAX_BYTES - total) {
                return TkResult.Failure(ChecksumError("${check.name} reads more than ${MAX_BYTES / 1024 / 1024} MB"))
            }
            total += range.size
        }
        if (check.store.form != StoreForm.FORCE && check.ranges.any { range -> check.store.ranges.any { it.overlaps(range) } }) {
            return TkResult.Failure(ChecksumError("${check.name}'s ranges include where its own checksum is kept"))
        }
        val running = check.algorithm.start()
        for (range in check.ranges) {
            var at = range.start
            while (at < range.endExclusive) {
                val size = minOf(CHUNK.toLong(), range.endExclusive - at).toInt()
                val bytes = image.readOrNull(at, size) ?: return TkResult.Failure(ChecksumError("${check.name}: ${range} isn't all in the image"))
                running.update(bytes)
                at += size
            }
        }
        return TkResult.Success(running.finish())
    }

    private const val CHUNK = 64 * 1024

    /** The bits the stored value has: its size, or the algorithm's width when that's less */
    private fun storedBits(check: ChecksumCheck): ULong {
        val bits = minOf(check.store.size * 8, check.algorithm.width)
        return if (bits >= 64) ULong.MAX_VALUE else (1uL shl bits) - 1u
    }

    private fun mask(value: ULong, check: ChecksumCheck): ULong = value and storedBits(check)

    private fun read(image: MemoryImage, address: Long, store: ChecksumStore): ULong? =
        image.readOrNull(address, store.size)?.let { store.byteOrder.decode(it, 0, store.size) }

    private fun write(image: MemoryImage, address: Long, store: ChecksumStore, value: ULong): Boolean {
        if (!image.contains(address, store.size)) return false
        image.write(address, store.byteOrder.encode(value, store.size))
        return true
    }

    // ------------------------------------------------ Forcing a result

    private fun force(image: MemoryImage, check: ChecksumCheck): Boolean {
        val store = check.store
        if (!image.contains(store.address, store.size)) return false
        if (check.ranges.none { range -> store.ranges.all { range.contains(it) } }) return false
        val target = mask(store.target, check)
        return when (val algorithm = check.algorithm) {
            is ChecksumAlgorithm.Sum -> {
                if (store.size != algorithm.word || algorithm.word * 8 < algorithm.width) return false
                val current = (compute(image, check) as? TkResult.Success)?.data ?: return false
                val word = read(image, store.address, store.copy(byteOrder = algorithm.byteOrder)) ?: return false
                val bits = if (store.size >= 8) ULong.MAX_VALUE else (1uL shl (store.size * 8)) - 1u
                write(image, store.address, store.copy(byteOrder = algorithm.byteOrder), (word + target - current) and bits)
            }
            is ChecksumAlgorithm.Xor -> {
                if (store.size != algorithm.word) return false
                val current = (compute(image, check) as? TkResult.Success)?.data ?: return false
                val word = read(image, store.address, store.copy(byteOrder = algorithm.byteOrder)) ?: return false
                write(image, store.address, store.copy(byteOrder = algorithm.byteOrder), word xor current xor target)
            }
            is ChecksumAlgorithm.Crc -> solveCrc(image, check, target)
            ChecksumAlgorithm.Adler32 -> false
        }
    }

    /**
     * A CRC is affine in its input bits: flipping bit j of the correction changes the result by a fixed amount.
     * Measure each bit's effect, then solve for the bits that turn the result into [target] (Gaussian elimination
     * over GF(2))
     */
    private fun solveCrc(image: MemoryImage, check: ChecksumCheck, target: ULong): Boolean {
        val store = check.store
        val bits = store.size * 8
        if (bits > 64) return false
        val original = image.read(store.address, store.size)
        val base = (compute(image, check) as? TkResult.Success)?.data ?: return false
        val columns = ULongArray(bits)
        val scratch = original.copyOf()
        for (j in 0 until bits) {
            scratch[j / 8] = (original[j / 8].toInt() xor (1 shl (j % 8))).toByte()
            image.write(store.address, scratch)
            columns[j] = ((compute(image, check) as? TkResult.Success)?.data ?: return false.also { image.write(store.address, original) }) xor base
            scratch[j / 8] = original[j / 8]
        }
        image.write(store.address, original)
        val solution = solveGf2(columns, base xor target, check.algorithm.width) ?: return false
        val patched = original.copyOf()
        for (j in 0 until bits) if (solution and (1uL shl j) != 0uL) patched[j / 8] = (patched[j / 8].toInt() xor (1 shl (j % 8))).toByte()
        image.write(store.address, patched)
        return true
    }

    /** x (a bit set over the columns) with XOR of the chosen columns == [want], or null when there's none */
    internal fun solveGf2(columns: ULongArray, want: ULong, width: Int): ULong? {
        // Rows are result bits; each row is a mask over the columns, plus the wanted bit
        val rows = Array(width) { r ->
            var mask = 0uL
            for (j in columns.indices) if (columns[j] and (1uL shl r) != 0uL) mask = mask or (1uL shl j)
            mask to ((want shr r) and 1u)
        }.toMutableList()
        val pivots = mutableListOf<Pair<Int, Int>>()
        var row = 0
        for (col in columns.indices) {
            val found = (row until rows.size).firstOrNull { rows[it].first and (1uL shl col) != 0uL } ?: continue
            val swap = rows[found]; rows[found] = rows[row]; rows[row] = swap
            for (other in rows.indices) {
                if (other != row && rows[other].first and (1uL shl col) != 0uL) {
                    rows[other] = (rows[other].first xor rows[row].first) to (rows[other].second xor rows[row].second)
                }
            }
            pivots += row to col
            row++
            if (row == rows.size) break
        }
        // Rows left without a pivot must want 0
        if ((row until rows.size).any { rows[it].second != 0uL }) return null
        var x = 0uL
        for ((r, col) in pivots) if (rows[r].second != 0uL) x = x or (1uL shl col)
        return x
    }
}
