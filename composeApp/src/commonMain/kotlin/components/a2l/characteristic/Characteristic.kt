package components.a2l.characteristic

import components.a2l.axis.AxisDescr
import components.a2l.compumethod.CompuMethod
import components.a2l.ifdata.IfData
import components.a2l.memorysegment.MemorySegment
import components.a2l.recordlayout.RecordLayout
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.AnnotList
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.BitMask
import components.a2l.classes.data.CharNum
import components.a2l.classes.data.ComparisonQuantity
import components.a2l.classes.data.ConversionType
import components.a2l.classes.data.DependantCharacteristic
import components.a2l.classes.data.DisplayIdentifier
import components.a2l.classes.data.EcuAddressExtension
import components.a2l.classes.data.ExtendedLimits
import components.a2l.classes.data.Format
import components.a2l.classes.data.FunctionList
import components.a2l.classes.data.IfDataList
import components.a2l.classes.data.MapList
import components.a2l.classes.data.MatrixDim
import components.a2l.classes.data.MaxRefresh
import components.a2l.classes.data.MemorySegmentReference
import components.a2l.classes.data.PhysUnit
import components.a2l.classes.data.StepSize
import components.a2l.classes.data.SymbolLink
import components.a2l.classes.data.VirtCharacteristic
import components.a2l.classes.enums.CharacteristicType
import components.a2l.classes.enums.Deposit
import components.calibrationdata.classes.AxisDataType
import components.helpers.checkItemsAre
import components.a2l.classes.enums.ByteOrder
import components.a2l.objects.A2lOptional
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
) {
    fun isNotEmpty(): Boolean {
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
    fun applyBitMask(input: Number): Double {
        val result = input.toInt().toULong() and this.getBitMask()
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
    fun getAnnotations(): List<Annotation> {
        return if (this.optionalParameters.containsKey(CharOpt.Annotations)) {
            val result = this.optionalParameters[CharOpt.Annotations] as? AnnotList ?: AnnotList()
            result.annot
        } else listOf()
    }

    /**
     * Checks and Returns the BitMask Value if set
     */
    fun getBitMask(): ULong {
        return if (this.optionalParameters.containsKey(CharOpt.BitMask)) {
            val res = this.optionalParameters[CharOpt.BitMask] as? BitMask ?: BitMask()
            res.mask
        } else ULong.MIN_VALUE
    }

    /**
     * Checks and Returns the ByteOrder if set
     */
    fun getByteOrder(): ByteOrder {
        return if (this.optionalParameters.containsKey(CharOpt.ByteOrder)) {
            this.optionalParameters[CharOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
        } else ByteOrder.LITTLE_ENDIAN
    }

    /**
     * Checks and Returns true if Calibration Access is set
     */
    fun getCalibrationAccess(): Boolean {
        return this.optionalParameters.containsKey(CharOpt.CalibrationAccess)
    }

    /**
     * Checks and Returns the Comparison Quantity if set
     */
    fun getComparisonQuantity(): String {
        return if (this.optionalParameters.containsKey(CharOpt.ComparisonQuantity)) {
            val res = this.optionalParameters[CharOpt.ComparisonQuantity] as? ComparisonQuantity ?: ComparisonQuantity()
            res.quantity
        } else ""
    }

    /**
     * Checks and Returns the Conversion Type if set
     */
    fun getConversionType(): String {
        return if (this.optionalParameters.containsKey(CharOpt.ConversionType)) {
            val res = this.optionalParameters[CharOpt.ConversionType] as? ConversionType ?: ConversionType()
            res.type
        } else ""
    }

    /**
     * Checks and Returns the Dependant Characteristics if set
     */
    fun getDependantCharacteristic(): List<String> {
        return if (this.optionalParameters.containsKey(CharOpt.DependantCharacteristic)) {
            val result = this.optionalParameters[CharOpt.DependantCharacteristic] as? DependantCharacteristic ?: DependantCharacteristic()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns the Deposit Structure if set
     */
    fun getDeposit(): Deposit {
        return if (this.optionalParameters.containsKey(CharOpt.Deposit)) {
            this.optionalParameters[CharOpt.Deposit] as? Deposit ?: Deposit.NOT_SET
        } else Deposit.NOT_SET
    }

    /**
     * Returns the Dimension Array
     */

    fun getDimArray(): Array<Int> {
        val mNumber = this.getNumber()
        val mMatrixDim = this.getMatrixDim()

        return if (mMatrixDim != MatrixDim()) {
            mMatrixDim.asArray()
        } else {
            arrayOf(mNumber)
        }
    }

    /**
     * Checks and Returns true if Discrete is set
     */
    fun isDiscrete(): Boolean {
        return this.optionalParameters.containsKey(CharOpt.Discrete)
    }

    /**
     * Checks and Returns the Display Identifier if set
     */
    fun getDisplayIdentifier(): String {
        return if (this.optionalParameters.containsKey(CharOpt.DisplayIdentifier)) {
            val res = this.optionalParameters[CharOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
            res.id
        } else ""
    }

    /**
     * Checks and Returns the Ecu Address Extension if set
     */
    fun getEcuAddressExtension(): String {
        return if (this.optionalParameters.containsKey(CharOpt.EcuAddressExtension)) {
            val res = this.optionalParameters[CharOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
            res.address
        } else ""
    }

    /**
     * Checks andReturns the Extended Limits if set
     */
    fun getExtendedLimits(): ExtendedLimits {
        return if (this.optionalParameters.containsKey(CharOpt.ExtendedLimits)) {
            this.optionalParameters[CharOpt.ExtendedLimits] as? ExtendedLimits ?: ExtendedLimits()
        } else ExtendedLimits()
    }

    /**
     * Checks and Returns the Format if set
     */
    fun getFormat(): String {
        return if (this.optionalParameters.containsKey(CharOpt.Format)) {
            val res = this.optionalParameters[CharOpt.Format] as? Format ?: Format()
            res.value
        } else ""
    }

    /**
     * Checks and Returns a list of Functions if set
     */
    fun getFunctionList(): List<String> {
        return if (this.optionalParameters.containsKey(CharOpt.FunctionList)) {
            val result = this.optionalParameters[CharOpt.FunctionList] as? FunctionList ?: FunctionList()
            result.list
        } else listOf()
    }

    /**
     * Returns True if GuardRails is set
     */
    fun getGuardRails(): Boolean {
        return this.optionalParameters.containsKey(CharOpt.GuardRails)
    }

    /**
     * Checks and Returns any IfData if set
     */
    fun getIfData(): List<IfData> {
        return if (this.optionalParameters.containsKey(CharOpt.IfData)) {
            val result = this.optionalParameters[CharOpt.IfData] as? IfDataList ?: IfDataList()
            // Check if items are correct type
            result.ifData
        } else listOf()
    }

    /**
     * Checks and Returns the MapList if set
     */
    fun getMapList(): List<String> {
        return if (this.optionalParameters.containsKey(CharOpt.MapList)) {
            val result = this.optionalParameters[CharOpt.MapList] as? MapList ?: MapList()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns the MatrixDim if set
     */
    fun getMatrixDim(): MatrixDim {
        return if (this.optionalParameters.containsKey(CharOpt.MatrixDim)) {
            this.optionalParameters[CharOpt.MatrixDim] as? MatrixDim ?: MatrixDim()
        } else MatrixDim()
    }

    /**
     * Checks and Returns the Max Refresh if set
     */
    fun getMaxRefresh(): MaxRefresh {
        return if (this.optionalParameters.containsKey(CharOpt.MaxRefresh)) {
            this.optionalParameters[CharOpt.MaxRefresh] as? MaxRefresh ?: MaxRefresh()
        } else MaxRefresh()
    }

    /**
     * Checks and Returns the Number if set
     */
    fun getNumber(): Int {
        return if (this.optionalParameters.containsKey(CharOpt.Number)) {
            val res = this.optionalParameters[CharOpt.Number] as? CharNum ?: CharNum()
            res.num
        } else if (this.optionalParameters.containsKey(CharOpt.MatrixDim)) {
            this.getMatrixDim().xDim
        } else Int.MIN_VALUE
    }

    /**
     * Checks and Returns Phys Unit if set
     */
    fun getPhysUnit(): String {
        return if (this.optionalParameters.containsKey(CharOpt.PhysUnit)) {
            val res = this.optionalParameters[CharOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
            res.unit
        } else ""
    }

    /**
     * Checks and Returns True if Axis is Read Only
     */
    fun isReadOnly(): Boolean {
        return this.optionalParameters.containsKey(CharOpt.ReadOnly)
    }

    /**
     * Checks and Returns the Record Layout used in this Characteristic
     */
    fun getRecordLayout(a2lData: A2lData): RecordLayout {
        return if (a2lData.recordLayouts.containsKey(this.refRecordLayout)) {
            a2lData.recordLayouts[this.refRecordLayout] ?: RecordLayout()
        } else RecordLayout()
    }

    /**
     * Checks and Returns the CompuMethod used in this Characteristic
     */
    fun getCompuMethod(a2lData: A2lData): CompuMethod {
        return if (a2lData.compuMethods.containsKey(this.conversion)) {
            a2lData.compuMethods[this.conversion] ?: CompuMethod()
        } else CompuMethod()
    }

    /**
     * Checks and Returns The Reference Memory Segment if set
     */
    fun getRefMemorySegment(a2lData: A2lData): MemorySegment {
        return if (this.optionalParameters.containsKey(CharOpt.RefMemorySegment)) {
            val mRefMemorySegment = this.optionalParameters[CharOpt.RefMemorySegment] as? MemorySegmentReference ?: MemorySegmentReference()

            if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
                return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
            } else MemorySegment()
        } else MemorySegment()
    }

    /**
     * Checks and Returns the Step Size if set
     */
    fun getStepSize(): Float {
        return if (this.optionalParameters.containsKey(CharOpt.StepSize)) {
            val res = this.optionalParameters[CharOpt.StepSize] as? StepSize ?: StepSize()
            res.size
        } else Float.NaN
    }

    /**
     * Checks and Returns the SymbolLink if set
     */
    fun getSymbolLink(): SymbolLink {
        return if (this.optionalParameters.containsKey(CharOpt.SymbolLink)) {
            this.optionalParameters[CharOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
        } else SymbolLink()
    }

    /**
     * Checks and Returns the VirtualCharacteristics if set
     */
    fun getVirtualCharacteristic(): List<String> {
        return if (this.optionalParameters.containsKey(CharOpt.VirtualCharacteristic)) {
            val result = this.optionalParameters[CharOpt.VirtualCharacteristic] as? VirtCharacteristic ?: VirtCharacteristic()
            result.list
        } else listOf()
    }

    /**
     * Checks for all AxisDescr contained in the Characteristic and returns a map of them
     */
    fun getAxisDescrMap(): Map<AxisDataType, AxisDescr> {
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
    fun getAxisDescrX(): AxisDescr {
        return if (this.optionalParameters.containsKey(CharOpt.AXIS_DESC_X)) {
            this.optionalParameters[CharOpt.AXIS_DESC_X] as? AxisDescr ?: AxisDescr()
        } else AxisDescr()
    }

    /**
     * Checks and Return Characteristic Axis Description Y if set
     */
    fun getAxisDescrY(): AxisDescr {
        return if (this.optionalParameters.containsKey(CharOpt.AXIS_DESC_Y)) {
            this.optionalParameters[CharOpt.AXIS_DESC_Y] as? AxisDescr ?: AxisDescr()
        } else AxisDescr()
    }

    /**
     * Checks and Return Characteristic Axis Description Z if set
     */
    fun getAxisDescrZ(): AxisDescr {
        return if (this.optionalParameters.containsKey(CharOpt.AXIS_DESC_Z)) {
            this.optionalParameters[CharOpt.AXIS_DESC_Z] as? AxisDescr ?: AxisDescr()
        } else AxisDescr()
    }

    /**
     * Checks and Return Characteristic Axis Description 4 if set
     */
    fun getAxisDescr4(): AxisDescr {
        return if (this.optionalParameters.containsKey(CharOpt.AXIS_DESC_4)) {
            this.optionalParameters[CharOpt.AXIS_DESC_4] as? AxisDescr ?: AxisDescr()
        } else AxisDescr()
    }

    /**
     * Checks and Return Characteristic Axis Description 5 if set
     */
    fun getAxisDescr5(): AxisDescr {
        return if (this.optionalParameters.containsKey(CharOpt.AXIS_DESC_5)) {
            this.optionalParameters[CharOpt.AXIS_DESC_5] as? AxisDescr ?: AxisDescr()
        } else AxisDescr()
    }
}