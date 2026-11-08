package org.mr0btain.tunerstoolkit.platform
import com.sun.jna.platform.win32.Guid.GUID
import com.sun.jna.platform.win32.KnownFolders
import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import com.sun.jna.platform.win32.Win32Exception

/**
 * Windows specific application directory resolver
 */

class WindowsDirectoryResolver: DirectoryResolver() {
    enum class FolderIdentifiers {
        APPDATA, LOCAL_APPDATA, COMMON_APPDATA, DOWNLOADS
    }

    private val appData: String get() = resolveFolder(FolderIdentifiers.APPDATA)
    private val localAppData: String get() = resolveFolder(FolderIdentifiers.LOCAL_APPDATA)
    private val commonAppData: String get() = resolveFolder(FolderIdentifiers.COMMON_APPDATA)
    private val downloads: String get() = resolveFolder(FolderIdentifiers.DOWNLOADS)

    override fun getUserDataDir(appName: String, roaming: Boolean): String {
        val directory = if (roaming) appData else localAppData
        return buildPath(directory, appName)
    }

    override fun getUserCacheDir(appName: String): String {
        return buildPath(localAppData, appName, "\\Cache")
    }

    override fun getUserConfigDir(appName: String, roaming: Boolean): String {
        val directory = if (roaming) appData else localAppData
        return buildPath(directory, appName, "Config")
    }

    override fun getUserLogDir(appName: String): String {
        return buildPath(localAppData, appName, "\\Logs")
    }

    override fun getUserDownloadsDir(appName: String): String {
        return buildPath(downloads)
    }

    override fun getSharedDir(appName: String): String {
        return buildPath(commonAppData, appName)
    }

    private fun resolveFolder(folderIdentifiers: FolderIdentifiers?): String {
        try {
            return Shell32Util.getKnownFolderPath(convertFolderIdToGuid(folderIdentifiers!!))
        } catch (e: Win32Exception) {
            throw FilesManagerException("SHGetKnownFolderPath returns an error: $e")
        } catch (e: UnsatisfiedLinkError) {
            // Fallback for any before Windows vista
            try {
                val folder = convertFolderIdToCsidl(folderIdentifiers!!)
                return Shell32Util.getFolderPath(folder)
            } catch (e2: Win32Exception) {
                throw FilesManagerException("SHGetFolderPath returns an error: $e2")
            }
        }
    }

    private fun convertFolderIdToCsidl(folderIdentifiers: FolderIdentifiers): Int {
        return when (folderIdentifiers) {
            FolderIdentifiers.APPDATA -> ShlObj.CSIDL_APPDATA
            FolderIdentifiers.LOCAL_APPDATA -> ShlObj.CSIDL_LOCAL_APPDATA
            FolderIdentifiers.COMMON_APPDATA -> ShlObj.CSIDL_COMMON_APPDATA
            else -> throw FilesManagerException(
                "Unknown folder ID $folderIdentifiers was specified."
            )
        }
    }

    private fun convertFolderIdToGuid(folderIdentifiers: FolderIdentifiers): GUID {
        return when (folderIdentifiers) {
            FolderIdentifiers.APPDATA -> KnownFolders.FOLDERID_RoamingAppData
            FolderIdentifiers.LOCAL_APPDATA -> KnownFolders.FOLDERID_LocalAppData
            FolderIdentifiers.COMMON_APPDATA -> KnownFolders.FOLDERID_ProgramData
            FolderIdentifiers.DOWNLOADS -> KnownFolders.FOLDERID_Downloads
        }
    }
}