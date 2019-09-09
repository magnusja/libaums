package com.github.mjdev.libaums.fs

import android.util.Log

import java.io.IOException

/**
 * Created by magnusja on 3/1/17.
 */

abstract class AbstractUsbFile : UsbFile {

    override val absolutePath: String
        get() {
            if (isRoot) {
                return "/"
            }

            return parent?.let { parent ->
                if (parent.isRoot) {
                    "/$name"
                } else parent.absolutePath + UsbFile.separator + name
            }.orEmpty() // should never happen
        }

    @Throws(IOException::class)
    override fun search(path: String): UsbFile? {
        var path = path

        if (!isDirectory) {
            throw UnsupportedOperationException("This is a file!")
        }

        Log.d(TAG, "search file: $path")

        if (isRoot && path == UsbFile.separator) {
            return this
        }

        if (isRoot && path.startsWith(UsbFile.separator)) {
            path = path.substring(1)
        }
        if (path.endsWith(UsbFile.separator)) {
            path = path.substring(0, path.length - 1)
        }

        val index = path.indexOf(UsbFile.separator)

        if (index < 0) {
            Log.d(TAG, "search entry: $path")

            return searchThis(path)
        } else {
            val subPath = path.substring(index + 1)
            val dirName = path.substring(0, index)
            Log.d(TAG, "search recursively $subPath in $dirName")

            val file = searchThis(dirName)
            if (file != null && file.isDirectory) {
                Log.d(TAG, "found directory $dirName")
                return file.search(subPath)
            }
        }

        Log.d(TAG, "not found $path")

        return null
    }

    @Throws(IOException::class)
    private fun searchThis(name: String): UsbFile? {
        for (file in listFiles()) {
            if (file.name == name)
                return file
        }

        return null
    }

    override fun hashCode(): Int {
        return absolutePath.hashCode()
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        // TODO add getFileSystem and check if file system is the same
        // TODO check reference
        return other is UsbFile && absolutePath == other.absolutePath
    }

    companion object {
        private val TAG = AbstractUsbFile::class.java.simpleName
    }
}
