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

/** An element as plain data, for checking two trees say the same thing (locations and layout aside) */
internal fun A2lElement.shape(): String = buildString {
    append(keyword).append(if (block) "{" else "")
    values.forEach { append(' ').append(it.kind.name.first()).append(':').append(it.text) }
    raw?.let { append(" raw:").append(it.trim().replace(Regex("\\s+"), " ")) }
    children.forEach { append('\n').append(it.shape().prependIndent("  ")) }
}

internal fun A2lTree.shape(): String = elements.joinToString("\n") { it.shape() }

/** Writes [text]'s tree back out and checks it reads as the same tree; returns the written text */
internal fun assertRoundTrip(text: String): String {
    val first = A2lParser(text, "first.a2l").parse()
    val written = A2lWriter.write(first)
    val second = A2lParser(written, "second.a2l")
    val tree = second.parse()
    assertEquals(first.shape(), tree.shape())
    return written
}
