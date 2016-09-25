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
import com.github.mjdev.libaums.partition.PartitionException;

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
public class Fat16BootSector implements BootSector {
    private static final int BYTES_PER_SECTOR_OFF = 11;
    private static final int SECTORS_PER_CLUSTER_OFF = 13;
    private static final int RESERVED_COUNT_OFF = 14;
    private static final int FAT_COUNT_OFF = 16;
    private static final int TOTAL_SECTORS_16_OFFSET = 19;
    private static final int SECTORS_PER_FAT_OFF = 22;
    private static final int FLAGS_OFF = 40;
    private static final int VOLUME_LABEL_OFF = 43;
    private static final int ROOT_DIR_CLUSTER_OFF = 17;
    private static final int EXTENDED_BOOT_SIGNATURE_OFFSET = 0x26;

    private boolean fatMirrored;
    private byte validFat;
    private ByteBuffer byteBuffer;

    private Fat16BootSector() {

    }


    /**
     * Reads a FAT12 boot sector from the given byteBuffer. The byteBuffer has to be 512
     * (the size of a boot sector) bytes.
     *
     * @param buffer The data where the boot sector is located.
     * @return A newly created boot sector.
     */
    public static Fat16BootSector read(ByteBuffer buffer) {
        Fat16BootSector result = new Fat16BootSector();
        result.byteBuffer = buffer;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        short flag = (short) result.get16(FLAGS_OFF);
        result.fatMirrored = ((byte) flag & 0x80) == 0;
        result.validFat = (byte) ((byte) flag & 0x7);
        return result;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Returns the number of bytes in one single sector of a FAT32 file system.
     *
     * @return Number of bytes.
     */
    public short getBytesPerSector() {
        return (short) get16(BYTES_PER_SECTOR_OFF);
    }

    /**
     * Returns the number of sectors in one single cluster of a FAT32 file
     * system.
     *
     * @return Number of bytes.
     */
    public short getSectorsPerCluster() {
        return getByteBuffer().get(SECTORS_PER_CLUSTER_OFF);
    }

    /**
     * Returns the number of reserved sectors at the beginning of the FAT32 file
     * system. This includes one sector for the boot sector.
     *
     * @return Number of sectors.
     */
    public short getReservedSectors() {
        return (short) get16(RESERVED_COUNT_OFF);
    }

    /**
     * Returns the number of the FATs in the FAT32 file system. This is mostly
     * 2.
     *
     * @return Number of FATs.
     */
    public byte getFatCount() {
        return getByteBuffer().get(FAT_COUNT_OFF);
    }

    /**
     * Returns the total number of sectors in the file system.
     *
     * @return Total number of sectors.
     */
    public long getTotalNumberOfSectors() {
        return get16(TOTAL_SECTORS_16_OFFSET);
    }

    /**
     * Returns the total number of sectors in one file allocation table. The
     * FATs have a fixed size.
     *
     * @return Number of sectors in one FAT.
     */
    public long getSectorsPerFat() {
        return get16(SECTORS_PER_FAT_OFF);
    }

    /**
     * Returns the start cluster of the root directory in the FAT32 file system.
     *
     * @return Root directory start cluster.
     */
    public long getRootDirStartCluster() {
        return 2;
    }

    @Override
    public short getFsInfoStartSector() {
        try {
            throw new PartitionException("The FS_info is only support on fat32", -1);
        } catch (PartitionException e) {
            e.printStackTrace();
        }
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
        return (int) (getSectorsPerCluster() * getNumberRootDirEntries());
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
        long sectSize = this.getBytesPerSector();
        long sectsPerFat = this.getSectorsPerFat();
        long resSects = this.getReservedSectors();

        long offset = resSects * sectSize;
        long fatSize = sectsPerFat * sectSize;

        offset += fatNumber * fatSize;

        return offset;
    }

    /**
     * Returns the offset in bytes from the beginning of the file system of the
     * data area. The data area is the area where the contents of directories
     * and files are saved.
     *
     * @return Offset in bytes.
     */
    public long getDataAreaOffset() {
        long sectSize = this.getBytesPerSector();
        long sectsPerFat = this.getSectorsPerFat();
        int fats = this.getFatCount();
        long offset = getFatOffset(0);
        offset += fats * sectsPerFat * sectSize;
        return offset;
    }

    /**
     * This returns the volume label stored in the boot sector. This is mostly
     * not used and you should instead use {@link FatDirectory#getVolumeLabel()}
     * of the root directory.
     *
     * @return The volume label.
     */
    public String getVolumeLabel() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            byte b = getByteBuffer().get(VOLUME_LABEL_OFF + i);
            if (b == 0)
                break;
            builder.append((char) b);
        }
        return builder.toString();
    }

    @Override
    public long getNumberRootDirEntries() {
        return get16(ROOT_DIR_CLUSTER_OFF);
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
