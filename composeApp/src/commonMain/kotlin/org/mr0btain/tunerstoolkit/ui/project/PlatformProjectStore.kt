package org.mr0btain.tunerstoolkit.ui.project

import org.mr0btain.tunerstoolkit.core.project.ProjectStore

/**
 * Where this platform keeps projects: a folder in the app's own data on desktop, Android and iOS, and the Origin
 * Private File System on the web (or memory, when the browser has neither)
 */
expect fun platformProjectStore(): ProjectStore
