package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

public class FAT {

	private static final String TAG = FAT.class.getSimpleName();
	
	private static final int FAT32_EOF_CLUSTER = 0x0FFFFFF8;

	private BlockDeviceDriver blockDevice;
	private long fatOffset[];
	private int fatNumbers[];
	private FsInfoStructure fsInfoStructure;
	
	/* package */ FAT(BlockDeviceDriver blockDevice, Fat32BootSector bootSector, FsInfoStructure fsInfoStructure) {
		this.blockDevice = blockDevice;
		this.fsInfoStructure = fsInfoStructure;
		if(!bootSector.isFatMirrored()) {
			int fatNumber = bootSector.getValidFat();
			fatNumbers = new int[] { fatNumber };
			Log.i(TAG, "fat is not mirrored, fat " + fatNumber + " is valid");
		} else {
			int fatCount = bootSector.getFatCount();
			fatNumbers = new int[fatCount];
			for(int i = 0; i < fatCount; i++) {
				fatNumbers[i] = i;
			}
			Log.i(TAG, "fat is mirrored, fat count: " + fatCount);
		}
		
		fatOffset = new long[fatNumbers.length];
		for(int i = 0; i < fatOffset.length; i++) {
			fatOffset[i] = bootSector.getFatOffset(fatNumbers[i]);
		}
	}
	
	/* package */ Long[] getChain(long startCluster) throws IOException {
		final ArrayList<Long> result = new ArrayList<Long>();
		final int bufferSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		long currentCluster = startCluster;
		long offset;
		long offsetInBlock;
		long lastOffset = -1;
		
		do {
			result.add(currentCluster);
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
			
			if(lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			
			currentCluster = buffer.getInt((int)offsetInBlock);
		} while (currentCluster < FAT32_EOF_CLUSTER);
		
		return result.toArray(new Long[0]);
	}
	
	/* package */ Long[] alloc(Long[] chain, int numberOfClusters) throws IOException {
		final ArrayList<Long> result = new ArrayList<Long>(chain.length + numberOfClusters);
		result.addAll(Arrays.asList(chain));
		final int bufferSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		final long cluster;
		if(chain.length != 0)
			cluster = chain[chain.length - 1];
		else
			cluster = -1;
		
		long lastAllocated = fsInfoStructure.getLastAllocatedClusterHint();
		if(lastAllocated == FsInfoStructure.INVALID_VALUE) {
			// we have to start from the beginning because there is no hint!
			lastAllocated = 2;
		}
		
		long currentCluster = lastAllocated;
		
		long offset;
		long offsetInBlock;
		long lastOffset = -1;
		
		// first we search all needed cluster and save them
		while(numberOfClusters > 0) {
			currentCluster++;
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
			
			if(lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			
			if(buffer.getInt((int)offsetInBlock) == 0) {
				result.add(currentCluster);
				numberOfClusters--;
			}
		}
		
		if(cluster != -1) {
			// now it is time to write the partial cluster chain
			// start with the last cluster in the existing chain
			offset = ((fatOffset[0] + cluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + cluster * 4) % bufferSize);
			if(lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			buffer.putInt((int)offsetInBlock, (int)result.get(chain.length).longValue());
		}
		
		// write the new allocated clusters now
		for(int i = chain.length; i < result.size() - 1; i++) {
			currentCluster = result.get(i);
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
			
			if(lastOffset != offset) {
				buffer.clear();
				blockDevice.write(lastOffset, buffer);
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			
			buffer.putInt((int)offsetInBlock, (int)result.get(i + 1).longValue());
		}
		
		// write end mark to last newly allocated cluster now
		currentCluster = result.get(result.size() - 1);
		offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
		offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
		if(lastOffset != offset) {
			buffer.clear();
			blockDevice.write(lastOffset, buffer);
			buffer.clear();
			blockDevice.read(offset, buffer);
			lastOffset = offset;
		}
		buffer.putInt((int)offsetInBlock, FAT32_EOF_CLUSTER);
		buffer.clear();
		blockDevice.write(offset, buffer);

		// refresh the info structure
		fsInfoStructure.setLastAllocatedClusterHint(currentCluster);
		fsInfoStructure.decreaseClusterCount(numberOfClusters);
		fsInfoStructure.write();
		
		Log.i(TAG, "allocating clusters finished");
		
		return result.toArray(new Long[0]);
	}
	
	/* package */ Long[] free(Long[] chain, int numberOfClusters) throws IOException {
		final int offsetInChain = chain.length - numberOfClusters;
		final int bufferSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		if(offsetInChain < 0)
			throw new IllegalStateException("trying to remove more clusters in chain than currently exist!");
		
		long currentCluster;
		
		long offset;
		long offsetInBlock;
		long lastOffset = -1;
		
		// free all unneeded clusters
		for(int i = offsetInChain; i < chain.length; i++) {
			currentCluster = chain[i];
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
			
			if(lastOffset != offset) {
				if(lastOffset != -1) {
					buffer.clear();
					blockDevice.write(lastOffset, buffer);
				}
				
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			
			buffer.putInt((int)offsetInBlock, 0);
		}
		
		if(offsetInChain > 0) {
			// write the end mark to last cluster in the new chain
			currentCluster = chain[offsetInChain - 1];
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);
			if(lastOffset != offset) {
				buffer.clear();
				blockDevice.write(lastOffset, buffer);
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			buffer.putInt((int)offsetInBlock, FAT32_EOF_CLUSTER);
			buffer.clear();
			blockDevice.write(offset, buffer);
		} else {
			buffer.clear();
			blockDevice.write(lastOffset, buffer);
		}
		
		Log.i(TAG, "freed " + numberOfClusters + " clusters");
		
		return Arrays.copyOfRange(chain, 0, offsetInChain);
	}
}
