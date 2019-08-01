package com.github.mjdev.libaums.server.http

import com.github.mjdev.libaums.fs.UsbFile

import java.io.IOException

interface UsbFileProvider {
    @Throws(IOException::class)
    fun determineFileToServe(uri: String): UsbFile
}
