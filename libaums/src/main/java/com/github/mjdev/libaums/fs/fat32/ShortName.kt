/*
 * (C) Copyright 2014 mjahnen <github@mgns.tech>
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

package com.github.mjdev.libaums.fs.fat32

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

/**
 * This class represents a 8.3 short name of a [FatDirectoryEntry].
 *
 *
 * The short name has 8 characters for the name and 3 characters for the
 * extension. The period between them is not saved in the short name. The short
 * name can only hold eight bit characters. Only upper case characters and these
 * special characters are allowed: $ %  - _ @ ~  ! ( )
 *
 *
 * For more information regarding short names, please refer to the official
 * FAT32 specification.
 *
 * @author mjahnen
 */
internal class ShortName {

    private var data: ByteBuffer

    /**
     * Returns a human readable String of the short name.
     *
     * @return The name.
     */
    val string: String
        get() {
            val name = CharArray(8)
            val extension = CharArray(3)

            for (i in 0..7) {
                name[i] = (data.get(i) and 0xFF.toByte()).toChar()
            }

            // if first byte is 0x05 it is actually 0xe5 (KANJI lead byte, see Fat32
            // specification)
            if (data.get(0).toInt() == 0x05) {
                // this has to be done because 0xe5 is the magic for an deleted entry
                name[0] = 0xe5.toChar()
            }

            for (i in 0..2) {
                extension[i] = (data.get(i + 8) and 0xFF.toByte()).toChar()
            }

            val strName = String(name).trim { it <= ' ' }
            val strExt = String(extension).trim { it <= ' ' }

            return if (strExt.isEmpty()) strName else "$strName.$strExt"
        }

    /**
     * Construct a new short name with the given name and extension. Name length
     * maximum is 8 and extension maximum length is 3.
     *
     * @param name
     * The name, must not be null or empty.
     * @param extension
     * The extension, must not be null, but can be empty.
     */
    constructor(name: String, extension: String) {
        val tmp = ByteArray(SIZE)
        // fill with spaces
        Arrays.fill(tmp, 0x20.toByte())

        val length = Math.min(name.length, 8)

        System.arraycopy(name.toByteArray(Charset.forName("ASCII")), 0, tmp, 0, length)
        System.arraycopy(extension.toByteArray(Charset.forName("ASCII")), 0, tmp, 8,
                extension.length)

        // 0xe5 means entry deleted, so we have to convert it
        if (tmp[0] == 0xe5.toByte()) {
            // KANJI lead byte, see Fat32 specification
            tmp[0] = 0x05
        }

        data = ByteBuffer.wrap(tmp)
    }

    /**
     * Construct a short name with the given data from a 32 byte
     * [FatDirectoryEntry].
     *
     * @param data
     * The 11 bytes representing the name.
     */
    private constructor(data: ByteBuffer) {
        this.data = data
    }

    /**
     * Serializes the short name so that it can be written to disk. This method
     * does not alter the position of the given ByteBuffer!
     *
     * @param buffer
     * The buffer where the data shall be stored.
     */
    fun serialize(buffer: ByteBuffer) {
        buffer.put(data.array(), 0, SIZE)
    }

    /**
     * Calculates the checksum of the short name which is needed for the long
     * file entries.
     *
     * @return The checksum.
     * @see FatLfnDirectoryEntry
     *
     * @see FatLfnDirectoryEntry.serialize
     */
    fun calculateCheckSum(): Byte {
        var sum = 0

        for (i in 0 until SIZE) {
            sum = (if (sum and 1 == 1) 0x80 else 0) + (sum and 0xff shr 1) + data.get(i).toInt()
        }

        return (sum and 0xff).toByte()
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ShortName) false else Arrays.equals(data.array(), other.data.array())

    }

    override fun toString(): String {
        return string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    companion object {

        private const val SIZE = 11

        /**
         * Construct a short name with the given data from a 32 byte
         * [FatDirectoryEntry].
         *
         * @param data
         * The 32 bytes from the entry.
         */
        fun parse(data: ByteBuffer): ShortName {
            val tmp = ByteArray(SIZE)
            data.get(tmp)
            return ShortName(ByteBuffer.wrap(tmp))
        }
    }
}
