package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.Imported
import kotlin.test.assertEquals
import kotlin.test.fail

/** Reads [text] as a whole A2L file */
internal fun readA2l(text: String, resolver: A2lFileResolver? = null): Imported<A2lFile> =
    when (val result = A2lImport.readText(text, "test.a2l", resolver)) {
        is TkResult.Success -> result.data
        is TkResult.Failure -> fail("Couldn't read the A2L: ${result.error.message}")
    }

/** Reads [body] as the inside of one MODULE */
internal fun readModule(body: String): Pair<A2lModule, List<Diagnostic>> {
    val imported = readA2l(
        """
        ASAP2_VERSION 1 71
        /begin PROJECT P ""
          /begin MODULE M ""
            $body
          /end MODULE
        /end PROJECT
        """.trimIndent()
    )
    return imported.value.modules.single() to imported.diagnostics
}

/** Reads [body] as a MODULE's inside and checks nothing was wrong with it */
internal fun cleanModule(body: String): A2lModule {
    val (module, diagnostics) = readModule(body)
    assertEquals(emptyList(), diagnostics.filter { it.severity != Severity.INFO }.map { it.toString() })
    return module
}

internal fun List<Diagnostic>.messages(severity: Severity? = null): List<String> =
    filter { severity == null || it.severity == severity }.map { it.message }
