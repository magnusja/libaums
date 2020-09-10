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

import android.util.Log
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.partition.PartitionTypes
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class represents the FAT32 file system and is responsible for setting
 * the FAT32 file system up and extracting the volume label and the root
 * directory.
 *
 * @author mjahnen
 */
class Fat32FileSystem
/**
 * This method constructs a FAT32 file system for the given block device.
 * There are no further checks that the block device actually represents a
 * valid FAT32 file system. That means it must be ensured that the device
 * actually holds a FAT32 file system in advance!
 *
 * @param blockDevice
 * The block device the FAT32 file system is located.
 * @param first512Bytes
 * First 512 bytes read from block device.
 * @throws IOException
 * If reading from the device fails.
 */
@Throws(IOException::class)
private constructor(blockDevice: BlockDeviceDriver, first512Bytes: ByteBuffer) : FileSystem {

    private val bootSector: Fat32BootSector = Fat32BootSector.read(first512Bytes)
    private val fat: FAT
    private val fsInfoStructure: FsInfoStructure
    override val rootDirectory: FatDirectory
    /**
     * Caches UsbFile instances returned by list files method. If we do not do
     * that we will get a new instance when searching for the same file.
     * Depending on what you do with the two different instances they can get out
     * of sync and only the one which is written latest will actually be persisted on
     * disk. This is especially problematic if you create files on different directory
     * instances.. See also issue 215.
     */
    internal val fileCache = WeakHashMap<String, UsbFile>()

    override val volumeLabel: String
        get() {
            return rootDirectory.volumeLabel?.let { it }.orEmpty()
        }

    override val capacity: Long
        get() = bootSector.totalNumberOfSectors * bootSector.bytesPerSector

    override val occupiedSpace: Long
        get() = capacity - freeSpace

    override val freeSpace: Long
        get() = fsInfoStructure.freeClusterCount * bootSector.bytesPerCluster

    override val chunkSize: Int
        get() = bootSector.bytesPerCluster

    override val type: Int
        get() = PartitionTypes.FAT32

    init {
        fsInfoStructure = FsInfoStructure.read(blockDevice, bootSector.fsInfoStartSector * bootSector.bytesPerSector)
        fat = FAT(blockDevice, bootSector, fsInfoStructure)
        rootDirectory = FatDirectory.readRoot(this, blockDevice, fat, bootSector)

        Log.d(TAG, bootSector.toString())
    }

    companion object {

        private val TAG = Fat32FileSystem::class.java.simpleName

        /**
         * This method constructs a FAT32 file system for the given block device.
         * There are no further checks if the block device actually represents a
         * valid FAT32 file system. That means it must be ensured that the device
         * actually holds a FAT32 file system in advance!
         *
         * @param blockDevice
         * The block device the FAT32 file system is located.
         * @throws IOException
         * If reading from the device fails.
         */
        @Throws(IOException::class)
        @JvmStatic
        fun read(blockDevice: BlockDeviceDriver): Fat32FileSystem? {

            val buffer = ByteBuffer.allocate(512)
            blockDevice.read(0, buffer)
            buffer.flip()

            return if (buffer.get(82).toChar() != 'F' ||
                    buffer.get(83).toChar() != 'A' ||
                    buffer.get(84).toChar() != 'T' ||
                    buffer.get(85).toChar() != '3' ||
                    buffer.get(86).toChar() != '2' ||
                    buffer.get(87).toChar() != ' ' ||
                    buffer.get(88).toChar() != ' ' ||
                    buffer.get(89).toChar() != ' ') {
                null
            } else Fat32FileSystem(blockDevice, buffer)

        }
    }
}
