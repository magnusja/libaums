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

import android.util.Log

import com.github.mjdev.libaums.driver.BlockDeviceDriver

/**
 * This class represents a cluster chain which can be followed in the FAT of a
 * FAT32 file system. You can [read][.read] from or
 * [write][.write] to it easily without having to worry
 * about the specific clusters.
 *
 * @author mjahnen
 */
class ClusterChain
/**
 * Constructs a new ClusterChain with the given information.
 *
 * @param startCluster
 * The start cluster which shall be followed in the FAT.
 * @param blockDevice
 * The block device where the fat fs is located.
 * @param fat
 * The file allocation table.
 * @param bootSector
 * The boot sector of the FAT32 fs.
 * @throws IOException
 */
/* package */ @Throws(IOException::class)
internal constructor(startCluster: Long, private val blockDevice: BlockDeviceDriver, private val fat: FAT,
                     bootSector: Fat32BootSector) {
    private var chain: Array<Long>? = null
    private val clusterSize: Long
    private val dataAreaOffset: Long

    /**
     * Gets the current allocated clusters for this chain.
     *
     * @return The number of clusters.
     * @see .setClusters
     * @see .getLength
     */
    /* package */
    /**
     * Sets a new cluster size for the cluster chain. This method allocates or
     * frees clusters in the FAT if the number of the new clusters is bigger or
     * lower than the current number of allocated clusters.
     *
     * @param newNumberOfClusters
     * The new number of clusters.
     * @throws IOException
     * If growing or allocating the chain fails.
     * @see .getClusters
     * @see .setLength
     */
    /* package */internal var clusters: Int
        get() = chain!!.size
        @Throws(IOException::class)
        set(newNumberOfClusters) {
            val oldNumberOfClusters = clusters
            if (newNumberOfClusters == oldNumberOfClusters)
                return

            if (newNumberOfClusters > oldNumberOfClusters) {
                Log.d(TAG, "grow chain")
                chain = fat.alloc(chain, newNumberOfClusters - oldNumberOfClusters)
            } else {
                Log.d(TAG, "shrink chain")
                chain = fat.free(chain, oldNumberOfClusters - newNumberOfClusters)
            }
        }

    /**
     * Returns the size in bytes the chain currently occupies on the disk.
     *
     * @return The size / length in bytes of the chain.
     * @see .setLength
     * @see .getClusters
     */
    /* package */
    /**
     * Sets the new length in bytes of this chain. This method allocates or
     * frees new space on the disk depending on the new length.
     *
     * @param newLength
     * The new length.
     * @throws IOException
     * If growing or allocating the chain fails.
     * @see .getLength
     * @see .setClusters
     */
    /* package */internal var length: Long
        get() = chain!!.size * clusterSize
        @Throws(IOException::class)
        set(newLength) {
            val newNumberOfClusters = (newLength + clusterSize - 1) / clusterSize
            clusters = newNumberOfClusters.toInt()
        }

    init {
        Log.d(TAG, "Init a cluster chain, reading from FAT")
        chain = fat.getChain(startCluster)
        clusterSize = bootSector.bytesPerCluster.toLong()
        dataAreaOffset = bootSector.dataAreaOffset
        Log.d(TAG, "Finished init of a cluster chain")
    }

    /**
     * Reads from the cluster chain at the given offset into the given buffer.
     * This method automatically searches for following clusters in the chain
     * and reads from them appropriately.
     *
     * @param offset
     * The offset in bytes where reading shall start.
     * @param dest
     * The destination buffer the contents of the chain shall be
     * copied to.
     * @throws IOException
     * If reading fails.
     */
    /* package */@Throws(IOException::class)
    internal fun read(offset: Long, dest: ByteBuffer) {
        var length = dest.remaining()

        var chainIndex = (offset / clusterSize).toInt()
        // if the offset is not a multiple of the cluster size we have to start
        // reading
        // directly in the cluster
        if (offset % clusterSize != 0L) {
            // offset in the cluster
            val clusterOffset = (offset % clusterSize).toInt()
            val size = Math.min(length, (clusterSize - clusterOffset).toInt())
            dest.limit(dest.position() + size)

            blockDevice.read(getFileSystemOffset(chain!![chainIndex], clusterOffset), dest)

            // round up to next cluster in the chain
            chainIndex++
            // make length now a multiple of the cluster size
            length -= size
        }

        // now we can proceed reading the clusters without an offset in the
        // cluster
        while (length > 0) {
            // we always read one cluster at a time, or if remaining size is
            // less than the cluster size, only "size" bytes
            val size = Math.min(clusterSize, length.toLong()).toInt()
            dest.limit(dest.position() + size)

            blockDevice.read(getFileSystemOffset(chain!![chainIndex], 0), dest)

            chainIndex++
            length -= size
        }
    }

    /**
     * Writes to the cluster chain at the given offset from the given buffer.
     * This method automatically searches for following clusters in the chain
     * and reads from them appropriately.
     *
     * @param offset
     * The offset in bytes where writing shall start.
     * @param source
     * The buffer which holds the contents which shall be transferred
     * into the cluster chain.
     * @throws IOException
     * If writing fails.
     */
    /* package */@Throws(IOException::class)
    internal fun write(offset: Long, source: ByteBuffer) {
        var length = source.remaining()

        var chainIndex = (offset / clusterSize).toInt()
        // if the offset is not a multiple of the cluster size we have to start
        // reading
        // directly in the cluster
        if (offset % clusterSize != 0L) {
            val clusterOffset = (offset % clusterSize).toInt()
            val size = Math.min(length, (clusterSize - clusterOffset).toInt())
            source.limit(source.position() + size)

            blockDevice.write(getFileSystemOffset(chain!![chainIndex], clusterOffset), source)

            // round up to next cluster in the chain
            chainIndex++
            // make length now a multiple of the cluster size
            length -= size
        }

        // now we can proceed reading the clusters without an offset in the
        // cluster
        while (length > 0) {
            // we always write one cluster at a time, or if remaining size is
            // less than the cluster size, only "size" bytes
            val size = Math.min(clusterSize, length.toLong()).toInt()
            source.limit(source.position() + size)

            blockDevice.write(getFileSystemOffset(chain!![chainIndex], 0), source)

            chainIndex++
            length -= size
        }
    }

    /**
     * Returns the offset of a cluster from the beginning of the FAT32 file
     * system in bytes.
     *
     * @param cluster
     * The desired cluster.
     * @param clusterOffset
     * The desired offset in bytes in the cluster.
     * @return Offset in bytes from the beginning of the disk (FAT32 file
     * system).
     */
    private fun getFileSystemOffset(cluster: Long, clusterOffset: Int): Long {
        return dataAreaOffset + clusterOffset.toLong() + (cluster - 2) * clusterSize
    }

    companion object {

        private val TAG = ClusterChain::class.java.simpleName
    }
}
