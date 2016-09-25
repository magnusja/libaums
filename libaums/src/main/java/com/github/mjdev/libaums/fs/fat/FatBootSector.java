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

package com.github.mjdev.libaums.fs.fat;

import com.github.mjdev.libaums.fs.BootSector;
import com.github.mjdev.libaums.fs.fat32.FatDirectory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class represents the FAT32 boot sector which is always located at the
 * beginning of every FAT32 file system. It holds important information about
 * the file system such as the cluster size and the start cluster of the root
 * directory.
 *
 * @author mjahnen
 */
public class FatBootSector implements BootSector {
    private static final int BYTES_PER_SECTOR_OFF = 11;
    private static final int SECTORS_PER_CLUSTER_OFF = 13;
    private static final int RESERVED_COUNT_OFF = 14;
    private static final int FAT_COUNT_OFF = 16;
    private static final int TOTAL_SECTORS_OFF = 32;
    private static final int SECTORS_PER_FAT_OFF = 36;
    private static final int FLAGS_OFF = 40;
    private static final int ROOT_DIR_CLUSTER_OFF = 44;
    private static final int VOLUME_LABEL_OFF = 48;

    private short bytesPerSector;
    private short sectorsPerCluster;
    private short reservedSectors;
    private byte fatCount;
    private long totalNumberOfSectors;
    private long sectorsPerFat;
    private long rootDirStartCluster;
    private boolean fatMirrored;
    private byte validFat;
    private String volumeLabel;
    private ByteBuffer byteBuffer;

    private FatBootSector() {

    }


    /**
     * Reads a FAT12 boot sector from the given byteBuffer. The byteBuffer has to be 512
     * (the size of a boot sector) bytes.
     *
     * @param buffer The data where the boot sector is located.
     * @return A newly created boot sector.
     */
    public static FatBootSector read(ByteBuffer buffer) {
        FatBootSector result = new FatBootSector();
        result.byteBuffer = buffer;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        result.bytesPerSector = (short) result.get16(BYTES_PER_SECTOR_OFF);
        result.sectorsPerCluster = (short) (buffer.get(SECTORS_PER_CLUSTER_OFF) & 0xff);
        result.reservedSectors = (short) result.get16(RESERVED_COUNT_OFF);
        result.fatCount = buffer.get(FAT_COUNT_OFF);
        result.totalNumberOfSectors = result.get32(TOTAL_SECTORS_OFF);
        result.sectorsPerFat = result.get32(SECTORS_PER_FAT_OFF);
        result.rootDirStartCluster = result.get32(ROOT_DIR_CLUSTER_OFF);
        short flag = (short) result.get16(FLAGS_OFF);
        result.fatMirrored = ((byte) flag & 0x80) == 0;
        result.validFat = (byte) ((byte) flag & 0x7);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            byte b = buffer.get(VOLUME_LABEL_OFF + i);
            if (b == 0)
                break;
            builder.append((char) b);
        }

        result.volumeLabel = builder.toString();

        return result;
    }

    /**
     * Returns the number of bytes in one single sector of a FAT32 file system.
     *
     * @return Number of bytes.
     */
    public short getBytesPerSector() {
        return bytesPerSector;
    }

    /**
     * Returns the number of sectors in one single cluster of a FAT32 file
     * system.
     *
     * @return Number of bytes.
     */
    public short getSectorsPerCluster() {
        return sectorsPerCluster;
    }

    /**
     * Returns the number of reserved sectors at the beginning of the FAT32 file
     * system. This includes one sector for the boot sector.
     *
     * @return Number of sectors.
     */
    public short getReservedSectors() {
        return reservedSectors;
    }

    /**
     * Returns the number of the FATs in the FAT32 file system. This is mostly
     * 2.
     *
     * @return Number of FATs.
     */
    public byte getFatCount() {
        return fatCount;
    }

    /**
     * Returns the total number of sectors in the file system.
     *
     * @return Total number of sectors.
     */
    public long getTotalNumberOfSectors() {
        return totalNumberOfSectors;
    }

    /**
     * Returns the total number of sectors in one file allocation table. The
     * FATs have a fixed size.
     *
     * @return Number of sectors in one FAT.
     */
    public long getSectorsPerFat() {
        return sectorsPerFat;
    }

    /**
     * Returns the start cluster of the root directory in the FAT32 file system.
     *
     * @return Root directory start cluster.
     */
    public long getRootDirStartCluster() {
        return rootDirStartCluster;
    }

    @Override
    public short getFsInfoStartSector() {
        return 0;
    }

    /**
     * Returns the start sector of the file system info structure.
     *
     * @return FSInfo Structure start sector.
     */

    /**
     * Returns if the different FATs in the file system are mirrored, ie. all of
     * them are holding the same data. This is used for backup purposes.
     *
     * @return True if the FAT is mirrored.
     * @see #getValidFat()
     * @see #getFatCount()
     */
    public boolean isFatMirrored() {
        return fatMirrored;
    }

    /**
     * Returns the valid FATs which shall be used if the FATs are not mirrored.
     *
     * @return Number of the valid FAT.
     * @see #isFatMirrored()
     * @see #getFatCount()
     */
    public byte getValidFat() {
        return validFat;
    }

    /**
     * Returns the amount in bytes in one cluster.
     *
     * @return Amount of bytes.
     */
    public int getBytesPerCluster() {
        return sectorsPerCluster * bytesPerSector;
    }

    /**
     * Returns the FAT offset in bytes from the beginning of the file system for
     * the given FAT number.
     *
     * @param fatNumber The number of the FAT.
     * @return Offset in bytes.
     * @see #isFatMirrored()
     * @see #getValidFat()
     */
    public long getFatOffset(int fatNumber) {
        return getBytesPerSector() * (getReservedSectors() + fatNumber * getSectorsPerFat());
    }

    /**
     * Returns the offset in bytes from the beginning of the file system of the
     * data area. The data area is the area where the contents of directories
     * and files are saved.
     *
     * @return Offset in bytes.
     */
    public long getDataAreaOffset() {
        return getFatOffset(0) + getFatCount() * getSectorsPerFat() * getBytesPerSector();
    }

    /**
     * This returns the volume label stored in the boot sector. This is mostly
     * not used and you should instead use {@link FatDirectory#getVolumeLabel()}
     * of the root directory.
     *
     * @return The volume label.
     */
    public String getVolumeLabel() {
        return volumeLabel;
    }

    @Override
    public long getDataClusterCount() {
        return getDataSize() / getBytesPerCluster();
    }

    /**
     * Returns the size of the data-containing portion of the file system.
     *
     * @return the number of bytes usable for storing user data
     */
    private long getDataSize() {
        return /*(getSectorCount() * getBytesPerSector()) -
                this.getFilesOffset()*/0;
    }


    protected int get16(int offset) {
        return byteBuffer.getShort(offset) & 0xffff;
    }

    protected long get32(int offset) {
        return byteBuffer.getInt(offset);
    }

    protected int get8(int offset) {
        return byteBuffer.get(offset) & 0xff;
    }

    protected void set16(int offset, int value) {
        byteBuffer.putShort(offset, (short) (value & 0xffff));
    }

    protected void set32(int offset, long value) {
        byteBuffer.putInt(offset, (int) (value & 0xffffffff));
    }

    protected void set8(int offset, int value) {
        if ((value & 0xff) != value) {
            throw new IllegalArgumentException(
                    value + " too big to be stored in a single octet");
        }

        byteBuffer.put(offset, (byte) (value & 0xff));
    }
}
