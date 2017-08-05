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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.partition.PartitionTypes;

/**
 * This class represents the Master Boot Record (MBR), which is a partition
 * table used by most block devices coming from Windows or Unix.
 * 
 * @author mjahnen
 * 
 */
public class MasterBootRecord implements PartitionTable {

	private static Map<Integer, Integer> partitionTypes = new HashMap<Integer, Integer>() {{
		put(0x0b, PartitionTypes.FAT32);
		put(0x0c, PartitionTypes.FAT32);
		put(0x1b, PartitionTypes.FAT32);
		put(0x1c, PartitionTypes.FAT32);

		put(0x01, PartitionTypes.FAT12);

		put(0x04, PartitionTypes.FAT16);
		put(0x06, PartitionTypes.FAT16);
		put(0x0e, PartitionTypes.FAT16);

		put(0x83, PartitionTypes.LINUX_EXT);

		put(0x07, PartitionTypes.NTFS_EXFAT);

		put(0xaf, PartitionTypes.APPLE_HFS_HFS_PLUS);
	}};

	private static final String TAG = MasterBootRecord.class.getSimpleName();
	private static final int TABLE_OFFSET = 446;
	private static final int TABLE_ENTRY_SIZE = 16;

	private List<PartitionTableEntry> partitions = new ArrayList<PartitionTableEntry>();

	private MasterBootRecord() {

	}

	/**
	 * Reads and parses the MBR located in the buffer.
	 * 
	 * @param buffer
	 *            The data which shall be examined.
	 * @return A new {@link #MasterBootRecord()} or null if the data does not
	 *         seem to be a MBR.
	 */
	public static MasterBootRecord read(ByteBuffer buffer) throws IOException {
		MasterBootRecord result = new MasterBootRecord();
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		if (buffer.limit() < 512) {
            throw new IOException("Size mismatch!");
        }

		// test if it is a valid master boot record
		if (buffer.get(510) != (byte) 0x55 || buffer.get(511) != (byte) 0xaa) {
			Log.i(TAG, "not a valid mbr partition table!");
			return null;
		}

		for (int i = 0; i < 4; i++) {
			int offset = TABLE_OFFSET + i * TABLE_ENTRY_SIZE;
			byte partitionType = buffer.get(offset + 4);
			// unused partition
			if (partitionType == 0)
				continue;
			if (partitionType == 0x05 || partitionType == 0x0f) {
				Log.w(TAG, "extended partitions are currently unsupported!");
				continue;
			}

			Integer type = partitionTypes.get((int) partitionType & 0xff);

			if(type == null) {
                Log.d(TAG, "Unknown partition type" + partitionType);
				type = PartitionTypes.UNKNOWN;
			}

			PartitionTableEntry entry = new PartitionTableEntry(type,
					buffer.getInt(offset + 8), buffer.getInt(offset + 12));

			result.partitions.add(entry);
		}

		return result;
	}

	@Override
	public int getSize() {
		return 512;
	}

	@Override
	public List<PartitionTableEntry> getPartitionTableEntries() {
		return partitions;
	}
}
