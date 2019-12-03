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

import android.util.Log
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.fs.AbstractUsbFile
import com.github.mjdev.libaums.fs.UsbFile
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


/**
 * This class represents a directory in the FAT32 file system. It can hold other
 * directories and files.
 *
 * @author mjahnen
 */
class FatDirectory
/**
 * Constructs a new FatDirectory with the given information.
 *
 * @param blockDevice
 * The block device the fs is located.
 * @param fat
 * The FAT of the fs.
 * @param bootSector
 * The boot sector if the fs.
 * @param parent
 * The parent directory of the newly created one.
 */
internal constructor(
            private val fs: Fat32FileSystem,
            private val blockDevice: BlockDeviceDriver,
            private val fat: FAT,
            private val bootSector: Fat32BootSector,
            /**
             * null if directory
             */
            private var entry: FatLfnDirectoryEntry?,
            /**
             * Null if this is the root directory.
             */
            override var parent: FatDirectory?) : AbstractUsbFile() {

    private lateinit var chain: ClusterChain
    /**
     * Entries read from the device.
     */
    private var entries: MutableList<FatLfnDirectoryEntry>? = null
    /**
     * Map for checking for existence when for example creating new files or
     * directories.
     *
     *
     * All items are stored in lower case because a FAT32 fs is not case
     * sensitive.
     */
    private val lfnMap: MutableMap<String, FatLfnDirectoryEntry>
    /**
     * Map for checking for existence of short names when generating short names
     * for new files or directories.
     */
    private val shortNameMap: MutableMap<ShortName, FatDirectoryEntry>

    /**
     * This method returns the volume label which can be stored in the root
     * directory of a FAT32 file system.
     *
     * @return The volume label.
     */
    internal var volumeLabel: String? = null
        private set

    private var hasBeenInited: Boolean = false

    /**
     *
     * @return True if this directory is the root directory.
     */
    override val isRoot: Boolean
        get() = entry == null

    override var length: Long
        get() = throw UnsupportedOperationException("This is a directory!")
        set(_) = throw UnsupportedOperationException("This is a directory!")

    override val isDirectory: Boolean
        get() = true

    override var name: String
        get() = if (entry != null) entry!!.name else "/"
        @Throws(IOException::class)
        set(newName) {
            check(!isRoot) { "Cannot rename root dir!" }
            parent!!.renameEntry(entry, newName)
        }

    init {
        lfnMap = HashMap()
        shortNameMap = HashMap()
    }

    /**
     * Initializes the [FatDirectory]. Creates the cluster chain if needed
     * and reads all entries from the cluster chain.
     *
     * @throws IOException
     * If reading from the device fails.
     */
    @Throws(IOException::class)
    private fun init() {
        if (!::chain.isInitialized) {
            chain = ClusterChain(entry!!.startCluster, blockDevice, fat, bootSector)
        }

        // entries is allocated here
        // an exception will be thrown if entries is used before the directory has been initialised
        // use of uninitialised entries can lead to data loss!
        if (entries == null) {
            entries = ArrayList()
        }

        // only read entries if we have no entries
        // otherwise newly created directories (. and ..) will read trash data
        if (entries!!.size == 0 && !hasBeenInited) {
            readEntries()
        }

        hasBeenInited = true
    }

    /**
     * Reads all entries from the directory and saves them into [.lfnMap],
     * [.entries] and [.shortNameMap].
     *
     * @throws IOException
     * If reading from the device fails.
     * @see .write
     */
    @Throws(IOException::class)
    private fun readEntries() {
        val buffer = ByteBuffer.allocate(chain.length.toInt())
        chain.read(0, buffer)
        // we have to buffer all long filename entries to parse them later
        val list = ArrayList<FatDirectoryEntry>()
        buffer.flip()
        while (buffer.remaining() > 0) {
            val e = FatDirectoryEntry.read(buffer) ?: break

            if (e.isLfnEntry) {
                list.add(e)
                continue
            }

            if (e.isVolumeLabel) {
                if (!isRoot) {
                    Log.w(TAG, "volume label in non root dir!")
                }
                volumeLabel = e.volumeLabel
                Log.d(TAG, "volume label: " + volumeLabel!!)
                continue
            }

            // we just skip deleted entries
            if (e.isDeleted) {
                list.clear()
                continue
            }

            val lfnEntry = FatLfnDirectoryEntry.read(e, list)
            addEntry(lfnEntry, e)
            list.clear()
        }
    }

    /**
     * Adds the long file name entry to [.lfnMap] and [.entries] and
     * the actual entry to [.shortNameMap].
     *
     *
     * This method does not write the changes to the disk. If you want to do so
     * call [.write] after adding an entry.
     *
     * @param lfnEntry
     * The long filename entry to add.
     * @param entry
     * The corresponding short name entry.
     * @see .removeEntry
     */
    private fun addEntry(lfnEntry: FatLfnDirectoryEntry, entry: FatDirectoryEntry) {
        entries!!.add(lfnEntry)
        lfnMap[lfnEntry.name.toLowerCase(Locale.getDefault())] = lfnEntry
        shortNameMap[entry.shortName!!] = entry
    }

    /**
     * Removes (if existing) the long file name entry from [.lfnMap] and
     * [.entries] and the actual entry from [.shortNameMap].
     *
     *
     * This method does not write the changes to the disk. If you want to do so
     * call [.write] after adding an entry.
     *
     * @param lfnEntry
     * The long filename entry to remove.
     * @see .addEntry
     */
    internal fun removeEntry(lfnEntry: FatLfnDirectoryEntry?) {
        entries!!.remove(lfnEntry)
        lfnMap.remove(lfnEntry!!.name.toLowerCase(Locale.getDefault()))
        shortNameMap.remove(lfnEntry.actualEntry.shortName)
    }

    /**
     * Renames a long filename entry to the desired new name.
     *
     *
     * This method immediately writes the change to the disk, thus no further
     * call to [.write] is needed.
     *
     * @param lfnEntry
     * The long filename entry to rename.
     * @param newName
     * The new name.
     * @throws IOException
     * If writing the change to the disk fails.
     */
    @Throws(IOException::class)
    internal fun renameEntry(lfnEntry: FatLfnDirectoryEntry?, newName: String) {
        if (lfnEntry!!.name == newName)
            return

        removeEntry(lfnEntry)
        lfnEntry.setName(newName,
                ShortNameGenerator.generateShortName(newName, shortNameMap.keys))
        addEntry(lfnEntry, lfnEntry.actualEntry)
        write()
    }

    /**
     * Writes the [.entries] to the disk. Any changes made by
     * [.addEntry] or
     * [.removeEntry] will then be committed to the
     * device.
     *
     * @throws IOException
     * @see {@link .write
     */
    @Throws(IOException::class)
    internal fun write() {
        init()
        val writeVolumeLabel = isRoot && volumeLabel != null
        // first lookup the total entries needed
        var totalEntryCount = 0
        for (entry in entries!!) {
            totalEntryCount += entry.entryCount
        }

        if (writeVolumeLabel)
            totalEntryCount++

        val totalBytes = (totalEntryCount * FatDirectoryEntry.SIZE).toLong()
        chain.length = totalBytes

        val buffer = ByteBuffer.allocate(chain.length.toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        if (writeVolumeLabel)
            FatDirectoryEntry.createVolumeLabel(volumeLabel!!).serialize(buffer)

        for (entry in entries!!) {
            entry.serialize(buffer)
        }

        if (totalBytes % bootSector.bytesPerCluster != 0L || totalBytes == 0L) {
            // add dummy entry filled with zeros to mark end of entries
            buffer.put(ByteArray(buffer.remaining()))
        }

        buffer.flip()
        chain.write(0, buffer)
    }

    @Throws(IOException::class)
    override fun createFile(name: String): FatFile {
        if (lfnMap.containsKey(name.toLowerCase(Locale.getDefault())))
            throw IOException("Item already exists!")

        init() // initialise the directory before creating files

        val shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keys)

        val entry = FatLfnDirectoryEntry(name, shortName)
        // alloc completely new chain
        val newStartCluster = fat.alloc(arrayOf(), 1)[0]
        entry.startCluster = newStartCluster

        Log.d(TAG, "adding entry: $entry with short name: $shortName")
        addEntry(entry, entry.actualEntry)
        // write changes immediately to disk
        write()

        val file = FatFile(blockDevice, fat, bootSector, entry, this)
        fs.fileCache[file.absolutePath] = file
        return file
    }

    @Throws(IOException::class)
    override fun createDirectory(name: String): FatDirectory {
        if (lfnMap.containsKey(name.toLowerCase(Locale.getDefault())))
            throw IOException("Item already exists!")

        init() // initialise the directory before creating files

        val shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keys)

        val entry = FatLfnDirectoryEntry(name, shortName)
        entry.setDirectory()
        // alloc completely new chain
        val newStartCluster = fat.alloc(arrayOf(), 1)[0]
        entry.startCluster = newStartCluster

        Log.d(TAG, "adding entry: $entry with short name: $shortName")
        addEntry(entry, entry.actualEntry)
        // write changes immediately to disk
        write()

        val result = FatDirectory(fs, blockDevice, fat, bootSector, entry, this)
        result.hasBeenInited = true

        result.entries = ArrayList() // initialise entries before adding sub-directories

        // first create the dot entry which points to the dir just created
        val dotEntry = FatLfnDirectoryEntry(null, ShortName(".", ""))
        dotEntry.setDirectory()
        dotEntry.startCluster = newStartCluster
        FatLfnDirectoryEntry.copyDateTime(entry, dotEntry)
        result.addEntry(dotEntry, dotEntry.actualEntry)

        // Second the dotdot entry which points to the parent directory (this)
        // if parent is the root dir then set start cluster to zero
        val dotDotEntry = FatLfnDirectoryEntry(null, ShortName("..",
                ""))
        dotDotEntry.setDirectory()
        dotDotEntry.startCluster = if (isRoot) 0 else this.entry!!.startCluster
        FatLfnDirectoryEntry.copyDateTime(entry, dotDotEntry)
        result.addEntry(dotDotEntry, dotDotEntry.actualEntry)

        // write changes immediately to disk
        result.write()

        fs.fileCache[result.absolutePath] = result
        return result
    }

    override fun createdAt(): Long {
        check(!isRoot) { "root dir!" }
        return entry!!.actualEntry.createdDateTime
    }

    override fun lastModified(): Long {
        check(!isRoot) { "root dir!" }
        return entry!!.actualEntry.lastModifiedDateTime
    }

    override fun lastAccessed(): Long {
        check(!isRoot) { "root dir!" }
        return entry!!.actualEntry.lastAccessedDateTime
    }

    @Throws(IOException::class)
    override fun list(): Array<String> {
        init()
        val list = ArrayList<String>(entries!!.size)
        for (entry in entries!!) {
            val name = entry.name
            if (name != "." && name != "..") {
                list.add(name)
            }
        }

        return list.toTypedArray()
    }

    @Throws(IOException::class)
    override fun listFiles(): Array<UsbFile> {
        init()
        val list = ArrayList<UsbFile>(entries!!.size)
        for (entry in entries!!) {
            val name = entry.name
            if (name == "." || name == "..")
                continue

            val entryAbsolutePath = if (isRoot) {
                UsbFile.separator + entry.name
            } else {
                absolutePath + UsbFile.separator + entry.name
            }

            val file = when {
                fs.fileCache[entryAbsolutePath] != null -> fs.fileCache[entryAbsolutePath]!!
                entry.isDirectory -> FatDirectory(fs, blockDevice, fat, bootSector, entry, this)
                else -> FatFile(blockDevice, fat, bootSector, entry, this)
            }

            fs.fileCache[entryAbsolutePath] = file
            list.add(file)
        }

        return list.toTypedArray()
    }

    @Throws(IOException::class)
    override fun read(offset: Long, destination: ByteBuffer) {
        throw UnsupportedOperationException("This is a directory!")
    }

    @Throws(IOException::class)
    override fun write(offset: Long, source: ByteBuffer) {
        throw UnsupportedOperationException("This is a directory!")
    }

    @Throws(IOException::class)
    override fun flush() {
        throw UnsupportedOperationException("This is a directory!")
    }

    @Throws(IOException::class)
    override fun close() {
        throw UnsupportedOperationException("This is a directory!")
    }

    @Throws(IOException::class)
    override fun moveTo(destination: UsbFile) {
        check(!isRoot) { "cannot move root dir!" }
        check(destination.isDirectory) { "destination cannot be a file!" }
        check(destination is FatDirectory) { "cannot move between different filesystems!" }

        // TODO check if destination is really on the same physical device or
        // partition!

        if (destination.lfnMap.containsKey(entry!!.name.toLowerCase(Locale.getDefault())))
            throw IOException("item already exists in destination!")

        init()
        destination.init()

        // now the actual magic happens!
        parent!!.removeEntry(entry)
        destination.addEntry(entry!!, entry!!.actualEntry)

        parent!!.write()
        destination.write()
        parent = destination
    }

    /**
     * This method moves an long filename entry currently stored in THIS
     * directory to the destination which also must be a directory.
     *
     *
     * Used by [FatFile] to move itself to another directory.
     *
     * @param entry
     * The entry which shall be moved.
     * @param destination
     * The destination directory.
     * @throws IOException
     * If writing fails or the item already exists in the
     * destination directory.
     * @throws IllegalStateException
     * If the destination is not a directory or destination is on a
     * different file system.
     */
    @Throws(IOException::class)
    internal fun move(entry: FatLfnDirectoryEntry, destination: UsbFile) {
        check(destination.isDirectory) { "destination cannot be a file!" }
        check(destination is FatDirectory) { "cannot move between different filesystems!" }

        // TODO check if destination is really on the same physical device or
        // partition!

        if (destination.lfnMap.containsKey(entry.name.toLowerCase(Locale.getDefault())))
            throw IOException("item already exists in destination!")

        init()
        destination.init()

        // now the actual magic happens!
        removeEntry(entry)
        destination.addEntry(entry, entry.actualEntry)

        write()
        destination.write()
    }

    @Throws(IOException::class)
    override fun delete() {
        check(!isRoot) { "Root dir cannot be deleted!" }

        init()
        val subElements = listFiles()

        for (file in subElements) {
            file.delete()
        }

        parent!!.removeEntry(entry)
        parent!!.write()
        chain.length = 0
    }

    companion object {

        private val TAG = FatDirectory::class.java.simpleName

        /**
         * Reads the root directory from a FAT32 file system.
         *
         * @param blockDevice
         * The block device the fs is located.
         * @param fat
         * The FAT of the fs.
         * @param bootSector
         * The boot sector if the fs.
         * @return Newly created root directory.
         * @throws IOException
         * If reading from the device fails.
         */
        @JvmStatic
        @Throws(IOException::class)
        internal fun readRoot(fs: Fat32FileSystem,
                              blockDevice: BlockDeviceDriver, fat: FAT,
                              bootSector: Fat32BootSector): FatDirectory {
            val result = FatDirectory(fs, blockDevice, fat, bootSector, null, null)
            result.chain = ClusterChain(bootSector.rootDirStartCluster, blockDevice, fat,
                    bootSector)
            result.init() // init calls readEntries
            return result
        }
    }
}
