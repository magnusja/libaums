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

package com.github.mjdev.libaums.partition

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.partition.fs.FileSystemPartitionTableCreator
import com.github.mjdev.libaums.partition.mbr.MasterBootRecordCreator
import java.io.IOException
import java.util.*

/**
 * Helper class to create different supported [PartitionTable]s.
 *
 * @author mjahnen
 */
object PartitionTableFactory {

    private val partitionTables = ArrayList<PartitionTableCreator>()

    class UnsupportedPartitionTableException : IOException()

    interface PartitionTableCreator {
        @Throws(IOException::class)
        fun read(blockDevice: BlockDeviceDriver): PartitionTable?
    }

    init {
        registerPartitionTable(FileSystemPartitionTableCreator())
        registerPartitionTable(MasterBootRecordCreator())
    }

    /**
     * Creates a [PartitionTable] suitable for the given block device. The
     * partition table should be located at the logical block address zero of
     * the device.
     *
     * @param blockDevice
     * The block device where the partition table is located.
     * @return The newly created [PartitionTable].
     * @throws IOException
     * If reading from the device fails.
     */
    @Throws(IOException::class)
    fun createPartitionTable(blockDevice: BlockDeviceDriver): PartitionTable {
        for (creator in partitionTables) {
            val table = creator.read(blockDevice)
            if (table != null) {
                return table
            }
        }

        throw UnsupportedPartitionTableException()
    }

    @Synchronized
    fun registerPartitionTable(creator: PartitionTableCreator) {
        partitionTables.add(creator)
    }
}
