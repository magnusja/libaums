package me.jahnen.libaums.server.http

import me.jahnen.libaums.core.fs.UsbFile

import java.io.IOException

interface UsbFileProvider {
    @Throws(IOException::class)
    fun determineFileToServe(uri: String): UsbFile
}
