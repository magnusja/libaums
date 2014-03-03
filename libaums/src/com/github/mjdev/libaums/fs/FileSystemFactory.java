package com.github.mjdev.libaums.fs;

import java.io.IOException;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystem;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

public class FileSystemFactory {
	public static FileSystem createFileSystem(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
		// we currentšy only support FAT32
		return Fat32FileSystem.read(blockDevice);
	}
}
