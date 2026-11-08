package org.mr0btain.tunerstoolkit.ui.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.core.project.ProjectRepository
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Saving the demo image as a project and opening its versions again, all in memory */
class ProjectHandlerTest {
    private val demo = File("../testdata/demo/tt-demo-1.hex").readBytes()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val documents = DocumentHandler(scope)
    private val messages = mutableListOf<String>()
    private val projects = ProjectHandler(scope, documents, ProjectRepository(MemoryProjectStore()), notify = { messages += it }, readInBackground = false)

    private fun open() = projects.openFile(demo, "tt-demo-1.hex")

    @Test
    fun saveEditsAsAProjectAndOpenThemAgain() {
        open()
        assertNull(projects.current)
        assertFalse(projects.hasUnsavedChanges)
        val original = documents.document!!.image.byteAt(0x10)

        documents.write(0x10, byteArrayOf(0x5A))
        assertTrue(projects.hasUnsavedChanges)
        projects.saveAsProject("Bench", "Stage 1", "")

        val current = assertNotNull(projects.current)
        assertEquals("Stage 1", current.versionName)
        assertFalse(projects.hasUnsavedChanges)
        assertEquals(listOf("Bench"), projects.projects.map { it.name })

        // Change it more and save in place
        documents.write(0x11, byteArrayOf(0x5B))
        projects.save()
        assertFalse(projects.hasUnsavedChanges)

        // A fresh open of that version has both changes, and undo can't take them off
        open()
        projects.openProject(projects.projects.single(), current.versionId)
        val image = documents.document!!.image
        assertEquals(0x5A, image.byteAt(0x10))
        assertEquals(0x5B, image.byteAt(0x11))
        assertFalse(image.canUndo)
        assertEquals(2, image.changedRanges().sumOf { it.size })

        // The original is still there underneath
        projects.openProject(projects.projects.single(), ProjectManifest.ORIGINAL)
        assertEquals(original, documents.document!!.image.byteAt(0x10))
        assertTrue(projects.current!!.isOriginal)
    }

    @Test
    fun saveAsAVersionKeepsTheOneItCameFrom() {
        open()
        documents.write(0x10, byteArrayOf(0x71))
        projects.saveAsProject("Bench", "Stage 1", "")
        val first = projects.current!!.versionId
        documents.write(0x10, byteArrayOf(0x72))
        projects.saveAsVersion("Stage 2", "More")

        val manifest = projects.projects.single()
        assertEquals(listOf("Stage 1", "Stage 2"), manifest.versions.map { it.name })
        assertEquals(first, manifest.versions[1].basedOn)

        projects.openProject(manifest, first)
        assertEquals(0x71, documents.document!!.image.byteAt(0x10))
    }

    @Test
    fun deletingTheOpenProjectLeavesTheImageOpen() {
        open()
        projects.saveAsProject("Bench", "", "")
        assertTrue(projects.current!!.isOriginal)
        projects.deleteProject(projects.current!!.manifest.id)
        assertNull(projects.current)
        assertTrue(projects.projects.isEmpty())
        assertNotNull(documents.document)
    }
}
