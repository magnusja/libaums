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
	public void read(long devOffset, ByteBuffer dest) throws IOException {
		if(devOffset % blockSize != 0) {
			Log.e(TAG, "device offset not a multiple of block size");
		}
		blockDevice.read(devOffset / blockSize + logicalBlockAddress, dest);
	}

	@Override
	public void write(long devOffset, ByteBuffer src) throws IOException {
		if(devOffset % blockSize != 0) {
			Log.e(TAG, "device offset not a multiple of block size");
		}
		blockDevice.write(devOffset / blockSize + logicalBlockAddress, src);
	}

	@Override
	public int getBlockSize() {
		return blockDevice.getBlockSize();
	}
}
