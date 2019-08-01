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

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

import android.util.Log

import com.github.mjdev.libaums.driver.BlockDeviceDriver

/**
 * This class holds information which shall support the [FAT]. For example
 * it has a method to get the last allocated cluster (
 * [.getLastAllocatedClusterHint]). The FAT can use this to make
 * searching for free clusters more efficient because it does not have to search
 * the hole FAT.
 *
 * @author mjahnen
 */
/* package */internal class FsInfoStructure
/**
 * Constructs a new info structure.
 *
 * @param blockDevice
 * The device where the info structure is located.
 * @param offset
 * The offset where the info structure starts.
 * @throws IOException
 * If reading fails.
 */
@Throws(IOException::class)
private constructor(private val blockDevice: BlockDeviceDriver, private val offset: Int) {
    private val buffer: ByteBuffer

    /**
     *
     * @return The free cluster count or [.INVALID_VALUE] if this hint is
     * not available.
     */
    /* package */
    /**
     * Sets the cluster count to the new value. This change is not immediately
     * written to the disk. If you want to write the change to disk, call
     * [.write].
     *
     * @param value
     * The new cluster count.
     * @see .getFreeClusterCount
     * @see .decreaseClusterCount
     */
    /* package */ var freeClusterCount: Long
        get() = buffer.getInt(FREE_COUNT_OFF).toLong()
        set(value) {
            buffer.putInt(FREE_COUNT_OFF, value.toInt())
        }

    /**
     *
     * @return The last allocated cluster or [.INVALID_VALUE] if this hint
     * is not available.
     */
    /* package */
    /**
     * Sets the last allocated cluster to the new value. This change is not
     * immediately written to the disk. If you want to write the change to disk,
     * call [.write].
     *
     * @param value
     * The new last allocated cluster
     * @see .getLastAllocatedClusterHint
     */
    /* package */ var lastAllocatedClusterHint: Long
        get() = buffer.getInt(NEXT_FREE_OFFSET).toLong()
        set(value) {
            buffer.putInt(NEXT_FREE_OFFSET, value.toInt())
        }

    init {
        buffer = ByteBuffer.allocate(512)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        blockDevice.read(offset.toLong(), buffer)
        buffer.clear()

        if (buffer.getInt(LEAD_SIGNATURE_OFF) != LEAD_SIGNATURE
                || buffer.getInt(STRUCT_SIGNATURE_OFF) != STRUCT_SIGNATURE
                || buffer.getInt(TRAIL_SIGNATURE_OFF) != TRAIL_SIGNATURE) {
            throw IOException("invalid fs info structure!")
        }
    }

    /**
     * Decreases the cluster count by the desired number of clusters. This is
     * ignored [.getFreeClusterCount] returns [.INVALID_VALUE],
     * thus the free cluster count is unknown. The cluster count can also be
     * increased by specifying a negative value!
     *
     * @param numberOfClusters
     * Value, free cluster count shall be decreased by.
     * @see .setFreeClusterCount
     * @see .getFreeClusterCount
     */
    /* package */ fun decreaseClusterCount(numberOfClusters: Long) {
        var freeClusterCount = freeClusterCount
        if (freeClusterCount != FsInfoStructure.INVALID_VALUE.toLong()) {
            freeClusterCount = freeClusterCount - numberOfClusters
        }
    }

    /**
     * Writes the info structure to the device. This does not happen
     * automatically, if contents were changed so a call to this method is
     * needed!
     *
     * @throws IOException
     * If writing to device fails.
     */
    /* package */@Throws(IOException::class)
    fun write() {
        Log.d(TAG, "writing to device")
        blockDevice.write(offset.toLong(), buffer)
        buffer.clear()
    }

    companion object {

        /* package */ var INVALID_VALUE = -0x1

        private val LEAD_SIGNATURE_OFF = 0
        private val STRUCT_SIGNATURE_OFF = 484
        private val TRAIL_SIGNATURE_OFF = 508
        private val FREE_COUNT_OFF = 488
        private val NEXT_FREE_OFFSET = 492

        private val LEAD_SIGNATURE = 0x41615252
        private val STRUCT_SIGNATURE = 0x61417272
        private val TRAIL_SIGNATURE = -0x55ab0000

        private val TAG = FsInfoStructure::class.java.simpleName

        /**
         * Reads the info structure from the device.
         *
         * @param blockDevice
         * The device where the info structure is located.
         * @param offset
         * The offset where the info structure starts.
         * @return The newly created object.
         * @throws IOException
         * If reading fails.
         */
        /* package */@Throws(IOException::class)
        fun read(blockDevice: BlockDeviceDriver, offset: Int): FsInfoStructure {
            return FsInfoStructure(blockDevice, offset)
        }
    }
}
