package org.mr0btain.tunerstoolkit.components.testing

/**
 * Reads [path] from testdata/local/ (git ignored, for real ECU files we can't commit), or null when it isn't there.
 * Only desktop can read it; other platforms always get null
 */
expect fun readLocalTestFile(path: String): ByteArray?
