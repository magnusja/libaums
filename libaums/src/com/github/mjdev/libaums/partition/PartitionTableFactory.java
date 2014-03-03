package com.github.mjdev.libaums.partition;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecord;

public class PartitionTableFactory {
	public static PartitionTable createPartitionTable(BlockDeviceDriver blockDevice) throws IOException {
		// we currently only support mbr
		ByteBuffer buffer = ByteBuffer.allocate(512);
		blockDevice.read(0, buffer);
		return MasterBootRecord.read(buffer);
	}
}
