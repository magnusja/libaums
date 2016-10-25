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

package com.github.mjdev.libaums.partition.mbr;

import android.util.Log;

import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.util.PrettyPrint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class represents the Master Boot Record (MBR), which is a partition
 * table used by most block devices coming from Windows or Unix.
 *
 * @author mjahnen
 */
public class MasterBootRecord implements PartitionTable {

    private static final String TAG = MasterBootRecord.class.getSimpleName();
    private static final int TABLE_OFFSET = 446;
    private static final int TABLE_ENTRY_SIZE = 16;
    private static final int PE_OFFSET_SECTOR_OFFSET = 0x08;
    private static final int MBR_OFFSET_WATERMARK = 0x03;
    private static final int PE_OFFSET_NUMBER_OF_SECTORS = 0x0c;
    public static final int VOLUME_LABEL_OFFSET = 54;

    public List<PartitionTableEntry> partitions = new ArrayList<>();

    private MasterBootRecord() {

    }

    /**
     * Reads and parses the MBR located in the buffer.
     *
     * @param buffer The data which shall be examined.
     * @return A new {@link #MasterBootRecord()} or null if the data does not
     * seem to be a MBR.
     */
    public static MasterBootRecord read(ByteBuffer buffer) {
        MasterBootRecord result = new MasterBootRecord();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Log.d(TAG, PrettyPrint.prettyPrint(buffer.array()));

        // test if it is a valid master boot record
        if (buffer.get(510) != (byte) 0x55 || buffer.get(511) != (byte) 0xaa) {
            Log.i(TAG, "not a valid mbr partition table!");
            return null;
        }

        byte[] executable = new byte[5];
        buffer.position(MBR_OFFSET_WATERMARK);
        buffer.get(executable);
        buffer.rewind();
        String watermark = new String(executable);
        if (watermark.matches("(IBM|MS|..DOS|..dos|NTFS)")) {
            Log.d(TAG, "Found FAT Floppy watermark:" + watermark);

        }
        Log.d(TAG, "volume label:" + getVolumeLabel(buffer));

        for (int partitionNumber = 0; partitionNumber < 4; partitionNumber++) {
            int offset = (TABLE_OFFSET + partitionNumber) * TABLE_ENTRY_SIZE;
            byte partitionType = buffer.get(offset + 4);
            Log.w(TAG, "partition table :" + (partitionNumber + 1));
            Log.w(TAG, "partition type:" + partitionType);
            Log.w(TAG, "offset:" + offset);
            if (partitionType == 0)
                continue;
            if (partitionType == 0x05 || partitionType == 0x0f) {
                Log.w(TAG, "extended partitions are currently unsupported!");
                continue;
            }
            PartitionTableEntry entry = new PartitionTableEntry(partitionType,
                    buffer.getInt(offset + 8), buffer.getInt(offset + 12));

           /* int sectorOffset = buffer
                    .getInt((partitionNumber * PE_RECORD_SIZE) + TABLE_OFFSET + PE_OFFSET_SECTOR_OFFSET);
            int numberOfSectors = buffer
                    .getInt((partitionNumber * PE_RECORD_SIZE) + TABLE_OFFSET + PE_OFFSET_NUMBER_OF_SECTORS);*/
            result.partitions.add(entry);
        }

        return result;
    }

    public static String getVolumeLabel(ByteBuffer buffer) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            final char c = (char) buffer.get(VOLUME_LABEL_OFFSET + i);
            if (c != 0) {
                sb.append(c);
            } else {
                break;
            }
        }

        return sb.toString();
    }

    @Override
    public int getSize() {
        return 512;
    }

    @Override
    public Collection<PartitionTableEntry> getPartitionTableEntries() {
        return partitions;
    }

}
