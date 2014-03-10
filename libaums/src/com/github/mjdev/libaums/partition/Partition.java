package com.github.mjdev.libaums.partition;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;

public class Partition implements BlockDeviceDriver {
	
	private static final String TAG = Partition.class.getSimpleName();
	
	//private PartitionTableEntry partitionTableEntry;
	private BlockDeviceDriver blockDevice;
	private int logicalBlockAddress;
	private int blockSize;
	private FileSystem fileSystem;
	
	private Partition() {
		
	}
	
	public static Partition createPartition(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
		Partition partition = null;
		
		// we currently only support fat32
		int partitionType = entry.getPartitionType();
		if(partitionType == 0x0b || partitionType == 0x0c) {
			partition = new Partition();
			//partition.partitionTableEntry = entry;
			partition.logicalBlockAddress = entry.getLogicalBlockAddress();
			partition.blockDevice = blockDevice;
			partition.blockSize = blockDevice.getBlockSize();
			partition.fileSystem = FileSystemFactory.createFileSystem(entry, partition);
		} else {
			Log.w(TAG, "unsupported partition type: " + entry.getPartitionType());
		}
		
		return partition;
	}
	
	public FileSystem getFileSystem() {
		return fileSystem;
	}
	
	public String getVolumeLabel() {
		return fileSystem.getVolumeLabel();
	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void read(long offset, ByteBuffer dest) throws IOException {
		long devOffset = offset / blockSize + logicalBlockAddress;
		// TODO try to make this more efficient by for example making tmp buffer global
		if(offset % blockSize != 0) {
			Log.w(TAG, "device offset not a multiple of block size");
			ByteBuffer tmp = ByteBuffer.allocate(blockSize);
			
			blockDevice.read(devOffset, tmp);
			tmp.clear();
			tmp.position((int) (offset % blockSize));
			dest.put(tmp);
			
			devOffset++;
		}
		
		if(dest.remaining() > 0)
			blockDevice.read(devOffset, dest);
	}

	@Override
	public void write(long offset, ByteBuffer src) throws IOException {
		long devOffset = offset / blockSize + logicalBlockAddress;
		// TODO try to make this more efficient by for example making tmp buffer global
		if(offset % blockSize != 0) {
			Log.w(TAG, "device offset not a multiple of block size");
			ByteBuffer tmp = ByteBuffer.allocate(blockSize);
			
			blockDevice.read(devOffset, tmp);
			tmp.clear();
			tmp.position((int) (offset % blockSize));
			int remaining = Math.min(tmp.remaining(), src.remaining());
			tmp.put(src.array(), src.position(), remaining);
			src.position(src.position() + remaining);
			tmp.clear();
			blockDevice.write(devOffset, tmp);
			
			devOffset++;
		}
		
		if(src.remaining() > 0)
			blockDevice.write(devOffset, src);
	}

	@Override
	public int getBlockSize() {
		return blockDevice.getBlockSize();
	}
}
