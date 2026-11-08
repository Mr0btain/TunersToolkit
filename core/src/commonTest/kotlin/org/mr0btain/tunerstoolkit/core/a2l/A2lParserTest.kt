package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ImportError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class A2lParserTest {
    private fun parse(text: String, resolver: A2lFileResolver? = null): Pair<A2lTree, A2lParser> {
        val parser = A2lParser(text, "main.a2l", resolver)
        return parser.parse() to parser
    }

    @Test
    fun blocksKeywordsAndParameters() {
        val (tree, parser) = parse(
            """
            /begin PROJECT P "Project" /begin HEADER "h" VERSION "1" PROJECT_NO X1 /end HEADER /end PROJECT
            """
        )
        assertTrue(parser.diagnostics.isEmpty(), parser.diagnostics.toString())
        val project = tree.elements.single()
        assertEquals("PROJECT", project.keyword)
        assertEquals(listOf("P", "Project"), project.values.map { it.text })
        val header = assertNotNull(project.child("HEADER"))
        assertEquals("1", header.child("VERSION")?.text(0))
        assertEquals("X1", header.child("PROJECT_NO")?.text(0))
    }

    @Test
    fun aNameThatLooksLikeAKeywordIsStillAParameter() {
        // The record layout here is called FORMAT: fixed parameters are read by position whatever they look like
        val (tree, parser) = parse("/begin CHARACTERISTIC c \"\" VALUE 0x10 FORMAT 0 NO_COMPU_METHOD 0 1 FORMAT \"%4.1\" /end CHARACTERISTIC")
        assertTrue(parser.diagnostics.isEmpty(), parser.diagnostics.toString())
        val characteristic = tree.elements.single()
        assertEquals("FORMAT", characteristic.text(4))
        assertEquals("%4.1", characteristic.child("FORMAT")?.text(0))
    }

    @Test
    fun unknownKeywordsAreKeptWithAWarning() {
        val (tree, parser) = parse(
            "/begin MODULE M \"\" MY_TOOL_SETTING 1 2 \"x\" /begin MY_BLOCK a b /end MY_BLOCK NUMBER 3 /end MODULE"
        )
        val module = tree.elements.single()
        assertEquals(listOf("MY_TOOL_SETTING", "MY_BLOCK", "NUMBER"), module.children.map { it.keyword })
        assertEquals(listOf("1", "2", "x"), module.children[0].values.map { it.text })
        assertFalse(module.children[0].known)
        assertEquals(listOf("a", "b"), module.children[1].values.map { it.text })
        assertEquals("3", module.child("NUMBER")?.text(0))
        assertEquals(2, parser.diagnostics.count { it.severity == Severity.WARNING })
    }

    @Test
    fun missingParametersAreAnError() {
        val (tree, parser) = parse("/begin MODULE M \"\" COEFFS_LINEAR 1 /end MODULE")
        assertEquals(listOf("1"), tree.elements.single().child("COEFFS_LINEAR")?.values?.map { it.text })
        val error = parser.diagnostics.single { it.severity == Severity.ERROR }
        assertTrue("COEFFS_LINEAR is missing parameters" in error.message, error.message)
        assertEquals(1, error.location?.line)
    }

    @Test
    fun aMissingEndClosesAtTheEndOfTheFile() {
        val (tree, parser) = parse("/begin PROJECT P \"\"\n/begin MODULE M \"\"\nNUMBER 1")
        val module = tree.elements.single().child("MODULE")
        assertEquals("1", module?.child("NUMBER")?.text(0))
        assertEquals(2, parser.diagnostics.count { "isn't closed" in it.message })
    }

    @Test
    fun aMismatchedEndIsReportedAndTakenAsTheRightOne() {
        val (tree, parser) = parse("/begin GROUP g \"\" ROOT /end FUNCTION NUMBER 2")
        assertEquals(listOf("GROUP", "NUMBER"), tree.elements.map { it.keyword })
        assertTrue(parser.diagnostics.any { "doesn't match" in it.message && it.location?.line == 1 })
    }

    @Test
    fun valueListsAndRepeatedGroups() {
        val (tree, parser) = parse(
            """
            /begin COMPU_VTAB v "" TAB_VERB 2 0 "off" 1 "on" DEFAULT_VALUE "?" /end COMPU_VTAB
            /begin REF_CHARACTERISTIC a NUMBER c /end REF_CHARACTERISTIC
            /begin VAR_CRITERION Car "" Sedan Wagon VAR_MEASUREMENT body /end VAR_CRITERION
            MATRIX_DIM 2 3 4
            """
        )
        assertTrue(parser.diagnostics.isEmpty(), parser.diagnostics.toString())
        val (vtab, refs, criterion, dim) = tree.elements
        assertEquals(listOf("v", "", "TAB_VERB", "2", "0", "off", "1", "on"), vtab.values.map { it.text })
        assertEquals("?", vtab.child("DEFAULT_VALUE")?.text(0))
        // A name list in its own block takes everything, keyword or not
        assertEquals(listOf("a", "NUMBER", "c"), refs.values.map { it.text })
        assertEquals(listOf("Car", "", "Sedan", "Wagon"), criterion.values.map { it.text })
        assertEquals("body", criterion.child("VAR_MEASUREMENT")?.text(0))
        assertEquals(listOf(2L, 3L, 4L), dim.values.map { it.asLong() })
    }

    @Test
    fun ifDataAndA2mlAreKeptAsText() {
        val (tree, parser) = parse(
            "/begin A2ML block \"IF_DATA\" taggedunion { \"XCP\" struct { int; }; }; /end A2ML\n" +
                "/begin IF_DATA XCP /begin SEGMENT 1 2 /end SEGMENT ; odd { } /end IF_DATA"
        )
        assertTrue(parser.diagnostics.isEmpty(), parser.diagnostics.toString())
        assertEquals(" block \"IF_DATA\" taggedunion { \"XCP\" struct { int; }; }; ", tree.elements[0].raw)
        assertEquals(" XCP /begin SEGMENT 1 2 /end SEGMENT ; odd { } ", tree.elements[1].raw)
    }

    @Test
    fun includesAreReadThroughTheResolver() {
        val files = mapOf(
            "parts/layouts.a2l" to "/begin RECORD_LAYOUT L FNC_VALUES 1 UBYTE ROW_DIR DIRECT /end RECORD_LAYOUT /include \"more.a2l\"",
            "more.a2l" to "NUMBER 9"
        )
        val resolver = A2lFileResolver { path, _ -> files[path]?.encodeToByteArray() }
        val (tree, parser) = parse("/begin MODULE M \"\" /include \"parts/layouts.a2l\" /end MODULE", resolver)
        assertTrue(parser.diagnostics.isEmpty(), parser.diagnostics.toString())
        assertEquals(listOf("RECORD_LAYOUT", "NUMBER"), tree.elements.single().children.map { it.keyword })
        assertEquals("parts/layouts.a2l", tree.elements.single().children[0].location.file)
    }

    @Test
    fun includesThatLoopOrAreMissingAreSkipped() {
        val files = mapOf("a.a2l" to "/include \"b.a2l\"", "b.a2l" to "/include \"a.a2l\" NUMBER 1")
        val resolver = A2lFileResolver { path, _ -> files[path]?.encodeToByteArray() }
        val (tree, parser) = parse("/include \"a.a2l\" /include \"gone.a2l\"", resolver)
        assertEquals(listOf("NUMBER"), tree.walk().map { it.keyword }.toList())
        assertTrue(parser.diagnostics.any { "includes itself" in it.message })
        assertTrue(parser.diagnostics.any { "couldn't be found" in it.message })

        val (_, without) = parse("/include \"a.a2l\"")
        assertEquals(Severity.WARNING, without.diagnostics.single().severity)
    }

    @Test
    fun aFileWithoutAProjectFails() {
        assertEquals(TkResult.Failure(ImportError.NO_DATA), A2lImport.readText("ASAP2_VERSION 1 71"))
        assertEquals(TkResult.Failure(ImportError.EMPTY_FILE), A2lImport.read(ByteArray(0)))
    }
}
