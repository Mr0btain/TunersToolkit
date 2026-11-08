package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.ByteOrder

/**
 * Builds the [A2lFile] model from a parsed [A2lTree]. An object missing something it can't do without (an address,
 * a known type) is left out with an error naming it; anything less is a warning and a default
 */
class A2lModelBuilder(private val tree: A2lTree) {
    private val found = mutableListOf<Diagnostic>()
    val diagnostics: List<Diagnostic> get() = found

    fun build(): A2lFile {
        val top = tree.elements
        val version = top.firstOrNull { it.keyword == "ASAP2_VERSION" }?.let { "${it.text(0)}.${it.text(1)}" }
        val a2mlVersion = top.firstOrNull { it.keyword == "A2ML_VERSION" }?.let { "${it.text(0)}.${it.text(1)}" }
        val projects = top.filter { it.keyword == "PROJECT" }
        if (projects.isEmpty()) {
            found += Diagnostic(Severity.ERROR, "There's no /begin PROJECT in the file", top.firstOrNull()?.location)
        } else if (projects.size > 1) {
            warn(projects[1], "More than one PROJECT; only the first is read")
        }
        return A2lFile(version, a2mlVersion, projects.firstOrNull()?.let(::project), tree)
    }

    private fun project(element: A2lElement): A2lProject {
        val header = element.child("HEADER")?.let { A2lHeader(it.text(0).orEmpty(), it.child("VERSION")?.text(0), it.child("PROJECT_NO")?.text(0)) }
        return A2lProject(
            name = element.text(0).orEmpty(),
            longId = element.text(1).orEmpty(),
            header = header,
            modules = element.children("MODULE").map(::module),
            element = element
        )
    }

    private fun module(element: A2lElement): A2lModule {
        val a2ml = element.child("A2ML")?.let { block ->
            val (grammar, problems) = A2ml.parse(block.raw.orEmpty(), block.location)
            found += problems
            grammar
        }
        val ifData = ifData(element)
        a2ml?.ifDataTags?.takeIf { it.isNotEmpty() }?.let { tags ->
            element.walk().filter { it.keyword == "IF_DATA" }.forEach { block ->
                val tag = tagOf(block)
                if (tag != null && tag !in tags) {
                    found += Diagnostic(Severity.WARNING, "IF_DATA $tag isn't in the module's A2ML", block.location)
                }
            }
        }
        return A2lModule(
            name = element.text(0).orEmpty(),
            longId = element.text(1).orEmpty(),
            a2ml = a2ml,
            modCommon = element.child("MOD_COMMON")?.let(::modCommon),
            modPar = element.child("MOD_PAR")?.let(::modPar),
            characteristics = element.children("CHARACTERISTIC").mapNotNull(::characteristic),
            axisPts = element.children("AXIS_PTS").mapNotNull(::axisPts),
            measurements = element.children("MEASUREMENT").mapNotNull(::measurement),
            compuMethods = element.children("COMPU_METHOD").mapNotNull(::compuMethod),
            compuTabs = element.children("COMPU_TAB").mapNotNull(::compuTab),
            compuVTabs = element.children("COMPU_VTAB").mapNotNull(::compuVTab),
            compuVTabRanges = element.children("COMPU_VTAB_RANGE").mapNotNull(::compuVTabRange),
            recordLayouts = element.children("RECORD_LAYOUT").mapNotNull(::recordLayout),
            functions = element.children("FUNCTION").map(::function),
            groups = element.children("GROUP").map(::group),
            units = element.children("UNIT").map(::unit),
            blobs = element.children("BLOB").mapNotNull(::blob),
            instances = element.children("INSTANCE").mapNotNull(::instance),
            typedefs = element.children.filter { it.keyword.startsWith("TYPEDEF_") }.map(::typedef),
            transformers = element.children("TRANSFORMER").map(::transformer),
            frames = element.children("FRAME").map(::frame),
            userRights = element.children("USER_RIGHTS").map(::userRights),
            variantCoding = element.child("VARIANT_CODING")?.let(::variantCoding),
            ifData = ifData,
            element = element
        ).also { checkDuplicates(it) }
    }

    // ------------------------------------------------ MOD_COMMON and MOD_PAR

    private fun modCommon(element: A2lElement) = ModCommon(
        comment = element.text(0).orEmpty(),
        byteOrder = element.child("BYTE_ORDER")?.let { byteOrder(it, it.text(0)) },
        deposit = element.child("DEPOSIT")?.let { enumOf<Deposit>(it, it.text(0)) },
        dataSize = element.child("DATA_SIZE")?.long(0)?.toInt(),
        sRecLayout = element.child("S_REC_LAYOUT")?.text(0),
        alignments = alignments(element)
    )

    private fun alignments(element: A2lElement): Map<String, Int> = element.children
        .filter { it.keyword.startsWith("ALIGNMENT_") }
        .mapNotNull { child -> child.long(0)?.let { child.keyword.removePrefix("ALIGNMENT_") to it.toInt() } }
        .toMap()

    private fun modPar(element: A2lElement) = ModPar(
        comment = element.text(0).orEmpty(),
        version = element.child("VERSION")?.text(0),
        addrEpk = element.children("ADDR_EPK").mapNotNull { it.long(0) },
        epk = element.child("EPK")?.text(0),
        supplier = element.child("SUPPLIER")?.text(0),
        customer = element.child("CUSTOMER")?.text(0),
        customerNo = element.child("CUSTOMER_NO")?.text(0),
        user = element.child("USER")?.text(0),
        phoneNo = element.child("PHONE_NO")?.text(0),
        ecu = element.child("ECU")?.text(0),
        cpuType = element.child("CPU_TYPE")?.text(0),
        noOfInterfaces = element.child("NO_OF_INTERFACES")?.long(0)?.toInt(),
        ecuCalibrationOffset = element.child("ECU_CALIBRATION_OFFSET")?.long(0),
        memorySegments = element.children("MEMORY_SEGMENT").mapNotNull { segment ->
            val address = segment.long(5)
            val size = segment.long(6)
            if (address == null || size == null) {
                error(segment, "MEMORY_SEGMENT ${segment.text(0)} has no address or size, left out")
                return@mapNotNull null
            }
            MemorySegment(
                name = segment.text(0).orEmpty(),
                longId = segment.text(1).orEmpty(),
                prgType = segment.text(2).orEmpty(),
                memoryType = segment.text(3).orEmpty(),
                attribute = segment.text(4).orEmpty(),
                address = address,
                size = size,
                offsets = (7..11).mapNotNull { segment.long(it) },
                ifData = ifData(segment)
            )
        },
        memoryLayouts = element.children("MEMORY_LAYOUT").map { layout ->
            MemoryLayout(layout.text(0).orEmpty(), layout.long(1) ?: 0, layout.long(2) ?: 0, (3..7).mapNotNull { layout.long(it) })
        },
        systemConstants = element.children("SYSTEM_CONSTANT").associate { it.text(0).orEmpty() to it.text(1).orEmpty() },
        calibrationMethods = element.children("CALIBRATION_METHOD").map { method ->
            CalibrationMethod(
                method = method.text(0).orEmpty(),
                version = method.long(1) ?: 0,
                handles = method.children("CALIBRATION_HANDLE").map { handle ->
                    CalibrationHandle(handle.values.mapNotNull { it.asLong() }, handle.child("CALIBRATION_HANDLE_TEXT")?.text(0))
                }
            )
        }
    )

    // ------------------------------------------------ Objects

    private fun characteristic(element: A2lElement): Characteristic? {
        val name = element.text(0) ?: return null.also { error(element, "CHARACTERISTIC without a name, left out") }
        val type = enumOf<CharacteristicType>(element, element.text(2), name) ?: return null.also {
            error(element, "CHARACTERISTIC $name has an unknown type ${element.text(2)}, left out", name)
        }
        val address = element.long(3) ?: return null.also { error(element, "CHARACTERISTIC $name has no address, left out", name) }
        val axes = element.children("AXIS_DESCR").mapNotNull { axisDescr(it, name) }
        val expected = when (type) {
            CharacteristicType.CURVE -> 1
            CharacteristicType.MAP -> 2
            CharacteristicType.CUBOID -> 3
            CharacteristicType.CUBE_4 -> 4
            CharacteristicType.CUBE_5 -> 5
            else -> 0
        }
        if (axes.size != expected) {
            warn(element, "CHARACTERISTIC $name is a $type with ${axes.size} AXIS_DESCR, it should have $expected", name)
        }
        return Characteristic(
            name = name,
            longId = element.text(1).orEmpty(),
            type = type,
            address = address,
            recordLayout = element.text(4).orEmpty(),
            maxDiff = element.double(5) ?: 0.0,
            conversion = element.text(6) ?: "NO_COMPU_METHOD",
            lowerLimit = element.double(7) ?: Double.NEGATIVE_INFINITY,
            upperLimit = element.double(8) ?: Double.POSITIVE_INFINITY,
            axes = axes,
            mapList = element.child("MAP_LIST")?.values?.map { it.text }.orEmpty(),
            options = options(element, name),
            element = element
        )
    }

    private fun axisDescr(element: A2lElement, owner: String): AxisDescr? {
        val type = enumOf<AxisType>(element, element.text(0), owner) ?: return null.also {
            error(element, "AXIS_DESCR of $owner has an unknown type ${element.text(0)}, left out", owner)
        }
        val fixAxis = element.child("FIX_AXIS_PAR")?.let { FixAxis.Par(it.double(0) ?: 0.0, it.long(1)?.toInt() ?: 0, it.long(2)?.toInt() ?: 0) }
            ?: element.child("FIX_AXIS_PAR_DIST")?.let { FixAxis.ParDist(it.double(0) ?: 0.0, it.double(1) ?: 0.0, it.long(2)?.toInt() ?: 0) }
            ?: element.child("FIX_AXIS_PAR_LIST")?.let { list -> FixAxis.ParList(list.values.mapNotNull { it.asDouble() }) }
        if (type == AxisType.FIX_AXIS && fixAxis == null) warn(element, "FIX_AXIS of $owner has no FIX_AXIS_PAR, _DIST or _LIST", owner)
        if ((type == AxisType.COM_AXIS || type == AxisType.RES_AXIS) && !element.has("AXIS_PTS_REF")) {
            warn(element, "$type of $owner has no AXIS_PTS_REF", owner)
        }
        if (type == AxisType.CURVE_AXIS && !element.has("CURVE_AXIS_REF")) warn(element, "CURVE_AXIS of $owner has no CURVE_AXIS_REF", owner)
        return AxisDescr(
            type = type,
            inputQuantity = element.text(1) ?: "NO_INPUT_QUANTITY",
            conversion = element.text(2) ?: "NO_COMPU_METHOD",
            maxAxisPoints = element.long(3)?.toInt() ?: 0,
            lowerLimit = element.double(4) ?: Double.NEGATIVE_INFINITY,
            upperLimit = element.double(5) ?: Double.POSITIVE_INFINITY,
            axisPtsRef = element.child("AXIS_PTS_REF")?.text(0),
            curveAxisRef = element.child("CURVE_AXIS_REF")?.text(0),
            fixAxis = fixAxis,
            deposit = element.child("DEPOSIT")?.let { enumOf<Deposit>(it, it.text(0), owner) },
            options = options(element, owner),
            element = element
        )
    }

    private fun axisPts(element: A2lElement): AxisPts? {
        val name = element.text(0) ?: return null.also { error(element, "AXIS_PTS without a name, left out") }
        val address = element.long(2) ?: return null.also { error(element, "AXIS_PTS $name has no address, left out", name) }
        return AxisPts(
            name = name,
            longId = element.text(1).orEmpty(),
            address = address,
            inputQuantity = element.text(3) ?: "NO_INPUT_QUANTITY",
            recordLayout = element.text(4).orEmpty(),
            maxDiff = element.double(5) ?: 0.0,
            conversion = element.text(6) ?: "NO_COMPU_METHOD",
            maxAxisPoints = element.long(7)?.toInt() ?: 0,
            lowerLimit = element.double(8) ?: Double.NEGATIVE_INFINITY,
            upperLimit = element.double(9) ?: Double.POSITIVE_INFINITY,
            deposit = element.child("DEPOSIT")?.let { enumOf<Deposit>(it, it.text(0), name) },
            options = options(element, name),
            element = element
        )
    }

    private fun measurement(element: A2lElement): Measurement? {
        val name = element.text(0) ?: return null.also { error(element, "MEASUREMENT without a name, left out") }
        val dataType = dataType(element, element.text(2), name) ?: return null.also {
            error(element, "MEASUREMENT $name has an unknown data type ${element.text(2)}, left out", name)
        }
        return Measurement(
            name = name,
            longId = element.text(1).orEmpty(),
            dataType = dataType,
            conversion = element.text(3) ?: "NO_COMPU_METHOD",
            resolution = element.double(4) ?: 0.0,
            accuracy = element.double(5) ?: 0.0,
            lowerLimit = element.double(6) ?: Double.NEGATIVE_INFINITY,
            upperLimit = element.double(7) ?: Double.POSITIVE_INFINITY,
            ecuAddress = element.child("ECU_ADDRESS")?.long(0),
            arraySize = element.child("ARRAY_SIZE")?.long(0)?.toInt(),
            errorMask = element.child("ERROR_MASK")?.long(0),
            virtual = element.child("VIRTUAL")?.values?.map { it.text }.orEmpty(),
            options = options(element, name),
            element = element
        )
    }

    private fun blob(element: A2lElement): Blob? {
        val name = element.text(0) ?: return null
        val address = element.long(2) ?: return null.also { error(element, "BLOB $name has no address, left out", name) }
        return Blob(name, element.text(1).orEmpty(), address, element.long(3) ?: 0, options(element, name), element)
    }

    private fun instance(element: A2lElement): Instance? {
        val name = element.text(0) ?: return null
        val address = element.long(3) ?: return null.also { error(element, "INSTANCE $name has no address, left out", name) }
        return Instance(name, element.text(1).orEmpty(), element.text(2).orEmpty(), address, options(element, name), element)
    }

    private fun typedef(element: A2lElement) = Typedef(
        kind = element.keyword,
        name = element.text(0).orEmpty(),
        longId = element.text(1).orEmpty(),
        components = element.children("STRUCTURE_COMPONENT").map {
            StructureComponent(it.text(0).orEmpty(), it.text(1).orEmpty(), it.long(2) ?: 0, options(it, it.text(0)))
        },
        element = element
    )

    private fun transformer(element: A2lElement) = Transformer(
        name = element.text(0).orEmpty(),
        version = element.text(1).orEmpty(),
        dll32 = element.text(2).orEmpty(),
        dll64 = element.text(3).orEmpty(),
        timeout = element.long(4) ?: 0,
        trigger = element.text(5).orEmpty(),
        inverse = element.text(6).orEmpty(),
        inObjects = element.child("TRANSFORMER_IN_OBJECTS")?.values?.map { it.text }.orEmpty(),
        outObjects = element.child("TRANSFORMER_OUT_OBJECTS")?.values?.map { it.text }.orEmpty()
    )

    /** The optional keywords every object can have */
    private fun options(element: A2lElement, owner: String?): ObjectOptions = ObjectOptions(
        bitMask = element.child("BIT_MASK")?.long(0),
        bitOperation = element.child("BIT_OPERATION")?.let {
            BitOperation(it.child("LEFT_SHIFT")?.long(0)?.toInt(), it.child("RIGHT_SHIFT")?.long(0)?.toInt(), it.has("SIGN_EXTEND"))
        },
        byteOrder = element.child("BYTE_ORDER")?.let { byteOrder(it, it.text(0), owner) },
        matrixDim = element.child("MATRIX_DIM")?.values?.mapNotNull { it.asLong()?.toInt() }.orEmpty(),
        number = element.child("NUMBER")?.long(0)?.toInt(),
        extendedLimits = element.child("EXTENDED_LIMITS")?.let { (it.double(0) ?: 0.0) to (it.double(1) ?: 0.0) },
        readOnly = element.has("READ_ONLY"),
        guardRails = element.has("GUARD_RAILS"),
        discrete = element.has("DISCRETE"),
        displayIdentifier = element.child("DISPLAY_IDENTIFIER")?.text(0),
        format = element.child("FORMAT")?.text(0),
        physUnit = element.child("PHYS_UNIT")?.text(0),
        maxRefresh = element.child("MAX_REFRESH")?.let { (it.long(0) ?: 0) to (it.long(1) ?: 0) },
        ecuAddressExtension = element.child("ECU_ADDRESS_EXTENSION")?.long(0),
        calibrationAccess = element.child("CALIBRATION_ACCESS")?.text(0),
        comparisonQuantity = element.child("COMPARISON_QUANTITY")?.text(0),
        dependentCharacteristic = element.child("DEPENDENT_CHARACTERISTIC")?.let(::formulaWithInputs),
        virtualCharacteristic = element.child("VIRTUAL_CHARACTERISTIC")?.let(::formulaWithInputs),
        symbolLink = element.child("SYMBOL_LINK")?.let { it.text(0).orEmpty() to (it.long(1) ?: 0) },
        modelLink = element.child("MODEL_LINK")?.text(0),
        encoding = element.child("ENCODING")?.text(0),
        monotony = element.child("MONOTONY")?.text(0),
        stepSize = element.child("STEP_SIZE")?.double(0),
        maxGrad = element.child("MAX_GRAD")?.double(0),
        layout = element.child("LAYOUT")?.let { enumOf<IndexMode>(it, it.text(0), owner) },
        addressType = element.child("ADDRESS_TYPE")?.let { enumOf<AddressType>(it, it.text(0), owner) },
        refMemorySegment = element.child("REF_MEMORY_SEGMENT")?.text(0),
        annotations = element.children("ANNOTATION").map { annotation ->
            Annotation(
                label = annotation.child("ANNOTATION_LABEL")?.text(0),
                origin = annotation.child("ANNOTATION_ORIGIN")?.text(0),
                text = annotation.child("ANNOTATION_TEXT")?.values?.map { it.text }.orEmpty()
            )
        },
        functionList = element.child("FUNCTION_LIST")?.values?.map { it.text }.orEmpty(),
        ifData = ifData(element)
    )

    private fun formulaWithInputs(element: A2lElement) = Formula(element.text(0).orEmpty(), element.values.drop(1).map { it.text })

    // ------------------------------------------------ Conversion

    private fun compuMethod(element: A2lElement): CompuMethod? {
        val name = element.text(0) ?: return null
        val type = enumOf<ConversionType>(element, element.text(2), name) ?: return null.also {
            error(element, "COMPU_METHOD $name has an unknown type ${element.text(2)}, left out", name)
        }
        val coeffs = element.child("COEFFS")?.values?.mapNotNull { it.asDouble() }?.takeIf { it.size == 6 }
        val linear = element.child("COEFFS_LINEAR")?.let { (it.double(0) ?: 1.0) to (it.double(1) ?: 0.0) }
        val formula = element.child("FORMULA")?.let { Formula(it.text(0).orEmpty(), inverse = it.child("FORMULA_INV")?.text(0)) }
        val tabRef = element.child("COMPU_TAB_REF")?.text(0)
        when {
            type == ConversionType.RAT_FUNC && coeffs == null -> warn(element, "RAT_FUNC $name has no COEFFS with six values", name)
            type == ConversionType.LINEAR && linear == null -> warn(element, "LINEAR $name has no COEFFS_LINEAR", name)
            type == ConversionType.FORM && formula == null -> warn(element, "FORM $name has no FORMULA", name)
            type in TABLE_TYPES && tabRef == null -> warn(element, "$type $name has no COMPU_TAB_REF", name)
        }
        return CompuMethod(
            name = name,
            longId = element.text(1).orEmpty(),
            type = type,
            format = element.text(3).orEmpty(),
            unit = element.text(4).orEmpty(),
            coeffs = coeffs,
            coeffsLinear = linear,
            compuTabRef = tabRef,
            formula = formula,
            refUnit = element.child("REF_UNIT")?.text(0),
            statusStringRef = element.child("STATUS_STRING_REF")?.text(0),
            element = element
        )
    }

    private fun compuTab(element: A2lElement): CompuTab? {
        val name = element.text(0) ?: return null
        val type = enumOf<ConversionType>(element, element.text(2), name) ?: ConversionType.TAB_INTP
        val pairs = element.values.drop(4).chunked(2).filter { it.size == 2 }.mapNotNull { (a, b) ->
            val x = a.asDouble()
            val y = b.asDouble()
            if (x == null || y == null) null else x to y
        }
        checkCount(element, name, pairs.size)
        return CompuTab(
            name, element.text(1).orEmpty(), type, pairs,
            element.child("DEFAULT_VALUE")?.text(0), element.child("DEFAULT_VALUE_NUMERIC")?.double(0), element
        )
    }

    private fun compuVTab(element: A2lElement): CompuVTab? {
        val name = element.text(0) ?: return null
        val type = enumOf<ConversionType>(element, element.text(2), name) ?: ConversionType.TAB_VERB
        val entries = element.values.drop(4).chunked(2).filter { it.size == 2 }.mapNotNull { (a, b) -> a.asDouble()?.let { it to b.text } }
        checkCount(element, name, entries.size)
        return CompuVTab(name, element.text(1).orEmpty(), type, entries, element.child("DEFAULT_VALUE")?.text(0), element)
    }

    private fun compuVTabRange(element: A2lElement): CompuVTabRange? {
        val name = element.text(0) ?: return null
        val entries = element.values.drop(3).chunked(3).filter { it.size == 3 }.mapNotNull { (min, max, text) ->
            val low = min.asDouble()
            val high = max.asDouble()
            if (low == null || high == null) null else VTabRange(low, high, text.text)
        }
        val declared = element.long(2)?.toInt()
        if (declared != null && declared != entries.size) {
            warn(element, "COMPU_VTAB_RANGE $name says it has $declared entries but has ${entries.size}", name)
        }
        return CompuVTabRange(name, element.text(1).orEmpty(), entries, element.child("DEFAULT_VALUE")?.text(0), element)
    }

    /** COMPU_TAB and COMPU_VTAB say how many entries they have in their fourth parameter */
    private fun checkCount(element: A2lElement, name: String, actual: Int) {
        val declared = element.long(3)?.toInt() ?: return
        if (declared != actual) warn(element, "${element.keyword} $name says it has $declared entries but has $actual", name)
    }

    private fun unit(element: A2lElement) = A2lUnit(
        name = element.text(0).orEmpty(),
        longId = element.text(1).orEmpty(),
        display = element.text(2).orEmpty(),
        type = element.text(3).orEmpty(),
        refUnit = element.child("REF_UNIT")?.text(0),
        siExponents = element.child("SI_EXPONENTS")?.values?.mapNotNull { it.asLong()?.toInt() },
        conversion = element.child("UNIT_CONVERSION")?.let { (it.double(0) ?: 1.0) to (it.double(1) ?: 0.0) }
    )

    // ------------------------------------------------ RECORD_LAYOUT

    private fun recordLayout(element: A2lElement): RecordLayout? {
        val name = element.text(0) ?: return null
        val entries = element.children.mapNotNull { layoutEntry(it, name) }.sortedBy { it.position }
        entries.filter { it !is LayoutEntry.FixNoAxisPts }.groupBy { it.position }.filterValues { it.size > 1 }.keys.forEach { position ->
            warn(element, "RECORD_LAYOUT $name has more than one entry at position $position", name)
        }
        return RecordLayout(
            name = name,
            entries = entries,
            alignments = alignments(element),
            staticRecordLayout = element.has("STATIC_RECORD_LAYOUT"),
            staticAddressOffsets = element.has("STATIC_ADDRESS_OFFSETS"),
            element = element
        )
    }

    private fun layoutEntry(element: A2lElement, layout: String): LayoutEntry? {
        val keyword = element.keyword
        val position = element.long(0)?.toInt() ?: 0
        fun type(index: Int = 1): DataType? = dataType(element, element.text(index), layout)
        fun axis(prefix: String) = keyword.removePrefix(prefix)
        return when {
            keyword == "FNC_VALUES" -> LayoutEntry.FncValues(
                position,
                type() ?: return null,
                enumOf<IndexMode>(element, element.text(2), layout) ?: IndexMode.ROW_DIR,
                enumOf<AddressType>(element, element.text(3), layout) ?: AddressType.DIRECT
            )
            keyword == "IDENTIFICATION" -> LayoutEntry.Identification(position, type() ?: return null)
            keyword.startsWith("AXIS_PTS_") -> LayoutEntry.AxisPts(
                axis("AXIS_PTS_"), position, type() ?: return null,
                enumOf<IndexOrder>(element, element.text(2), layout) ?: IndexOrder.INDEX_INCR,
                enumOf<AddressType>(element, element.text(3), layout) ?: AddressType.DIRECT
            )
            keyword.startsWith("AXIS_RESCALE_") -> LayoutEntry.AxisRescale(
                axis("AXIS_RESCALE_"), position, type() ?: return null, element.long(2)?.toInt() ?: 0,
                enumOf<IndexOrder>(element, element.text(3), layout) ?: IndexOrder.INDEX_INCR,
                enumOf<AddressType>(element, element.text(4), layout) ?: AddressType.DIRECT
            )
            keyword.startsWith("NO_AXIS_PTS_") -> LayoutEntry.NoAxisPts(axis("NO_AXIS_PTS_"), position, type() ?: return null)
            keyword.startsWith("NO_RESCALE_") -> LayoutEntry.NoRescale(axis("NO_RESCALE_"), position, type() ?: return null)
            keyword.startsWith("FIX_NO_AXIS_PTS_") -> LayoutEntry.FixNoAxisPts(axis("FIX_NO_AXIS_PTS_"), position)
            keyword.startsWith("SRC_ADDR_") -> LayoutEntry.SrcAddr(axis("SRC_ADDR_"), position, type() ?: return null)
            keyword.startsWith("RIP_ADDR_") -> LayoutEntry.RipAddr(axis("RIP_ADDR_"), position, type() ?: return null)
            keyword.startsWith("SHIFT_OP_") -> LayoutEntry.ShiftOp(axis("SHIFT_OP_"), position, type() ?: return null)
            keyword.startsWith("OFFSET_") -> LayoutEntry.Offset(axis("OFFSET_"), position, type() ?: return null)
            keyword.startsWith("DIST_OP_") -> LayoutEntry.DistOp(axis("DIST_OP_"), position, type() ?: return null)
            keyword == "RESERVED" -> LayoutEntry.Reserved(position, element.text(1) ?: "BYTE")
            else -> null
        }
    }

    // ------------------------------------------------ Organisation

    private fun function(element: A2lElement) = A2lFunction(
        name = element.text(0).orEmpty(),
        longId = element.text(1).orEmpty(),
        version = element.child("FUNCTION_VERSION")?.text(0),
        defCharacteristics = names(element, "DEF_CHARACTERISTIC"),
        refCharacteristics = names(element, "REF_CHARACTERISTIC"),
        inMeasurements = names(element, "IN_MEASUREMENT"),
        outMeasurements = names(element, "OUT_MEASUREMENT"),
        locMeasurements = names(element, "LOC_MEASUREMENT"),
        subFunctions = names(element, "SUB_FUNCTION")
    )

    private fun group(element: A2lElement) = A2lGroup(
        name = element.text(0).orEmpty(),
        longId = element.text(1).orEmpty(),
        root = element.has("ROOT"),
        refCharacteristics = names(element, "REF_CHARACTERISTIC"),
        refMeasurements = names(element, "REF_MEASUREMENT"),
        subGroups = names(element, "SUB_GROUP"),
        functions = names(element, "FUNCTION_LIST")
    )

    private fun frame(element: A2lElement) = Frame(
        element.text(0).orEmpty(), element.text(1).orEmpty(), element.long(2) ?: 0, element.long(3) ?: 0,
        element.children("FRAME_MEASUREMENT").flatMap { it.values.map(A2lValue::text) }
    )

    private fun userRights(element: A2lElement) = UserRights(
        element.text(0).orEmpty(),
        element.children("REF_GROUP").map { group -> group.values.map { it.text } },
        element.has("READ_ONLY")
    )

    private fun variantCoding(element: A2lElement) = VariantCoding(
        separator = element.child("VAR_SEPARATOR")?.text(0) ?: ".",
        naming = element.child("VAR_NAMING")?.text(0) ?: "NUMERIC",
        criteria = element.children("VAR_CRITERION").map { criterion ->
            VarCriterion(
                name = criterion.text(0).orEmpty(),
                longId = criterion.text(1).orEmpty(),
                values = criterion.values.drop(2).map { it.text },
                measurement = criterion.child("VAR_MEASUREMENT")?.text(0),
                selectionCharacteristic = criterion.child("VAR_SELECTION_CHARACTERISTIC")?.text(0)
            )
        },
        forbiddenCombinations = element.children("VAR_FORBIDDEN_COMB").map { comb ->
            comb.values.chunked(2).filter { it.size == 2 }.map { (criterion, value) -> criterion.text to value.text }
        },
        characteristics = element.children("VAR_CHARACTERISTIC").map { characteristic ->
            VarCharacteristic(
                name = characteristic.text(0).orEmpty(),
                criteria = characteristic.values.drop(1).map { it.text },
                addresses = characteristic.child("VAR_ADDRESS")?.values?.mapNotNull { it.asLong() }.orEmpty()
            )
        }
    )

    private fun names(element: A2lElement, keyword: String): List<String> =
        element.children(keyword).flatMap { child -> child.values.map { it.text } }

    // ------------------------------------------------ Helpers

    private fun ifData(element: A2lElement): List<IfData> = element.children("IF_DATA").map { block ->
        IfData(tagOf(block).orEmpty(), block.raw.orEmpty(), block.location)
    }

    /** The first name in an IF_DATA block: whose data it is */
    private fun tagOf(block: A2lElement): String? =
        block.raw?.trimStart()?.takeWhile { A2lLexer.isIdentPart(it) }?.takeIf { it.isNotEmpty() }

    private fun byteOrder(element: A2lElement, text: String?, owner: String? = null): ByteOrder? = when (text) {
        // MSB_LAST is little endian; BIG_ENDIAN and LITTLE_ENDIAN are deprecated synonyms that some files mix up
        "MSB_LAST", "LITTLE_ENDIAN", "MSB_LAST_MSW_FIRST" -> ByteOrder.LITTLE_ENDIAN
        "MSB_FIRST", "BIG_ENDIAN", "MSB_FIRST_MSW_LAST" -> ByteOrder.BIG_ENDIAN
        else -> null.also { warn(element, "Unknown BYTE_ORDER $text", owner) }
    }.also {
        if (text == "MSB_LAST_MSW_FIRST" || text == "MSB_FIRST_MSW_LAST") {
            warn(element, "BYTE_ORDER $text (word swapped) is read as plain ${if (it == ByteOrder.LITTLE_ENDIAN) "MSB_LAST" else "MSB_FIRST"}", owner)
        }
    }

    private fun dataType(element: A2lElement, text: String?, owner: String?): DataType? =
        DataType.of(text) ?: null.also { warn(element, "Unknown data type $text", owner) }

    private inline fun <reified E : Enum<E>> enumOf(element: A2lElement, text: String?, owner: String? = null): E? {
        if (text == null) return null
        return enumValues<E>().firstOrNull { it.name == text } ?: null.also { warn(element, "Unknown ${element.keyword} value $text", owner) }
    }

    private fun checkDuplicates(module: A2lModule) {
        fun <T> check(kind: String, items: List<T>, name: (T) -> String, element: (T) -> A2lElement) {
            items.groupBy(name).filterValues { it.size > 1 }.forEach { (duplicate, copies) ->
                warn(element(copies[1]), "$kind $duplicate is defined ${copies.size} times; the first is used", duplicate)
            }
        }
        check("CHARACTERISTIC", module.characteristics, { it.name }, { it.element })
        check("AXIS_PTS", module.axisPts, { it.name }, { it.element })
        check("MEASUREMENT", module.measurements, { it.name }, { it.element })
        check("COMPU_METHOD", module.compuMethods, { it.name }, { it.element })
        check("RECORD_LAYOUT", module.recordLayouts, { it.name }, { it.element })
    }

    private fun warn(element: A2lElement, message: String, objectName: String? = null) {
        found += Diagnostic(Severity.WARNING, message, element.location, objectName)
    }

    private fun error(element: A2lElement, message: String, objectName: String? = null) {
        found += Diagnostic(Severity.ERROR, message, element.location, objectName)
    }

    private companion object {
        val TABLE_TYPES = setOf(ConversionType.TAB_INTP, ConversionType.TAB_NOINTP, ConversionType.TAB_VERB)
    }
}
