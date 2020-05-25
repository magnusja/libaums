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

package com.github.mjdev.libaums.partition.mbr

import android.util.Log
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableEntry
import com.github.mjdev.libaums.partition.PartitionTypes
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class represents the Master Boot Record (MBR), which is a partition
 * table used by most block devices coming from Windows or Unix.
 *
 * @author mjahnen
 */
class MasterBootRecord private constructor() : PartitionTable {

    private val partitions = ArrayList<PartitionTableEntry>()

    override val size: Int
        get() = 512

    override val partitionTableEntries: List<PartitionTableEntry>
        get() = partitions

    companion object {

        private val partitionTypes = object : HashMap<Int, Int>() {
            init {
                put(0x0b, PartitionTypes.FAT32)
                put(0x0c, PartitionTypes.FAT32)
                put(0x1b, PartitionTypes.FAT32)
                put(0x1c, PartitionTypes.FAT32)

                put(0x01, PartitionTypes.FAT12)

                put(0x04, PartitionTypes.FAT16)
                put(0x06, PartitionTypes.FAT16)
                put(0x0e, PartitionTypes.FAT16)

                put(0x83, PartitionTypes.LINUX_EXT)

                put(0x07, PartitionTypes.NTFS_EXFAT)

                put(0xaf, PartitionTypes.APPLE_HFS_HFS_PLUS)
            }
        }

        private val TAG = MasterBootRecord::class.java.simpleName
        private const val TABLE_OFFSET = 446
        private const val TABLE_ENTRY_SIZE = 16

        /**
         * Reads and parses the MBR located in the buffer.
         *
         * @param buffer
         * The data which shall be examined.
         * @return A new [.MasterBootRecord] or null if the data does not
         * seem to be a MBR.
         */
        @Throws(IOException::class)
        fun read(buffer: ByteBuffer): MasterBootRecord? {
            val result = MasterBootRecord()
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            if (buffer.limit() < 512) {
                throw IOException("Size mismatch!")
            }

            // test if it is a valid master boot record
            if (buffer.get(510) != 0x55.toByte() || buffer.get(511) != 0xaa.toByte()) {
                Log.i(TAG, "not a valid mbr partition table!")
                return null
            }

            for (i in 0..3) {
                val offset = TABLE_OFFSET + i * TABLE_ENTRY_SIZE
                val partitionType = buffer.get(offset + 4)
                // unused partition
                if (partitionType.toInt() == 0)
                    continue
                if (partitionType.toInt() == 0x05 || partitionType.toInt() == 0x0f) {
                    Log.w(TAG, "extended partitions are currently unsupported!")
                    continue
                }

                var type = partitionTypes[partitionType.toInt() and 0xff]

                if (type == null) {
                    Log.d(TAG, "Unknown partition type$partitionType")
                    type = PartitionTypes.UNKNOWN
                }

                val entry = PartitionTableEntry(type,
                        buffer.getInt(offset + 8), buffer.getInt(offset + 12))

                result.partitions.add(entry)
            }

            return result
        }
    }
}
