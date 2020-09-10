/*
 * (C) Copyright 2014-2016 mjahnen <github@mgns.tech>
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

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.fs.AbstractUsbFile
import com.github.mjdev.libaums.fs.UsbFile
import java.io.IOException
import java.nio.ByteBuffer

class FatFile
/**
 * Constructs a new file with the given information.
 *
 * @param blockDevice
 * The device where the file system is located.
 * @param fat
 * The FAT used to follow cluster chains.
 * @param bootSector
 * The boot sector of the file system.
 * @param entry
 * The corresponding entry in a FAT directory.
 * @param parent
 * The parent directory of the newly constructed file.
 */
internal constructor(private val blockDevice: BlockDeviceDriver, private val fat: FAT, private val bootSector: Fat32BootSector,
                    private val entry: FatLfnDirectoryEntry, override var parent: FatDirectory?) : AbstractUsbFile() {
    private lateinit var chain: ClusterChain

    override val isDirectory: Boolean
        get() = false

    override var name: String
        get() = entry.name
        @Throws(IOException::class)
        set(newName) = parent!!.renameEntry(entry, newName)

    override var length: Long
        get() = entry.fileSize
        @Throws(IOException::class)
        set(newLength) {
            initChain()
            chain.length = newLength
            entry.fileSize = newLength
        }

    override val isRoot: Boolean
        get() = false

    /**
     * Initializes the cluster chain to access the contents of the file.
     *
     * @throws IOException
     * If reading from FAT fails.
     */
    @Throws(IOException::class)
    private fun initChain() {
        if (!::chain.isInitialized) {
            chain = ClusterChain(entry.startCluster, blockDevice, fat, bootSector)
        }
    }

    override fun createdAt(): Long {
        return entry.actualEntry.createdDateTime
    }

    override fun lastModified(): Long {
        return entry.actualEntry.lastModifiedDateTime
    }

    override fun lastAccessed(): Long {
        return entry.actualEntry.lastAccessedDateTime
    }

    override fun list(): Array<String> {
        throw UnsupportedOperationException("This is a file!")
    }

    @Throws(IOException::class)
    override fun listFiles(): Array<UsbFile> {
        throw UnsupportedOperationException("This is a file!")
    }

    @Throws(IOException::class)
    override fun read(offset: Long, destination: ByteBuffer) {
        initChain()
        entry.setLastAccessedTimeToNow()
        chain.read(offset, destination)
    }

    @Throws(IOException::class)
    override fun write(offset: Long, source: ByteBuffer) {
        initChain()
        val length = offset + source.remaining()
        if (length > this.length)
            this.length = length
        entry.setLastModifiedTimeToNow()
        chain.write(offset, source)
    }

    @Throws(IOException::class)
    override fun flush() {
        // we only have to update the parent because we are always writing
        // everything
        // immediately to the device
        // the parent directory is responsible for updating the
        // FatDirectoryEntry which
        // contains things like the file size and the date time fields
        parent!!.write()
    }

    @Throws(IOException::class)
    override fun close() {
        flush()
    }

    @Throws(IOException::class)
    override fun createDirectory(name: String): UsbFile {
        throw UnsupportedOperationException("This is a file!")
    }

    @Throws(IOException::class)
    override fun createFile(name: String): UsbFile {
        throw UnsupportedOperationException("This is a file!")
    }

    @Throws(IOException::class)
    override fun moveTo(destination: UsbFile) {
        parent!!.move(entry, destination)
        parent = destination as FatDirectory
    }

    @Throws(IOException::class)
    override fun delete() {
        initChain()
        parent!!.removeEntry(entry)
        parent!!.write()
        chain.length = 0
    }

}
