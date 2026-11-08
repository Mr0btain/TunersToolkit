package org.mr0btain.tunerstoolkit.platform
class FilesManagerException : RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)
}