package org.mr0btain.tunerstoolkit

class JVMPlatform: Platform {
    override val name: String = "Java"
}

actual fun getPlatform(): Platform = JVMPlatform()