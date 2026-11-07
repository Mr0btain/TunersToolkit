package org.mr0btain.tunerstoolkit.components.parsers.a2l.modpar
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SystemConstant
import org.mr0btain.tunerstoolkit.components.parsers.a2l.memorysegment.MemoryLayout
import org.mr0btain.tunerstoolkit.components.parsers.a2l.memorysegment.MemorySegment
import kotlinx.serialization.Serializable

// TODO(Figure out an enum class for ECU, CPU_TYPE, and VERSION)

/**
 * Describes the management data to be specified for an device.
 * Except for the comment all parameters are optional.
 * @param name Unique description
 * @param addrEpk Address of EPROM identifier
 * @param calibrationMethod Declares the implemented calibration methods in
 * the control unit.
 * @param cpuType Type of cpu the data is from
 * @param customer Firm or customer
 * @param customerNumber Customer number
 * @param ecu Control unit
 * @param ecuCalibrationOffset Offset that has to be added to each address of a
 * characteristic.
 * @param ecuCalibrationOffsetSet Check to see if it was set or not
 * @param memoryLayout The layout of memory
 * @param memorySegments Declares the available memory segments.
 * @param noOfInterfaces Number of interfaces
 * @param phoneNumber Phone number of the calibration engineer
 * responsible
 * @param systemConstants System-defined constants used in calibration
 * @param supplier Manufacturer or supplier
 * @param version Version identifier
 */

@Serializable
data class ModPar(
    val name: String = "",
    val addrEpk: ULong = ULong.MIN_VALUE,
    val calibrationMethod: List<String> = listOf(),
    val cpuType: String = "",
    val customer: String = "",
    val customerNumber: String = "",
    val ecu: String = "",
    val ecuCalibrationOffset: ULong = ULong.MIN_VALUE, // TODO: Added to every characteristic
    val ecuCalibrationOffsetSet: Boolean = false,
    val epk: String = "",
    val memoryLayout: List<MemoryLayout> = listOf(),
    val memorySegments: Map<String, MemorySegment> = mapOf(),
    val noOfInterfaces: Int = Int.MIN_VALUE,
    val phoneNumber: String = "",
    val supplier: String = "",
    val systemConstants: Map<String, SystemConstant> = mapOf(),
    val user: String = "",
    val version: String = ""
)