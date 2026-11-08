package org.mr0btain.tunerstoolkit.ui.definition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.a2l.EpkResult
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Opening the demo A2L with its image, indexing it, and keeping it with a project */
class DefinitionHandlerTest {
    private val hex = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val a2l = File("../testdata/demo/tt-demo-1.a2l").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val definitions = DefinitionHandler(scope, documents, inBackground = false)
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), definitions, readInBackground = false)

    @Test
    fun indexesTheImageWhicheverOpensFirst() {
        definitions.open(a2l, "tt-demo-1.a2l")
        assertNull(definitions.onImage, "no image yet")
        projects.openFile(hex, "tt-demo-1.hex")
        val onImage = assertNotNull(definitions.onImage)
        assertEquals(definitions.definition!!.set.objects.size, onImage.build.decoded)
        assertIs<EpkResult.Match>(onImage.epk)
        assertTrue(onImage.overlaps.isEmpty())

        val ignition = definitions.definition!!.set["IgnitionBase"]!!
        val values = assertIs<TkResult.Success<*>>(definitions.decode(ignition))
        assertNotNull(values.data)

        // Closing the image drops the index; the definition stays
        documents.close()
        assertNull(definitions.onImage)
        assertNotNull(definitions.definition)
    }

    @Test
    fun aBrokenA2lIsReported() {
        definitions.open(ByteArray(0), "empty.a2l")
        assertNull(definitions.definition)
        assertNotNull(definitions.openError)
    }

    @Test
    fun theDefinitionIsSavedWithTheProjectAndOpensWithIt() {
        projects.openFile(hex, "tt-demo-1.hex")
        definitions.open(a2l, "tt-demo-1.a2l")
        projects.saveAsProject("Bench", "Stage 1", "")
        val saved = projects.projects.single()
        assertEquals("tt-demo-1.a2l", saved.definition?.fileName)
        assertFalse(projects.hasUnsavedChanges)

        definitions.close()
        projects.openFile(hex, "tt-demo-1.hex")
        assertNull(definitions.definition)
        projects.openProject(saved)
        assertEquals("tt-demo-1.a2l", definitions.definition?.name)
        assertNotNull(definitions.onImage)
        assertFalse(projects.hasUnsavedChanges)
    }

    @Test
    fun aDefinitionOpenedLaterIsUnsavedUntilSaved() {
        projects.openFile(hex, "tt-demo-1.hex")
        projects.saveAsProject("Bench", "Stage 1", "")
        assertFalse(projects.hasUnsavedChanges)
        definitions.open(a2l, "tt-demo-1.a2l")
        assertTrue(projects.hasUnsavedChanges)
        projects.saveAsVersion("With the A2L", "")
        assertFalse(projects.hasUnsavedChanges)
        assertNotNull(projects.projects.single().definition)
    }
}
