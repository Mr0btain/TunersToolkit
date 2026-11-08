package org.mr0btain.tunerstoolkit.ui.project

import org.mr0btain.tunerstoolkit.appContext
import org.mr0btain.tunerstoolkit.core.project.FileSystemProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectStore
import java.io.File

/** The app's private files, `files/store` */
actual fun platformProjectStore(): ProjectStore = FileSystemProjectStore(File(appContext.filesDir, "store").absolutePath)
