package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

public class ClusterChain {
	
	private BlockDeviceDriver blockDevice;
	private FAT fat;
	private Long[] chain;
	private long clusterSize;
	private long dataAreaOffset;
	
	public ClusterChain(long startCluster, BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector) throws IOException {
		this.fat = fat;
		this.blockDevice = blockDevice;
		chain = fat.getChain(startCluster);
		clusterSize = bootSector.getBytesperCluster();
		dataAreaOffset = bootSector.getDataAreaOffset();
	}
	
	public void read(long offset, ByteBuffer dest) throws IOException {
		int len = dest.remaining();

        int chainIndex = (int) (offset / clusterSize);
        
        if (offset % clusterSize != 0) {
            int clusterOffset = (int) (offset % clusterSize);
            int size = Math.min(len, (int) (clusterSize - (offset % clusterSize)));
            dest.limit(dest.position() + size);

            blockDevice.read(getDeviceOffset(chain[chainIndex], clusterOffset), dest);
            
            len -= size;
            chainIndex++;
        }

        while (len > 0) {
            int size = (int) Math.min(clusterSize, len);
            dest.limit(dest.position() + size);

            blockDevice.read(getDeviceOffset(chain[chainIndex], 0), dest);

            len -= size;
            chainIndex++;
        }
	}
	
	private long getDeviceOffset(long cluster, int clusterOffset) {
		return dataAreaOffset + clusterOffset + (cluster - 2) * clusterSize;
	}
	
	public long getLength() {
		return chain.length * clusterSize;
	}
}
