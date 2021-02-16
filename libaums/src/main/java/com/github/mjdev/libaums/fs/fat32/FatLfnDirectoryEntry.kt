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

/**
 * This class represents a long file name entry. The long file name can be
 * accessed via [.getName]. This class delegates most actions to the
 * [.actualEntry]. It is responsible for parsing and serializing long file
 * names and the actual entry with the corresponding short name.
 *
 *
 * To understand the structure of long file name entries it is advantageous to
 * look at the official FAT32 specification.
 *
 * @author mjahnen
 */
internal class FatLfnDirectoryEntry
/**
 * Creates a new [.FatLfnDirectoryEntry] with the given information.
 *
 * @param actualEntry
 * The actual entry in the FAT directory.
 * @param lfnName
 * The long file name, can be null.
 */ private constructor(actualEntry: FatDirectoryEntry,
        /**
         * The long file name or null if the actual entry does not have a long file
         * name.
         */
                        private var lfnName: String?) {

    /**
     * The actual entry which holds information like the start cluster and the
     * file size.
     * Returns the actual entry which holds the information like start cluster
     * or file size.
     *
     * @return The actual entry.
     */
    var actualEntry: FatDirectoryEntry = actualEntry
        private set

    /**
     * This method returns the entry count needed to store the long file name
     * and the actual entry.
     *
     * @return The amount of entries.
     */
    val entryCount: Int
        get() {
            // we always have the actual entry
            var result = 1

            // if long filename exists add needed entries
            lfnName?.let { lfnName ->
                val len = lfnName.length
                result += len / 13
                if (len % 13 != 0)
                    result++
            }

            return result
        }

    /**
     * Returns the name for this entry. If the long file name is not specified
     * it returns the short name.
     *
     * @return The name of the entry
     */
    internal val name: String
        get() {
            lfnName?.let { return it }
            // https://en.wikipedia.org/wiki/8.3_filename#Compatibility
            val sname = actualEntry.shortName!!.string
            var name = sname
            var ext = ""

            val split = sname.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size == 2) {
                name = split[0]
                ext = split[1]
            }

            if (actualEntry.isShortNameLowerCase)
                name = name.toLowerCase()
            if (actualEntry.isShortNameExtLowerCase)
                ext = ext.toLowerCase()

            if (ext.isNotEmpty())
                name = "$name.$ext"

            return name
        }

    /**
     * file size for this entry
     */
    var fileSize: Long
        get() = actualEntry.fileSize
        set(newSize) {
            actualEntry.fileSize = newSize
        }

    /**
     * defines start cluster for this entry
     */
    var startCluster: Long
        get() = actualEntry.startCluster
        set(newStartCluster) {
            actualEntry.startCluster = newStartCluster
        }

    /**
     *
     * @return True if this entry denotes a directory.
     */
    val isDirectory: Boolean
        get() = actualEntry.isDirectory


    /**
     * Creates a completely new [.FatLfnDirectoryEntry].
     *
     * @param name
     * The long file name.
     * @param shortName
     * The generated short name.
     * @return The newly created entry.
     */
    constructor(name: String?, shortName: ShortName): this(FatDirectoryEntry(), name) {
        actualEntry.shortName = shortName
    }

    /**
     * Serializes the long file name and the actual entry in the order needed
     * into the buffer. Updates the position of the buffer.
     *
     * @param buffer
     * The buffer were the serialized data shall be stored.
     */
    fun serialize(buffer: ByteBuffer) {
        lfnName?.let{ lfnName ->
            val checksum = actualEntry.shortName!!.calculateCheckSum()
            val entrySize = entryCount

            // long filename is stored in reverse order
            var index = entrySize - 2
            // first write last entry
            var entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13,
                    checksum, index + 1, true)
            entry.serialize(buffer)

            while (index-- > 0) {
                entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13, checksum, index + 1,
                        false)
                entry.serialize(buffer)
            }
        }

        // finally write the actual entry
        actualEntry.serialize(buffer)
    }

    /**
     * Sets a new long name and the corresponding short name.
     *
     * @param newName
     * The new long name.
     * @param shortName
     * The new short name.
     */
    fun setName(newName: String, shortName: ShortName) {
        lfnName = newName
        actualEntry.shortName = shortName
    }

    /**
     * Sets the last accessed time of the actual entry to now.
     */
    fun setLastAccessedTimeToNow() {
        actualEntry.lastAccessedDateTime = System.currentTimeMillis()
    }

    /**
     * Sets the last modified time of the actual entry to now.
     */
    fun setLastModifiedTimeToNow() {
        actualEntry.lastModifiedDateTime = System.currentTimeMillis()
    }

    /**
     * Sets this entry to indicate a directory.
     */
    fun setDirectory() {
        actualEntry.setDirectory()
    }

    override fun toString(): String {
        return "[FatLfnDirectoryEntry getName()=$name]"
    }

    companion object {

        /**
         * Reads a [.FatLfnDirectoryEntry] with the given information.
         *
         * @param actualEntry
         * The actual entry.
         * @param lfnParts
         * The entries where the long file name is stored in reverse
         * order.
         * @return The newly created entry.
         */
        fun read(actualEntry: FatDirectoryEntry,
                               lfnParts: List<FatDirectoryEntry>): FatLfnDirectoryEntry {
            val builder = StringBuilder(13 * lfnParts.size)

            if (lfnParts.isNotEmpty()) {
                // stored in reverse order on the disk
                for (i in lfnParts.indices.reversed()) {
                    lfnParts[i].extractLfnPart(builder)
                }

                return FatLfnDirectoryEntry(actualEntry, builder.toString())
            }

            return FatLfnDirectoryEntry(actualEntry, null)
        }

        /**
         * Copies created, last accessed and last modified date and time fields from
         * one entry to another.
         *
         * @param from
         * The source.
         * @param to
         * The destination.
         */
        fun copyDateTime(from: FatLfnDirectoryEntry, to: FatLfnDirectoryEntry) {
            val actualFrom = from.actualEntry
            val actualTo = to.actualEntry
            actualTo.createdDateTime = actualFrom.createdDateTime
            actualTo.lastAccessedDateTime = actualFrom.lastAccessedDateTime
            actualTo.lastModifiedDateTime = actualFrom.lastModifiedDateTime
        }
    }
}
