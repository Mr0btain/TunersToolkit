package org.mr0btain.tunerstoolkit.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsTest {
    @Test
    fun countsBySeverity() {
        val diagnostics = Diagnostics()
        assertFalse(diagnostics.hasErrors)
        assertEquals("0 errors, 0 warnings", diagnostics.summary())

        diagnostics.info("Loaded")
        diagnostics.warning("Unknown keyword FOO", SourceLocation("a.a2l", 12, 5))
        diagnostics.error("Bad address", SourceLocation("a.a2l", 40), objectName = "KF_TEST")

        assertTrue(diagnostics.hasErrors)
        assertEquals(1, diagnostics.errorCount)
        assertEquals(1, diagnostics.warningCount)
        assertEquals(3, diagnostics.all.size)
        assertEquals("1 error, 1 warning", diagnostics.summary())
    }

    @Test
    fun readableText() {
        val diagnostic = Diagnostic(Severity.ERROR, "Bad address", SourceLocation("a.a2l", 40, 3), "KF_TEST")
        assertEquals("ERROR a.a2l:40:3 [KF_TEST]: Bad address", diagnostic.toString())
        assertEquals("WARNING: Odd", Diagnostic(Severity.WARNING, "Odd").toString())
    }
}
