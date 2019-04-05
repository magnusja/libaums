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

package com.github.mjdev.libaums.fs.fat16;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class represents the File Allocation Table (FAT) in a FAT32 file system.
 * The FAT is used to allocate the space of the disk to the different files and
 * directories.
 * <p>
 * The FAT distributes clusters with a specific cluster size
 * {@link Fat16BootSector #getBytesPerCluster()}
 * . Every entry in the FAT is 16 bit. The FAT is a (linked) list where the
 * clusters can be followed until a cluster chain ends.
 * <p>
 * For more information you should refer to the official documentation of FAT32.
 *
 * @author mjahnen
 */
public class FAT {

    private static final String TAG = FAT.class.getSimpleName();

    /**
     * End of file / chain marker. This is used to determine when following a
     * cluster chain should be stopped. (Last allocated cluster has been found.)
     */
    private static final int FAT16_EOF_CLUSTER = 0xFFFF;
    public static final int SECTOR_SIZE = 512;
    private final long freeSpace;
    private final long occupiedSpace;
    private final Fat16BootSector bootSector;

    private HashMap<Long, Long[]> chains = new HashMap<>();
    private BlockDeviceDriver blockDevice;
    private long fatOffset[];
    private int fatNumbers[];

    /**
     * Constructs a new FAT.
     *
     * @param blockDevice The block device where the FAT is located.
     * @param bootSector  The corresponding boot sector of the FAT32 file system.
     */
    /* package */FAT(BlockDeviceDriver blockDevice, Fat16BootSector bootSector) {
        this.blockDevice = blockDevice;
        if (!bootSector.isFatMirrored()) {
            int fatNumber = bootSector.getValidFat();
            fatNumbers = new int[]{fatNumber};
            Log.i(TAG, "fat is not mirrored, fat " + fatNumber + " is valid");
        } else {
            int fatCount = bootSector.getFatCount();
            fatNumbers = new int[fatCount];
            for (int i = 0; i < fatCount; i++) {
                fatNumbers[i] = i;
            }
            Log.i(TAG, "fat is mirrored, fat count: " + fatCount);
        }

        fatOffset = new long[fatNumbers.length];
        for (int i = 0; i < fatOffset.length; i++) {
            fatOffset[i] = bootSector.getFatOffset(fatNumbers[i]);
        }


        long usedClusterCount = 0;
        long freeClusterCount = 0;
        try {
            ByteBuffer bb = UnsignedUtil.allocateLittleEndian(512);

            for (long blockNumber = (fatOffset[0] / 512); blockNumber < fatOffset[1] / 512; blockNumber++) {
                blockDevice.read(blockNumber * 512, bb);
                //need to skep first 4
                //for every other byte
                byte[] array = bb.array();
                boolean isFirstSector = blockNumber == 32;
                for (int i = isFirstSector ? 12 : 0; i < array.length; i++) {
                    byte b = array[i];
                    if ((i % 2 == 1)) {
                        if (b == 0x00) {
                            freeClusterCount++;
                        } else {
                            usedClusterCount++;
                        }
                    }
                }
                bb.position(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.freeSpace = freeClusterCount * bootSector.getBytesPerCluster();
        this.occupiedSpace = usedClusterCount * bootSector.getBytesPerCluster();
        this.bootSector = bootSector;
    }

    /**
     * This methods gets a chain by following the given start cluster to an end
     * mark.
     *
     * @param startCluster The start cluster where the chain starts.
     * @return The chain including the start cluster.
     * @throws IOException If reading from device fails.
     */
    /* package */Long[] getChain(long startCluster) throws IOException {

        if (startCluster == 0) {
            // if the start cluster is 0, we have an empty file
            return new Long[0];
        }

        if (chains.containsKey(startCluster))
            return chains.get(startCluster);

        final ArrayList<Long> result = new ArrayList<Long>();
        final int bufferSize = blockDevice.getBlockSize() * 2;

        ByteBuffer buffer = UnsignedUtil.allocateLittleEndian(bufferSize);

        int currentCluster = (int) startCluster;
        long clusterLocationInFat = (startCluster * 2);
        long fatOffset;
        long lastOffset = -1;

        do {
            result.add((long) currentCluster);
            fatOffset = (this.fatOffset[0] + clusterLocationInFat);

            // if we have a new fatOffset we are forced to read again
            if (lastOffset != fatOffset) {
                buffer.clear();
                blockDevice.read(fatOffset, buffer);
                lastOffset = fatOffset;
            }

            buffer.position(0);
            currentCluster = UnsignedUtil.getUnsignedInt16(0, buffer);
            clusterLocationInFat = currentCluster * 2;
        } while (currentCluster != FAT16_EOF_CLUSTER);

        Long[] returnResult = result.toArray(new Long[0]);
        chains.put(startCluster, returnResult);
        return returnResult;
    }

    Long[] getChain(long startCluster, long fileSize) throws IOException {
        Long[] chain = getChain(startCluster);

        long bytesOverCluster = fileSize - (bootSector.getBytesPerCluster() * chain.length);
        long neededClusters = 0;
        if (bytesOverCluster > 0) {
            neededClusters = (bytesOverCluster / bootSector.getBytesPerCluster());

            if ((bytesOverCluster % bootSector.getBytesPerCluster()) != 0) {
                neededClusters++;
            }
        }

        if (neededClusters == 0) {
            return chain;
        }

        Long[] alloc = findFreeClusters((int) neededClusters);

        ArrayList<Long> clustersToReserve = new ArrayList<>();
        clustersToReserve.add(chain[chain.length - 1]);
        clustersToReserve.addAll(Arrays.asList(alloc));
        reserveClusters(clustersToReserve.toArray(new Long[0]));


        ArrayList<Long> result = new ArrayList<>();
        result.addAll(Arrays.asList(chain));
        result.addAll(Arrays.asList(alloc));

        Long[] returnChain = result.toArray(new Long[0]);
        chains.put(startCluster, returnChain);
        return returnChain;
    }

    /**
     * This methods searches for free clusters in the chain and then assigns it
     * to the existing chain which is given at a parameter. The current chain
     * given as parameter can also be empty so that a completely new chain (with
     * a new start cluster) is created.
     *
     * @param numberOfClusters The number of clusters which shall newly be allocated.
     * @return The new chain including the old and the newly allocated clusters.
     * @throws IOException If reading or writing to the FAT fails.
     */
    /* package */Long[] alloc(int numberOfClusters) throws IOException {
        Long[] returnChain = findFreeClusters(numberOfClusters);
        reserveClusters(returnChain);


        return returnChain;
    }

    private Long[] findFreeClusters(int numberOfClusters) throws IOException {
        long startOfFAT = fatOffset[0];

        Long[] returnChain = new Long[numberOfClusters];

        ByteBuffer fatBuffer = UnsignedUtil.allocateLittleEndian(SECTOR_SIZE);

        int clustersPerSector = SECTOR_SIZE / 2;
        int currentBlock = 0;
        int chainIndex = 0;
        for (int clusterIndex = 0; clusterIndex < 0xFFFF; clusterIndex++) {
            if (clusterIndex % clustersPerSector == 0) {
                fatBuffer.position(0);
                blockDevice.read(startOfFAT + (currentBlock * SECTOR_SIZE), fatBuffer);
                fatBuffer.position(0);
                currentBlock++;
            }

            short clusterValue = fatBuffer.getShort();
            if (clusterValue == 0) {
                returnChain[chainIndex] = (long) clusterIndex;
                chainIndex++;
            }

            if (chainIndex == numberOfClusters) {
                break;
            }
        }
        return returnChain;
    }

    private void reserveClusters(Long[] returnChain) throws IOException {
        ByteBuffer sectorBuffer = UnsignedUtil.allocateLittleEndian(512);

        for (int x = 0; x < returnChain.length; x++) {

            long byteNeedingWritten = returnChain[x];
            long sectorLocationInFAT = (byteNeedingWritten * 2) / 512;
            int byteLocationInSector = ((int) (byteNeedingWritten * 2) % 512);

            sectorBuffer.position(0);
            blockDevice.read(bootSector.getFatOffset(0) + (sectorLocationInFAT * 512), sectorBuffer);
            if (x < returnChain.length - 1) {//if not last cluster in chain
                UnsignedUtil.setUnsignedInt16(byteLocationInSector, returnChain[x + 1].intValue(), sectorBuffer);
            } else {
                UnsignedUtil.setUnsignedInt16(byteLocationInSector, 0xFFFF, sectorBuffer);
            }


            for (int i = 0; i < bootSector.getFatCount(); i++) {
                sectorBuffer.position(0);
                blockDevice.write(bootSector.getFatOffset(i) + (sectorLocationInFAT * 512), sectorBuffer);
            }
        }

    }


    public long getFreeSpace() {
        return freeSpace;
    }

    public long getOccupiedSpace() {
        return occupiedSpace;
    }

    /**
     * This methods frees the desired cluster chain in the FAT
     *
     * @param startCluster The existing cluster chain location
     * @return The new chain without the unneeded clusters.
     * @throws IOException           If reading or writing to the FAT fails.
     * @throws IllegalStateException If more clusters are requested to be freed than currently
     *                               exist in the chain.
     */
    public void free(long startCluster) throws IOException {
        Long[] chain = getChain(startCluster);
        ByteBuffer sectorBuffer = UnsignedUtil.allocateLittleEndian(512);

        for (int x = 0; x < chain.length; x++) {

            long byteNeedingWritten = chain[x];
            long sectorLocationInFAT = (byteNeedingWritten * 2) / 512;
            int byteLocationInSector = ((int) (byteNeedingWritten * 2) % 512);

            sectorBuffer.position(0);
            blockDevice.read(fatOffset[0] + (sectorLocationInFAT * 512), sectorBuffer);
            sectorBuffer.putShort(byteLocationInSector, (short) 0);

            sectorBuffer.position(0);

            for (int i = 0; i < bootSector.getFatCount(); i++) {
                sectorBuffer.position(0);
                blockDevice.write(bootSector.getFatOffset(i) + (sectorLocationInFAT * 512), sectorBuffer);
            }

        }

    }
}
