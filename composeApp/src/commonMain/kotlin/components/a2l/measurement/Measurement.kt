package components.a2l.measurement

import components.a2l.ifdata.IfData
import components.a2l.memorysegment.MemorySegment
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.AnnotList
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.ArraySize
import components.a2l.classes.data.BitMask
import components.a2l.classes.data.MatrixDim
import components.a2l.classes.data.MaxRefresh
import components.a2l.classes.data.SymbolLink
import components.a2l.classes.data.BitOperation
import components.a2l.classes.data.DisplayIdentifier
import components.a2l.classes.data.EcuAddress
import components.a2l.classes.data.EcuAddressExtension
import components.a2l.classes.data.ErrorMask
import components.a2l.classes.data.Format
import components.a2l.classes.data.FunctionList
import components.a2l.classes.data.IfDataList
import components.a2l.classes.data.MemorySegmentReference
import components.a2l.classes.data.PhysUnit
import components.a2l.classes.data.VirtualList
import components.a2l.classes.enums.DataType
import components.a2l.classes.enums.IndexMode
import components.helpers.checkItemsAre
import components.a2l.classes.enums.ByteOrder
import components.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * The MEASUREMENT keyword is used to describe the parameters for the processing
 * of a measurement object.
 */

@Serializable
data class Measurement(
    val name: String = "",
    val longIdentifier: String = "",
    val dataType: DataType = DataType.NOT_SET,
    val conversion: String = "",
    val resolution: UInt = UInt.MIN_VALUE,
    val accuracy: Double = Double.NaN,
    val lowerLimit: Double = Double.NaN,
    val upperLimit: Double = Double.NaN,
    val optionalParameters: Map<MeasOpt, A2lOptional> = mapOf()
) {
    fun isNotEmpty(): Boolean {
        return when {
            name.isBlank() -> false
            dataType == DataType.NOT_SET -> false
            conversion.isBlank() -> false
            else -> true
        }
    }

    /**
     * Checks and Returns a list of Annotations if set
     */
    fun getAnnotations(): List<Annotation> {
        return if (this.optionalParameters.containsKey(MeasOpt.Annotation)) {
            val result = this.optionalParameters[MeasOpt.Annotation] as? AnnotList ?: AnnotList()
            result.annot
        } else listOf()
    }

    /**
     * Checks and Returns the Array Size
     */
    fun getArraySize(): UInt {
        return if (this.optionalParameters.containsKey(MeasOpt.ArraySize)) {
            val res = this.optionalParameters[MeasOpt.ArraySize] as? ArraySize ?: ArraySize()
            res.size
        } else UInt.MIN_VALUE
    }

    /**
     * Checks and Returns the BitMask Value if set
     */
    fun getBitMask(): ULong {
        return if (this.optionalParameters.containsKey(MeasOpt.BitMask)) {
            val res = this.optionalParameters[MeasOpt.BitMask] as? BitMask ?: BitMask()
            res.mask
        } else ULong.MIN_VALUE
    }

    /**
     * Checks and Returns a list of BitOperations
     */
    fun getBitOperations(): BitOperation {
        return if (this.optionalParameters.containsKey(MeasOpt.BitOperations)) {
            this.optionalParameters[MeasOpt.BitOperations] as? BitOperation ?: BitOperation()
        } else BitOperation()
    }

    /**
     * Checks and Returns the ByteOrder if set
     */
    fun getByteOrder(): ByteOrder {
        return if (this.optionalParameters.containsKey(MeasOpt.ByteOrder)) {
            this.optionalParameters[MeasOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
        } else ByteOrder.LITTLE_ENDIAN
    }

    /**
     * Checks and Returns true if Discrete is set
     */
    fun isDiscrete(): Boolean {
        return this.optionalParameters.containsKey(MeasOpt.Discrete)
    }

    /**
     * Checks and Returns the Display Identifier if set
     */
    fun getDisplayIdentifier(): String {
        return if (this.optionalParameters.containsKey(MeasOpt.DisplayIdentifier)) {
            val res = this.optionalParameters[MeasOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
            res.id
        } else ""
    }

    /**
     * Checks and Returns the ECU Address if set
     */
    fun getEcuAddress(): ULong {
        return if (this.optionalParameters.containsKey(MeasOpt.EcuAddress)) {
            val res = this.optionalParameters[MeasOpt.EcuAddress] as? EcuAddress ?: EcuAddress()
            res.address
        } else ULong.MIN_VALUE
    }

    /**
     * Checks and Returns the Ecu Address Extension if set
     */
    fun getEcuAddressExtension(): String {
        return if (this.optionalParameters.containsKey(MeasOpt.EcuAddressExtension)) {
            val res = this.optionalParameters[MeasOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
            res.address
        } else ""
    }

    /**
     * Checks and Returns the Error Mask if set
     */
    fun getErrorMask(): ULong {
        return if (this.optionalParameters.containsKey(MeasOpt.ErrorMask)) {
            val res = this.optionalParameters[MeasOpt.ErrorMask] as? ErrorMask ?: ErrorMask()
            res.mask
        } else ULong.MIN_VALUE
    }

    /**
     * Checks and Returns the Format if set
     */
    fun getFormat(): String {
        return if (this.optionalParameters.containsKey(MeasOpt.Format)) {
            val res = this.optionalParameters[MeasOpt.Format] as? Format ?: Format()
            res.value
        } else ""
    }

    /**
     * Checks and Returns a list of Functions if set
     */
    fun getFunctionList(): List<String> {
        return if (this.optionalParameters.containsKey(MeasOpt.FunctionList)) {
            val result = this.optionalParameters[MeasOpt.FunctionList] as? FunctionList ?: FunctionList()
            // Check is items are correct type
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns any IfData if set
     */
    fun getIfData(): List<IfData> {
        return if (this.optionalParameters.containsKey(MeasOpt.IfData)) {
            val result = this.optionalParameters[MeasOpt.IfData] as? IfDataList ?: IfDataList()
            // Check if items are correct type
            result.ifData
        } else listOf()
    }

    /**
     * Checks and Returns the Layout if set
     */
    fun getLayout(): IndexMode {
        return if (this.optionalParameters.containsKey(MeasOpt.Layout)) {
            this.optionalParameters[MeasOpt.Layout] as? IndexMode ?: IndexMode.NOT_SET
        } else IndexMode.NOT_SET
    }

    /**
     * Checks and Returns the MatrixDim if set
     */
    fun getMatrixDim(): MatrixDim {
        return if (this.optionalParameters.containsKey(MeasOpt.MatrixDim)) {
            this.optionalParameters[MeasOpt.MatrixDim] as? MatrixDim ?: MatrixDim()
        } else MatrixDim()
    }

    /**
     * Checks and Returns the Max Refresh if set
     */
    fun getMaxRefresh(): MaxRefresh {
        return if (this.optionalParameters.containsKey(MeasOpt.MaxRefresh)) {
            this.optionalParameters[MeasOpt.MaxRefresh] as? MaxRefresh ?: MaxRefresh()
        } else MaxRefresh()
    }

    /**
     * Checks and Returns Phys Unit if set
     */
    fun getPhysUnit(): String {
        return if (this.optionalParameters.containsKey(MeasOpt.PhysUnit)) {
            val res = this.optionalParameters[MeasOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
            res.unit
        } else ""
    }

    /**
     * Checks and Returns if ReadWrite is enabled if set
     */
    fun isReadWrite(): Boolean {
        return this.optionalParameters.containsKey(MeasOpt.ReadWrite)
    }

    /**
     * Checks and Returns The Reference Memory Segment if set
     */
    fun getRefMemorySegment(a2lData: A2lData): MemorySegment {
        return if (this.optionalParameters.containsKey(MeasOpt.RefMemorySegment)) {
            val mRefMemorySegment = this.optionalParameters[MeasOpt.RefMemorySegment] as? MemorySegmentReference ?: MemorySegmentReference()

            if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
                return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
            } else MemorySegment()
        } else MemorySegment()
    }

    /**
     * Checks and Returns the SymbolLink if set
     */
    fun getSymbolLink(): SymbolLink {
        return if (this.optionalParameters.containsKey(MeasOpt.SymbolLink)) {
            this.optionalParameters[MeasOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
        } else SymbolLink()
    }

    /**
     * Checks and Returns Virtual if set
     */
    fun getVirtual(): List<String> {
        return if (this.optionalParameters.containsKey(MeasOpt.Virtual)) {
            val result = this.optionalParameters[MeasOpt.Virtual] as? VirtualList ?: VirtualList()
            result.list
        } else listOf()
    }
}