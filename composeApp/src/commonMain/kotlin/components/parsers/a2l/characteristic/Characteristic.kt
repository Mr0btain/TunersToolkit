package components.parsers.a2l.characteristic

import components.parsers.a2l.axis.AxisDescr
import components.parsers.a2l.compumethod.CompuMethod
import components.parsers.a2l.memorysegment.MemorySegment
import components.parsers.a2l.recordlayout.RecordLayout
import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.data.AnnotList
import components.parsers.a2l.classes.data.Annotation
import components.parsers.a2l.classes.data.BitMask
import components.parsers.a2l.classes.data.CharNum
import components.parsers.a2l.classes.data.ComparisonQuantity
import components.parsers.a2l.classes.data.ConversionType
import components.parsers.a2l.classes.data.DependantCharacteristic
import components.parsers.a2l.classes.data.DisplayIdentifier
import components.parsers.a2l.classes.data.EcuAddressExtension
import components.parsers.a2l.classes.data.ExtendedLimits
import components.parsers.a2l.classes.data.Format
import components.parsers.a2l.classes.data.FunctionList
import components.parsers.a2l.classes.data.IfDataList
import components.parsers.a2l.classes.data.MapList
import components.parsers.a2l.classes.data.MatrixDim
import components.parsers.a2l.classes.data.MaxRefresh
import components.parsers.a2l.classes.data.MemorySegmentReference
import components.parsers.a2l.classes.data.PhysUnit
import components.parsers.a2l.classes.data.StepSize
import components.parsers.a2l.classes.data.SymbolLink
import components.parsers.a2l.classes.data.VirtCharacteristic
import components.parsers.a2l.classes.enums.CharacteristicType
import components.parsers.a2l.classes.enums.Deposit
import components.calibrationdata.classes.AxisDataType
import components.parsers.a2l.classes.enums.ByteOrder
import components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * Characteristic: an adjustable calibration value usually a map or scalar in the ECU memory
 */

@Serializable
data class Characteristic(
    val name: String = "",
    val identifier: String = "",
    val type: CharacteristicType = CharacteristicType.NOT_SET,
    val address: ULong = ULong.MIN_VALUE,
    val refRecordLayout: String = "",
    val maxDiff: Double = Double.NaN,
    val conversion: String = "",
    val lowerLimit: Double = Double.NaN,
    val upperLimit: Double = Double.NaN,
    val optionalParameters: Map<CharOpt, A2lOptional> = mapOf()
)

fun Characteristic.validate(): Boolean {
    return when {
        name.isBlank() -> false
        type == CharacteristicType.NOT_SET -> false
        address == ULong.MIN_VALUE -> false
        else -> true
    }
}

/**
 * Apply the Bitmask specified
 */
fun Characteristic.applyBitMask(input: Number): Double {
    val result = input.toInt().toULong() and getBitMask()
    var shift = 0
    val bits = result.toString(2)

    for (i in 0..bits.length) {
        val j = bits.length - 1 - i
        if (bits[j] == '1') {
            shift = i
            break
        }
    }

    return (result shr(shift)).toDouble()
}

/**
 * Checks and Returns a list of Annotations if set
 */
fun Characteristic.getAnnotations(): List<Annotation> {
    return if (optionalParameters.containsKey(CharOpt.Annotations)) {
        val result = optionalParameters[CharOpt.Annotations] as? AnnotList ?: AnnotList()
        result.annot
    } else listOf()
}

/**
 * Checks and Returns the BitMask Value if set
 */
fun Characteristic.getBitMask(): ULong {
    return if (optionalParameters.containsKey(CharOpt.BitMask)) {
        val res = optionalParameters[CharOpt.BitMask] as? BitMask ?: BitMask()
        res.mask
    } else ULong.MIN_VALUE
}

/**
 * Checks and Returns the ByteOrder if set
 */
fun Characteristic.getByteOrder(): ByteOrder {
    return if (optionalParameters.containsKey(CharOpt.ByteOrder)) {
        optionalParameters[CharOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
    } else ByteOrder.LITTLE_ENDIAN
}

/**
 * Checks and Returns true if Calibration Access is set
 */
fun Characteristic.getCalibrationAccess(): Boolean {
    return optionalParameters.containsKey(CharOpt.CalibrationAccess)
}

/**
 * Checks and Returns the Comparison Quantity if set
 */
fun Characteristic.getComparisonQuantity(): String {
    return if (optionalParameters.containsKey(CharOpt.ComparisonQuantity)) {
        val res = optionalParameters[CharOpt.ComparisonQuantity] as? ComparisonQuantity ?: ComparisonQuantity()
        res.quantity
    } else ""
}

/**
 * Checks and Returns the Conversion Type if set
 */
fun Characteristic.getConversionType(): String {
    return if (optionalParameters.containsKey(CharOpt.ConversionType)) {
        val res = optionalParameters[CharOpt.ConversionType] as? ConversionType ?: ConversionType()
        res.type
    } else ""
}

/**
 * Checks and Returns the Dependant Characteristics if set
 */
fun Characteristic.getDependantCharacteristic(): List<String> {
    return if (optionalParameters.containsKey(CharOpt.DependantCharacteristic)) {
        val result = optionalParameters[CharOpt.DependantCharacteristic] as? DependantCharacteristic
            ?: DependantCharacteristic()
        result.list
    } else listOf()
}

/**
 * Checks and Returns the Deposit Structure if set
 */
fun Characteristic.getDeposit(): Deposit {
    return if (optionalParameters.containsKey(CharOpt.Deposit)) {
        optionalParameters[CharOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
    } else Deposit.NOT_SET
}

/**
 * Returns the Dimension Array
 */

fun Characteristic.getDimArray(): Array<Int> {
    val mNumber = getNumber()
    val mMatrixDim = getMatrixDim()

    return if (mMatrixDim != MatrixDim()) {
        mMatrixDim.asArray()
    } else {
        arrayOf(mNumber)
    }
}

/**
 * Checks and Returns true if Discrete is set
 */
fun Characteristic.isDiscrete(): Boolean {
    return optionalParameters.containsKey(CharOpt.Discrete)
}

/**
 * Checks and Returns the Display Identifier if set
 */
fun Characteristic.getDisplayIdentifier(): String {
    return if (optionalParameters.containsKey(CharOpt.DisplayIdentifier)) {
        val res = optionalParameters[CharOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
        res.id
    } else ""
}

/**
 * Checks and Returns the Ecu Address Extension if set
 */
fun Characteristic.getEcuAddressExtension(): String {
    return if (optionalParameters.containsKey(CharOpt.EcuAddressExtension)) {
        val res = optionalParameters[CharOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
        res.address
    } else ""
}

/**
 * Checks andReturns the Extended Limits if set
 */
fun Characteristic.getExtendedLimits(): ExtendedLimits {
    return if (optionalParameters.containsKey(CharOpt.ExtendedLimits)) {
        optionalParameters[CharOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
    } else ExtendedLimits()
}

/**
 * Checks and Returns the Format if set
 */
fun Characteristic.getFormat(): String {
    return if (optionalParameters.containsKey(CharOpt.Format)) {
        val res = optionalParameters[CharOpt.Format] as? Format ?: Format()
        res.value
    } else ""
}

/**
 * Checks and Returns a list of Functions if set
 */
fun Characteristic.getFunctionList(): List<String> {
    return if (optionalParameters.containsKey(CharOpt.FunctionList)) {
        val result = optionalParameters[CharOpt.FunctionList] as? FunctionList ?: FunctionList()
        result.list
    } else listOf()
}

/**
 * Returns True if GuardRails is set
 */
fun Characteristic.getGuardRails(): Boolean {
    return optionalParameters.containsKey(CharOpt.GuardRails)
}

/**
 * Checks and Returns any IfData if set
 */
fun Characteristic.getIfData(): List<components.parsers.a2l.ifdata.IfData> {
    return if (optionalParameters.containsKey(CharOpt.IfData)) {
        val result = optionalParameters[CharOpt.IfData] as? IfDataList ?: IfDataList()
        // Check if items are correct type
        result.ifData
    } else listOf()
}

/**
 * Checks and Returns the MapList if set
 */
fun Characteristic.getMapList(): List<String> {
    return if (optionalParameters.containsKey(CharOpt.MapList)) {
        val result = optionalParameters[CharOpt.MapList] as? MapList ?: MapList()
        result.list
    } else listOf()
}

/**
 * Checks and Returns the MatrixDim if set
 */
fun Characteristic.getMatrixDim(): MatrixDim {
    return if (optionalParameters.containsKey(CharOpt.MatrixDim)) {
        optionalParameters[CharOpt.MatrixDim] as? MatrixDim ?: MatrixDim()
    } else MatrixDim()
}

/**
 * Checks and Returns the Max Refresh if set
 */
fun Characteristic.getMaxRefresh(): MaxRefresh {
    return if (optionalParameters.containsKey(CharOpt.MaxRefresh)) {
        optionalParameters[CharOpt.MaxRefresh] as? MaxRefresh ?: MaxRefresh()
    } else MaxRefresh()
}

/**
 * Checks and Returns the Number if set
 */
fun Characteristic.getNumber(): Int {
    return if (optionalParameters.containsKey(CharOpt.Number)) {
        val res = optionalParameters[CharOpt.Number] as? CharNum ?: CharNum()
        res.num
    } else if (optionalParameters.containsKey(CharOpt.MatrixDim)) {
        this.getMatrixDim().xDim
    } else Int.MIN_VALUE
}

/**
 * Checks and Returns Phys Unit if set
 */
fun Characteristic.getPhysUnit(): String {
    return if (optionalParameters.containsKey(CharOpt.PhysUnit)) {
        val res = optionalParameters[CharOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
        res.unit
    } else ""
}

/**
 * Checks and Returns True if Axis is Read Only
 */
fun Characteristic.isReadOnly(): Boolean {
    return optionalParameters.containsKey(CharOpt.ReadOnly)
}

/**
 * Checks and Returns the Record Layout used in this Characteristic
 */
fun Characteristic.getRecordLayout(a2lData: A2lData): RecordLayout {
    return if (a2lData.recordLayouts.containsKey(refRecordLayout)) {
        a2lData.recordLayouts[refRecordLayout] ?: RecordLayout()
    } else RecordLayout()
}

/**
 * Checks and Returns the CompuMethod used in this Characteristic
 */
fun Characteristic.getCompuMethod(a2lData: A2lData): CompuMethod {
    return if (a2lData.compuMethods.containsKey(conversion)) {
        a2lData.compuMethods[conversion] ?: CompuMethod()
    } else CompuMethod()
}

/**
 * Checks and Returns The Reference Memory Segment if set
 */
fun Characteristic.getRefMemorySegment(a2lData: A2lData): MemorySegment {
    return if (optionalParameters.containsKey(CharOpt.RefMemorySegment)) {
        val mRefMemorySegment = optionalParameters[CharOpt.RefMemorySegment] as? MemorySegmentReference
            ?: MemorySegmentReference()

        if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
            return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
        } else MemorySegment()
    } else MemorySegment()
}

/**
 * Checks and Returns the Step Size if set
 */
fun Characteristic.getStepSize(): Float {
    return if (optionalParameters.containsKey(CharOpt.StepSize)) {
        val res = optionalParameters[CharOpt.StepSize] as? StepSize ?: StepSize()
        res.size
    } else Float.NaN
}

/**
 * Checks and Returns the SymbolLink if set
 */
fun Characteristic.getSymbolLink(): SymbolLink {
    return if (optionalParameters.containsKey(CharOpt.SymbolLink)) {
        optionalParameters[CharOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
    } else SymbolLink()
}

/**
 * Checks and Returns the VirtualCharacteristics if set
 */
fun Characteristic.getVirtualCharacteristic(): List<String> {
    return if (optionalParameters.containsKey(CharOpt.VirtualCharacteristic)) {
        val result = optionalParameters[CharOpt.VirtualCharacteristic] as? VirtCharacteristic ?: VirtCharacteristic()
        result.list
    } else listOf()
}

/**
 * Checks for all AxisDescr contained in the Characteristic and returns a map of them
 */
fun Characteristic.getAxisDescrMap(): Map<AxisDataType, AxisDescr> {
    val mResult = mutableMapOf<AxisDataType, AxisDescr>()

    this.optionalParameters.forEach { (id, value) ->
        if (value is AxisDescr) {
            when (id) {
                CharOpt.AXIS_DESC_X -> mResult[AxisDataType.X] = value
                CharOpt.AXIS_DESC_Y -> mResult[AxisDataType.Y] = value
                CharOpt.AXIS_DESC_Z -> mResult[AxisDataType.Z] = value
                CharOpt.AXIS_DESC_4 -> mResult[AxisDataType.Axis4] = value
                CharOpt.AXIS_DESC_5 -> mResult[AxisDataType.Axis5] = value
                else -> {}
            }
        }
    }

    return mResult
}

/**
 * Checks and Return Characteristic Axis Description X if set
 */
fun Characteristic.getAxisDescrX(): AxisDescr {
    return if (optionalParameters.containsKey(CharOpt.AXIS_DESC_X)) {
        optionalParameters[CharOpt.AXIS_DESC_X] as? AxisDescr ?: AxisDescr()
    } else AxisDescr()
}

/**
 * Checks and Return Characteristic Axis Description Y if set
 */
fun Characteristic.getAxisDescrY(): AxisDescr {
    return if (optionalParameters.containsKey(CharOpt.AXIS_DESC_Y)) {
        optionalParameters[CharOpt.AXIS_DESC_Y] as? AxisDescr ?: AxisDescr()
    } else AxisDescr()
}

/**
 * Checks and Return Characteristic Axis Description Z if set
 */
fun Characteristic.getAxisDescrZ(): AxisDescr {
    return if (optionalParameters.containsKey(CharOpt.AXIS_DESC_Z)) {
        optionalParameters[CharOpt.AXIS_DESC_Z] as? AxisDescr ?: AxisDescr()
    } else AxisDescr()
}

/**
 * Checks and Return Characteristic Axis Description 4 if set
 */
fun Characteristic.getAxisDescr4(): AxisDescr {
    return if (optionalParameters.containsKey(CharOpt.AXIS_DESC_4)) {
        optionalParameters[CharOpt.AXIS_DESC_4] as? AxisDescr ?: AxisDescr()
    } else AxisDescr()
}

/**
 * Checks and Return Characteristic Axis Description 5 if set
 */
fun Characteristic.getAxisDescr5(): AxisDescr {
    return if (optionalParameters.containsKey(CharOpt.AXIS_DESC_5)) {
        optionalParameters[CharOpt.AXIS_DESC_5] as? AxisDescr ?: AxisDescr()
    } else AxisDescr()
}