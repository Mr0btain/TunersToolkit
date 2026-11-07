package org.mr0btain.tunerstoolkit.components.testing

/**
 * Reads [path] from the checkout's testdata/ folder, or null when it isn't there: `demo/...` for the committed demo
 * files, `local/...` for real ECU files (git ignored, never committed). Only desktop can read it; other platforms
 * always get null
 */
expect fun readTestDataFile(path: String): ByteArray?
