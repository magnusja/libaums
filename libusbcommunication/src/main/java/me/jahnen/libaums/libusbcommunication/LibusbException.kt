package me.jahnen.libaums.libusbcommunication

open class LibusbException(
    message: String,
    val libusbError: LibusbError,
    cause: Throwable? = null
) :
    ErrNoIOException("$message: ${libusbError.message} [${libusbError.code}]", cause)
