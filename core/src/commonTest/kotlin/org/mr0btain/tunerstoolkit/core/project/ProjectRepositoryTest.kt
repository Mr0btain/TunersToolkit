package org.mr0btain.tunerstoolkit.core.project

import kotlinx.coroutines.test.runTest
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.Patch
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectRepositoryTest {
    private var clock = 1_000L
    private var ids = 0
    private val store = MemoryProjectStore()
    private val repository = ProjectRepository(store, now = { clock++ }, newId = { "id${ids++}" })
    private val sourceBytes = ":00000001FF\n".encodeToByteArray()
    private val source = ProjectSource(fileName = "ecu.hex", format = "INTEL_HEX", size = 0)

    private fun <T> TkResult<T, ProjectError>.ok(): T = assertNotNull(getOrNull(), "Expected success, got $this")

    private fun image() = MemoryImage.of(0x1000L to ByteArray(16) { it.toByte() })

    @Test
    fun createKeepsTheSourceAndOnlyTheOriginal() = runTest {
        val project = repository.create("  ", source, sourceBytes).ok()
        assertEquals("ecu.hex", project.name)
        assertEquals(sourceBytes.size.toLong(), project.source.size)
        assertTrue(project.versions.isEmpty())
        assertContentEquals(sourceBytes, repository.source(project.id).ok())
        assertEquals(emptyList(), repository.patches(project.id, ProjectManifest.ORIGINAL).ok())
        assertEquals(listOf(project), repository.list().ok())
    }

    @Test
    fun versionsSaveAndReadBackTheirPatches() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val image = image()
        image.write(0x1002, byteArrayOf(0x7F, 0x7F))
        image.write(0x100F, byteArrayOf(0))
        val patches = image.patchesFromOriginal()

        val (updated, version) = repository.addVersion(project.id, "", patches, note = "More boost").ok()
        assertEquals("Version 1", version.name)
        assertEquals("More boost", version.note)
        assertEquals(3, version.changedBytes)
        assertEquals(listOf(version), updated.versions)
        assertEquals(version, updated.latest)

        val read = repository.patches(project.id, version.id).ok()
        assertEquals(patches, read)

        // Applied to a fresh copy of the original, they give the edited image
        val reopened = image()
        reopened.apply(read)
        assertTrue(reopened.contentEquals(image))
    }

    @Test
    fun updatingAVersionReplacesItsPatches() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val first = listOf(Patch(0x1000, byteArrayOf(0), byteArrayOf(1)))
        val (_, version) = repository.addVersion(project.id, "Stage 1", first).ok()
        val second = listOf(Patch(0x1004, byteArrayOf(4, 5), byteArrayOf(9, 9)))

        val updated = repository.updateVersion(project.id, version.id, second).ok()
        assertEquals(second, repository.patches(project.id, version.id).ok())
        assertEquals(2, updated.version(version.id)?.changedBytes)
        assertEquals("Stage 1", updated.version(version.id)?.name)
    }

    @Test
    fun theOriginalCantBeChanged() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val result = repository.updateVersion(project.id, ProjectManifest.ORIGINAL, emptyList())
        assertEquals(TkResult.Failure(ProjectError.READ_ONLY), result)
        assertEquals(TkResult.Failure(ProjectError.READ_ONLY), repository.deleteVersion(project.id, ProjectManifest.ORIGINAL))
    }

    @Test
    fun deletingAVersionAndAProject() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val (_, version) = repository.addVersion(project.id, "Stage 1", emptyList()).ok()
        val updated = repository.deleteVersion(project.id, version.id).ok()
        assertTrue(updated.versions.isEmpty())
        assertEquals(TkResult.Failure(ProjectError.VERSION_NOT_FOUND), repository.patches(project.id, version.id))

        repository.delete(project.id).ok()
        assertEquals(TkResult.Failure(ProjectError.NOT_FOUND), repository.manifest(project.id))
        assertTrue(store.list("").isEmpty())
    }

    @Test
    fun listIsNewestFirstAndSkipsBrokenProjects() = runTest {
        val older = repository.create("Older", source, sourceBytes).ok()
        val newer = repository.create("Newer", source, sourceBytes).ok()
        store.write(ProjectRepository.manifestPath("broken"), "not json".encodeToByteArray())

        assertEquals(listOf(newer, older), repository.list().ok())

        // Saving a version makes a project the most recent
        repository.addVersion(older.id, "v", emptyList()).ok()
        assertEquals(listOf(older.id, newer.id), repository.list().ok().map { it.id })
    }

    @Test
    fun manifestsFromANewerAppAreRefused() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val text = store.read(ProjectRepository.manifestPath(project.id))!!.decodeToString()
        store.write(ProjectRepository.manifestPath(project.id), text.replace("\"formatVersion\": 1", "\"formatVersion\": 99").encodeToByteArray())
        assertEquals(TkResult.Failure(ProjectError.NEWER_FORMAT), repository.manifest(project.id))
    }

    @Test
    fun renamingKeepsTheVersions() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        repository.addVersion(project.id, "Stage 1", emptyList()).ok()
        val renamed = repository.update(project.id) { it.copy(name = "Golf R", info = it.info.copy(ecu = "TT-DEMO-1"), versions = emptyList()) }.ok()
        assertEquals("Golf R", renamed.name)
        assertEquals("TT-DEMO-1", renamed.info.ecu)
        assertEquals(1, renamed.versions.size)
    }

    @Test
    fun aVersionsNameAndNoteChange() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val (_, version) = repository.addVersion(project.id, "Stage 1", emptyList()).ok()
        val updated = repository.updateVersionInfo(project.id, version.id, "Stage 1 (road)", "Tested").ok()
        assertEquals("Stage 1 (road)", updated.version(version.id)?.name)
        assertEquals("Tested", repository.manifest(project.id).ok().version(version.id)?.note)
        assertEquals(TkResult.Failure(ProjectError.VERSION_NOT_FOUND), repository.updateVersionInfo(project.id, "nope", "x", ""))
    }

    @Test
    fun aDefinitionIsKeptWithTheProject() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        assertEquals(TkResult.Success(null), repository.definition(project.id))
        val a2l = "ASAP2_VERSION 1 71".encodeToByteArray()
        val updated = repository.setDefinition(project.id, "golf.a2l", a2l).ok()
        assertEquals(ProjectDefinition("golf.a2l", a2l.size.toLong()), updated.definition)
        assertContentEquals(a2l, repository.definition(project.id).ok())
        // Renaming and versions keep it
        repository.update(project.id) { it.copy(name = "Golf R") }.ok()
        repository.addVersion(project.id, "Stage 1", emptyList()).ok()
        assertEquals("golf.a2l", repository.manifest(project.id).ok().definition?.fileName)
    }

    @Test
    fun otherFilesAreKeptByNameAndOnlySafeNames() = runTest {
        val project = repository.create("Golf", source, sourceBytes).ok()
        val updated = repository.setFile(project.id, "checksums.ttsum", "{}".encodeToByteArray()).ok()
        assertEquals(mapOf("checksums.ttsum" to 2L), updated.files)
        assertContentEquals("{}".encodeToByteArray(), repository.file(project.id, "checksums.ttsum").ok())
        assertEquals(emptyMap(), repository.setFile(project.id, "checksums.ttsum", null).ok().files)
        assertEquals(TkResult.Success(null), repository.file(project.id, "checksums.ttsum"))
        assertEquals(TkResult.Failure(ProjectError.SAVE_FAILED), repository.setFile(project.id, "../escape", ByteArray(1)))
        assertEquals(TkResult.Success(null), repository.file(project.id, "../../projects"))
    }
}
