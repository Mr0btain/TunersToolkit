package org.mr0btain.tunerstoolkit.core.testing

import org.mr0btain.tunerstoolkit.core.checksum.ChecksumModules
import org.mr0btain.tunerstoolkit.core.checksum.Checksums
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * A made up engine controller, "TT-DEMO-1", for tests and the app's demo project: an image and an A2L describing it,
 * the same every run. Nothing here comes from a real ECU, so it's free to commit and share.
 *
 * The written copies are in testdata/demo (DemoFilesTest checks they match; run it with -DregenerateDemo=true to
 * rewrite them after changing this).
 *
 * Layout: a 32K flash segment at 0x00800000 (code, then calibration from 0x00804000) and a small boot segment at 0,
 * ME7-style 24 bit addresses: the app's current A2L decode breaks on addresses from 0x80000000 up (Fixtures.ecuLikeImage
 * covers those in core's own tests). Little endian (MSB_LAST). Maps are stored COLUMN_DIR: all of the first X point's Y values, then the
 * next X point's, so value (x, y) is at index x * yCount + y.
 *
 * It only uses what the app's current A2L parser reads (no floats yet); grow it as the new parser does.
 */
object DemoEcu {
    const val FLASH_START = 0x0080_0000L
    const val FLASH_SIZE = 0x8000
    const val CAL_START = 0x0080_4000L
    const val BOOT_START = 0x0000_0000L
    const val BOOT_SIZE = 0x100
    const val EPK_ADDRESS = 0x0080_0100L
    const val EPK = "TTDEMO1/0.1/2026-11-07"

    private val order = ByteOrder.LITTLE_ENDIAN

    /** A conversion from raw to physical: phys = raw * factor + offset */
    private class Conversion(
        val name: String,
        val unit: String,
        val format: String,
        val factor: Double,
        val offset: Double,
        val linear: Boolean = false
    ) {
        fun raw(physical: Double): Long = ((physical - offset) / factor).roundToLong()

        /** RAT_FUNC's coefficients go from physical to raw: raw = (b * phys + c) / f. LINEAR's from raw to physical */
        fun a2l(): String = buildString {
            appendLine("    /begin COMPU_METHOD")
            appendLine("        $name")
            appendLine("        \"\"")
            appendLine(if (linear) "        LINEAR" else "        RAT_FUNC")
            appendLine("        \"$format\"")
            appendLine("        \"$unit\"")
            val perUnit = 1 / factor
            when {
                linear -> appendLine("        COEFFS_LINEAR ${num(factor)} ${num(offset)}")
                // A whole number of raw steps per unit, like 32768 for 1/32768: raw = 32768 * phys
                perUnit == perUnit.roundToLong().toDouble() -> appendLine("        COEFFS 0 ${num(perUnit)} ${num(-offset * perUnit)} 0 0 1")
                else -> appendLine("        COEFFS 0 1 ${num(-offset)} 0 0 ${num(factor)}")
            }
            appendLine("    /end COMPU_METHOD")
        }
    }

    private val rpm = Conversion("rpm", "rpm", "%6.0", 1.0, 0.0)
    private val loadPct = Conversion("load_pct", "%", "%5.1", 0.75, 0.0)
    private val pedalPct = Conversion("pedal_pct", "%", "%5.1", 0.5, 0.0)
    private val tempC = Conversion("temp_c", "degC", "%5.1", 0.75, -48.0)
    private val ignDeg = Conversion("ign_deg", "deg", "%5.2", 0.375, -35.25)
    private val factorQ15 = Conversion("factor_q15", "-", "%7.5", 1.0 / 32768, 0.0)
    private val factorQ7 = Conversion("factor_q7", "-", "%6.4", 1.0 / 128, 0.0)
    private val pressureHpa = Conversion("pressure_hpa", "hPa", "%6.1", 0.1, 0.0, linear = true)
    private val torqueNm = Conversion("torque_nm", "Nm", "%5.0", 1.0, 0.0)
    private val microseconds = Conversion("time_us", "us", "%6.0", 1.0, 0.0)
    private val identity = Conversion("ident", "", "%5.0", 1.0, 0.0)

    /** TAB_VERB, its COMPU_METHOD and COMPU_VTAB are written on their own; raw is the value */
    private val onOff = Conversion("onoff", "", "%3.0", 1.0, 0.0)
    private val conversions = listOf(rpm, loadPct, pedalPct, tempC, ignDeg, factorQ15, factorQ7, pressureHpa, torqueNm, microseconds, identity)

    private enum class Type(val a2l: String, val size: Int, val signed: Boolean) {
        UBYTE("UBYTE", 1, false),
        UWORD("UWORD", 2, false),
        SWORD("SWORD", 2, true),
        ULONG("ULONG", 4, false)
    }

    /** Shared axes (AXIS_PTS) */
    val rpmAxis = listOf(600, 800, 1000, 1250, 1500, 1750, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6800).map(Int::toDouble)
    val loadAxis = listOf(10, 20, 30, 40, 50, 60, 70, 80, 100, 120, 140, 160).map(Int::toDouble)
    val pedalAxis = listOf(0, 5, 10, 20, 30, 40, 50, 60, 80, 100).map(Int::toDouble)
    val tempAxis = listOf(-30, -10, 0, 10, 20, 40, 60, 80, 90).map(Int::toDouble)

    /** What was written where, so tests can check a reader against it */
    data class Placed(val name: String, val address: Long, val values: List<Double>)

    class Demo(val image: MemoryImage, val a2l: String, val placed: Map<String, Placed>)

    fun build(): Demo {
        val random = Random(2026)
        val flash = ByteArray(FLASH_SIZE) { 0xFF.toByte() }
        // Something code-like before the calibration area
        for (i in 0 until (CAL_START - FLASH_START).toInt()) flash[i] = random.nextInt(256).toByte()
        EPK.encodeToByteArray().copyInto(flash, (EPK_ADDRESS - FLASH_START).toInt())
        val boot = ByteArray(BOOT_SIZE) { random.nextInt(256).toByte() }

        val image = MemoryImage.Builder().apply {
            put(FLASH_START, flash)
            put(BOOT_START, boot)
            startAddress = BOOT_START
        }.build().image

        val writer = Writer(image)
        val a2l = writer.a2l()
        // Real checksums, so the checksum view has something right to show (ChecksumModules.DEMO says where)
        val checksums = Checksums.fix(image, ChecksumModules.demo.checkList)
        check(checksums.allOk) { "The demo's checksums couldn't be made right" }
        return Demo(image.snapshot(), a2l, writer.placed)
    }

    /** Lays the calibration out and writes the A2L that describes it */
    private class Writer(val image: MemoryImage) {
        var next = CAL_START
        val placed = linkedMapOf<String, Placed>()
        val layouts = StringBuilder()
        val axisPts = StringBuilder()
        val characteristics = StringBuilder()
        val usedLayouts = mutableSetOf<String>()

        fun align(size: Int) {
            if (size > 1) next = (next + size - 1) / size * size
        }

        /** Writes [values] as [type] through [conversion] at the next address, returns where */
        fun put(type: Type, conversion: Conversion, values: List<Double>): Long {
            align(type.size)
            val start = next
            for (value in values) {
                val raw = conversion.raw(value)
                if (type.signed) image.writeSigned(next, type.size, raw, order)
                else image.writeUnsigned(next, type.size, raw.toULong(), order)
                next += type.size
            }
            return start
        }

        fun putCount(type: Type, count: Int): Long = put(type, identity, listOf(count.toDouble()))

        fun layout(name: String, body: String) {
            if (usedLayouts.add(name)) {
                layouts.appendLine("    /begin RECORD_LAYOUT $name")
                body.trimIndent().lines().forEach { layouts.appendLine("        $it") }
                layouts.appendLine("    /end RECORD_LAYOUT")
                layouts.appendLine()
            }
        }

        fun characteristic(name: String, description: String, kind: String, address: Long, layout: String,
                           conversion: Conversion, lower: Double, upper: Double, extra: String = "", axes: String = "") {
            characteristics.appendLine("    /begin CHARACTERISTIC")
            characteristics.appendLine("        $name")
            characteristics.appendLine("        \"$description\"")
            characteristics.appendLine("        $kind")
            characteristics.appendLine("        ${address.toA2lAddress()}")
            characteristics.appendLine("        $layout")
            characteristics.appendLine("        ${num(upper - lower)}")
            characteristics.appendLine("        ${conversion.name}")
            characteristics.appendLine("        ${num(lower)}")
            characteristics.appendLine("        ${num(upper)}")
            characteristics.appendLine("        FORMAT \"${conversion.format}\"")
            if (extra.isNotEmpty()) characteristics.appendLine("        $extra")
            if (axes.isNotEmpty()) characteristics.append(axes)
            characteristics.appendLine("    /end CHARACTERISTIC")
            characteristics.appendLine()
        }

        fun axisDescr(kind: String, input: String, conversion: Conversion, points: Int, lower: Double, upper: Double, extra: String) = buildString {
            appendLine("        /begin AXIS_DESCR")
            appendLine("            $kind")
            appendLine("            $input")
            appendLine("            ${conversion.name}")
            appendLine("            $points")
            appendLine("            ${num(lower)}")
            appendLine("            ${num(upper)}")
            appendLine("            FORMAT \"${conversion.format}\"")
            appendLine("            $extra")
            appendLine("        /end AXIS_DESCR")
        }

        fun value(name: String, description: String, type: Type, conversion: Conversion, value: Double, lower: Double, upper: Double) {
            val layoutName = "Val_${type.a2l}"
            layout(layoutName, "FNC_VALUES 1 ${type.a2l} COLUMN_DIR DIRECT")
            val address = put(type, conversion, listOf(value))
            placed[name] = Placed(name, address, listOf(value))
            characteristic(name, description, "VALUE", address, layoutName, conversion, lower, upper)
        }

        fun sharedAxis(name: String, description: String, input: String, type: Type, conversion: Conversion, points: List<Double>) {
            val layoutName = "Axis_${type.a2l}"
            layout(layoutName, """
                NO_AXIS_PTS_X 1 ${type.a2l}
                AXIS_PTS_X 2 ${type.a2l} INDEX_INCR DIRECT
            """)
            align(type.size)
            val address = next
            putCount(type, points.size)
            put(type, conversion, points)
            placed[name] = Placed(name, address, points)
            axisPts.appendLine("    /begin AXIS_PTS")
            axisPts.appendLine("        $name")
            axisPts.appendLine("        \"$description\"")
            axisPts.appendLine("        ${address.toA2lAddress()}")
            axisPts.appendLine("        $input")
            axisPts.appendLine("        $layoutName")
            axisPts.appendLine("        ${num(points.last() - points.first())}")
            axisPts.appendLine("        ${conversion.name}")
            axisPts.appendLine("        ${points.size}")
            axisPts.appendLine("        ${num(points.first())}")
            axisPts.appendLine("        ${num(points.last())}")
            axisPts.appendLine("        FORMAT \"${conversion.format}\"")
            axisPts.appendLine("        DEPOSIT ABSOLUTE")
            axisPts.appendLine("    /end AXIS_PTS")
            axisPts.appendLine()
        }

        /** A map with its axes in its own record (STD_AXIS on both) */
        fun stdMap(name: String, description: String, valueType: Type, conversion: Conversion,
                   xInput: String, x: List<Double>, xConversion: Conversion,
                   yInput: String, y: List<Double>, yConversion: Conversion,
                   lower: Double, upper: Double, f: (Double, Double) -> Double) {
            val layoutName = "Map_StdUw_${valueType.a2l}"
            layout(layoutName, """
                NO_AXIS_PTS_X 1 UWORD
                NO_AXIS_PTS_Y 2 UWORD
                AXIS_PTS_X 3 UWORD INDEX_INCR DIRECT
                AXIS_PTS_Y 4 UWORD INDEX_INCR DIRECT
                FNC_VALUES 5 ${valueType.a2l} COLUMN_DIR DIRECT
            """)
            align(2)
            val address = next
            putCount(Type.UWORD, x.size)
            putCount(Type.UWORD, y.size)
            put(Type.UWORD, xConversion, x)
            put(Type.UWORD, yConversion, y)
            val values = x.flatMap { xv -> y.map { yv -> f(xv, yv).coerceIn(lower, upper) } }
            put(valueType, conversion, values)
            placed[name] = Placed(name, address, values)
            characteristic(name, description, "MAP", address, layoutName, conversion, lower, upper, axes =
                axisDescr("STD_AXIS", xInput, xConversion, x.size, x.first(), x.last(), "DEPOSIT ABSOLUTE") +
                    axisDescr("STD_AXIS", yInput, yConversion, y.size, y.first(), y.last(), "DEPOSIT ABSOLUTE"))
        }

        /** A map using two shared axes (COM_AXIS), only its values in its record */
        fun comMap(name: String, description: String, valueType: Type, conversion: Conversion,
                   xAxis: String, xInput: String, x: List<Double>, xConversion: Conversion,
                   yAxis: String, yInput: String, y: List<Double>, yConversion: Conversion,
                   lower: Double, upper: Double, f: (Double, Double) -> Double) {
            val layoutName = "Values_${valueType.a2l}"
            layout(layoutName, "FNC_VALUES 1 ${valueType.a2l} COLUMN_DIR DIRECT")
            val values = x.flatMap { xv -> y.map { yv -> f(xv, yv).coerceIn(lower, upper) } }
            val address = put(valueType, conversion, values)
            placed[name] = Placed(name, address, values)
            characteristic(name, description, "MAP", address, layoutName, conversion, lower, upper, axes =
                axisDescr("COM_AXIS", xInput, xConversion, x.size, x.first(), x.last(), "AXIS_PTS_REF $xAxis") +
                    axisDescr("COM_AXIS", yInput, yConversion, y.size, y.first(), y.last(), "AXIS_PTS_REF $yAxis"))
        }

        fun a2l(): String {
            // ------------------------------- Shared axes -------------------------------
            sharedAxis("RpmAxis16", "Engine speed points for the main maps", "EngineSpeed", Type.UWORD, rpm, rpmAxis)
            sharedAxis("LoadAxis12", "Engine load points for the main maps", "EngineLoad", Type.UWORD, loadPct, loadAxis)
            sharedAxis("PedalAxis10", "Pedal position points", "PedalPosition", Type.UBYTE, pedalPct, pedalAxis)

            // ------------------------------- Values -------------------------------
            value("RevLimit", "Engine speed limit", Type.UWORD, rpm, 6800.0, 0.0, 9000.0)
            value("IdleSpeedTarget", "Idle speed, warm engine", Type.UWORD, rpm, 820.0, 500.0, 1500.0)
            value("FanOnTemp", "Coolant temperature that turns the fan on", Type.UBYTE, tempC, 96.0, -48.0, 143.25)
            value("LaunchControl", "Launch control on or off", Type.UBYTE, onOff, 0.0, 0.0, 1.0)
            value("MaxBoost", "Highest boost pressure allowed", Type.UWORD, pressureHpa, 2300.0, 1000.0, 3000.0)
            value("InjDeadTimeOffset", "Injector dead time correction", Type.SWORD, microseconds, -35.0, -500.0, 500.0)
            value("SerialNumber", "Unit serial number", Type.ULONG, identity, 20261107.0, 0.0, 4294967295.0)

            // ------------------------------- Value block and text -------------------------------
            layout("Values_UWORD", "FNC_VALUES 1 UWORD COLUMN_DIR DIRECT")
            val trims = listOf(1.0, 0.98, 1.02, 1.0)
            val trimAddress = put(Type.UWORD, factorQ15, trims)
            placed["InjectorTrim"] = Placed("InjectorTrim", trimAddress, trims)
            characteristic("InjectorTrim", "Fuel trim per injector", "VAL_BLK", trimAddress, "Values_UWORD", factorQ15, 0.5, 1.5, extra = "NUMBER 4")

            layout("Values_UBYTE", "FNC_VALUES 1 UBYTE COLUMN_DIR DIRECT")
            val softwareId = "TTDEMO1-0001".padEnd(16, '\u0000')
            val idAddress = next
            image.write(idAddress, softwareId.encodeToByteArray())
            next += softwareId.length
            placed["SoftwareId"] = Placed("SoftwareId", idAddress, softwareId.map { it.code.toDouble() })
            characteristic("SoftwareId", "Software identification", "ASCII", idAddress, "Values_UBYTE", identity, 0.0, 255.0, extra = "NUMBER 16")

            // ------------------------------- Curves -------------------------------
            layout("Curve_StdUb_UBYTE", """
                NO_AXIS_PTS_X 1 UBYTE
                AXIS_PTS_X 2 UBYTE INDEX_INCR DIRECT
                FNC_VALUES 3 UBYTE COLUMN_DIR DIRECT
            """)
            val warmupAddress = next
            putCount(Type.UBYTE, tempAxis.size)
            put(Type.UBYTE, tempC, tempAxis)
            val warmup = tempAxis.map { t -> (1.6 - (t + 30) / 120 * 0.6).coerceIn(1.0, 1.6) }
            put(Type.UBYTE, factorQ7, warmup)
            placed["WarmupEnrichment"] = Placed("WarmupEnrichment", warmupAddress, warmup)
            characteristic("WarmupEnrichment", "Extra fuel while the engine warms up", "CURVE", warmupAddress, "Curve_StdUb_UBYTE",
                factorQ7, 1.0, 1.99, axes = axisDescr("STD_AXIS", "CoolantTemp", tempC, tempAxis.size, tempAxis.first(), tempAxis.last(), "DEPOSIT ABSOLUTE"))

            val throttle = pedalAxis.map { p -> (p / 100).let { it * it * 0.6 + it * 0.4 } * 100 }
            val throttleAddress = put(Type.UBYTE, pedalPct, throttle)
            placed["ThrottleProgression"] = Placed("ThrottleProgression", throttleAddress, throttle)
            characteristic("ThrottleProgression", "Throttle opening for each pedal position", "CURVE", throttleAddress, "Values_UBYTE",
                pedalPct, 0.0, 100.0, axes = axisDescr("COM_AXIS", "PedalPosition", pedalPct, pedalAxis.size, 0.0, 100.0, "AXIS_PTS_REF PedalAxis10"))

            val gearLimits = listOf(250.0, 320.0, 380.0, 420.0, 420.0, 400.0)
            val gearAddress = put(Type.UWORD, torqueNm, gearLimits)
            placed["GearTorqueLimit"] = Placed("GearTorqueLimit", gearAddress, gearLimits)
            characteristic("GearTorqueLimit", "Most torque allowed in each gear", "CURVE", gearAddress, "Values_UWORD",
                torqueNm, 0.0, 600.0, axes = axisDescr("FIX_AXIS", "Gear", identity, 6, 1.0, 6.0, "FIX_AXIS_PAR 1 0 6"))

            // ------------------------------- Maps -------------------------------
            stdMap("IgnitionBase", "Base ignition advance", Type.UBYTE, ignDeg,
                "EngineSpeed", rpmAxis, rpm, "EngineLoad", loadAxis, loadPct, -10.0, 50.0) { speed, load ->
                10 + speed / 300 - load / 8
            }
            stdMap("VolumetricEfficiency", "Air charge for each speed and load", Type.UWORD, factorQ15,
                "EngineSpeed", rpmAxis, rpm, "EngineLoad", loadAxis, loadPct, 0.2, 1.2) { speed, load ->
                0.35 + load / 200 + 0.15 * kotlin.math.sin(speed / 6800 * kotlin.math.PI)
            }
            comMap("BoostTarget", "Boost pressure target", Type.UWORD, pressureHpa,
                "RpmAxis16", "EngineSpeed", rpmAxis, rpm, "LoadAxis12", "EngineLoad", loadAxis, loadPct, 1000.0, 2400.0) { speed, load ->
                1000 + (load - 60).coerceAtLeast(0.0) * 12 * (speed / 6800).coerceAtMost(0.8) / 0.8
            }
            comMap("LambdaTarget", "Air fuel ratio target", Type.UWORD, factorQ15,
                "RpmAxis16", "EngineSpeed", rpmAxis, rpm, "LoadAxis12", "EngineLoad", loadAxis, loadPct, 0.7, 1.05) { _, load ->
                if (load <= 80) 1.0 else 1.0 - (load - 80) / 80 * 0.22
            }

            return text()
        }

        fun text(): String = buildString {
            appendLine("ASAP2_VERSION 1 61")
            appendLine("/* TT-DEMO-1: a made up engine controller from Tuners Toolkit's DemoEcu generator.")
            appendLine("   Not a real ECU. Free to use, change and share. */")
            appendLine("/begin PROJECT TT_DEMO \"Tuners Toolkit demo\"")
            appendLine()
            appendLine("/begin HEADER \"Made up for testing\"")
            appendLine("    VERSION \"0.1\"")
            appendLine("    PROJECT_NO TTDEMO1")
            appendLine("/end HEADER")
            appendLine()
            appendLine("/begin MODULE DEMO_ECU \"Demo engine controller\"")
            appendLine()
            appendLine("/begin MOD_COMMON \"\"")
            appendLine("    BYTE_ORDER MSB_LAST")
            appendLine("    ALIGNMENT_BYTE 1")
            appendLine("    ALIGNMENT_WORD 2")
            appendLine("    ALIGNMENT_LONG 4")
            appendLine("/end MOD_COMMON")
            appendLine()
            appendLine("/begin MOD_PAR \"TT-DEMO-1\"")
            appendLine("    VERSION \"TTDEMO1 0.1\"")
            appendLine("    ADDR_EPK ${EPK_ADDRESS.toA2lAddress()}")
            appendLine("    EPK \"$EPK\"")
            appendLine("    ECU \"TT-DEMO-1\"")
            appendLine("    CPU_TYPE \"Generic 32 bit\"")
            appendLine("    /begin MEMORY_SEGMENT Flash \"Code and calibration\" CODE FLASH INTERN ${FLASH_START.toA2lAddress()} 0x${FLASH_SIZE.toString(16).uppercase()} -1 -1 -1 -1 -1")
            appendLine("    /end MEMORY_SEGMENT")
            appendLine("    /begin MEMORY_SEGMENT Boot \"Boot block\" RESERVED FLASH INTERN ${BOOT_START.toA2lAddress()} 0x${BOOT_SIZE.toString(16).uppercase()} -1 -1 -1 -1 -1")
            appendLine("    /end MEMORY_SEGMENT")
            appendLine("/end MOD_PAR")
            appendLine()
            conversions.forEach { append(it.a2l()); appendLine() }
            appendLine("    /begin COMPU_METHOD")
            appendLine("        onoff")
            appendLine("        \"\"")
            appendLine("        TAB_VERB")
            appendLine("        \"%3.0\"")
            appendLine("        \"\"")
            appendLine("        COMPU_TAB_REF onoff")
            appendLine("    /end COMPU_METHOD")
            appendLine()
            appendLine("    /begin COMPU_VTAB")
            appendLine("        onoff")
            appendLine("        \"\"")
            appendLine("        TAB_VERB")
            appendLine("        2")
            appendLine("        0 \"Off\"")
            appendLine("        1 \"On\"")
            appendLine("    /end COMPU_VTAB")
            appendLine()
            append(layouts)
            append(axisPts)
            append(characteristics)
            measurement("EngineSpeed", "Engine speed", "UWORD", rpm, 0.0, 9000.0, 0xD000_0000L)
            measurement("EngineLoad", "Engine load", "UWORD", loadPct, 0.0, 200.0, 0xD000_0002L)
            measurement("CoolantTemp", "Coolant temperature", "UBYTE", tempC, -48.0, 143.25, 0xD000_0004L)
            measurement("PedalPosition", "Pedal position", "UBYTE", pedalPct, 0.0, 100.0, 0xD000_0005L)
            measurement("Gear", "Engaged gear", "UBYTE", identity, 0.0, 6.0, 0xD000_0006L)
            function("Ignition", "Spark timing", listOf("IgnitionBase", "RpmAxis16", "LoadAxis12"), listOf("EngineSpeed", "EngineLoad"))
            function("Fueling", "Fuel delivery", listOf("VolumetricEfficiency", "LambdaTarget", "WarmupEnrichment", "InjectorTrim", "InjDeadTimeOffset"), listOf("EngineSpeed", "EngineLoad", "CoolantTemp"))
            function("Boost", "Boost control", listOf("BoostTarget", "MaxBoost"), listOf("EngineSpeed", "EngineLoad"))
            function("Driveability", "Pedal and torque", listOf("ThrottleProgression", "PedalAxis10", "GearTorqueLimit", "LaunchControl"), listOf("PedalPosition", "Gear"))
            function("Limits", "Speed and temperature limits", listOf("RevLimit", "IdleSpeedTarget", "FanOnTemp"), listOf("EngineSpeed", "CoolantTemp"))
            function("Identification", "Software and unit identity", listOf("SoftwareId", "SerialNumber"), emptyList())
            appendLine("/end MODULE")
            appendLine("/end PROJECT")
        }

        private fun StringBuilder.measurement(name: String, description: String, type: String, conversion: Conversion, lower: Double, upper: Double, address: Long) {
            appendLine("    /begin MEASUREMENT")
            appendLine("        $name")
            appendLine("        \"$description\"")
            appendLine("        $type")
            appendLine("        ${conversion.name}")
            appendLine("        1")
            appendLine("        100")
            appendLine("        ${num(lower)}")
            appendLine("        ${num(upper)}")
            appendLine("        ECU_ADDRESS ${address.toA2lAddress()}")
            appendLine("        FORMAT \"${conversion.format}\"")
            appendLine("    /end MEASUREMENT")
            appendLine()
        }

        private fun StringBuilder.function(name: String, description: String, characteristics: List<String>, inputs: List<String>) {
            appendLine("    /begin FUNCTION")
            appendLine("        $name")
            appendLine("        \"$description\"")
            appendLine("        /begin DEF_CHARACTERISTIC")
            appendLine("            ${characteristics.joinToString(" ")}")
            appendLine("        /end DEF_CHARACTERISTIC")
            if (inputs.isNotEmpty()) {
                appendLine("        /begin IN_MEASUREMENT")
                appendLine("            ${inputs.joinToString(" ")}")
                appendLine("        /end IN_MEASUREMENT")
            }
            appendLine("    /end FUNCTION")
            appendLine()
        }
    }

    /** `0x80004000`, the way A2L files write addresses */
    private fun Long.toA2lAddress(): String = toHexAddress()

    /** Numbers as A2L writes them: no exponent, no trailing zeros */
    private fun num(value: Double): String {
        if (value == value.roundToLong().toDouble() && kotlin.math.abs(value) < 1e15) return value.roundToLong().toString()
        val text = ((value * 1e9).roundToLong() / 1e9).toString()
        return if ('E' in text || 'e' in text) value.toString() else text
    }
}
