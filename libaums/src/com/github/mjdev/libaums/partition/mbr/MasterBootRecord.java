package com.github.mjdev.libaums.partition.mbr;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.util.Log;

import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

public class MasterBootRecord implements PartitionTable {
	
	private static final String TAG = MasterBootRecord.class.getSimpleName();
	private static final int TABLE_OFFSET = 446;
	private static final int TABLE_ENTRY_SIZE = 16;
	
	public List<PartitionTableEntry> partitions = new ArrayList<PartitionTableEntry>();
	
	private MasterBootRecord() {
		
	}
	
	public static MasterBootRecord read(ByteBuffer buffer) {
		MasterBootRecord result = new MasterBootRecord();
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		// test if it is a valid master boot record
		if (buffer.get(510) != (byte) 0x55 || buffer.get(511) != (byte) 0xaa) {
			Log.i(TAG, "not a valid mbr partition table!");
			return null;
		}
		
		for(int i = 0; i < 4; i++) {
			int offset = TABLE_OFFSET + i * TABLE_ENTRY_SIZE;
			byte partitionType = buffer.get(offset + 4);
			if(partitionType == 0) continue;
			if(partitionType == 0x05 || partitionType == 0x0f) {
				Log.w(TAG, "extended partitions are currently unsupported!");
				continue;
			}
			
			PartitionTableEntry entry = new PartitionTableEntry(partitionType, buffer.getInt(offset + 8), buffer.getInt(offset + 12));
			
			result.partitions.add(entry);
		}
		
		return result;
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
