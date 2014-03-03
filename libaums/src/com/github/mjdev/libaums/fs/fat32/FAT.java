package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

public class FAT {
	
	private static final int FAT32_EOF_CLUSTER = 0x0FFFFFF8;

	private Fat32BootSector bootSector;
	private BlockDeviceDriver blockDevice;
	private long fatOffset;
	private int fatNumber;
	
	public FAT(BlockDeviceDriver blockDevice, Fat32BootSector bootSector, int fatNumber) {
		this.fatNumber = fatNumber;
		this.bootSector = bootSector;
		this.blockDevice = blockDevice;
		fatOffset = bootSector.getFatOffset(fatNumber);
	}
	
	public Long[] getChain(long startCluster) throws IOException {
		final ArrayList<Long> result = new ArrayList<Long>();
		final int blockSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(blockSize);
		
		long currentCluster = startCluster;
		long offset;
		long offsetInBlock;
		long lastOffset = -1;
		
		do {
			result.add(currentCluster);
			offset = ((fatOffset + currentCluster * 4) / blockSize) * blockSize;
			offsetInBlock = ((fatOffset + currentCluster * 4) % blockSize);
			
			if(lastOffset != offset)
				blockDevice.read(offset, buffer);
			lastOffset = offset;
			
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			currentCluster = buffer.getInt((int)offsetInBlock);
			buffer.clear();
		} while (currentCluster < FAT32_EOF_CLUSTER);
		
		return result.toArray(new Long[0]);
	}
}
