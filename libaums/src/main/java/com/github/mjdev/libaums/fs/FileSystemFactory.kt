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

package com.github.mjdev.libaums.fs

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystemCreator
import com.github.mjdev.libaums.partition.PartitionTableEntry
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

    private val fileSystems = ArrayList<FileSystemCreator>()
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
        registerFileSystem(Fat32FileSystemCreator())
    }

    @Throws(IOException::class, FileSystemFactory.UnsupportedFileSystemException::class)
    fun createFileSystem(entry: PartitionTableEntry,
                         blockDevice: BlockDeviceDriver): FileSystem {
        for (creator in fileSystems) {
            val fs = creator.read(entry, blockDevice)
            if (fs != null) {
                return fs
            }
        }

        throw UnsupportedFileSystemException()
    }

    /**
     * Register a new file system.
     * @param creator The creator which is able to check if a [BlockDeviceDriver] is holding
     * the correct type of file system and is able to instantiate a [FileSystem]
     * instance.
     */
    @Synchronized
    @JvmStatic
    fun registerFileSystem(creator: FileSystemCreator) {
        fileSystems.add(creator)
    }
}
