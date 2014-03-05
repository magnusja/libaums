package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

public class Fat32FileSystem implements FileSystem {
		
	private Fat32BootSector bootSector;
	private FAT fat;
	private FsInfoStructure fsInfoStructure;
	private FatDirectory rootDirectory;
	
	private Fat32FileSystem(BlockDeviceDriver blockDevice) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		blockDevice.read(0, buffer);
		bootSector = Fat32BootSector.read(buffer);
		fsInfoStructure = FsInfoStructure.read(blockDevice, bootSector.getFsInfoStartSector() * bootSector.getBytesPerSector());
		fat = new FAT(blockDevice, bootSector, fsInfoStructure);
		rootDirectory = FatDirectory.readRoot(blockDevice, fat, bootSector);
	}
	
	public static Fat32FileSystem read(BlockDeviceDriver blockDevice) throws IOException {
		return new Fat32FileSystem(blockDevice);
	}

	@Override
	public UsbFile getRootDirectory() {
		return rootDirectory;
	}

	@Override
	public String getVolumeLabel() {
		String volumeLabel = rootDirectory.getVolumeLabel();
		if(volumeLabel == null) {
			volumeLabel = bootSector.getVolumeLabel();
		}
		return volumeLabel;
	}
}
