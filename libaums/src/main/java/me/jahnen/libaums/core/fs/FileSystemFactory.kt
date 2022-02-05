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

package me.jahnen.libaums.core.fs

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.fs.fat32.Fat32FileSystemCreator
import me.jahnen.libaums.core.partition.PartitionTableEntry
import java.io.IOException
import java.util.*

/**
 * This is a helper class to create different supported file systems. The file
 * system is determined by {link
 * [com.github.mjdev.libaums.partition.PartitionTableEntry].
 *
 * @author mjahnen
 */
object FileSystemFactory {
    private data class PrioritizedFileSystemCreator(val priority: Int, val count: Int, val creator: FileSystemCreator)

    private var count = 0
    private val fileSystems = TreeSet(
        compareBy<PrioritizedFileSystemCreator> { it.priority }.thenBy { it.count }
    )

    /**
     * The default priority of a creator registered with the file system.  Creators will be evaluated
     * in order from lowest priority number to highest priority number.  If two creators are
     * registered with the same priority then the one inserted first will be evaluated first.
     */
    const val DEFAULT_PRIORITY = 0

    /**
     * Set the timezone a file system should use to decode timestamps, if the file system only stores
     * local date and time and has no reference which zone these timestamp correspond to. (True for
     * FAT32, e.g.)
     * @param zone The timezone to use.
     */
    @JvmStatic
    var timeZone = TimeZone.getDefault()

    class UnsupportedFileSystemException : IOException()

    init {
        registerFileSystem(Fat32FileSystemCreator(), DEFAULT_PRIORITY + 1)
    }

    @Synchronized
    @Throws(IOException::class, FileSystemFactory.UnsupportedFileSystemException::class)
    fun createFileSystem(entry: PartitionTableEntry,
                         blockDevice: BlockDeviceDriver): FileSystem {
        fileSystems.forEach {
            val fs = it.creator.read(entry, blockDevice)
            if (fs != null) {
                return fs
            }
        }

        throw UnsupportedFileSystemException()
    }

    /**
     * Register a new file system at the default priority.
     * @param creator The creator which is able to check if a [BlockDeviceDriver] is holding
     * the correct type of file system and is able to instantiate a [FileSystem]
     * instance.
     */
    @Synchronized
    @JvmStatic
    fun registerFileSystem(creator: FileSystemCreator) {
        registerFileSystem(creator, DEFAULT_PRIORITY)
    }

    /**
     * Register a new file system with the given priority.
     * @param creator The creator which is able to check if a [BlockDeviceDriver] is holding
     * the correct type of file system and is able to instantiate a [FileSystem]
     * instance.
     *
     * @param priority The priority this file system creator has when attempting to
     * create a file system.
     */
    @Synchronized
    @JvmStatic
    fun registerFileSystem(creator: FileSystemCreator, priority: Int) {
        fileSystems.add(PrioritizedFileSystemCreator(priority, count++, creator))
    }


    /**
     * Removes all registered file systems.
     */
    @Synchronized
    @JvmStatic
    fun clearFileSystems() {
        fileSystems.clear()
    }

}
