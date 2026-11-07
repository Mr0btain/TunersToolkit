package org.mr0btain.tunerstoolkit.components.testing

import java.io.File

/** :composeApp:run passes the folder as tunerstoolkit.testdata; otherwise it's looked for from the working directory */
actual fun readLocalTestFile(path: String): ByteArray? {
    val roots = listOfNotNull(System.getProperty("tunerstoolkit.testdata"), "testdata/local", "../testdata/local")
    return roots.map { File(it, path) }.firstOrNull { it.isFile }?.readBytes()
}
