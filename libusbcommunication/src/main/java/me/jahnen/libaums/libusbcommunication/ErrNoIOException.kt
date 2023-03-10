package me.jahnen.libaums.libusbcommunication

import me.jahnen.libaums.core.ErrNo
import java.io.IOException

/**
 * IOException that captures the errno and errstr of the current thread.
 */
open class ErrNoIOException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    val errno = ErrNo.errno
    val errstr = ErrNo.errstr
}