package components.a2l.modpar

import components.a2l.classes.data.SystemConstant
import components.a2l.memorysegment.MemoryLayout
import components.a2l.memorysegment.MemorySegment
import components.a2l.memorysegment.MemorySegmentBuilder
import components.a2l.memorysegment.parseMemoryLayout

/**
 * Parses a list of strings and returns a ModPar with the parameters included if they exist
 */

class ModParBuilder {
    fun build(input: List<String>): ModPar {
        var mAddrEpk = ULong.MIN_VALUE
        val mCalibrationMethod = mutableListOf<String>()
        var mCpuType = ""
        var mCustomer = ""
        var mCustomerNumber = ""
        var mEcu = ""
        var mEcuCalibrationOffset = ULong.MIN_VALUE // TODO: Added to every characteristic
        var mEcuCalibrationOffsetSet = false
        var mEpk = ""
        val mMemoryLayout = mutableListOf<MemoryLayout>()
        val mMemorySegments = mutableMapOf<String, MemorySegment>()
        var mNoOfInterfaces = Int.MIN_VALUE
        var mPhoneNumber = ""
        var mSupplier = ""
        val mSystemConstants = mutableMapOf<String, SystemConstant>()
        var mUser = ""
        var mVersion = ""

        input.forEachIndexed { index, line ->
            when {
                line.startsWith("ADDR_EPK") -> {
                    val result = line.replace("ADDR_EPK", "").trim().replace("0x", "").toULongOrNull(16)
                    if (result != null) {
                        mAddrEpk = result
                    }
                }

                line.startsWith("CALIBRATION_METHOD") -> {
                    val result = line.replace(" CALIBRATION_METHOD", "").trim().split(" ")
                    result.forEach {
                        mCalibrationMethod.add(it)
                    }
                }

                line.startsWith("CPU_TYPE") -> {
                    mCpuType = line.replace("CPU_TYPE", "")
                        .replace("\"", "").trim()
                }

                line.startsWith("CUSTOMER") -> {
                    mCustomer = line.replace("CUSTOMER", "")
                        .replace("\"", "").trim()
                }

                line.startsWith("CUSTOMER_NO") -> {
                    mCustomerNumber = line.replace("CUSTOMER_NO", "")
                        .replace("\"", "").trim()
                }

                line.startsWith("CUSTOMER_NUMBER") -> {
                    mCustomerNumber = line.replace("CUSTOMER_NUMBER", "")
                        .replace("\"", "").trim()
                }

                line.startsWith("ECU") -> {
                    mEcu = line.replace("ECU", "")
                        .replace("\"", "").trim()
                }

                line.startsWith("ECU_CALIBRATION_OFFSET") -> {
                    val result = line.replace("ECU_CALIBRATION_OFFSET", "").trim().toULongOrNull()
                    if (result != null) {
                        mEcuCalibrationOffset = result
                        mEcuCalibrationOffsetSet = true
                    }
                }

                line.startsWith("EPK") -> {
                    mEpk = line.replace("EPK", "")
                    .replace("\"", "").trim()
                }

                line.startsWith("/begin") -> {
                    when {
                        line.contains("MEMORY_LAYOUT") -> {
                            val subList = input.subList(index, input.size)
                            var end = 0

                            subList.forEachIndexed { ind, lin ->
                                if (lin.startsWith("/end MEMORY_LAYOUT") && end == 0) {
                                    end = ind
                                }
                            }
                            val mMemLayout = subList.subList(0, end).parseMemoryLayout()
                            if (mMemLayout != MemoryLayout()) {
                                mMemoryLayout.add(mMemLayout)
                            }
                        }

                        line.contains("MEMORY_SEGMENT") -> {
                            val subList = input.subList(index, input.size)
                            var end = 0

                            subList.forEachIndexed { ind, lin ->
                                if (lin.startsWith("/end MEMORY_SEGMENT") && end == 0) {
                                    end = ind
                                }
                            }
                            val mMemSegList = subList.subList(0, end)
                            val mMemSeg = MemorySegmentBuilder().build(mMemSegList)
                            if (mMemSeg != MemorySegment()) {
                                mMemorySegments[mMemSeg.name] = mMemSeg
                            }
                        }
                    }
                }

                line.startsWith("NO_OF_INTERFACES") -> {
                    val result = line.replace("NO_OF_INTERFACES", "").trim().toIntOrNull()
                    if (result != null) {
                        mNoOfInterfaces = result
                    }
                }

                line.startsWith("PHONE_NO") -> {
                    mPhoneNumber = line.replace("PHONE_NO", "").trim()
                }

                line.startsWith("SUPPLIER") -> {
                    mSupplier = line.replace("SUPPLIER", "").trim()
                }

                line.startsWith("USER") -> {
                    mUser = line.replace("USER", "").trim()
                }

                line.startsWith("VERSION") -> {
                    mVersion = line.replace("VERSION", "").trim()
                }

                // If it's a System Constant format and add it
                (line.startsWith("SYSTEM_CONSTANT")) -> {
                    val data = line.replace("SYSTEM_CONSTANT", "").trim().split(" ")
                    if (data.size >= 2) {
                        val b = data[1].toDoubleOrNull()
                        if (b != null) {
                            mSystemConstants[data[0]] = SystemConstant( data[0], b )
                        }
                    }
                }
            }
        }


        return ModPar(
            name = input[0].replace("/begin MOD_PAR", "").replace("\"", "").trim(),
            addrEpk = mAddrEpk,
            calibrationMethod = mCalibrationMethod,
            cpuType = mCpuType,
            customer = mCustomer,
            customerNumber = mCustomerNumber,
            ecu = mEcu,
            ecuCalibrationOffset = mEcuCalibrationOffset,
            ecuCalibrationOffsetSet = mEcuCalibrationOffsetSet,
            epk = mEpk,
            memoryLayout = mMemoryLayout,
            memorySegments = mMemorySegments,
            noOfInterfaces = mNoOfInterfaces,
            phoneNumber = mPhoneNumber,
            supplier = mSupplier,
            systemConstants = mSystemConstants,
            user = mUser,
            version = mVersion,
        )
    }
}