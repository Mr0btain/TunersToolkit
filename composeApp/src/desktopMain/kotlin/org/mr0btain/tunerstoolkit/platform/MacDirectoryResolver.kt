package org.mr0btain.tunerstoolkit.platform
/**
 * Mac specific application directory resolver
 */

class MacDirectoryResolver: DirectoryResolver() {
    private val appSupport = "/Library/Application Support"
    private val shared = "/Users/Shared/Library/Application Support"
    private val downloads = "/Downloads"
    private val logs = "/Library/Logs"
    private val preferences = "/Library/Preferences"
    private val cache = "/Library/Caches"

    override fun getUserDataDir(appName: String, roaming: Boolean): String {
        return buildPath(getUserHome(), appSupport, appName)
    }

    override fun getUserCacheDir(appName: String): String {
        return buildPath(getUserHome(), cache, appName)
    }

    override fun getUserConfigDir(appName: String, roaming: Boolean): String {
        return buildPath(getUserHome(), preferences, appName)
    }

    override fun getUserLogDir(appName: String): String {
        return buildPath(getUserHome(), logs, appName)
    }

    override fun getUserDownloadsDir(appName: String): String {
        return buildPath(getUserHome(), downloads, appName)
    }

    override fun getSharedDir(appName: String): String {
        return buildPath(shared, appName)
    }
}