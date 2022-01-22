package me.jahnen.libaums.server.http.exception

import java.io.IOException

/**
 * Created by magnusja on 16/12/16.
 */

class NotAFileException : IOException("Directory listing is not supported, please request a file.")
