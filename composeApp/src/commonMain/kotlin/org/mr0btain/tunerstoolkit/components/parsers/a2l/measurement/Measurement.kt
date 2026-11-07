package org.mr0btain.tunerstoolkit.components.parsers.a2l.measurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.memorysegment.MemorySegment
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AnnotList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Annotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ArraySize
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.BitMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MatrixDim
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MaxRefresh
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SymbolLink
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.BitOperation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DisplayIdentifier
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.EcuAddress
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.EcuAddressExtension
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.ErrorMask
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Format
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FunctionList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.IfDataList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.MemorySegmentReference
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.PhysUnit
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.VirtualList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.DataType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.IndexMode
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.ByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
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
)

fun Measurement.validate(): Boolean {
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
fun Measurement.getAnnotations(): List<Annotation> {
    return if (optionalParameters.containsKey(MeasOpt.Annotation)) {
        val result = optionalParameters[MeasOpt.Annotation] as? AnnotList ?: AnnotList()
        result.annot
    } else listOf()
}

/**
 * Checks and Returns the Array Size
 */
fun Measurement.getArraySize(): UInt {
    return if (optionalParameters.containsKey(MeasOpt.ArraySize)) {
        val res = optionalParameters[MeasOpt.ArraySize] as? ArraySize ?: ArraySize()
        res.size
    } else UInt.MIN_VALUE
}

/**
 * Checks and Returns the BitMask Value if set
 */
fun Measurement.getBitMask(): ULong {
    return if (optionalParameters.containsKey(MeasOpt.BitMask)) {
        val res = optionalParameters[MeasOpt.BitMask] as? BitMask ?: BitMask()
        res.mask
    } else ULong.MIN_VALUE
}

/**
 * Checks and Returns a list of BitOperations
 */
fun Measurement.getBitOperations(): BitOperation {
    return if (optionalParameters.containsKey(MeasOpt.BitOperations)) {
        optionalParameters[MeasOpt.BitOperations] as? BitOperation ?: BitOperation()
    } else BitOperation()
}

/**
 * Checks and Returns the ByteOrder if set
 */
fun Measurement.getByteOrder(): ByteOrder {
    return if (optionalParameters.containsKey(MeasOpt.ByteOrder)) {
        optionalParameters[MeasOpt.ByteOrder] as? ByteOrder ?: ByteOrder.LITTLE_ENDIAN
    } else ByteOrder.LITTLE_ENDIAN
}

/**
 * Checks and Returns true if Discrete is set
 */
fun Measurement.isDiscrete(): Boolean {
    return optionalParameters.containsKey(MeasOpt.Discrete)
}

/**
 * Checks and Returns the Display Identifier if set
 */
fun Measurement.getDisplayIdentifier(): String {
    return if (optionalParameters.containsKey(MeasOpt.DisplayIdentifier)) {
        val res = optionalParameters[MeasOpt.DisplayIdentifier] as? DisplayIdentifier ?: DisplayIdentifier()
        res.id
    } else ""
}

/**
 * Checks and Returns the ECU Address if set
 */
fun Measurement.getEcuAddress(): ULong {
    return if (optionalParameters.containsKey(MeasOpt.EcuAddress)) {
        val res = optionalParameters[MeasOpt.EcuAddress] as? EcuAddress ?: EcuAddress()
        res.address
    } else ULong.MIN_VALUE
}

/**
 * Checks and Returns the Ecu Address Extension if set
 */
fun Measurement.getEcuAddressExtension(): String {
    return if (optionalParameters.containsKey(MeasOpt.EcuAddressExtension)) {
        val res = optionalParameters[MeasOpt.EcuAddressExtension] as? EcuAddressExtension ?: EcuAddressExtension()
        res.address
    } else ""
}

/**
 * Checks and Returns the Error Mask if set
 */
fun Measurement.getErrorMask(): ULong {
    return if (optionalParameters.containsKey(MeasOpt.ErrorMask)) {
        val res = optionalParameters[MeasOpt.ErrorMask] as? ErrorMask ?: ErrorMask()
        res.mask
    } else ULong.MIN_VALUE
}

/**
 * Checks and Returns the Format if set
 */
fun Measurement.getFormat(): String {
    return if (optionalParameters.containsKey(MeasOpt.Format)) {
        val res = optionalParameters[MeasOpt.Format] as? Format ?: Format()
        res.value
    } else ""
}

/**
 * Checks and Returns a list of Functions if set
 */
fun Measurement.getFunctionList(): List<String> {
    return if (optionalParameters.containsKey(MeasOpt.FunctionList)) {
        val result = optionalParameters[MeasOpt.FunctionList] as? FunctionList ?: FunctionList()
        // Check is items are correct type
        result.list
    } else listOf()
}

/**
 * Checks and Returns any IfData if set
 */
fun Measurement.getIfData(): List<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData> {
    return if (optionalParameters.containsKey(MeasOpt.IfData)) {
        val result = optionalParameters[MeasOpt.IfData] as? IfDataList ?: IfDataList()
        // Check if items are correct type
        result.ifData
    } else listOf()
}

/**
 * Checks and Returns the Layout if set
 */
fun Measurement.getLayout(): IndexMode {
    return if (optionalParameters.containsKey(MeasOpt.Layout)) {
        optionalParameters[MeasOpt.Layout] as? IndexMode ?: IndexMode.NOT_SET
    } else IndexMode.NOT_SET
}

/**
 * Checks and Returns the MatrixDim if set
 */
fun Measurement.getMatrixDim(): MatrixDim {
    return if (optionalParameters.containsKey(MeasOpt.MatrixDim)) {
        optionalParameters[MeasOpt.MatrixDim] as? MatrixDim ?: MatrixDim()
    } else MatrixDim()
}

/**
 * Checks and Returns the Max Refresh if set
 */
fun Measurement.getMaxRefresh(): MaxRefresh {
    return if (optionalParameters.containsKey(MeasOpt.MaxRefresh)) {
        optionalParameters[MeasOpt.MaxRefresh] as? MaxRefresh ?: MaxRefresh()
    } else MaxRefresh()
}

/**
 * Checks and Returns Phys Unit if set
 */
fun Measurement.getPhysUnit(): String {
    return if (optionalParameters.containsKey(MeasOpt.PhysUnit)) {
        val res = optionalParameters[MeasOpt.PhysUnit] as? PhysUnit ?: PhysUnit()
        res.unit
    } else ""
}

/**
 * Checks and Returns if ReadWrite is enabled if set
 */
fun Measurement.isReadWrite(): Boolean {
    return optionalParameters.containsKey(MeasOpt.ReadWrite)
}

/**
 * Checks and Returns The Reference Memory Segment if set
 */
fun Measurement.getRefMemorySegment(a2lData: A2lData): MemorySegment {
    return if (optionalParameters.containsKey(MeasOpt.RefMemorySegment)) {
        val mRefMemorySegment = optionalParameters[MeasOpt.RefMemorySegment] as? MemorySegmentReference
            ?: MemorySegmentReference()

        if (mRefMemorySegment.reference.isNotBlank() && a2lData.modPar.memorySegments.containsKey(mRefMemorySegment.reference)) {
            return a2lData.modPar.memorySegments[mRefMemorySegment.reference] ?: MemorySegment()
        } else MemorySegment()
    } else MemorySegment()
}

/**
 * Checks and Returns the SymbolLink if set
 */
fun Measurement.getSymbolLink(): SymbolLink {
    return if (optionalParameters.containsKey(MeasOpt.SymbolLink)) {
        optionalParameters[MeasOpt.SymbolLink] as? SymbolLink ?: SymbolLink()
    } else SymbolLink()
}

/**
 * Checks and Returns Virtual if set
 */
fun Measurement.getVirtual(): List<String> {
    return if (optionalParameters.containsKey(MeasOpt.Virtual)) {
        val result = optionalParameters[MeasOpt.Virtual] as? VirtualList ?: VirtualList()
        result.list
    } else listOf()
}