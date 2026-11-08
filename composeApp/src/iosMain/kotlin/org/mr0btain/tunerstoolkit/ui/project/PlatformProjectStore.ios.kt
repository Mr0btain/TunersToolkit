package org.mr0btain.tunerstoolkit.ui.project

import org.mr0btain.tunerstoolkit.core.project.FileSystemProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectStore
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** Application Support, which is backed up but not shown in the Files app */
actual fun platformProjectStore(): ProjectStore {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    return FileSystemProjectStore("$support/TunersToolkit/store")
}
