package org.mr0btain.tunerstoolkit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform