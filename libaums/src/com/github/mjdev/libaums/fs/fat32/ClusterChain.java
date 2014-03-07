package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

public class ClusterChain {
	
	private static final String TAG = ClusterChain.class.getSimpleName();
	
	private BlockDeviceDriver blockDevice;
	private FAT fat;
	private Long[] chain;
	private long clusterSize;
	private long dataAreaOffset;
	
	public ClusterChain(long startCluster, BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector) throws IOException {
		this.fat = fat;
		this.blockDevice = blockDevice;
		chain = fat.getChain(startCluster);
		clusterSize = bootSector.getBytesPerCluster();
		dataAreaOffset = bootSector.getDataAreaOffset();
	}
	
	public void read(long offset, ByteBuffer dest) throws IOException {
		int len = dest.remaining();

        int chainIndex = (int) (offset / clusterSize);
        if(offset % clusterSize != 0) {
            int clusterOffset = (int) (offset % clusterSize);
            int size = Math.min(len, (int) (clusterSize - clusterOffset));
            dest.limit(dest.position() + size);

            blockDevice.read(getDeviceOffset(chain[chainIndex], clusterOffset), dest);

            chainIndex++;
            len -= size;
        }

        while(len > 0) {
            int size = (int) Math.min(clusterSize, len);
            dest.limit(dest.position() + size);

            blockDevice.read(getDeviceOffset(chain[chainIndex], 0), dest);

            chainIndex++;
            len -= size;
        }
	}
	
	public void write(long offset, ByteBuffer source) throws IOException {
		int len = source.remaining();

        int chainIndex = (int) (offset / clusterSize);
        if(offset % clusterSize != 0) {
            int clusterOffset = (int) (offset % clusterSize);
            int size = Math.min(len, (int) (clusterSize - clusterOffset));
            source.limit(source.position() + size);

            blockDevice.write(getDeviceOffset(chain[chainIndex], clusterOffset), source);

            chainIndex++;
            len -= size;
        }

        while(len > 0) {
            int size = (int) Math.min(clusterSize, len);
            source.limit(source.position() + size);

            blockDevice.write(getDeviceOffset(chain[chainIndex], 0), source);

            chainIndex++;
            len -= size;
        }
	}
	
	private long getDeviceOffset(long cluster, int clusterOffset) {
		return dataAreaOffset + clusterOffset + (cluster - 2) * clusterSize;
	}
	
	public void setClusters(int newNumberOfClusters) throws IOException {
		int oldNumberOfClusters = getClusters();
		if(newNumberOfClusters == oldNumberOfClusters) return;
		
		if(newNumberOfClusters > oldNumberOfClusters) {
			Log.d(TAG, "grow chain");
			chain = fat.alloc(chain, newNumberOfClusters - oldNumberOfClusters);
		} else {
			Log.d(TAG, "shrink chain");
			chain = fat.free(chain, oldNumberOfClusters - newNumberOfClusters);
		}
	}
	
	public int getClusters() {
		return chain.length;
	}
	
	public void setLength(long newLength) throws IOException {
		final long newNumberOfClusters = ((newLength + clusterSize - 1) / clusterSize);
		setClusters((int)newNumberOfClusters);
	}
	
	public long getLength() {
		return chain.length * clusterSize;
	}
}
