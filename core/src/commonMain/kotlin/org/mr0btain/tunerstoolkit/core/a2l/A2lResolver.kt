package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationAxis
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.Expression
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.Storage
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.diagnostics.SourceLocation
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/**
 * Turns an A2L module into [CalibrationObject]s: every CHARACTERISTIC and AXIS_PTS with its RECORD_LAYOUT, axes and
 * COMPU_METHOD looked up, MOD_COMMON's defaults filled in, ECU_CALIBRATION_OFFSET added to addresses, and variant
 * addresses from VARIANT_CODING. An object whose layout, axis or conversion can't be resolved is left out with an
 * error naming it, and the rest still resolve. Never throws.
 *
 * Not resolved yet: INSTANCEs of TYPEDEFs (1.7), and the ALTERNATE_* value orders
 */
class A2lResolver(private val module: A2lModule) {
    private val found = mutableListOf<Diagnostic>()
    private val conversions = mutableMapOf<String, Conversion?>()

    private val defaultOrder: ByteOrder = module.modCommon?.byteOrder ?: ByteOrder.BIG_ENDIAN
    private val defaultDeposit: Deposit = module.modCommon?.deposit ?: Deposit.ABSOLUTE
    private val calibrationOffset: Long = module.modPar?.ecuCalibrationOffset ?: 0
    private val constants: Map<String, String> = module.modPar?.systemConstants.orEmpty()
    private val variants: Map<String, List<Long>> = module.variantCoding?.characteristics.orEmpty().associate { it.name to it.addresses }

    val diagnostics: List<Diagnostic> get() = found

    fun resolve(): CalibrationSet {
        if (module.modCommon?.byteOrder == null && (module.characteristics.isNotEmpty() || module.axisPts.isNotEmpty())) {
            info(module.element.location, "MOD_COMMON has no BYTE_ORDER, so MSB_FIRST (big endian) is used where objects don't say")
        }
        if (module.instances.isNotEmpty()) {
            info(module.element.location, "${module.instances.size} INSTANCEs of TYPEDEFs aren't shown yet")
        }
        val objects = module.axisPts.mapNotNull(::axisPts) + module.characteristics.mapNotNull(::characteristic)
        return CalibrationSet(objects, found.toList())
    }

    // ------------------------------------------------ Objects

    private fun characteristic(c: Characteristic): CalibrationObject? {
        val name = c.name
        val layout = layout(c.recordLayout, c.element, name) ?: return null
        val conversion = conversion(c.conversion, c.element, name) ?: return null
        val kind = when (c.type) {
            CharacteristicType.VALUE -> ObjectKind.VALUE
            CharacteristicType.CURVE -> ObjectKind.CURVE
            CharacteristicType.MAP -> ObjectKind.MAP
            CharacteristicType.CUBOID -> ObjectKind.CUBOID
            CharacteristicType.CUBE_4 -> ObjectKind.CUBE_4
            CharacteristicType.CUBE_5 -> ObjectKind.CUBE_5
            CharacteristicType.VAL_BLK -> ObjectKind.VAL_BLK
            CharacteristicType.ASCII -> ObjectKind.ASCII
        }
        if (c.axes.size < kind.axisCount) {
            return null.also { error(c.element.location, "$name is a ${c.type} with ${c.axes.size} AXIS_DESCR, left out", name) }
        }
        val axisDescrs = c.axes.take(kind.axisCount)
        val axes = axisDescrs.mapIndexed { i, descr -> axis(descr, i, layout, name) ?: return null }
        val storage = storage(layout, axes.size, c.options.byteOrder, c.element, name, valueOrder = c.options.layout) ?: return null
        val dimensions = when (kind) {
            ObjectKind.VAL_BLK -> c.options.matrixDim.dropLastWhile { it == 1 }.ifEmpty { listOf(c.options.number ?: c.options.matrixDim.firstOrNull() ?: 1) }
            ObjectKind.ASCII -> listOf(c.options.number ?: c.options.matrixDim.fold(1) { a, b -> a * b })
            else -> emptyList()
        }
        if (kind == ObjectKind.VAL_BLK && c.options.number == null && c.options.matrixDim.isEmpty()) {
            warn(c.element.location, "VAL_BLK $name has neither NUMBER nor MATRIX_DIM, so it's taken as one value", name)
        }
        val compu = module.compuMethod(c.conversion)
        return CalibrationObject(
            name = name,
            description = c.longId,
            kind = kind,
            address = c.address + calibrationOffset,
            storage = storage,
            conversion = conversion,
            axes = axes,
            dimensions = dimensions,
            unit = c.options.physUnit ?: compu?.unit.orEmpty(),
            format = c.options.format ?: compu?.format,
            displayName = c.options.displayIdentifier,
            lowerLimit = c.lowerLimit,
            upperLimit = c.upperLimit,
            extendedLimits = c.options.extendedLimits?.let { it.first..it.second },
            readOnly = c.options.readOnly || c.options.calibrationAccess == "NO_CALIBRATION",
            bitMask = c.options.bitMask,
            encoding = c.options.encoding,
            variantAddresses = variants[name].orEmpty().map { it + calibrationOffset },
            source = c.location
        )
    }

    private fun axisPts(a: AxisPts): CalibrationObject? {
        val name = a.name
        val layout = layout(a.recordLayout, a.element, name) ?: return null
        val conversion = conversion(a.conversion, a.element, name) ?: return null
        if (layout.axisPts("X") == null && layout.entries.none { it is LayoutEntry.AxisRescale && it.axis == "X" }) {
            return null.also { error(a.element.location, "AXIS_PTS $name's RECORD_LAYOUT ${layout.name} has no AXIS_PTS_X or AXIS_RESCALE_X, left out", name) }
        }
        val compu = module.compuMethod(a.conversion)
        val axis = CalibrationAxis(
            source = AxisSource.Record,
            conversion = conversion,
            maxPoints = a.maxAxisPoints,
            input = a.inputQuantity.takeIf { it != NO_INPUT },
            unit = a.options.physUnit ?: compu?.unit.orEmpty(),
            format = a.options.format ?: compu?.format,
            lowerLimit = a.lowerLimit,
            upperLimit = a.upperLimit,
            fixedCount = layout.fixNoAxisPts("X")?.count,
            difference = (a.deposit ?: defaultDeposit) == Deposit.DIFFERENCE
        )
        val storage = storage(layout, 1, a.options.byteOrder, a.element, name) ?: return null
        return CalibrationObject(
            name = name,
            description = a.longId,
            kind = ObjectKind.AXIS,
            address = a.address + calibrationOffset,
            storage = storage,
            conversion = conversion,
            axes = listOf(axis),
            unit = axis.unit,
            format = axis.format,
            displayName = a.options.displayIdentifier,
            lowerLimit = a.lowerLimit,
            upperLimit = a.upperLimit,
            extendedLimits = a.options.extendedLimits?.let { it.first..it.second },
            readOnly = a.options.readOnly || a.options.calibrationAccess == "NO_CALIBRATION",
            variantAddresses = variants[name].orEmpty().map { it + calibrationOffset },
            source = a.element.location
        )
    }

    private fun axis(descr: AxisDescr, index: Int, layout: RecordLayout, owner: String): CalibrationAxis? {
        val axisName = AXES[index]
        val location = descr.element.location
        val conversion = conversion(descr.conversion, descr.element, owner) ?: return null
        val source: AxisSource = when (descr.type) {
            AxisType.STD_AXIS -> {
                val stored = layout.axisPts(axisName) != null || layout.entries.any { it is LayoutEntry.Offset && it.axis == axisName }
                if (!stored) {
                    return null.also { error(location, "$owner's STD_AXIS $axisName isn't in RECORD_LAYOUT ${layout.name} (no AXIS_PTS_$axisName or OFFSET_$axisName), left out", owner) }
                }
                AxisSource.Record
            }
            AxisType.COM_AXIS, AxisType.RES_AXIS -> {
                val ref = descr.axisPtsRef ?: return null.also { error(location, "$owner's ${descr.type} $axisName has no AXIS_PTS_REF, left out", owner) }
                if (module.axisPts(ref) == null) {
                    return null.also { error(location, "$owner's ${descr.type} $axisName refers to AXIS_PTS $ref, which isn't defined; left out", owner) }
                }
                if (descr.type == AxisType.COM_AXIS) AxisSource.Shared(ref) else AxisSource.Rescaled(ref)
            }
            AxisType.FIX_AXIS -> AxisSource.Fixed(fixPoints(descr.fixAxis, owner, location) ?: return null)
            AxisType.CURVE_AXIS -> {
                val ref = descr.curveAxisRef ?: return null.also { error(location, "$owner's CURVE_AXIS $axisName has no CURVE_AXIS_REF, left out", owner) }
                if (module.characteristic(ref) == null) {
                    return null.also { error(location, "$owner's CURVE_AXIS $axisName refers to $ref, which isn't defined; left out", owner) }
                }
                AxisSource.Curve(ref)
            }
        }
        val compu = module.compuMethod(descr.conversion)
        return CalibrationAxis(
            source = source,
            conversion = conversion,
            maxPoints = descr.maxAxisPoints,
            input = descr.inputQuantity.takeIf { it != NO_INPUT },
            unit = descr.options.physUnit ?: compu?.unit.orEmpty(),
            format = descr.options.format ?: compu?.format,
            lowerLimit = descr.lowerLimit,
            upperLimit = descr.upperLimit,
            fixedCount = layout.fixNoAxisPts(axisName)?.count,
            difference = (descr.deposit ?: defaultDeposit) == Deposit.DIFFERENCE
        )
    }

    private fun fixPoints(fix: FixAxis?, owner: String, location: SourceLocation): List<Double>? {
        val count = when (fix) {
            is FixAxis.Par -> fix.count
            is FixAxis.ParDist -> fix.count
            is FixAxis.ParList -> fix.values.size
            null -> return null.also { error(location, "$owner's FIX_AXIS has no FIX_AXIS_PAR, _DIST or _LIST, left out", owner) }
        }
        if (count !in 1..MAX_FIXED_POINTS) {
            return null.also { error(location, "$owner's FIX_AXIS has $count points, left out", owner) }
        }
        return when (fix) {
            is FixAxis.Par -> {
                if (fix.shift !in 0..62) return null.also { error(location, "$owner's FIX_AXIS_PAR shift ${fix.shift} isn't valid, left out", owner) }
                List(count) { fix.offset + it * (1L shl fix.shift).toDouble() }
            }
            is FixAxis.ParDist -> List(count) { fix.offset + it * fix.distance }
            is FixAxis.ParList -> fix.values
        }
    }

    // ------------------------------------------------ Record layouts

    private fun layout(name: String, element: A2lElement, owner: String): RecordLayout? =
        module.recordLayout(name) ?: null.also { error(element.location, "$owner's RECORD_LAYOUT $name isn't defined, left out", owner) }

    /**
     * The fields of [layout] for an object with [axisCount] axes
     * @param valueOrder The object's own LAYOUT (1.7), which wins over FNC_VALUES's
     */
    private fun storage(layout: RecordLayout, axisCount: Int, byteOrder: ByteOrder?, element: A2lElement, owner: String, valueOrder: IndexMode? = null): Storage? {
        val location = element.location
        val fields = mutableListOf<RecordField>()
        for (entry in layout.entries) {
            val axisName = (entry as? LayoutEntry.AxisPts)?.axis ?: axisOf(entry)
            val axis = axisName?.let { AXES.indexOf(it) }
            // Counts, offsets and steps for an axis the object doesn't have still take up their space
            if (axisName != null && (axis == null || axis !in 0 until axisCount)) {
                when (entry) {
                    is LayoutEntry.AxisPts, is LayoutEntry.AxisRescale -> {
                        return null.also { error(location, "$owner's RECORD_LAYOUT ${layout.name} has points for axis $axisName, which $owner doesn't have; left out", owner) }
                    }
                    is LayoutEntry.FixNoAxisPts -> continue
                    else -> {
                        warn(location, "$owner's RECORD_LAYOUT ${layout.name} has ${entry.keyword()} for axis $axisName, which $owner doesn't have; skipped over", owner)
                        fields += RecordField.Skip(entry.dataType() ?: DataType.UBYTE, entry.keyword())
                        continue
                    }
                }
            }
            fields += when (entry) {
                is LayoutEntry.FncValues -> {
                    val order = when (valueOrder ?: entry.indexMode) {
                        IndexMode.ROW_DIR -> ValueOrder.X_FIRST
                        IndexMode.COLUMN_DIR -> ValueOrder.Y_FIRST
                        else -> return null.also { error(location, "$owner's values are stored ${valueOrder ?: entry.indexMode}, which isn't supported yet; left out", owner) }
                    }
                    RecordField.Values(entry.dataType, order, pointer(entry.addressType))
                }
                is LayoutEntry.AxisPts -> RecordField.AxisPoints(axis!!, entry.dataType, entry.indexOrder == IndexOrder.INDEX_DECR, pointer(entry.addressType))
                is LayoutEntry.AxisRescale -> RecordField.Rescale(axis!!, entry.dataType, entry.maxPairs, entry.indexOrder == IndexOrder.INDEX_DECR, pointer(entry.addressType))
                is LayoutEntry.NoAxisPts -> RecordField.AxisCount(axis!!, entry.dataType)
                is LayoutEntry.NoRescale -> RecordField.RescaleCount(axis!!, entry.dataType)
                is LayoutEntry.Offset -> RecordField.AxisOffset(axis!!, entry.dataType)
                is LayoutEntry.DistOp -> RecordField.AxisDistance(axis!!, entry.dataType)
                is LayoutEntry.ShiftOp -> RecordField.AxisShift(axis!!, entry.dataType)
                is LayoutEntry.FixNoAxisPts -> continue
                is LayoutEntry.Identification -> RecordField.Skip(entry.dataType, "IDENTIFICATION")
                is LayoutEntry.SrcAddr -> RecordField.Skip(entry.dataType, "SRC_ADDR_${entry.axis}")
                is LayoutEntry.RipAddr -> RecordField.Skip(entry.dataType, "RIP_ADDR_${entry.axis}")
                is LayoutEntry.Reserved -> RecordField.Skip(reservedType(entry.dataSize), "RESERVED")
            }
        }
        return Storage(fields, byteOrder ?: defaultOrder, alignments(layout), layout.staticRecordLayout)
    }

    /** The axis a layout entry is for; RIP_ADDR_W and the entries that aren't per axis have none */
    private fun axisOf(entry: LayoutEntry): String? = when (entry) {
        is LayoutEntry.AxisRescale -> entry.axis
        is LayoutEntry.NoAxisPts -> entry.axis
        is LayoutEntry.NoRescale -> entry.axis
        is LayoutEntry.FixNoAxisPts -> entry.axis
        is LayoutEntry.Offset -> entry.axis
        is LayoutEntry.DistOp -> entry.axis
        is LayoutEntry.ShiftOp -> entry.axis
        else -> null
    }

    private fun LayoutEntry.keyword(): String = when (this) {
        is LayoutEntry.NoAxisPts -> "NO_AXIS_PTS_$axis"
        is LayoutEntry.NoRescale -> "NO_RESCALE_$axis"
        is LayoutEntry.Offset -> "OFFSET_$axis"
        is LayoutEntry.DistOp -> "DIST_OP_$axis"
        is LayoutEntry.ShiftOp -> "SHIFT_OP_$axis"
        else -> this::class.simpleName.orEmpty()
    }

    private fun LayoutEntry.dataType(): DataType? = when (this) {
        is LayoutEntry.NoAxisPts -> dataType
        is LayoutEntry.NoRescale -> dataType
        is LayoutEntry.Offset -> dataType
        is LayoutEntry.DistOp -> dataType
        is LayoutEntry.ShiftOp -> dataType
        else -> null
    }

    private fun pointer(type: AddressType): DataType? = if (type == AddressType.DIRECT) null else DataType.unsigned(type.pointerSize)

    private fun reservedType(size: String): DataType = when (size) {
        "WORD" -> DataType.UWORD
        "LONG" -> DataType.ULONG
        "LONG64", "INT64" -> DataType.A_UINT64
        else -> DataType.UBYTE
    }

    /** MOD_COMMON's ALIGNMENT_* with the layout's own on top, as each data type's alignment */
    private fun alignments(layout: RecordLayout): Map<DataType, Int> {
        val byName = module.modCommon?.alignments.orEmpty() + layout.alignments
        val result = mutableMapOf<DataType, Int>()
        for ((name, alignment) in byName) {
            val types = ALIGNMENT_TYPES[name] ?: continue
            for (type in types) result[type] = alignment.coerceAtLeast(1)
        }
        return result
    }

    // ------------------------------------------------ Conversions

    /** The COMPU_METHOD [name] as a [Conversion]; null (with an error) when it can't be used */
    private fun conversion(name: String, element: A2lElement, owner: String): Conversion? {
        if (name == NO_COMPU_METHOD) return Conversion.Identical
        if (name in conversions) {
            return conversions[name].also { if (it == null) error(element.location, "$owner's COMPU_METHOD $name can't be used, left out", owner) }
        }
        val method = module.compuMethod(name)
        val conversion = if (method == null) {
            warn(element.location, "$owner's COMPU_METHOD $name isn't defined, so its raw values are shown", owner)
            Conversion.Identical
        } else {
            method(method)?.let { base -> status(method, base) }
        }
        conversions[name] = conversion
        if (conversion == null) error(element.location, "$owner's COMPU_METHOD $name can't be used, left out", owner)
        return conversion
    }

    private fun method(method: CompuMethod): Conversion? {
        val location = method.element.location
        val name = method.name
        return when (method.type) {
            ConversionType.IDENTICAL -> Conversion.Identical
            ConversionType.LINEAR -> method.coeffsLinear?.let { Conversion.Linear(it.first, it.second) }
                ?: null.also { error(location, "LINEAR $name has no COEFFS_LINEAR", name) }
            ConversionType.RAT_FUNC -> {
                val c = method.coeffs ?: return null.also { error(location, "RAT_FUNC $name has no COEFFS", name) }
                rational(c[0], c[1], c[2], c[3], c[4], c[5]).also {
                    if (it == null) error(location, "RAT_FUNC $name's COEFFS ${c.joinToString(" ")} don't give a value for any raw value", name)
                }
            }
            ConversionType.FORM -> {
                val formula = method.formula ?: return null.also { error(location, "FORM $name has no FORMULA", name) }
                val forward = expression(formula.expression, location, name) ?: return null
                val inverse = formula.inverse?.let { expression(it, location, name) }
                Conversion.Formula(forward, inverse)
            }
            ConversionType.TAB_INTP, ConversionType.TAB_NOINTP -> {
                val tab = method.compuTabRef?.let(module::compuTab)
                    ?: return null.also { error(location, "${method.type} $name's COMPU_TAB ${method.compuTabRef.orEmpty()} isn't defined", name) }
                Conversion.Table(tab.pairs, method.type == ConversionType.TAB_INTP, tab.defaultNumeric)
            }
            ConversionType.TAB_VERB -> {
                val ref = method.compuTabRef ?: return null.also { error(location, "TAB_VERB $name has no COMPU_TAB_REF", name) }
                verbal(ref) ?: null.also { error(location, "TAB_VERB $name's COMPU_VTAB $ref isn't defined", name) }
            }
        }
    }

    /** RAT_FUNC as a linear conversion when it is one, which is nearly always */
    private fun rational(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Conversion? {
        if (a == 0.0 && d == 0.0 && e == 0.0) {
            // raw = (b p + c) / f, so p = (f raw - c) / b
            if (b == 0.0 || f == 0.0) return null
            return Conversion.Linear(f / b, -c / b)
        }
        return Conversion.Rational(a, b, c, d, e, f)
    }

    private fun verbal(ref: String): Conversion.Verbal? {
        module.compuVTab(ref)?.let { return Conversion.Verbal(it.entries, default = it.defaultValue) }
        module.compuVTabRange(ref)?.let { range ->
            return Conversion.Verbal(emptyList(), range.entries.map { Conversion.VerbalRange(it.min, it.max, it.text) }, range.defaultValue)
        }
        return null
    }

    private fun status(method: CompuMethod, base: Conversion): Conversion {
        val ref = method.statusStringRef ?: return base
        val status = verbal(ref) ?: return base.also { warn(method.element.location, "${method.name}'s STATUS_STRING_REF $ref isn't defined", method.name) }
        return Conversion.WithStatus(base, status)
    }

    private fun expression(text: String, location: SourceLocation, owner: String): Expression? =
        when (val result = Expression.parse(text, constants)) {
            is TkResult.Success -> result.data
            is TkResult.Failure -> null.also { error(location, result.error.message, owner) }
        }

    // ------------------------------------------------ Diagnostics

    private fun info(location: SourceLocation?, message: String) {
        found += Diagnostic(Severity.INFO, message, location)
    }

    private fun warn(location: SourceLocation?, message: String, objectName: String? = null) {
        found += Diagnostic(Severity.WARNING, message, location, objectName)
    }

    private fun error(location: SourceLocation?, message: String, objectName: String? = null) {
        found += Diagnostic(Severity.ERROR, message, location, objectName)
    }

    companion object {
        /** A2L's names for axes 1 to 5 */
        val AXES = listOf("X", "Y", "Z", "4", "5")

        const val NO_COMPU_METHOD = "NO_COMPU_METHOD"
        const val NO_INPUT = "NO_INPUT_QUANTITY"
        const val MAX_FIXED_POINTS = 1_000_000

        private val ALIGNMENT_TYPES = mapOf(
            "BYTE" to listOf(DataType.UBYTE, DataType.SBYTE),
            "WORD" to listOf(DataType.UWORD, DataType.SWORD),
            "LONG" to listOf(DataType.ULONG, DataType.SLONG),
            "INT64" to listOf(DataType.A_UINT64, DataType.A_INT64),
            "FLOAT16_IEEE" to listOf(DataType.FLOAT16_IEEE),
            "FLOAT32_IEEE" to listOf(DataType.FLOAT32_IEEE),
            "FLOAT64_IEEE" to listOf(DataType.FLOAT64_IEEE)
        )

        /** Resolves every module of [file]; diagnostics are in each set */
        fun resolve(file: A2lFile): List<CalibrationSet> = file.modules.map { A2lResolver(it).resolve() }
    }
}
