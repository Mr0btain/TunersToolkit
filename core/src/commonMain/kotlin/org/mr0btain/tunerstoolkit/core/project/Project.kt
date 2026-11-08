package org.mr0btain.tunerstoolkit.core.project

import kotlinx.serialization.Serializable
import org.mr0btain.tunerstoolkit.core.error.Error
import org.mr0btain.tunerstoolkit.core.image.Patch

/**
 * A project: the file the ECU image was read from, kept exactly as it was, plus saved versions of it. A version is
 * only the changes from that original, so it stays small and any two versions can be compared byte for byte.
 *
 * In a [ProjectStore] a project is a folder `projects/<id>/` holding `project.json` (this), `source.bin` (the
 * original file) and `versions/<version id>.json` (each version's [VersionPatches]).
 * @property formatVersion Bumped when the layout changes, so older saves can be moved over
 * @property versions Saved versions, oldest first. The original is always there too, as [ORIGINAL]
 */
@Serializable
data class ProjectManifest(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val source: ProjectSource,
    val info: ProjectInfo = ProjectInfo(),
    val versions: List<ProjectVersion> = emptyList(),
    val formatVersion: Int = FORMAT_VERSION
) {
    fun version(id: String): ProjectVersion? = versions.firstOrNull { it.id == id }

    /** The newest saved version, or null when only the original is there */
    val latest: ProjectVersion? get() = versions.maxByOrNull { it.createdAt }

    companion object {
        const val FORMAT_VERSION = 1

        /** The id that means the original image, with no changes. It's never in [versions] */
        const val ORIGINAL = "original"
    }
}

/**
 * The file a project's image came from
 * @property format Which BinaryFormat it was read as, by name
 * @property rawBaseAddress Where a raw binary starts in memory; ignored for formats that carry addresses
 */
@Serializable
data class ProjectSource(
    val fileName: String,
    val format: String,
    val size: Long,
    val rawBaseAddress: Long = 0
)

/** What the project is for, all optional; filled in by hand, and from the A2L and ident strings later */
@Serializable
data class ProjectInfo(
    val vehicle: String = "",
    val ecu: String = "",
    val softwareNumber: String = "",
    val notes: String = ""
)

/**
 * One saved version
 * @property basedOn The version it was saved from ([ProjectManifest.ORIGINAL] for the original)
 * @property changedBytes How many bytes differ from the original
 */
@Serializable
data class ProjectVersion(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val note: String = "",
    val basedOn: String = ProjectManifest.ORIGINAL,
    val changedBytes: Long = 0
)

/** A version's changes from the original, as saved */
@Serializable
data class VersionPatches(val patches: List<SavedPatch>, val formatVersion: Int = ProjectManifest.FORMAT_VERSION)

/** A [Patch] as saved, its bytes in hex */
@Serializable
data class SavedPatch(val address: Long, val before: String, val after: String) {
    @OptIn(ExperimentalStdlibApi::class)
    fun toPatch(): Patch = Patch(address, before.hexToByteArray(), after.hexToByteArray())

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun of(patch: Patch): SavedPatch = SavedPatch(patch.address, patch.before.toHexString(), patch.after.toHexString())
    }
}

/** Why a project couldn't be read or saved */
enum class ProjectError(override val message: String) : Error {
    NOT_FOUND("That project isn't there any more"),
    VERSION_NOT_FOUND("That version isn't there any more"),
    UNREADABLE("The project's files couldn't be read"),
    NEWER_FORMAT("The project was saved by a newer Tuners Toolkit"),
    READ_ONLY("The original can't be changed; save a new version instead"),
    SAVE_FAILED("The project couldn't be saved")
}
