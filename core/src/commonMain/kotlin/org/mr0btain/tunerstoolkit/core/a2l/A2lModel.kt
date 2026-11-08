package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/*
 * The A2L as data: what the file says, with names still names (a CHARACTERISTIC's conversion is the COMPU_METHOD's
 * name, not the method). Resolving names into calibration objects is the next step's job. Every class keeps the
 * A2lElement it came from, so nothing in the file is lost and diagnostics can point at it.
 */

/** A whole A2L file */
class A2lFile(
    val asap2Version: String?,
    val a2mlVersion: String?,
    val project: A2lProject?,
    val tree: A2lTree
) {
    val modules: List<A2lModule> get() = project?.modules.orEmpty()
}

class A2lProject(val name: String, val longId: String, val header: A2lHeader?, val modules: List<A2lModule>, val element: A2lElement)

class A2lHeader(val comment: String, val version: String?, val projectNo: String?)

class A2lModule(
    val name: String,
    val longId: String,
    val a2ml: A2ml?,
    val modCommon: ModCommon?,
    val modPar: ModPar?,
    val characteristics: List<Characteristic>,
    val axisPts: List<AxisPts>,
    val measurements: List<Measurement>,
    val compuMethods: List<CompuMethod>,
    val compuTabs: List<CompuTab>,
    val compuVTabs: List<CompuVTab>,
    val compuVTabRanges: List<CompuVTabRange>,
    val recordLayouts: List<RecordLayout>,
    val functions: List<A2lFunction>,
    val groups: List<A2lGroup>,
    val units: List<A2lUnit>,
    val blobs: List<Blob>,
    val instances: List<Instance>,
    val typedefs: List<Typedef>,
    val transformers: List<Transformer>,
    val frames: List<Frame>,
    val userRights: List<UserRights>,
    val variantCoding: VariantCoding?,
    val ifData: List<IfData>,
    val element: A2lElement
) {
    private val characteristicsByName = characteristics.asReversed().associateBy { it.name }
    private val axisPtsByName = axisPts.asReversed().associateBy { it.name }
    private val measurementsByName = measurements.asReversed().associateBy { it.name }
    private val compuMethodsByName = compuMethods.asReversed().associateBy { it.name }
    private val recordLayoutsByName = recordLayouts.asReversed().associateBy { it.name }
    private val compuTabsByName = compuTabs.asReversed().associateBy { it.name }
    private val compuVTabsByName = compuVTabs.asReversed().associateBy { it.name }
    private val compuVTabRangesByName = compuVTabRanges.asReversed().associateBy { it.name }
    private val unitsByName = units.asReversed().associateBy { it.name }

    fun characteristic(name: String): Characteristic? = characteristicsByName[name]
    fun axisPts(name: String): AxisPts? = axisPtsByName[name]
    fun measurement(name: String): Measurement? = measurementsByName[name]
    fun compuMethod(name: String): CompuMethod? = compuMethodsByName[name]
    fun recordLayout(name: String): RecordLayout? = recordLayoutsByName[name]
    fun compuTab(name: String): CompuTab? = compuTabsByName[name]
    fun compuVTab(name: String): CompuVTab? = compuVTabsByName[name]
    fun compuVTabRange(name: String): CompuVTabRange? = compuVTabRangesByName[name]
    fun unit(name: String): A2lUnit? = unitsByName[name]
}

// ------------------------------------------------ MOD_COMMON and MOD_PAR

/**
 * Defaults for the module
 * @property alignments ALIGNMENT_BYTE/WORD/LONG/INT64/FLOAT16_IEEE/FLOAT32_IEEE/FLOAT64_IEEE by the part after ALIGNMENT_
 */
class ModCommon(
    val comment: String,
    val byteOrder: ByteOrder?,
    val deposit: Deposit?,
    val dataSize: Int?,
    val sRecLayout: String?,
    val alignments: Map<String, Int>
)

class ModPar(
    val comment: String,
    val version: String?,
    val addrEpk: List<Long>,
    val epk: String?,
    val supplier: String?,
    val customer: String?,
    val customerNo: String?,
    val user: String?,
    val phoneNo: String?,
    val ecu: String?,
    val cpuType: String?,
    val noOfInterfaces: Int?,
    val ecuCalibrationOffset: Long?,
    val memorySegments: List<MemorySegment>,
    val memoryLayouts: List<MemoryLayout>,
    val systemConstants: Map<String, String>,
    val calibrationMethods: List<CalibrationMethod>
)

/**
 * @property prgType CODE, DATA, OFFLINE_DATA, VARIABLES, SERAM, RESERVED, CALIBRATION_VARIABLES, EXCLUDE_FROM_FLASH
 * @property memoryType EEPROM, EPROM, FLASH, RAM, ROM, REGISTER, NOT_IN_ECU
 * @property attribute INTERN or EXTERN
 * @property offsets The five mirror offsets, -1 where unused
 */
class MemorySegment(
    val name: String,
    val longId: String,
    val prgType: String,
    val memoryType: String,
    val attribute: String,
    val address: Long,
    val size: Long,
    val offsets: List<Long>,
    val ifData: List<IfData>
)

class MemoryLayout(val prgType: String, val address: Long, val size: Long, val offsets: List<Long>)

class CalibrationMethod(val method: String, val version: Long, val handles: List<CalibrationHandle>)
class CalibrationHandle(val values: List<Long>, val text: String?)

// ------------------------------------------------ Types

enum class Deposit { ABSOLUTE, DIFFERENCE }

enum class CharacteristicType { VALUE, CURVE, MAP, CUBOID, CUBE_4, CUBE_5, VAL_BLK, ASCII }

enum class AxisType { STD_AXIS, FIX_AXIS, COM_AXIS, RES_AXIS, CURVE_AXIS }

enum class ConversionType { IDENTICAL, LINEAR, RAT_FUNC, FORM, TAB_INTP, TAB_NOINTP, TAB_VERB }

/** How a map's values are laid out: ROW_DIR goes along x first, COLUMN_DIR along y first */
enum class IndexMode { ROW_DIR, COLUMN_DIR, ALTERNATE_CURVES, ALTERNATE_WITH_X, ALTERNATE_WITH_Y }

enum class IndexOrder { INDEX_INCR, INDEX_DECR }

/** DIRECT is the data itself; the P types are pointers of that size to it */
enum class AddressType(val pointerSize: Int) { DIRECT(0), PBYTE(1), PWORD(2), PLONG(4), PLONGLONG(8) }

// ------------------------------------------------ Objects

/**
 * The optional keywords any object can have. Absent ones are null (or false)
 */
class ObjectOptions(
    val bitMask: Long? = null,
    val bitOperation: BitOperation? = null,
    val byteOrder: ByteOrder? = null,
    val matrixDim: List<Int> = emptyList(),
    val number: Int? = null,
    val extendedLimits: Pair<Double, Double>? = null,
    val readOnly: Boolean = false,
    val guardRails: Boolean = false,
    val discrete: Boolean = false,
    val displayIdentifier: String? = null,
    val format: String? = null,
    val physUnit: String? = null,
    val maxRefresh: Pair<Long, Long>? = null,
    val ecuAddressExtension: Long? = null,
    val calibrationAccess: String? = null,
    val comparisonQuantity: String? = null,
    val dependentCharacteristic: Formula? = null,
    val virtualCharacteristic: Formula? = null,
    val symbolLink: Pair<String, Long>? = null,
    val modelLink: String? = null,
    val encoding: String? = null,
    val monotony: String? = null,
    val stepSize: Double? = null,
    val maxGrad: Double? = null,
    val layout: IndexMode? = null,
    val addressType: AddressType? = null,
    val refMemorySegment: String? = null,
    val annotations: List<Annotation> = emptyList(),
    val functionList: List<String> = emptyList(),
    val ifData: List<IfData> = emptyList()
)

class BitOperation(val leftShift: Int?, val rightShift: Int?, val signExtend: Boolean)

/** A formula with the names it reads (X1 is the first): DEPENDENT_ and VIRTUAL_CHARACTERISTIC, and FORM conversions */
class Formula(val expression: String, val inputs: List<String> = emptyList(), val inverse: String? = null)

class Annotation(val label: String?, val origin: String?, val text: List<String>)

class Characteristic(
    val name: String,
    val longId: String,
    val type: CharacteristicType,
    val address: Long,
    val recordLayout: String,
    val maxDiff: Double,
    val conversion: String,
    val lowerLimit: Double,
    val upperLimit: Double,
    val axes: List<AxisDescr>,
    val mapList: List<String>,
    val options: ObjectOptions,
    val element: A2lElement
) {
    val location: SourceLocation get() = element.location
}

sealed interface FixAxis {
    /** Values offset + n * 2^shift for n in 0 until count */
    class Par(val offset: Double, val shift: Int, val count: Int) : FixAxis
    /** Values offset + n * distance for n in 0 until count */
    class ParDist(val offset: Double, val distance: Double, val count: Int) : FixAxis
    class ParList(val values: List<Double>) : FixAxis
}

class AxisDescr(
    val type: AxisType,
    val inputQuantity: String,
    val conversion: String,
    val maxAxisPoints: Int,
    val lowerLimit: Double,
    val upperLimit: Double,
    val axisPtsRef: String?,
    val curveAxisRef: String?,
    val fixAxis: FixAxis?,
    val deposit: Deposit?,
    val options: ObjectOptions,
    val element: A2lElement
)

class AxisPts(
    val name: String,
    val longId: String,
    val address: Long,
    val inputQuantity: String,
    val recordLayout: String,
    val maxDiff: Double,
    val conversion: String,
    val maxAxisPoints: Int,
    val lowerLimit: Double,
    val upperLimit: Double,
    val deposit: Deposit?,
    val options: ObjectOptions,
    val element: A2lElement
)

class Measurement(
    val name: String,
    val longId: String,
    val dataType: DataType,
    val conversion: String,
    val resolution: Double,
    val accuracy: Double,
    val lowerLimit: Double,
    val upperLimit: Double,
    val ecuAddress: Long?,
    val arraySize: Int?,
    val errorMask: Long?,
    val virtual: List<String>,
    val options: ObjectOptions,
    val element: A2lElement
)

class Blob(val name: String, val longId: String, val address: Long, val size: Long, val options: ObjectOptions, val element: A2lElement)

class Instance(val name: String, val longId: String, val typedefName: String, val address: Long, val options: ObjectOptions, val element: A2lElement)

/**
 * TYPEDEF_CHARACTERISTIC, _AXIS, _MEASUREMENT, _STRUCTURE or _BLOB (1.7): kept as their parameters and
 * elements; INSTANCE resolution reads them when it's needed
 */
class Typedef(val kind: String, val name: String, val longId: String, val components: List<StructureComponent>, val element: A2lElement)

class StructureComponent(val name: String, val typedefName: String, val offset: Long, val options: ObjectOptions)

class Transformer(val name: String, val version: String, val dll32: String, val dll64: String, val timeout: Long, val trigger: String, val inverse: String, val inObjects: List<String>, val outObjects: List<String>)

// ------------------------------------------------ Conversion

class CompuMethod(
    val name: String,
    val longId: String,
    val type: ConversionType,
    val format: String,
    val unit: String,
    /** RAT_FUNC's a..f: physical x from internal y solves y = (a x² + b x + c) / (d x² + e x + f) */
    val coeffs: List<Double>?,
    /** LINEAR's a, b: physical = a * internal + b */
    val coeffsLinear: Pair<Double, Double>?,
    val compuTabRef: String?,
    val formula: Formula?,
    val refUnit: String?,
    val statusStringRef: String?,
    val element: A2lElement
)

class CompuTab(val name: String, val longId: String, val type: ConversionType, val pairs: List<Pair<Double, Double>>, val defaultValue: String?, val defaultNumeric: Double?, val element: A2lElement)

class CompuVTab(val name: String, val longId: String, val type: ConversionType, val entries: List<Pair<Double, String>>, val defaultValue: String?, val element: A2lElement)

class CompuVTabRange(val name: String, val longId: String, val entries: List<VTabRange>, val defaultValue: String?, val element: A2lElement)

class VTabRange(val min: Double, val max: Double, val text: String)

class A2lUnit(val name: String, val longId: String, val display: String, val type: String, val refUnit: String?, val siExponents: List<Int>?, val conversion: Pair<Double, Double>?)

// ------------------------------------------------ RECORD_LAYOUT

/**
 * What one keyword of a RECORD_LAYOUT says is where. [position] orders the parts of the record
 * @property axis X, Y, Z, 4 or 5; null for the ones that aren't per axis
 */
sealed interface LayoutEntry {
    val position: Int

    class FncValues(override val position: Int, val dataType: DataType, val indexMode: IndexMode, val addressType: AddressType) : LayoutEntry
    class Identification(override val position: Int, val dataType: DataType) : LayoutEntry
    class AxisPts(val axis: String, override val position: Int, val dataType: DataType, val indexOrder: IndexOrder, val addressType: AddressType) : LayoutEntry
    class AxisRescale(val axis: String, override val position: Int, val dataType: DataType, val maxPairs: Int, val indexOrder: IndexOrder, val addressType: AddressType) : LayoutEntry
    class NoAxisPts(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    class NoRescale(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    /** The axis has a fixed number of points and no count in memory; has no position, so it sorts first */
    class FixNoAxisPts(val axis: String, val count: Int) : LayoutEntry { override val position: Int get() = 0 }
    class SrcAddr(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    /** [axis] W is the result (RIP_ADDR_W) */
    class RipAddr(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    class ShiftOp(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    class Offset(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    class DistOp(val axis: String, override val position: Int, val dataType: DataType) : LayoutEntry
    /** Bytes to skip: [dataSize] is BYTE, WORD or LONG */
    class Reserved(override val position: Int, val dataSize: String) : LayoutEntry
}

class RecordLayout(
    val name: String,
    /** In the order of their positions */
    val entries: List<LayoutEntry>,
    val alignments: Map<String, Int>,
    val staticRecordLayout: Boolean,
    val staticAddressOffsets: Boolean,
    val element: A2lElement
) {
    val fncValues: LayoutEntry.FncValues? get() = entries.firstNotNullOfOrNull { it as? LayoutEntry.FncValues }
    fun axisPts(axis: String): LayoutEntry.AxisPts? = entries.firstOrNull { it is LayoutEntry.AxisPts && it.axis == axis } as LayoutEntry.AxisPts?
    fun noAxisPts(axis: String): LayoutEntry.NoAxisPts? = entries.firstOrNull { it is LayoutEntry.NoAxisPts && it.axis == axis } as LayoutEntry.NoAxisPts?
    fun fixNoAxisPts(axis: String): LayoutEntry.FixNoAxisPts? = entries.firstOrNull { it is LayoutEntry.FixNoAxisPts && it.axis == axis } as LayoutEntry.FixNoAxisPts?
}

// ------------------------------------------------ Organisation

class A2lFunction(
    val name: String,
    val longId: String,
    val version: String?,
    val defCharacteristics: List<String>,
    val refCharacteristics: List<String>,
    val inMeasurements: List<String>,
    val outMeasurements: List<String>,
    val locMeasurements: List<String>,
    val subFunctions: List<String>
)

class A2lGroup(
    val name: String,
    val longId: String,
    val root: Boolean,
    val refCharacteristics: List<String>,
    val refMeasurements: List<String>,
    val subGroups: List<String>,
    val functions: List<String>
)

class Frame(val name: String, val longId: String, val scalingUnit: Long, val rate: Long, val measurements: List<String>)

class UserRights(val userLevelId: String, val refGroups: List<List<String>>, val readOnly: Boolean)

class VariantCoding(
    val separator: String,
    val naming: String,
    val criteria: List<VarCriterion>,
    val forbiddenCombinations: List<List<Pair<String, String>>>,
    val characteristics: List<VarCharacteristic>
)

class VarCriterion(val name: String, val longId: String, val values: List<String>, val measurement: String?, val selectionCharacteristic: String?)

/** A characteristic with one copy per variant; [addresses] are in the order of the criteria's value combinations */
class VarCharacteristic(val name: String, val criteria: List<String>, val addresses: List<Long>)

// ------------------------------------------------ IF_DATA

/**
 * Tool specific data, kept as written. [tag] says whose (XCP, CANAPE_EXT, …); the module's A2ML says what's in it
 */
class IfData(val tag: String, val raw: String, val location: SourceLocation)
