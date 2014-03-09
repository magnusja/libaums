package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.UsbFile;

public class FatFile implements UsbFile {
	
	private BlockDeviceDriver blockDevice;
	private FAT fat;
	private Fat32BootSector bootSector;
	
	private FatDirectory parent;
	private ClusterChain chain;
	private FatLfnDirectoryEntry entry;
	
	private FatFile(BlockDeviceDriver blockDevice, FAT fat,
			Fat32BootSector bootSector, FatLfnDirectoryEntry entry,
			FatDirectory parent) {
		this.blockDevice = blockDevice;
		this.fat = fat;
		this.bootSector = bootSector;
		this.entry = entry;
		this.parent = parent;
	}
	
	public static FatFile create(FatLfnDirectoryEntry entry, BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector, FatDirectory parent) throws IOException {
		FatFile result = new FatFile(blockDevice, fat, bootSector, entry, parent);
		return result;
	}
	
	private void initChain() throws IOException {
		if(chain == null) {
			chain = new ClusterChain(entry.getStartCluster(), blockDevice, fat, bootSector);
		}
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public String getName() {
		return entry.getName();
	}

	@Override
	public UsbFile getParent() {
		return parent;
	}

	@Override
	public String[] list() {
		throw new UnsupportedOperationException("This is a file!");
	}

	@Override
	public UsbFile[] listFiles() throws IOException {
		throw new UnsupportedOperationException("This is a file!");
	}
	
	@Override
	public long getLength() {
		return entry.getFileSize();
	}
	
	@Override
	public void setLength(long newLength) throws IOException {
		chain.setLength(newLength);
		entry.setFileSize(newLength);
	}

	@Override
	public void read(long offset, ByteBuffer destination) throws IOException {
		initChain();
		entry.setLastAccessedTimeToNow();
		chain.read(offset, destination);
	}

	@Override
	public void write(long offset, ByteBuffer source) throws IOException {
		initChain();
		long length = offset + source.remaining();
		if(length > getLength())
			setLength(length);
		entry.setLastModifiedTimeToNow();
		chain.write(offset, source);
	}

	@Override
	public void flush() throws IOException {
		// we only have to update the parent because we are always writing everything
		// immediately to the device
		// parent updates things like length and dates
		parent.write();
	}

	@Override
	public void close() throws IOException {
		flush();
	}

}
