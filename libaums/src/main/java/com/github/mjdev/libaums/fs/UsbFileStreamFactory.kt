package com.github.mjdev.libaums.fs

import java.io.BufferedInputStream
import java.io.BufferedOutputStream

/**
 * Created by magnusja on 13/12/16.
 */

object UsbFileStreamFactory {

    @JvmStatic
    fun createBufferedOutputStream(file: UsbFile, fs: FileSystem): BufferedOutputStream {
        return BufferedOutputStream(UsbFileOutputStream(file), fs.chunkSize)
    }

    @JvmStatic
    fun createBufferedInputStream(file: UsbFile, fs: FileSystem): BufferedInputStream {
        return BufferedInputStream(UsbFileInputStream(file), fs.chunkSize)
    }
}
