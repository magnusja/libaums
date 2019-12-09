/*
 * (C) Copyright 2016 mjahnen <github@mgns.tech>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.github.mjdev.libaums.fs

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * UsbFileOutputStream provides common OutputStream access to a UsbFile.
 */
class UsbFileOutputStream @JvmOverloads constructor(private val file: UsbFile, append: Boolean = false) : OutputStream() {
    private var currentByteOffset: Long = 0

    init {
        if (file.isDirectory) {
            throw UnsupportedOperationException("UsbFileOutputStream cannot be created on directory!")
        }

        if (append) {
            currentByteOffset = file.length
        }
    }

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        val byteBuffer = ByteBuffer.wrap(byteArrayOf(oneByte.toByte()))
        file.write(currentByteOffset, byteBuffer)

        currentByteOffset++
    }

    @Throws(IOException::class)
    override fun close() {
        file.length = currentByteOffset
        file.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        file.flush()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray) {
        val byteBuffer = ByteBuffer.wrap(buffer)
        file.write(currentByteOffset, byteBuffer)

        currentByteOffset += buffer.size.toLong()
    }

    @Throws(IOException::class)
    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        val byteBuffer = ByteBuffer.wrap(buffer)

        byteBuffer.position(offset)
        byteBuffer.limit(count + offset)

        file.write(currentByteOffset, byteBuffer)

        currentByteOffset += count.toLong()
    }
}
