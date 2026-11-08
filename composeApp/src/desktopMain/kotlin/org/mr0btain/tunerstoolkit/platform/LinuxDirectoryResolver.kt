package org.mr0btain.tunerstoolkit.platform
/**
 * Linux specific application directory resolver
 */

@Suppress("PrivatePropertyName")
class LinuxDirectoryResolver: DirectoryResolver() {
    // val XDG_CONFIG_DIRS: String = "XDG_CONFIG_DIRS"
    // val XDG_DATA_DIRS: String = "XDG_DATA_DIRS"
    private val XDG_CACHE_HOME: String = "XDG_CACHE_HOME"
    private val XDG_CONFIG_HOME: String = "XDG_CONFIG_HOME"
    private val XDG_DATA_HOME: String = "XDG_DATA_HOME"
    private val XDG_DOWNLOAD_DIR: String = "XDG_DOWNLOAD_DIR"
    private val sysEnv: Map<String, String> = System.getenv()

    override fun getUserDataDir(appName: String, roaming: Boolean): String {
        val dir = getOrDefault(
            XDG_DATA_HOME,
            buildPath(getUserHome(), "/.local/share")
        )

        return buildPath(dir, appName)
    }

    override fun getUserCacheDir(appName: String): String {
        val dir = getOrDefault(XDG_CACHE_HOME, buildPath(getUserHome(), "/.cache"))
        return buildPath(dir, appName)
    }

    override fun getUserConfigDir(appName: String, roaming: Boolean): String {
        val dir = getOrDefault(XDG_CONFIG_HOME, buildPath(getUserHome(), "/.config"))
        return buildPath(dir, appName)
    }

    override fun getUserLogDir(appName: String): String {
        val dir = getOrDefault(XDG_CACHE_HOME, buildPath(getUserHome(), "/.cache"))
        return buildPath(dir, appName, "/logs")
    }

    override fun getUserDownloadsDir(appName: String): String {
        val dir = getOrDefault(XDG_DOWNLOAD_DIR, buildPath(getUserHome(), "/Downloads"))
        return buildPath(dir, appName)
    }

    override fun getSharedDir(appName: String): String {
        return buildPath("/srv", appName)
    }

    private fun getOrDefault(key: String, def: String): String {
        val `val` = sysEnv[key]
        return `val` ?: def
    }

}