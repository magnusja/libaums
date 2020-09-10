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
import com.github.mjdev.libaums.util.LRUCache
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class represents the File Allocation Table (FAT) in a FAT32 file system.
 * The FAT is used to allocate the space of the disk to the different files and
 * directories.
 *
 *
 * The FAT distributes clusters with a specific cluster size
 * [#getBytesPerCluster()][com.github.mjdev.libaums.fs.fat32.Fat32BootSector]
 * . Every entry in the FAT is 32 bit. The FAT is a (linked) list where the
 * clusters can be followed until a cluster chain ends.
 *
 *
 * For more information you should refer to the official documentation of FAT32.
 *
 * @author mjahnen
 */
class FAT
/**
 * Constructs a new FAT.
 *
 * @param blockDevice
 * The block device where the FAT is located.
 * @param bootSector
 * The corresponding boot sector of the FAT32 file system.
 * @param fsInfoStructure
 * The info structure where the last allocated block and the free
 * clusters are saved.
 */
internal constructor(private val blockDevice: BlockDeviceDriver, bootSector: Fat32BootSector,
                                   private val fsInfoStructure: FsInfoStructure) {
    private val fatOffset: LongArray
    private var fatNumbers: IntArray
    private val cache = LRUCache<Long, Array<Long>>(64)

    init {
        if (!bootSector.isFatMirrored) {
            val fatNumber = bootSector.validFat.toInt()
            fatNumbers = intArrayOf(fatNumber)
            Log.i(TAG, "fat is not mirrored, fat $fatNumber is valid")
        } else {
            val fatCount = bootSector.fatCount.toInt()
            fatNumbers = IntArray(fatCount)
            for (i in 0 until fatCount) {
                fatNumbers[i] = i
            }
            Log.i(TAG, "fat is mirrored, fat count: $fatCount")
        }

        fatOffset = LongArray(fatNumbers.size)
        for (i in fatOffset.indices) {
            fatOffset[i] = bootSector.getFatOffset(fatNumbers[i])
        }
    }

    /**
     * This methods gets a chain by following the given start cluster to an end
     * mark.
     *
     * @param startCluster
     * The start cluster where the chain starts.
     * @return The chain including the start cluster.
     * @throws IOException
     * If reading from device fails.
     */
    @Throws(IOException::class)
    internal fun getChain(startCluster: Long): Array<Long> {

        if (startCluster == 0L) {
            // if the start cluster is 0, we have an empty file
            return arrayOf()
        }

        val cachedChain = cache[startCluster]
        if (cachedChain != null) {
            return cachedChain
        }

        val result = ArrayList<Long>()
        val bufferSize = blockDevice.blockSize * 2
        // for performance reasons we always read or write two times the block
        // size
        // this is esp. good for long cluster chains because it reduces of read
        // or writes
        // and mostly cluster chains are located consecutively in the FAT
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        var currentCluster = startCluster
        var offset: Long
        var offsetInBlock: Long
        var lastOffset: Long = -1

        do {
            result.add(currentCluster)
            offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                buffer.clear()
                blockDevice.read(offset, buffer)
                lastOffset = offset
            }

            currentCluster = (buffer.getInt(offsetInBlock.toInt()) and 0x0FFFFFFF).toLong()
        } while (currentCluster < FAT32_EOF_CLUSTER)

        val arr = result.toTypedArray()
        cache[startCluster] = arr

        return arr
    }

    /**
     * This methods searches for free clusters in the chain and then assigns it
     * to the existing chain which is given at a parameter. The current chain
     * given as parameter can also be empty so that a completely new chain (with
     * a new start cluster) is created.
     *
     * @param chain
     * The existing chain or an empty array to create a completely
     * new chain.
     * @param numberOfClusters
     * The number of clusters which shall newly be allocated.
     * @return The new chain including the old and the newly allocated clusters.
     * @throws IOException
     * If reading or writing to the FAT fails.
     */
    @Throws(IOException::class)
    internal fun alloc(chain: Array<Long>, numberOfClusters: Int): Array<Long> {
        var numberOfClusters = numberOfClusters

        // save original number of clusters for fs info structure
        val originalNumberOfClusters = numberOfClusters

        val result = ArrayList<Long>(chain.size + numberOfClusters)
        result.addAll(Arrays.asList(*chain))
        // for performance reasons we always read or write two times the block
        // size
        // this is esp. good for long cluster chains because it reduces of read
        // or writes
        // and mostly cluster chains are located consecutively in the FAT
        val bufferSize = blockDevice.blockSize * 2
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val cluster: Long = if (chain.isNotEmpty())
            chain[chain.size - 1]
        else
            -1

        var lastAllocated = fsInfoStructure.lastAllocatedClusterHint
        if (lastAllocated == FsInfoStructure.INVALID_VALUE.toLong()) {
            // we have to start from the beginning because there is no hint!
            lastAllocated = 2
        }

        var currentCluster = lastAllocated

        var offset: Long
        var offsetInBlock: Long
        var lastOffset: Long = -1

        // first we search all needed cluster and save them
        while (numberOfClusters > 0) {
            currentCluster++
            offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                buffer.clear()
                blockDevice.read(offset, buffer)
                lastOffset = offset
            }

            if (buffer.getInt(offsetInBlock.toInt()) == 0) {
                result.add(currentCluster)
                numberOfClusters--
            }
        }

        // TODO we should write in in all FATs when they are mirrored!
        if (cluster.toInt() != -1) {
            // now it is time to write the partial cluster chain
            // start with the last cluster in the existing chain
            offset = (fatOffset[0] + cluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + cluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                buffer.clear()
                blockDevice.read(offset, buffer)
                lastOffset = offset
            }
            buffer.putInt(offsetInBlock.toInt(), result[chain.size].toInt())
        }

        // write the new allocated clusters now
        for (i in chain.size until result.size - 1) {
            currentCluster = result[i]
            offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                buffer.clear()
                blockDevice.write(lastOffset, buffer)
                buffer.clear()
                blockDevice.read(offset, buffer)
                lastOffset = offset
            }

            buffer.putInt(offsetInBlock.toInt(), result[i + 1].toInt())
        }

        // write end mark to last newly allocated cluster now
        currentCluster = result[result.size - 1]
        offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
        offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

        // if we have a new offset we are forced to read again
        if (lastOffset != offset) {
            buffer.clear()
            blockDevice.write(lastOffset, buffer)
            buffer.clear()
            blockDevice.read(offset, buffer)
            //lastOffset = offset
        }
        buffer.putInt(offsetInBlock.toInt(), FAT32_EOF_CLUSTER)
        buffer.clear()
        blockDevice.write(offset, buffer)

        // refresh the info structure
        fsInfoStructure.lastAllocatedClusterHint = currentCluster
        fsInfoStructure.decreaseClusterCount(originalNumberOfClusters.toLong())
        fsInfoStructure.write()

        Log.i(TAG, "allocating clusters finished")

        val arr = result.toTypedArray()

        cache[arr[0]] = arr

        return arr
    }

    /**
     * This methods frees the desired number of clusters in the FAT and then
     * sets the last remaining cluster to the end mark. If all clusters are
     * requested to be freed the last step will be omitted.
     *
     *
     * This methods frees the clusters starting at the end of the existing
     * cluster chain.
     *
     * @param chain
     * The existing chain where the clusters shall be freed from.
     * @param numberOfClusters
     * The amount of clusters which shall be freed.
     * @return The new chain without the unneeded clusters.
     * @throws IOException
     * If reading or writing to the FAT fails.
     * @throws IllegalStateException
     * If more clusters are requested to be freed than currently
     * exist in the chain.
     */
    @Throws(IOException::class)
    internal fun free(chain: Array<Long>, numberOfClusters: Int): Array<Long> {
        val offsetInChain = chain.size - numberOfClusters
        // for performance reasons we always read or write two times the block
        // size
        // this is esp. good for long cluster chains because it reduces of read
        // or writes
        // and mostly cluster chains are located consecutively in the FAT
        val bufferSize = blockDevice.blockSize * 2
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        check(offsetInChain >= 0) { "trying to remove more clusters in chain than currently exist!" }

        var currentCluster: Long

        var offset: Long
        var offsetInBlock: Long
        var lastOffset: Long = -1

        // free all unneeded clusters
        for (i in offsetInChain until chain.size) {
            currentCluster = chain[i]
            offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                if (lastOffset.toInt() != -1) {
                    buffer.clear()
                    blockDevice.write(lastOffset, buffer)
                }

                buffer.clear()
                blockDevice.read(offset, buffer)
                lastOffset = offset
            }

            buffer.putInt(offsetInBlock.toInt(), 0)
        }

        // TODO we should write in in all FATs when they are mirrored!
        if (offsetInChain > 0) {
            // write the end mark to last cluster in the new chain
            currentCluster = chain[offsetInChain - 1]
            offset = (fatOffset[0] + currentCluster * 4) / bufferSize * bufferSize
            offsetInBlock = (fatOffset[0] + currentCluster * 4) % bufferSize

            // if we have a new offset we are forced to read again
            if (lastOffset != offset) {
                buffer.clear()
                blockDevice.write(lastOffset, buffer)
                buffer.clear()
                blockDevice.read(offset, buffer)
                //lastOffset = offset
            }
            buffer.putInt(offsetInBlock.toInt(), FAT32_EOF_CLUSTER)
            buffer.clear()
            blockDevice.write(offset, buffer)
        } else {
            // if we freed all clusters we have to write the last change of the
            // for loop above
            buffer.clear()
            blockDevice.write(lastOffset, buffer)
        }

        Log.i(TAG, "freed $numberOfClusters clusters")

        // increase the free cluster count by decreasing with a negative value
        fsInfoStructure.decreaseClusterCount((-numberOfClusters).toLong())
        fsInfoStructure.write()

        val arr = Arrays.copyOfRange(chain, 0, offsetInChain)

        if (arr.isNotEmpty()) {
            cache[arr[0]] = arr
        }

        return arr
    }

    companion object {

        private val TAG = FAT::class.java.simpleName

        /**
         * End of file / chain marker. This is used to determine when following a
         * cluster chain should be stopped. (Last allocated cluster has been found.)
         */
        private const val FAT32_EOF_CLUSTER = 0x0FFFFFF8
    }
}
