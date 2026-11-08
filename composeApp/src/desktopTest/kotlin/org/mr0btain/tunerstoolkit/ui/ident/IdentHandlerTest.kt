package org.mr0btain.tunerstoolkit.ui.ident

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.ident.IdentKind
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The demo identifies itself, and a project saved from it says so */
class IdentHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val ident = IdentHandler(scope, documents, inBackground = false)
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), readInBackground = false)

    @Test
    fun theDemoFillsInItsProject() {
        projects.openFile(hex, "tt-demo-1.hex")
        val calibration = ident.findings.first { it.kind == IdentKind.CALIBRATION }
        assertEquals("TTDEMO1/0.1/2026-11-07", calibration.value)

        projects.saveAsProject("Bench", "Stage 1", "", ident.suggest())
        assertEquals("TTDEMO1/0.1/2026-11-07", projects.projects.single().info.softwareNumber)

        // What's typed in stays
        val typed = projects.projects.single().info.copy(softwareNumber = "Mine")
        assertEquals("Mine", ident.suggest(typed).softwareNumber)

        documents.close()
        assertEquals(emptyList(), ident.findings)
    }
}
