package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checklist: structure, MOD_PAR and MOD_COMMON */
class A2lStructureTest {
    @Test
    fun versionsProjectHeaderAndSeveralModules() {
        val imported = readA2l(
            """
            ASAP2_VERSION 1 71
            A2ML_VERSION 1 31
            /begin PROJECT Car "A car"
              /begin HEADER "About" VERSION "2.0" PROJECT_NO P42 /end HEADER
              /begin MODULE Engine "Engine ECU" /end MODULE
              /begin MODULE Gearbox "Gearbox ECU" /end MODULE
            /end PROJECT
            """.trimIndent()
        )
        assertTrue(imported.diagnostics.isEmpty(), imported.diagnostics.toString())
        val file = imported.value
        assertEquals("1.71", file.asap2Version)
        assertEquals("1.31", file.a2mlVersion)
        val project = assertNotNull(file.project)
        assertEquals("Car", project.name)
        assertEquals("A car", project.longId)
        assertEquals("About", project.header?.comment)
        assertEquals("2.0", project.header?.version)
        assertEquals("P42", project.header?.projectNo)
        assertEquals(listOf("Engine", "Gearbox"), file.modules.map { it.name })
        assertEquals("Gearbox ECU", file.modules[1].longId)
    }

    @Test
    fun modPar() {
        val module = cleanModule(
            """
            /begin MOD_PAR "Comment"
              VERSION "V1"
              ADDR_EPK 0x80000000
              ADDR_EPK 0x80010000
              EPK "EPK-1"
              SUPPLIER "S" CUSTOMER "C" CUSTOMER_NO "123" USER "U" PHONE_NO "555" ECU "ECU1" CPU_TYPE "TC1797"
              NO_OF_INTERFACES 2
              ECU_CALIBRATION_OFFSET 0x1000
              /begin CALIBRATION_METHOD "InCircuit" 2
                /begin CALIBRATION_HANDLE 0x10 0x20 0x30 CALIBRATION_HANDLE_TEXT "page" /end CALIBRATION_HANDLE
              /end CALIBRATION_METHOD
              /begin MEMORY_LAYOUT PRG_DATA 0x8000 0x1000 -1 -1 -1 -1 -1 /end MEMORY_LAYOUT
              /begin MEMORY_SEGMENT Cal "Calibration" DATA FLASH INTERN 0x80020000 0x10000 -1 -1 -1 -1 -1
                /begin IF_DATA XCP /begin SEGMENT 0 2 0 0 0 /end SEGMENT /end IF_DATA
              /end MEMORY_SEGMENT
              SYSTEM_CONSTANT "CYLINDERS" "4"
              SYSTEM_CONSTANT "VARIANT" "B"
            /end MOD_PAR
            """
        )
        val modPar = assertNotNull(module.modPar)
        assertEquals("Comment", modPar.comment)
        assertEquals("V1", modPar.version)
        assertEquals(listOf(0x80000000L, 0x80010000L), modPar.addrEpk)
        assertEquals("EPK-1", modPar.epk)
        assertEquals(listOf("S", "C", "123", "U", "555", "ECU1", "TC1797"), listOf(modPar.supplier, modPar.customer, modPar.customerNo, modPar.user, modPar.phoneNo, modPar.ecu, modPar.cpuType))
        assertEquals(2, modPar.noOfInterfaces)
        assertEquals(0x1000L, modPar.ecuCalibrationOffset)
        val method = modPar.calibrationMethods.single()
        assertEquals("InCircuit", method.method)
        assertEquals(2, method.version)
        assertEquals(listOf(0x10L, 0x20L, 0x30L), method.handles.single().values)
        assertEquals("page", method.handles.single().text)
        assertEquals(0x8000L, modPar.memoryLayouts.single().address)
        val segment = modPar.memorySegments.single()
        assertEquals("Cal", segment.name)
        assertEquals("DATA", segment.prgType)
        assertEquals("FLASH", segment.memoryType)
        assertEquals("INTERN", segment.attribute)
        assertEquals(0x80020000L, segment.address)
        assertEquals(0x10000L, segment.size)
        assertEquals(listOf(-1L, -1L, -1L, -1L, -1L), segment.offsets)
        assertEquals("XCP", segment.ifData.single().tag)
        assertEquals(mapOf("CYLINDERS" to "4", "VARIANT" to "B"), modPar.systemConstants)
    }

    @Test
    fun modCommon() {
        val module = cleanModule(
            """
            /begin MOD_COMMON "Defaults"
              BYTE_ORDER MSB_FIRST DEPOSIT ABSOLUTE DATA_SIZE 16 S_REC_LAYOUT S1
              ALIGNMENT_BYTE 1 ALIGNMENT_WORD 2 ALIGNMENT_LONG 4 ALIGNMENT_INT64 8
              ALIGNMENT_FLOAT16_IEEE 2 ALIGNMENT_FLOAT32_IEEE 4 ALIGNMENT_FLOAT64_IEEE 8
            /end MOD_COMMON
            """
        )
        val common = assertNotNull(module.modCommon)
        assertEquals("Defaults", common.comment)
        assertEquals(ByteOrder.BIG_ENDIAN, common.byteOrder)
        assertEquals(Deposit.ABSOLUTE, common.deposit)
        assertEquals(16, common.dataSize)
        assertEquals("S1", common.sRecLayout)
        assertEquals(
            mapOf("BYTE" to 1, "WORD" to 2, "LONG" to 4, "INT64" to 8, "FLOAT16_IEEE" to 2, "FLOAT32_IEEE" to 4, "FLOAT64_IEEE" to 8),
            common.alignments
        )
    }

    @Test
    fun byteOrderNames() {
        fun order(name: String) = readModule("/begin MOD_COMMON \"\" BYTE_ORDER $name /end MOD_COMMON").first.modCommon?.byteOrder
        assertEquals(ByteOrder.LITTLE_ENDIAN, order("MSB_LAST"))
        assertEquals(ByteOrder.BIG_ENDIAN, order("MSB_FIRST"))
        assertEquals(ByteOrder.LITTLE_ENDIAN, order("LITTLE_ENDIAN"))
        assertEquals(ByteOrder.BIG_ENDIAN, order("BIG_ENDIAN"))
        assertNull(order("SIDEWAYS"))
        assertTrue(readModule("/begin MOD_COMMON \"\" BYTE_ORDER MSB_LAST_MSW_FIRST /end MOD_COMMON").second.any { "word swapped" in it.message })
    }

    @Test
    fun a2mlAndIfData() {
        val (module, diagnostics) = readModule(
            """
            /begin A2ML
              struct Protocol_Layer { uint; uint; taggedstruct { "SEED_AND_KEY" char[256]; }; };
              enum Mode { "ON" = 1, "OFF" = 0 };
              block "IF_DATA" taggedunion {
                "XCP" taggedstruct { block "PROTOCOL_LAYER" struct Protocol_Layer; ("SEGMENT" uint)*; };
                "CANAPE_EXT" struct { int; long; };
              };
            /end A2ML
            /begin IF_DATA XCP /begin PROTOCOL_LAYER 1 2 /end PROTOCOL_LAYER /end IF_DATA
            /begin IF_DATA ETK something /end IF_DATA
            """
        )
        val a2ml = assertNotNull(module.a2ml)
        assertEquals(setOf("XCP", "CANAPE_EXT"), a2ml.ifDataTags)
        val mode = a2ml.types["Mode"] as A2mlType.Enum
        assertEquals(mapOf("ON" to 1L, "OFF" to 0L), mode.values)
        val protocol = a2ml.types["Protocol_Layer"] as A2mlType.Struct
        assertEquals(listOf(256L), (protocol.members[2].type as A2mlType.TaggedStruct).members.single().dimensions)
        val xcp = (a2ml.resolve(a2ml.blocks.single().type) as A2mlType.TaggedUnion).members.first()
        val xcpMembers = (xcp.type as A2mlType.TaggedStruct).members
        assertEquals(A2mlType.Ref("struct", "Protocol_Layer"), xcpMembers[0].type)
        assertTrue(xcpMembers[0].block)
        assertTrue(xcpMembers[1].repeated)
        assertEquals(listOf("XCP", "ETK"), module.ifData.map { it.tag })
        assertEquals(listOf("IF_DATA ETK isn't in the module's A2ML"), diagnostics.messages(Severity.WARNING))
    }

    @Test
    fun brokenA2mlIsReportedAndTheRestRead() {
        val (module, diagnostics) = readModule(
            "/begin A2ML struct { what; }; block \"IF_DATA\" taggedunion { \"XCP\" uint; }; /end A2ML"
        )
        assertEquals(setOf("XCP"), module.a2ml?.ifDataTags)
        assertTrue(diagnostics.any { it.message.startsWith("A2ML:") })
    }
}
