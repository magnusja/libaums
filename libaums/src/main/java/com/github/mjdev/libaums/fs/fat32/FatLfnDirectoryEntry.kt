/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
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
/* package */internal class FatLfnDirectoryEntry {

    /**
     * The actual entry which holds information like the start cluster and the
     * file size.
     */
    /**
     * Returns the actual entry which holds the information like start cluster
     * or file size.
     *
     * @return The actual entry.
     */
    /* package */ var actualEntry: FatDirectoryEntry? = null
        private set
    /**
     * The long file name or null if the actual entry does not have a long file
     * name.
     */
    private var lfnName: String? = null

    /**
     * This method returns the entry count needed to store the long file name
     * and the actual entry.
     *
     * @return The amount of entries.
     */
    /* package */// we always have the actual entry
    // if long filename exists add needed entries
    val entryCount: Int
        get() {
            var result = 1
            if (lfnName != null) {
                val len = lfnName!!.length
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
    /* package */// https://en.wikipedia.org/wiki/8.3_filename#Compatibility
    val name: String
        get() {
            if (lfnName != null)
                return lfnName
            val sname = actualEntry!!.shortName!!.string
            var name = sname
            var ext = ""

            val split = sname.split(".".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size == 2) {
                name = split[0]
                ext = split[0]
            }

            if (actualEntry!!.isShortNameLowerCase)
                name = name.toLowerCase()
            if (actualEntry!!.isShortNameExtLowerCase)
                ext = ext.toLowerCase()

            if (!ext.isEmpty())
                name = "$name.$ext"

            return name
        }

    /**
     * Returns the file size for the actual entry.
     *
     * @return The file size in bytes.
     * @see .isDirectory
     * @see .setFileSize
     */
    /* package */
    /**
     * Sets the file size in bytes for the actual entry.
     *
     * @param newSize
     * The new size in bytes.
     * @see .isDirectory
     * @see .getFileSize
     */
    /* package */ var fileSize: Long
        get() = actualEntry!!.fileSize
        set(newSize) {
            actualEntry!!.fileSize = newSize
        }

    /**
     * Gets the start cluster for the actual entry.
     *
     * @return The start cluster.
     * @see .getStartCluster
     */
    /* package */
    /**
     * Sets the start cluster for the actual entry.
     *
     * @param newStartCluster
     * The new start cluster.
     * @see .getStartCluster
     */
    /* package */ var startCluster: Long
        get() = actualEntry!!.startCluster
        set(newStartCluster) {
            actualEntry!!.startCluster = newStartCluster
        }

    /**
     *
     * @return True if this entry denotes a directory.
     */
    /* package */ val isDirectory: Boolean
        get() = actualEntry!!.isDirectory

    private constructor() {

    }

    /**
     * Creates a new [.FatLfnDirectoryEntry] with the given information.
     *
     * @param actualEntry
     * The actual entry in the FAT directory.
     * @param lfnName
     * The long file name, can be null.
     */
    private constructor(actualEntry: FatDirectoryEntry, lfnName: String) {
        this.actualEntry = actualEntry
        this.lfnName = lfnName
    }

    /**
     * Serializes the long file name and the actual entry in the order needed
     * into the buffer. Updates the position of the buffer.
     *
     * @param buffer
     * The buffer were the serialized data shall be stored.
     */
    /* package */ fun serialize(buffer: ByteBuffer) {
        if (lfnName != null) {
            val checksum = actualEntry!!.shortName!!.calculateCheckSum()
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
        actualEntry!!.serialize(buffer)
    }

    /**
     * Sets a new long name and the corresponding short name.
     *
     * @param newName
     * The new long name.
     * @param shortName
     * The new short name.
     */
    /* package */ fun setName(newName: String, shortName: ShortName) {
        lfnName = newName
        actualEntry!!.shortName = shortName
    }

    /**
     * Sets the last accessed time of the actual entry to now.
     */
    /* package */ fun setLastAccessedTimeToNow() {
        actualEntry!!.lastAccessedDateTime = System.currentTimeMillis()
    }

    /**
     * Sets the last modified time of the actual entry to now.
     */
    /* package */ fun setLastModifiedTimeToNow() {
        actualEntry!!.lastModifiedDateTime = System.currentTimeMillis()
    }

    /**
     * Sets this entry to indicate a directory.
     */
    /* package */ fun setDirectory() {
        actualEntry!!.setDirectory()
    }

    override fun toString(): String {
        return "[FatLfnDirectoryEntry getName()=$name]"
    }

    companion object {

        /**
         * Creates a completely new [.FatLfnDirectoryEntry].
         *
         * @param name
         * The long file name.
         * @param shortName
         * The generated short name.
         * @return The newly created entry.
         */
        /* package */ fun createNew(name: String, shortName: ShortName): FatLfnDirectoryEntry {
            val result = FatLfnDirectoryEntry()

            result.lfnName = name
            result.actualEntry = FatDirectoryEntry.createNew()
            result.actualEntry!!.shortName = shortName

            return result
        }

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
        /* package */ fun read(actualEntry: FatDirectoryEntry,
                               lfnParts: List<FatDirectoryEntry>): FatLfnDirectoryEntry {
            val builder = StringBuilder(13 * lfnParts.size)

            if (lfnParts.size > 0) {
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
        /* package */ fun copyDateTime(from: FatLfnDirectoryEntry, to: FatLfnDirectoryEntry) {
            val actualFrom = from.actualEntry
            val actualTo = from.actualEntry
            actualTo!!.createdDateTime = actualFrom!!.createdDateTime
            actualTo.lastAccessedDateTime = actualFrom.lastAccessedDateTime
            actualTo.lastModifiedDateTime = actualFrom.lastModifiedDateTime
        }
    }
}
