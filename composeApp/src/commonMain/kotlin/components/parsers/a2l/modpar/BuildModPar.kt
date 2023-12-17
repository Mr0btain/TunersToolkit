package components.parsers.a2l.modpar

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.classes.data.SystemConstant
import components.parsers.a2l.memorysegment.MemoryLayout
import components.parsers.a2l.memorysegment.MemorySegment
import components.parsers.a2l.memorysegment.buildMemorySegment
import components.parsers.a2l.memorysegment.parseMemoryLayout

/**
 * Parses a list of strings and returns a ModPar with the parameters included if they exist
 */

fun buildModPar(input: List<String>): Pair<ModPar, List<LogDataEntry>> {
    val process = "BuildModPar"
    val errors = mutableListOf<LogDataEntry>()
    var addrEpk = ULong.MIN_VALUE
    val calibrationMethod = mutableListOf<String>()
    var cpuType = ""
    var customer = ""
    var customerNumber = ""
    var ecu = ""
    var ecuCalibrationOffset = ULong.MIN_VALUE // TODO: Added to every characteristic
    var ecuCalibrationOffsetSet = false
    var epk = ""
    val memoryLayout = mutableListOf<MemoryLayout>()
    val memorySegments = mutableMapOf<String, MemorySegment>()
    var noOfInterfaces = Int.MIN_VALUE
    var phoneNumber = ""
    var supplier = ""
    val systemConstants = mutableMapOf<String, SystemConstant>()
    var user = ""
    var version = ""

    input.forEachIndexed { index, line ->
        when {
            line.startsWith("ADDR_EPK") -> {
                val result = line.replace("ADDR_EPK", "").trim().replace("0x", "").toULongOrNull(16)
                if (result != null) {
                    addrEpk = result
                }
            }

            line.startsWith("CALIBRATION_METHOD") -> {
                val result = line.replace(" CALIBRATION_METHOD", "").trim().split(" ")
                result.forEach {
                    calibrationMethod.add(it)
                }
            }

            line.startsWith("CPU_TYPE") -> {
                cpuType = line.replace("CPU_TYPE", "")
                    .replace("\"", "").trim()
            }

            line.startsWith("CUSTOMER") -> {
                customer = line.replace("CUSTOMER", "")
                    .replace("\"", "").trim()
            }

            line.startsWith("CUSTOMER_NO") -> {
                customerNumber = line.replace("CUSTOMER_NO", "")
                    .replace("\"", "").trim()
            }

            line.startsWith("CUSTOMER_NUMBER") -> {
                customerNumber = line.replace("CUSTOMER_NUMBER", "")
                    .replace("\"", "").trim()
            }

            line.startsWith("ECU") -> {
                ecu = line.replace("ECU", "")
                    .replace("\"", "").trim()
            }

            line.startsWith("ECU_CALIBRATION_OFFSET") -> {
                val result = line.replace("ECU_CALIBRATION_OFFSET", "").trim().toULongOrNull()
                if (result != null) {
                    ecuCalibrationOffset = result
                    ecuCalibrationOffsetSet = true
                }
            }

            line.startsWith("EPK") -> {
                epk = line.replace("EPK", "")
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
                        val (memLayout, error) = subList.subList(0, end).parseMemoryLayout()

                        if (error.isEmpty()) {
                            memoryLayout.add(memLayout)
                        } else errors.addAll(error)
                    }

                    line.contains("MEMORY_SEGMENT") -> {
                        val subList = input.subList(index, input.size)
                        var end = 0

                        subList.forEachIndexed { ind, lin ->
                            if (lin.startsWith("/end MEMORY_SEGMENT") && end == 0) {
                                end = ind
                            }
                        }
                        val memSegList = subList.subList(0, end)
                        val (memSeg, error) = buildMemorySegment(memSegList)

                        if (error.isEmpty()) {
                            memorySegments[memSeg.name] = memSeg
                        } else errors.addAll(error)
                    }
                }
            }

            line.startsWith("NO_OF_INTERFACES") -> {
                safeCall {
                    noOfInterfaces = line.replace("NO_OF_INTERFACES", "").trim().toInt()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add NumberOfInterfaces: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }

            }

            line.startsWith("PHONE_NO") -> {
                phoneNumber = line.replace("PHONE_NO", "").trim()
            }

            line.startsWith("SUPPLIER") -> {
                supplier = line.replace("SUPPLIER", "").trim()
            }

            line.startsWith("USER") -> {
                user = line.replace("USER", "").trim()
            }

            line.startsWith("VERSION") -> {
                version = line.replace("VERSION", "").trim()
            }

            // If it's a System Constant format and add it
            (line.startsWith("SYSTEM_CONSTANT")) -> {
                safeCall {
                    val data = line.replace("SYSTEM_CONSTANT", "")
                        .replace("\"", "")
                        .trim()
                        .split(" ")

                    systemConstants[data[0]] = SystemConstant(data[0], data[1].toDouble())
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add SystemConstant!: \n $line : \n ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }
        }
    }


    return Pair(
        ModPar(
            name = input[0].replace("/begin MOD_PAR", "").replace("\"", "").trim(),
            addrEpk = addrEpk,
            calibrationMethod = calibrationMethod,
            cpuType = cpuType,
            customer = customer,
            customerNumber = customerNumber,
            ecu = ecu,
            ecuCalibrationOffset = ecuCalibrationOffset,
            ecuCalibrationOffsetSet = ecuCalibrationOffsetSet,
            epk = epk,
            memoryLayout = memoryLayout,
            memorySegments = memorySegments,
            noOfInterfaces = noOfInterfaces,
            phoneNumber = phoneNumber,
            supplier = supplier,
            systemConstants = systemConstants,
            user = user,
            version = version,
        ),
        errors
    )
}