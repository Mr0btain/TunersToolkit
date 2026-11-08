package org.mr0btain.tunerstoolkit.ui.project

import org.mr0btain.tunerstoolkit.platform.DirectoryResolverFactory
import org.mr0btain.tunerstoolkit.core.project.FileSystemProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectStore
import java.io.File

/** `<user data dir>/TunersToolkit/store`, next to the legacy screens' `projects` folder */
actual fun platformProjectStore(): ProjectStore {
    val dataDir = DirectoryResolverFactory().instance.getUserDataDir("TunersToolkit", false)
    return FileSystemProjectStore(File(dataDir, "store").absolutePath)
}
