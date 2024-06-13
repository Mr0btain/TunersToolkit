package components.datastorage.components

import java.util.*

/**
 * DirectoryResolver instance, fires up an AppDirectoryResolver for the OS that were running on
 */

class DirectoryResolverFactory {
    val instance: DirectoryResolver
        get() {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())

            return when {
                // Mac OS
                os.startsWith("mac os x") -> MacDirectoryResolver()

                // Windows OS
                os.startsWith("windows") -> WindowsDirectoryResolver()

                // Else we just assume its Linux
                else -> LinuxDirectoryResolver()

            }
        }
}