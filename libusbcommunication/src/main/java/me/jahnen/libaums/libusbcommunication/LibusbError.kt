package me.jahnen.libaums.libusbcommunication

enum class LibusbError(val code: Int, val message: String) {
    SUCCESS(0, "Success (no error)"),
    IO(-1, "Input/output error"),
    INVALID_PARAM(-2, "Invalid parameter"),
    ACCESS(-3, "Access denied (insufficient permissions)"),
    NO_DEVICE(-4, "No such device (it may have been disconnected)"),
    NOT_FOUND(-5, "Entity not found"),
    BUSY(-6, "Resource busy"),
    TIMEOUT(-7, "Operation timed out"),
    OVERFLOW(-8, "Overflow"),
    PIPE(-9, "Pipe error"),
    INTERRUPTED(-10, "System call interrupted (perhaps due to signal)"),
    NO_MEM(-11, "Insufficient memory"),
    NOT_SUPPORTED(-12, "Operation not supported or unimplemented on this platform"),
    OTHER(-99, "Other error");

    companion object {
        fun fromCode(code: Int): LibusbError {
            return values().firstOrNull { it.code == code } ?: OTHER
        }
    }
}